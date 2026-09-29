package com.factoryworks.core.transfer;

import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A {@link DelegatingResourceHandler} whose slot-less overloads go back through its own slots.
 *
 * <p><b>Why this exists.</b> The transfer API states insert and extract twice: once per slot, and
 * once for "anywhere you can put it". {@link DelegatingResourceHandler} forwards the second pair
 * straight to the delegate:
 *
 * <pre>{@code
 * public int extract(T resource, int amount, TransactionContext transaction) {
 *     return getDelegate().extract(resource, amount, transaction);
 * }
 * }</pre>
 *
 * <p>The delegate then runs {@link ResourceHandler}'s own loop over <em>its</em> slots, so a
 * subclass that refuses a slot in {@code extract(int, ...)} is never consulted -- the refusal
 * holds for a caller that names the slot and evaporates for one that does not. Every face in this
 * mod is a routing rule rather than a passthrough, so every one of them was reachable that way: a
 * pipe could take the coal out from under a lit furnace, or drain the water back out of a Boiler
 * that is supposed to be consuming it.
 *
 * <p>Overriding both slot-less methods to loop through {@code this} is what restores the rule.
 * It is deliberately the same loop {@code ResourceHandler} writes, so a handler that refuses
 * nothing behaves exactly as before.
 */
public class GuardedResourceHandler<T extends Resource> extends DelegatingResourceHandler<T> {

    public GuardedResourceHandler(ResourceHandler<T> delegate) {
        super(delegate);
    }

    @Override
    public int insert(T resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        int inserted = 0;
        int size = size();
        for (int index = 0; index < size && inserted < amount; index++) {
            inserted += insert(index, resource, amount - inserted, transaction);
        }
        return inserted;
    }

    @Override
    public int extract(T resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        int extracted = 0;
        int size = size();
        for (int index = 0; index < size && extracted < amount; index++) {
            extracted += extract(index, resource, amount - extracted, transaction);
        }
        return extracted;
    }
}
