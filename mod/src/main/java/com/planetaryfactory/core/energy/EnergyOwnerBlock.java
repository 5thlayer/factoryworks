package com.planetaryfactory.core.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block with no block entity whose energy face is another block's (#328): the pack's own hull
 * blocks, which {@link EnergyOwner} cannot reach because that is asked of a block entity.
 *
 * <p>The Assembling Machine's three hull blocks answer the anchor's face. Unresolved, a pole counts
 * one machine once per block it reaches and offers and draws it that many times.
 */
public interface EnergyOwnerBlock {

    /** The block whose energy the one at {@code pos} stands for. */
    BlockPos energyOwner(BlockPos pos, BlockState state);
}
