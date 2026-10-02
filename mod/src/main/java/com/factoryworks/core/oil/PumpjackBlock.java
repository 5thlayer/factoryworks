package com.factoryworks.core.oil;

import com.mojang.serialization.MapCodec;
import com.factoryworks.core.PFBlockEntities;
import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.machine.footprint.FootprintTurn;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jspecify.annotations.Nullable;

/** The Pumpjack's anchor (ADR-0081): the footprint block over the well, holding {@link PumpjackBlockEntity}. */
public class PumpjackBlock extends HorizontalDirectionalBlock implements EntityBlock, FootprintTurn {

    private static final MapCodec<PumpjackBlock> CODEC = simpleCodec(PumpjackBlock::new);

    public PumpjackBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Oritech's Pump model is drawn from the block entity. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PumpjackBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                          BlockEntityType<T> type) {
        if (level.isClientSide() || type != PFBlockEntities.PUMPJACK.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> ((PumpjackBlockEntity) entity).serverTick();
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                               boolean movedByPiston) {
        PumpjackPortBlockEntity.leave(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        PFBlocks.PUMPJACK_FOOTPRINT.teardown(level, pos, state.getValue(FACING), pos);
    }
}
