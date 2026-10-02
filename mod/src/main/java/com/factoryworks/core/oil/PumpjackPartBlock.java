package com.factoryworks.core.oil;

import java.util.function.Supplier;

import com.factoryworks.core.machine.footprint.FootprintMachine;
import com.factoryworks.core.machine.footprint.FootprintPartBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Pumpjack part, which is a Pipeworks port of its own so that a pipe at any face of the footprint
 * joins the segment the anchor fills (ADR-0110).
 */
public class PumpjackPartBlock extends FootprintPartBlock implements EntityBlock {

    public PumpjackPartBlock(Properties properties, Supplier<FootprintMachine> machine) {
        super(properties, machine);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PumpjackPartBlockEntity(pos, state);
    }

    // Leave before the super's teardown guard, or a part its own machine tears down stays in the segment (#557).
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                               boolean movedByPiston) {
        PumpjackPortBlockEntity.leave(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
