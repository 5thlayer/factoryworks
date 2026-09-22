package com.planetaryfactory.core.machine;

import com.planetaryfactory.core.transfer.GuardedResourceHandler;

import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Tiers 2 and 3's fluid face, on every block of the footprint (ADR-0074, ADR-0075): it takes only a
 * fluid the Held recipe names, and gives nothing back.
 */
public class AssemblingMachineFluidHandler extends GuardedResourceHandler<FluidResource> {

    private final AssemblingMachineBlockEntity machine;

    public AssemblingMachineFluidHandler(AssemblingMachineBlockEntity machine) {
        super(machine.tank());
        this.machine = machine;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return machine.acceptsFluid(resource) && super.isValid(index, resource);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (!machine.acceptsFluid(resource)) {
            return 0;
        }
        return super.insert(index, resource, amount, transaction);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return 0;
    }
}
