package com.planetaryfactory.core.smelting;

import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class FurnaceItemHandler extends DelegatingResourceHandler<ItemResource> {
    private final FurnaceBlockEntity blockEntity;

    public FurnaceItemHandler(FurnaceBlockEntity blockEntity) {
        super(new VanillaContainerWrapper(blockEntity));
        this.blockEntity = blockEntity;
    }

    public FurnaceBlockEntity furnace() {
        return blockEntity;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (!FurnaceSlots.canExtract(convertIndex(index))) {
            return 0;
        }
        return super.extract(index, resource, amount, transaction);
    }
}
