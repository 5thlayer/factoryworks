package com.factoryworks.core.fluid;

import javax.annotation.Nullable;

import com.factoryworks.core.PFBlockEntities;
import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.machine.footprint.FootprintTurn;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Terra's Boiler (#224, ADR-0048): the block that turns fuel and water into steam.
 *
 * <p>The shell. The rate is {@link BoilerSpec}'s, the stall is {@link BoilerCycle}'s and the tanks
 * are {@link BoilerBlockEntity}'s; none of the first two touches Minecraft, which is what lets both
 * be asserted without a world.
 *
 * <p><b>One block, one tier.</b> Factorio has one boiler and so does this pack: ADR-0033 has the
 * reactor emitting superheated steam directly with no heat layer, so there is no second rung here
 * for a ladder to climb.
 *
 * <p>The anchor of a 3x2 footprint (ADR-0114). Fluid and fuel both reach the machine on every face --
 * Factorio decides in-or-out by the inserter rather than by the machine -- and the facing exists so
 * the player can see which side the firebox is on.
 */
public class BoilerBlock extends BaseEntityBlock implements FootprintTurn {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    /**
     * A block codec is only read by data generation, which this pack does not run -- present
     * because {@link BaseEntityBlock} makes it abstract, the same reason {@code OffshorePumpBlock}
     * carries one.
     */
    public static final com.mojang.serialization.MapCodec<BoilerBlock> CODEC =
            simpleCodec(BoilerBlock::new);

    public BoilerBlock(BlockBehaviour.Properties props) {
        super(props
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BoilerBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, PFBlockEntities.BOILER.get(),
                (tickLevel, pos, tickState, entity) -> entity.serverTick());
    }

    /** Plain right-click opens the Boiler, because it has a fuel slot -- the rig's own rule. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof BoilerBlockEntity boiler) {
            player.openMenu(boiler);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * The anchor going takes its parts with it. The fuel it still holds is dropped by
     * {@code BlockEntity.preRemoveSideEffects}, whichever part the player broke (#592).
     */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
            boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        PFBlocks.BOILER_FOOTPRINT.teardown(level, pos, state.getValue(FACING), pos);
    }
}
