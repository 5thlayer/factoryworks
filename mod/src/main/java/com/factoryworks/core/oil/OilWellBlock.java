package com.factoryworks.core.oil;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** An oil well (ADR-0081): flush with the terrain, holding an amount of crude, and never broken. */
public class OilWellBlock extends BaseEntityBlock {

    private static final MapCodec<OilWellBlock> CODEC = simpleCodec(OilWellBlock::new);

    public OilWellBlock(BlockBehaviour.Properties props) {
        super(props
                .mapColor(MapColor.COLOR_BLACK)
                .strength(-1.0F, 3_600_000.0F)
                .noLootTable()
                .pushReaction(PushReaction.BLOCK)
                .sound(SoundType.STONE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OilWellBlockEntity(pos, state);
    }
}
