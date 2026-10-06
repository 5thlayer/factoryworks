package com.factoryworks.core.dismantle;

import io.github._5thlayer.pipeworks.block.FluidPipeBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Two Pipeworks pipes are joined where each draws an arm toward the other (ADR-0086). Between two pipes an arm is
 * a link, as a fluid inventory is no pipe; Pipeworks has no query for whether two existing nodes are linked.
 */
final class PipeworksPipeJoin implements JoinRule {

    @Override
    public boolean joined(Level level, BlockPos a, BlockPos b) {
        for (Direction side : Direction.values()) {
            if (a.relative(side).equals(b)) {
                return FluidPipeBlock.drawsArm(level.getBlockState(a), side)
                        && FluidPipeBlock.drawsArm(level.getBlockState(b), side.getOpposite());
            }
        }
        return false;
    }
}
