package com.planetaryfactory.core.energy;

import com.planetaryfactory.core.PFBlockEntities;
import com.planetaryfactory.core.PlanetaryFactoryCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * A pole: a position in an Electric Network and a supply area whose contents it keeps sorted.
 *
 * <h2>The pole moves no energy itself</h2>
 *
 * <p>ADR-0062 makes the network the carrier. A pole reports itself to {@link ElectricNetworks}
 * every tick and rescans its area now and then; the network, once per level tick, settles every
 * pole it links in one set of books. A pole holds no buffer and has no energy face: nothing feeds
 * it, because generators are pulled from where they stand.
 *
 * <h2>Roles are decided by tag</h2>
 *
 * <p>Anything in the area answering {@link Capabilities.Energy#BLOCK} is a consumer unless its
 * block is tagged {@link #GENERATORS} or {@link #ACCUMULATORS}. The face alone cannot decide it: a
 * machine whose face allows extraction would otherwise be drained as a generator.
 *
 * <p>The tag read is the energy owner's, not the block's: {@link SupplyScan} resolves a hull block to
 * its controller and a slave engine to its master first, and keeps each owner once (#292).
 *
 * <h2>Why the lists are cached</h2>
 *
 * <p>A substation's area is 18x18x5 = 1620 blocks. A capability lookup per block per tick is not
 * affordable, and it is also pointless: machines do not appear and vanish every tick. The area is
 * rescanned on {@link #RESCAN_INTERVAL}, so a newly placed machine waits at most two seconds.
 */
public class SupplyAreaPoleBlockEntity extends BlockEntity {

    /** Blocks a network draws FE from ahead of accumulators (ADR-0062). */
    public static final TagKey<Block> GENERATORS = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "generators"));

    /** Blocks a network discharges to cover a shortfall and charges from generator surplus. */
    public static final TagKey<Block> ACCUMULATORS = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "accumulators"));

    /** Ticks between rescans of the supply area. Two seconds. */
    private static final int RESCAN_INTERVAL = 40;

    private List<BlockPos> consumers = List.of();
    private List<BlockPos> generators = List.of();
    private List<BlockPos> accumulators = List.of();
    private int sinceRescan = RESCAN_INTERVAL;

    /**
     * What the network did on the last tick, for the Jade line and for nothing else. Not persisted
     * and not synced: Jade asks the server when a player looks.
     */
    private NetworkReading lastReading = NetworkReading.NONE;

    public SupplyAreaPoleBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.SUPPLY_AREA_POLE.get(), pos, state);
    }

    public PoleTier tier() {
        // The block entity type is registered against the pole blocks and nothing else, so this
        // cannot fail. Saying so loudly beats defaulting to SMALL.
        if (getBlockState().getBlock() instanceof SupplyAreaPoleBlock pole) {
            return pole.tier();
        }
        throw new IllegalStateException(
                "supply-area pole block entity on " + getBlockState().getBlock()
                        + " at " + getBlockPos() + ", which is not a pole");
    }

    /** This pole as {@link PoleLinks} sees it: where it stands and how far it reaches. */
    public PoleLinks.Pole shape() {
        BlockPos p = getBlockPos();
        return new PoleLinks.Pole(p.getX(), p.getY(), p.getZ(), tier());
    }

    /**
     * Whether this pole is an unlimited generator: the creative pole (#272). Read off the
     * blockstate, because a chunk load rebuilds a block entity from the type and never asks the
     * block which of the poles it is.
     */
    public boolean isCreative() {
        return getBlockState().getBlock() instanceof CreativeSupplyAreaPoleBlock;
    }

    void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }
        if (++sinceRescan >= RESCAN_INTERVAL) {
            sinceRescan = 0;
            scan(level);
        }
        ElectricNetworks.of(level).report(this);
    }

    /** How many consumers answered this pole's last scan. Jade reads this; nothing else does. */
    public int machineCount() {
        return consumers.size();
    }

    /** FE the pole's network handed to consumers on the last tick. */
    public long deliveredFePerTick() {
        return lastReading.delivered();
    }

    /** FE the pole's network was asked for on the last tick, whether or not it was there. */
    public long demandedFePerTick() {
        return lastReading.demanded();
    }

    /** The pole's network as it stood after the last tick (#285). Jade reads this. */
    public NetworkReading networkReading() {
        return lastReading;
    }

    List<BlockPos> consumers() {
        return consumers;
    }

    List<BlockPos> generators() {
        return generators;
    }

    List<BlockPos> accumulators() {
        return accumulators;
    }

    void recordNetworkTick(NetworkReading reading) {
        lastReading = reading;
    }

    private void scan(Level level) {
        // The scan is SupplyAreaScan's, shared with the Supply Area Box's outlines (#158) so the
        // overlay cannot disagree with the network about what this pole reaches.
        SupplyScan.Roles<BlockPos> roles = SupplyAreaScan.of(level, getBlockPos(), tier());
        consumers = roles.consumers();
        generators = roles.generators();
        accumulators = roles.accumulators();
    }

    static EnergyHandler handler(Level level, BlockPos pos) {
        // A pole supplies wirelessly, so it has no natural side to ask through. Most blocks answer
        // on a null context; the faces are a fallback for anything that insists on one.
        EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, pos, null);
        if (handler != null) {
            return handler;
        }
        for (Direction side : Direction.values()) {
            handler = level.getCapability(Capabilities.Energy.BLOCK, pos, side);
            if (handler != null) {
                return handler;
            }
        }
        return null;
    }
}
