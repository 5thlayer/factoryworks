package com.planetaryfactory.core.radar;

import com.mojang.serialization.MapCodec;
import com.planetaryfactory.core.PFBlockEntities;
import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.machine.footprint.FootprintTurn;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jspecify.annotations.Nullable;

/** The Radar's anchor (#368): the footprint block that holds {@link RadarBlockEntity}. */
public class RadarBlock extends HorizontalDirectionalBlock implements EntityBlock, FootprintTurn {

    private static final MapCodec<RadarBlock> CODEC = simpleCodec(RadarBlock::new);

    public RadarBlock(Properties properties) {
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

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                          BlockEntityType<T> type) {
        if (level.isClientSide() || type != PFBlockEntities.RADAR.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> ((RadarBlockEntity) entity).serverTick();
    }

    /** The anchor going takes its parts with it; its own item comes from its loot table. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                               boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        PFBlocks.RADAR_FOOTPRINT.teardown(level, pos, state.getValue(FACING), pos);
    }
}
