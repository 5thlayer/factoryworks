package com.factoryworks.core.chest;

import com.factoryworks.core.PFBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PackChestBlockEntity extends ChestBlockEntity {
    private final ChestTier tier;

    public PackChestBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.CHEST.get(), pos, state);
        this.tier = ((PackChestBlock) state.getBlock()).tier();
        // The superclass sizes its list at 27 before this runs.
        setItems(NonNullList.withSize(tier.slots(), ItemStack.EMPTY));
    }

    @Override
    public int getContainerSize() {
        return tier.slots();
    }

    // The default is vanilla's "Chest".
    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    // ChestMenu has no four-row factory that takes a container.
    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        MenuType<ChestMenu> type = tier.rows() == 4 ? MenuType.GENERIC_9x4 : MenuType.GENERIC_9x6;
        return new ChestMenu(type, id, inventory, this, tier.rows());
    }
}
