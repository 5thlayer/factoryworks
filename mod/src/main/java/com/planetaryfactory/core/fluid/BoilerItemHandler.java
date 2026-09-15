package com.planetaryfactory.core.fluid;

import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class BoilerItemHandler extends DelegatingResourceHandler<ItemResource> {
    private final BoilerBlockEntity blockEntity;

    public BoilerItemHandler(BoilerBlockEntity blockEntity) {
        super(VanillaContainerWrapper.of(blockEntity));
        this.blockEntity = blockEntity;
    }

    public BoilerBlockEntity boiler() {
        return blockEntity;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (!BoilerSlots.canExtract(convertIndex(index))) {
            return 0;
        }
        return super.extract(index, resource, amount, transaction);
    }
}
