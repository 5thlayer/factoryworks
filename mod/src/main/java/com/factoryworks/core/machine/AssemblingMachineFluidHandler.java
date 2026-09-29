package com.factoryworks.core.machine;

import com.factoryworks.core.transfer.GuardedResourceHandler;

import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A crafting machine's fluid face, on every block of the footprint (ADR-0074, ADR-0075, ADR-0096):
 * a fluid the Held recipe names goes to its own input tank up to the Overload Limit (#519), anything
 * else is refused, and only the output tanks give anything back.
 */
public class AssemblingMachineFluidHandler extends GuardedResourceHandler<FluidResource> {

    private final AssemblingMachineBlockEntity machine;

    public AssemblingMachineFluidHandler(AssemblingMachineBlockEntity machine) {
        super(machine.tank());
        this.machine = machine;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return machine.inputTankFor(resource) == index && super.isValid(index, resource);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (machine.inputTankFor(resource) != index) {
            return 0;
        }
        return super.insert(index, resource, Math.min(amount, machine.fluidOverloadRoom(index)), transaction);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (!machine.isOutputTank(index)) {
            return 0;
        }
        return super.extract(index, resource, amount, transaction);
    }
}
