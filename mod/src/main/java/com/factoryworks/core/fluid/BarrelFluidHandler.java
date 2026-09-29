package com.factoryworks.core.fluid;

import com.factoryworks.core.PFDataComponents;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.ItemAccessFluidHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class BarrelFluidHandler extends ItemAccessFluidHandler {
    public BarrelFluidHandler(ItemAccess access) {
        super(access, PFDataComponents.FLUID_CONTENT.get(), BarrelSpec.CAPACITY_MB);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return single() ? super.insert(index, resource, amount, transaction) : 0;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return single() ? super.extract(index, resource, amount, transaction) : 0;
    }

    private boolean single() {
        return itemAccess.getAmount() == 1;
    }
}
