package com.planetaryfactory.core.energy;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a pole standing here reaches: every block in its supply area, sorted into roles by the energy
 * owner it answers to (#292, ADR-0062).
 *
 * <p>This is the scan the pole runs to build its network, and it is <b>also</b> what the Supply Area
 * Box outlines (#158). Extracted so there is one of it rather than two: a client that re-derived
 * "which machines are in here" would drift from the server's answer, and the ways it would drift are
 * not obvious. A slave Steam Engine resolves to its master, which may stand outside the area
 * entirely; a machine's hull block answers its controller's face and is never a consumer in its own
 * right. A naive "does this block have an Energy capability" sweep gets both wrong and lights up
 * blocks the network never feeds -- an overlay lying more precisely than before.
 *
 * <p>The rule itself is {@link SupplyScan#classify}, which is Minecraft-free and unit-tested in
 * {@code SupplyScanTest}. This class is only the part that needs a {@link Level}: gathering the
 * positions and asking each one for its owner and its role.
 */
public final class SupplyAreaScan {

    private SupplyAreaScan() {
    }

    /**
     * Every owner a pole of this tier based at {@code origin} would reach, by role.
     *
     * <p>The pole's own block is skipped -- it exposes no face to itself (ADR-0062) -- and an
     * unloaded position contributes nothing rather than being guessed at.
     */
    public static SupplyScan.Roles<BlockPos> of(Level level, BlockPos origin, PoleTier tier) {
        List<BlockPos> positions = new ArrayList<>();
        SupplyArea.forEachOffset(tier, (dx, dy, dz) -> {
            BlockPos pos = origin.offset(dx, dy, dz);
            if (!pos.equals(origin) && level.isLoaded(pos)) {
                positions.add(pos.immutable());
            }
        });
        // Every block stands for its energy owner (#292): a machine's hull for its controller, a
        // slave engine for its master. The owner may lie outside this area; it is still the one the
        // network draws, and it is kept once however many of its blocks are in here.
        return SupplyScan.classify(positions,
                pos -> owner(level, pos),
                pos -> role(level, origin, pos));
    }

    /** A block entity's owner, or failing that a hull block's (#328), or {@code null}. */
    private static BlockPos owner(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        if (level.getBlockEntity(pos) instanceof EnergyOwner owned) {
            return owned.planetaryfactory$energyOwner();
        }
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof EnergyOwnerBlock hull ? hull.energyOwner(pos, state) : null;
    }

    private static SupplyScan.Role role(Level level, BlockPos origin, BlockPos pos) {
        if (pos.equals(origin) || !level.isLoaded(pos)
                || SupplyAreaPoleBlockEntity.handler(level, pos) == null) {
            return SupplyScan.Role.NONE;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(SupplyAreaPoleBlockEntity.GENERATORS)) {
            return SupplyScan.Role.GENERATOR;
        }
        return state.is(SupplyAreaPoleBlockEntity.ACCUMULATORS)
                ? SupplyScan.Role.ACCUMULATOR
                : SupplyScan.Role.CONSUMER;
    }
}
