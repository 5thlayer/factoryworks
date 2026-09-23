package com.planetaryfactory.core.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block entity whose energy belongs to another block's (#292, ADR-0062).
 *
 * <p>Implemented by mixin on foreign block entities -- Oritech's machine core and Steam Engine --
 * so the pole can ask the question without importing the mod that answers it.
 * {@link SupplyScan} follows the answer until it stops, so a hull block of a slave engine reaches
 * the row's master in two hops.
 */
public interface EnergyOwner {

    /** The block whose energy this one's face stands for, or {@code null} if it is its own. */
    BlockPos planetaryfactory$energyOwner();

    /** A block entity's owner, or failing that a hull block's (#328), or {@code null}. */
    static BlockPos of(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof EnergyOwner owned) {
            return owned.planetaryfactory$energyOwner();
        }
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof EnergyOwnerBlock hull ? hull.energyOwner(pos, state) : null;
    }
}
