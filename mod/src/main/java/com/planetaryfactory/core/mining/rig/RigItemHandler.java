package com.planetaryfactory.core.mining.rig;

import com.planetaryfactory.core.transfer.GuardedResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The rig's one item face (#193), returned for every {@code Direction} and for the null side.
 *
 * <p>Fuel goes in on a burner rig and ore comes out; nothing else is accepted and nothing else can
 * be taken. The rules are in {@link RigSlots}, where they are checkable without a world.
 *
 * <p>This is the face a hopper, a funnel or a player's shift-click uses. It is <em>not</em> how the
 * rig delivers what it mines -- for that the rig pushes into the handler on the tile it faces,
 * which is ADR-0043's mechanic.
 */
public class RigItemHandler extends GuardedResourceHandler<ItemResource> {
    private final RigBlockEntity blockEntity;

    public RigItemHandler(RigBlockEntity blockEntity) {
        super(VanillaContainerWrapper.of(blockEntity));
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
