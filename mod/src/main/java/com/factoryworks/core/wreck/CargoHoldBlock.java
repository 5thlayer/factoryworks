package com.factoryworks.core.wreck;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The wreck's cargo hold (ADR-0107): an unbreakable block holding the Hold's items. */
public class CargoHoldBlock extends BaseEntityBlock {

    public static final MapCodec<CargoHoldBlock> CODEC = simpleCodec(CargoHoldBlock::new);

    public CargoHoldBlock(BlockBehaviour.Properties props) {
        super(WreckBlocks.indestructible(props).noLootTable());
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
        return new CargoHoldBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof CargoHoldBlockEntity hold) {
            player.openMenu(hold);
        }
        return InteractionResult.CONSUME;
    }
}
