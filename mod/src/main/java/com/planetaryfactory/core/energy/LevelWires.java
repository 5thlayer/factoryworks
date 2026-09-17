package com.planetaryfactory.core.energy;

import com.mojang.serialization.Codec;
import com.planetaryfactory.core.PlanetaryFactoryCore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import com.planetaryfactory.core.network.PoleWiresPacket;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * A level's wires, saved with it (ADR-0068). The rules are {@link PoleWiring}'s and the storage
 * {@link WireSet}'s; this is where the two meet a world.
 */
public final class LevelWires extends SavedData {

    private static final Codec<LevelWires> CODEC = WireSet.CODEC
            .xmap(LevelWires::new, data -> data.wires)
            .fieldOf("wires")
            .codec();

    public static final SavedDataType<LevelWires> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "pole_wires"),
            LevelWires::new, CODEC);

    private final WireSet wires;

    public LevelWires() {
        this(new WireSet());
    }

    private LevelWires(WireSet wires) {
        this.wires = wires;
    }

    public static LevelWires of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public WireSet wires() {
        return wires;
    }

    public boolean contains(BlockPos a, BlockPos b) {
        return wires.contains(pos(a), pos(b));
    }

    /** The Pick's second click, from the pole at {@code anchor} to the pole at {@code target}. */
    public PoleWiring.Click click(ServerLevel level, BlockPos anchor, BlockPos target) {
        PoleLinks.Pole a = poleAt(level, anchor);
        PoleLinks.Pole b = poleAt(level, target);
        if (a == null || b == null) {
            return PoleWiring.Click.REFUSED;
        }
        PoleWiring.Click click = PoleWiring.click(a, b, wires);
        if (click == PoleWiring.Click.WIRED || click == PoleWiring.Click.CUT) {
            changed(level, anchor, target);
        }
        return click;
    }

    /**
     * A pole's base was placed at {@code pos}: it wires itself as {@link PoleWiring#onPlace} says.
     * An extension added on top of a column changes no wire, since a wire names a column's base.
     */
    public void placed(ServerLevel level, BlockPos pos) {
        PoleLinks.Pole placed = poleAt(level, pos);
        if (placed == null) {
            return;
        }
        // A pole placed under a standing column of the same tier is that column growing downwards,
        // not a new pole (#309): its wires move to the new base and it adds none of its own. Where
        // the placement also joins a column below, the new base is that lower column's.
        BlockPos above = pos.above();
        if (level.getBlockState(above).is(level.getBlockState(pos).getBlock())) {
            BlockPos base = PoleColumn.baseOf(level, pos);
            rekeyed(level, pos(above), pos(base));
            return;
        }
        if (!PoleColumn.isBase(level, pos)) {
            return;
        }
        List<PoleLinks.Pole> wired = PoleWiring.onPlace(placed, standingNear(level, pos, placed.tier()), wires);
        List<BlockPos> touched = new ArrayList<>();
        touched.add(pos);
        for (PoleLinks.Pole other : wired) {
            wires.add(pos(pos), new PoleLinks.Pos(other.x(), other.y(), other.z()));
            touched.add(new BlockPos(other.x(), other.y(), other.z()));
        }
        changed(level, touched.toArray(BlockPos[]::new));
    }

    /** Moves a column's wires to its new base, telling every end's chunk about it. */
    private void rekeyed(ServerLevel level, PoleLinks.Pos from, PoleLinks.Pos to) {
        List<BlockPos> touched = new ArrayList<>();
        touched.add(block(from));
        touched.add(block(to));
        for (PoleLinks.Wire wire : wires.all()) {
            if (wire.a().equals(from)) {
                touched.add(block(wire.b()));
            } else if (wire.b().equals(from)) {
                touched.add(block(wire.a()));
            }
        }
        wires.rekey(from, to);
        changed(level, touched.toArray(BlockPos[]::new));
    }

    /** The base of a pole column at {@code pos} was broken: its wires go with it. */
    public void broken(ServerLevel level, BlockPos pos) {
        List<BlockPos> touched = new ArrayList<>();
        touched.add(pos);
        for (PoleLinks.Wire wire : wires.all()) {
            if (wire.a().equals(pos(pos))) {
                touched.add(block(wire.b()));
            } else if (wire.b().equals(pos(pos))) {
                touched.add(block(wire.a()));
            }
        }
        wires.removeAllOf(pos(pos));
        changed(level, touched.toArray(BlockPos[]::new));
    }

    /**
     * Every pole base within the placed pole's reach, found through the loaded chunks' block
     * entities rather than by walking blocks. A wire's reach is the shorter of its two ends', so the
     * placed pole's own reach bounds the search.
     */
    private static List<PoleLinks.Pole> standingNear(ServerLevel level, BlockPos pos, PoleTier tier) {
        int reach = (int) Math.ceil(tier.wireReach());
        List<PoleLinks.Pole> found = new ArrayList<>();
        for (int cx = SectionPos.blockToSectionCoord(pos.getX() - reach);
             cx <= SectionPos.blockToSectionCoord(pos.getX() + reach); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(pos.getZ() - reach);
                 cz <= SectionPos.blockToSectionCoord(pos.getZ() + reach); cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos at = be.getBlockPos();
                    if (be instanceof SupplyAreaPoleBlockEntity pole && !at.equals(pos)
                            && PoleColumn.isBase(level, at)) {
                        found.add(pole.shape());
                    }
                }
            }
        }
        return found;
    }

    /** Save, rebuild the networks, and resend each touched chunk's wires to whoever watches it. */
    private void changed(ServerLevel level, BlockPos... touched) {
        setDirty();
        ElectricNetworks.of(level).wiresChanged();
        java.util.Set<ChunkPos> chunks = new java.util.LinkedHashSet<>();
        for (BlockPos p : touched) {
            chunks.add(ChunkPos.containing(p));
        }
        for (ChunkPos chunk : chunks) {
            PacketDistributor.sendToPlayersTrackingChunk(level, chunk,
                    new PoleWiresPacket(chunk.x(), chunk.z(), wires.touching(chunk.x(), chunk.z())));
        }
    }

    /** A chunk reached a player: send the wires with an end in it. */
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        ChunkPos chunk = event.getPos();
        List<PoleLinks.Wire> touching = of(event.getLevel()).wires.touching(chunk.x(), chunk.z());
        PacketDistributor.sendToPlayer(event.getPlayer(), new PoleWiresPacket(chunk.x(), chunk.z(), touching));
    }

    private static BlockPos block(PoleLinks.Pos p) {
        return new BlockPos(p.x(), p.y(), p.z());
    }

    static PoleLinks.Pos pos(BlockPos p) {
        return new PoleLinks.Pos(p.getX(), p.getY(), p.getZ());
    }

    /** The pole whose column holds {@code pos}, named by its base, or null if there is none. */
    private static PoleLinks.Pole poleAt(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof SupplyAreaPoleBlock pole)) {
            return null;
        }
        BlockPos base = PoleColumn.baseOf(level, pos);
        return new PoleLinks.Pole(base.getX(), base.getY(), base.getZ(), pole.tier());
    }
}
