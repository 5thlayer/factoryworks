package com.factoryworks.core.wreck;

import java.util.Arrays;

import javax.annotation.Nullable;

import com.factoryworks.core.PFBlocks;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * One block of the wreck's cargo hold (ADR-0107, #548): unbreakable, and ten of them make the hold.
 * Only the block flagged {@link #ANCHOR} has a block entity; every other block finds it in the
 * world, because the template is rotated per world and a stored offset would point wrong.
 */
public class CargoHoldBlock extends BaseEntityBlock {

    public static final MapCodec<CargoHoldBlock> CODEC = simpleCodec(CargoHoldBlock::new);

    public static final BooleanProperty ANCHOR = BooleanProperty.create("anchor");

    public CargoHoldBlock(BlockBehaviour.Properties props) {
        super(WreckBlocks.indestructible(props).noLootTable());
        registerDefaultState(stateDefinition.any().setValue(ANCHOR, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ANCHOR);
    }

    /** The hold's inventory as seen from any of its blocks; null when the hold has no single anchor. */
    @Nullable
    public static CargoHoldBlockEntity anchorOf(LevelReader level, BlockPos pos) {
        return HoldAnchor.resolve(pos.immutable(),
                        at -> Arrays.stream(Direction.values()).map(at::relative).toList(),
                        at -> level.getBlockState(at).is(PFBlocks.CARGO_HOLD.get()),
                        at -> level.getBlockState(at).getValue(ANCHOR))
                .map(level::getBlockEntity)
                .filter(CargoHoldBlockEntity.class::isInstance)
                .map(CargoHoldBlockEntity.class::cast)
                .orElse(null);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(ANCHOR) ? new CargoHoldBlockEntity(pos, state) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        CargoHoldBlockEntity hold = anchorOf(level, pos);
        if (hold != null) {
            player.openMenu(hold);
        }
        return InteractionResult.CONSUME;
    }
}
