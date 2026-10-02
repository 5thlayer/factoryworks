package com.factoryworks.core.dismantle;

import io.github._5thlayer.pipeworks.block.FluidPipeBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Two Pipeworks pipes are joined where their segment links each other, which is what their arms draw (ADR-0086). */
final class PipeworksPipeJoin implements JoinRule {

    @Override
    public boolean joined(Level level, BlockPos a, BlockPos b) {
        for (Direction side : Direction.values()) {
            if (a.relative(side).equals(b)) {
                return FluidPipeBlock.isLinked(level.getBlockState(a), side)
                        && FluidPipeBlock.isLinked(level.getBlockState(b), side.getOpposite());
            }
        }
        return false;
    }
}
