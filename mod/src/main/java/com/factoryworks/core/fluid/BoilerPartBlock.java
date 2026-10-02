package com.factoryworks.core.fluid;

import java.util.function.Supplier;

import com.factoryworks.core.machine.footprint.FootprintMachine;
import com.factoryworks.core.machine.footprint.FootprintPartBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A Boiler part, drawn as an iron block until the 3x2 model replaces it (#595). The three that are
 * ports hold a block entity; the back corners hold none (ADR-0114).
 */
public class BoilerPartBlock extends FootprintPartBlock implements EntityBlock {

    public BoilerPartBlock(Properties properties, Supplier<FootprintMachine> machine) {
        super(properties, machine);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BoilerPartBlockEntity.portOf(state) == null ? null : new BoilerPartBlockEntity(pos, state);
    }

    // Leave before the super's teardown guard, or a part its own machine tears down stays in the segment (#557).
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                               boolean movedByPiston) {
        BoilerPortBlockEntity.leave(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
