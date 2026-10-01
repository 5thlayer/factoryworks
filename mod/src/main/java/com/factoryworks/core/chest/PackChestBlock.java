package com.factoryworks.core.chest;

import com.factoryworks.core.PFBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** A vanilla chest sized by its tier (#540); pairing is off for every chest by mixin (ADR-0106). */
public class PackChestBlock extends ChestBlock {
    private final ChestTier tier;

    public PackChestBlock(ChestTier tier, BlockBehaviour.Properties properties) {
        super(PFBlockEntities.CHEST::get, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE,
                properties.strength(tier.strength()).requiresCorrectToolForDrops());
        this.tier = tier;
    }

    public ChestTier tier() {
        return tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PackChestBlockEntity(pos, state);
    }
}
