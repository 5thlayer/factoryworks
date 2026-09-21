package com.planetaryfactory.core.machine;

import com.planetaryfactory.core.transfer.GuardedResourceHandler;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The Assembling Machine's item face (#329), returned for every {@code Direction} and the null side.
 *
 * <p>In: an input slot takes only the Held recipe's ingredient for that slot, and a slot the recipe
 * does not use takes nothing ({@link AssemblingInputSlots}, ADR-0073). A machine with no Held recipe
 * therefore takes nothing at all -- deliberately, since it makes nothing. Out: the output slot alone,
 * so a loader cannot strip ingredients a craft is waiting on. Direction never decides either way, which
 * is Factorio's arrangement and the furnace's.
 *
 * <p><b>Over Oritech's inventory, with Oritech's routing pinned off</b> (ADR-0074). Oritech's storage overrides
 * the per-slot insert too: in {@code FILL_EVENLY} mode an insert naming slot 0 is spread over every
 * input slot, past this filter. The machine pins its input mode to {@code FILL_LEFT_TO_RIGHT}, where
 * the per-slot insert is {@code ItemStacksResourceHandler}'s plain one, so there is one routing layer
 * -- this one. See {@link AssemblingMachineBlockEntity#cycleInputMode}.
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
