package com.planetaryfactory.core.mining.rig;

import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class RigItemHandler extends DelegatingResourceHandler<ItemResource> {
    private final RigBlockEntity blockEntity;

    public RigItemHandler(RigBlockEntity blockEntity) {
        super(new VanillaContainerWrapper(blockEntity));
        this.blockEntity = blockEntity;
    }

    public RigBlockEntity rig() {
        return blockEntity;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (!RigSlots.canExtract(convertIndex(index))) {
            return 0;
        }
        return super.extract(index, resource, amount, transaction);
    }
}
