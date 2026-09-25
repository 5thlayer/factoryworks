package com.planetaryfactory.core.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that answers for itself what Rotate and Reverse Rotate do to it once placed (#405,
 * ADR-0087). A block without it takes vanilla's {@code rotate}.
 */
public interface TurnsInPlace {

    PlacedTurn.Verdict<BlockState> turnInPlace(BlockState state, Level level, BlockPos pos, boolean reverse);
}
