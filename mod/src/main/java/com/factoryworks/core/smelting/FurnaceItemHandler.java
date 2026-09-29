package com.factoryworks.core.smelting;

import com.factoryworks.core.transfer.GuardedResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The furnace's one item face (#155), returned for every {@code Direction} and for the null side.
 *
 * <p>Direction never changes what happens here; the item does. Routing on the way in is
 * {@link FurnaceBlockEntity#canPlaceItem}, which the container wrapper asks; extraction reaches the
 * output slot alone, so a funnel cannot strip a furnace of its own fuel or of the input it has not
 * smelted yet. The rules themselves are in {@link FurnaceSlots}, where they are checkable without a
 * world.
 *
 * <p>That is Factorio's arrangement -- there the inserter's direction decides in or out and the
 * furnace has no faces -- and it is also what makes a belt funnel work on whichever face a player
 * puts it on.
 */
public class FurnaceItemHandler extends GuardedResourceHandler<ItemResource> {
    private final FurnaceBlockEntity blockEntity;

    public FurnaceItemHandler(FurnaceBlockEntity blockEntity) {
        super(VanillaContainerWrapper.of(blockEntity));
        this.blockEntity = blockEntity;
    }

    public FurnaceBlockEntity furnace() {
        return blockEntity;
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (convertIndex(index) == FurnaceSlots.INPUT) {
            amount = Math.min(amount, blockEntity.overloadRoom(resource.toStack(1)));
            if (amount == 0) {
                return 0;
            }
        }
        return super.insert(index, resource, amount, transaction);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (!FurnaceSlots.canExtract(convertIndex(index))) {
            return 0;
        }
        return super.extract(index, resource, amount, transaction);
    }
}
