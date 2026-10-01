package com.factoryworks.core.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * A block's item and fluid faces asked as a pipe asks them: through the capability, never off the
 * block entity, since a face that is not registered is inert with nothing logged (#265).
 */
final class Faces {

    private Faces() {
    }

    /** Null when the block registers no item face. */
    static ResourceHandler<ItemResource> item(GameTestHelper helper, BlockPos at) {
        return helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(at), null);
    }

    /** Null when the block registers no fluid face. */
    static ResourceHandler<FluidResource> fluid(GameTestHelper helper, BlockPos at) {
        return helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(at), null);
    }

    /** One insert or extract, on whichever overload the lambda names. */
    interface Move<R extends Resource> {
        int apply(ResourceHandler<R> face, Transaction tx);
    }

    static <R extends Resource> int simulate(ResourceHandler<R> face, Move<R> move) {
        try (Transaction tx = Transaction.openRoot()) {
            return move.apply(face, tx);
        }
    }

    static <R extends Resource> void expectMoved(GameTestHelper helper, BlockPos at, String what, int expected,
            ResourceHandler<R> face, Move<R> move) {
        int moved;
        try (Transaction tx = Transaction.openRoot()) {
            moved = move.apply(face, tx);
            tx.commit();
        }
        if (moved != expected) {
            helper.fail(what + " moved " + moved + ", expected " + expected, at);
        }
    }
}
