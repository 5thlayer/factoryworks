package com.planetaryfactory.core.dismantle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.block.blocks.pipes.AbstractPipeBlock;

/** Two Oritech pipes are joined where each is open towards the other, as Oritech's network is (ADR-0086). */
final class OritechPipeJoin implements JoinRule {

    @Override
    public boolean joined(Level level, BlockPos a, BlockPos b) {
        for (Direction side : Direction.values()) {
            if (a.relative(side).equals(b)) {
                return open(level, a, side) && open(level, b, side.getOpposite());
            }
        }
        return false;
    }

    private static boolean open(Level level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof AbstractPipeBlock pipe && pipe.isConnectingInDirection(state, side, pos, level, false);
    }
}
