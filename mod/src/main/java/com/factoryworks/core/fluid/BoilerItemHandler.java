package com.factoryworks.core.fluid;

import com.factoryworks.core.transfer.GuardedResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The Boiler's one item face (#224), returned for every {@code Direction} and for the null side.
 *
 * <p>Fuel in and nothing out, which is {@link BoilerSlots}' whole rule: every slot refuses
 * extraction, because a Boiler holds only the fuel it is burning and letting a funnel take that
 * back is pulling the coal out from under it mid-tick. Direction never changes what happens here;
 * the item does.
 */
public class BoilerItemHandler extends GuardedResourceHandler<ItemResource> {
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
