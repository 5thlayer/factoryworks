package com.planetaryfactory.core.energy;

import com.mojang.serialization.Codec;
import com.planetaryfactory.core.PlanetaryFactoryCore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

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
            changed(level);
        }
        return click;
    }

    /**
     * A pole's base was placed at {@code pos}: it wires itself as {@link PoleWiring#onPlace} says.
     * An extension added on top of a column changes no wire, since a wire names a column's base.
     */
    public void placed(ServerLevel level, BlockPos pos) {
        PoleLinks.Pole placed = poleAt(level, pos);
        if (placed == null || !PoleColumn.isBase(level, pos)) {
            return;
        }
        List<PoleLinks.Pole> wired = PoleWiring.onPlace(placed, standingNear(level, pos, placed.tier()), wires);
        for (PoleLinks.Pole other : wired) {
            wires.add(pos(pos), new PoleLinks.Pos(other.x(), other.y(), other.z()));
        }
        changed(level);
    }

    /** The base of a pole column at {@code pos} was broken: its wires go with it. */
    public void broken(ServerLevel level, BlockPos pos) {
        wires.removeAllOf(pos(pos));
        changed(level);
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

    private void changed(ServerLevel level) {
        setDirty();
        ElectricNetworks.of(level).wiresChanged();
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
