package com.factoryworks.core.wreck;

import com.factoryworks.core.PFBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CargoHoldBlockEntity extends BaseContainerBlockEntity {

    private NonNullList<ItemStack> items =
            NonNullList.withSize(CargoHoldCorpus.get().slots(), ItemStack.EMPTY);

    public CargoHoldBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.CARGO_HOLD.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    // The hopper's menu is five slots (ADR-0107); a corpus of another size fails its size check here.
    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new HopperMenu(id, inventory, this);
    }

    /** Fills slots in order, merging into matching stacks first; returns what did not fit. */
    public ItemStack put(ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int slot = 0; slot < items.size() && !rest.isEmpty(); slot++) {
            ItemStack held = items.get(slot);
            if (!held.isEmpty() && ItemStack.isSameItemSameComponents(held, rest)) {
                int moved = Math.min(rest.getCount(), getMaxStackSize(held) - held.getCount());
                if (moved > 0) {
                    held.grow(moved);
                    rest.shrink(moved);
                }
            }
        }
        for (int slot = 0; slot < items.size() && !rest.isEmpty(); slot++) {
            if (items.get(slot).isEmpty()) {
                int moved = Math.min(rest.getCount(), getMaxStackSize(rest));
                items.set(slot, rest.split(moved));
            }
        }
        setChanged();
        return rest;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }
}
