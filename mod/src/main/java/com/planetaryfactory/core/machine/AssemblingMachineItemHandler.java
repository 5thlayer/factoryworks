package com.planetaryfactory.core.machine;

import com.planetaryfactory.core.transfer.GuardedResourceHandler;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The Assembling Machine's item face, on every side. Inputs are filtered by
 * {@link AssemblingInputSlots}; only the output can be extracted.
 *
 * <p>Only sound while the machine pins Oritech's input mode: {@code FILL_EVENLY} spreads a per-slot
 * insert past this filter (ADR-0074).
 */
public class AssemblingMachineItemHandler extends GuardedResourceHandler<ItemResource> {

    private final AssemblingMachineBlockEntity machine;

    public AssemblingMachineItemHandler(AssemblingMachineBlockEntity machine) {
        super(machine.inventory);
        this.machine = machine;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return machine.acceptsInput(index, resource) && super.isValid(index, resource);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (!machine.acceptsInput(index, resource)) {
            return 0;
        }
        return super.insert(index, resource, amount, transaction);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (index != AssemblingMachineBlockEntity.OUTPUT) {
            return 0;
        }
        return super.extract(index, resource, amount, transaction);
    }
}
