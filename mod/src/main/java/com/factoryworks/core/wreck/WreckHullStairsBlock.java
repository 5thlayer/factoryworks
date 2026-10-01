package com.factoryworks.core.wreck;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** The bevel of the wreck's hull (ADR-0107): unbreakable, and shaped by the template alone. */
public class WreckHullStairsBlock extends StairBlock {

    public WreckHullStairsBlock(BlockState base, BlockBehaviour.Properties props) {
        super(base, WreckBlocks.indestructible(props).noLootTable());
    }

    /**
     * The nose's corners are diagonal to each other, which vanilla's stair rule reads as no corner
     * at all; recomputed on a neighbour change, they would turn straight under the player's hand
     * (#549).
     */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos,
            BlockState neighbourState, RandomSource random) {
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos,
                neighbourState, random).setValue(SHAPE, state.getValue(SHAPE));
    }
}
