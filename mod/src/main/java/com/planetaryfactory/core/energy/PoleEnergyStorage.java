package com.planetaryfactory.core.energy;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The pole's FE face: what a grid mod's bridge block inserts into.
 *
 * <p>Insert-only. Energy leaves a pole by being pushed into the machines in its area, never by
 * being pulled back out of its face, so {@link #extract} is a flat refusal rather than a hole a
 * cable could drain the buffer through.
 *
 * <p>The ledger is the state and the journal is how a transaction is undone. NeoForge's transfer
 * API lets a caller open a transaction, insert, and then abort -- which is how the pole's own tick
 * measures a machine's room -- so an insert has to be revertible rather than immediately final.
 * {@link com.planetaryfactory.core.energy.LongSnapshotJournal} is that undo, and it is shared with
 * the furnace's face so the rule is spelled once. Doing this by hand is what the removed
 * close-callback version was, and it could not see nested transactions at all.
 */
public final class PoleEnergyStorage implements EnergyHandler {

    private final SupplyAreaPoleBlockEntity pole;
    private final LongSnapshotJournal journal;

    PoleEnergyStorage(SupplyAreaPoleBlockEntity pole) {
        this.pole = pole;
        this.journal = new LongSnapshotJournal(
                () -> pole.ledger().storedFe(), pole.ledger()::setStoredFe, pole::setChanged);
    }

    @Override
    public long getAmountAsLong() {
        return pole.ledger().storedFe();
    }

    @Override
    public long getCapacityAsLong() {
        return pole.ledger().capacityFe();
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        if (amount <= 0) {
            return 0;
        }
        // Snapshot before the mutation, unconditionally: a full buffer takes nothing and the
        // snapshot is then a no-op to hand back, which is cheaper than asking the ledger twice
        // what it will accept and then asking it again to accept it.
        journal.updateSnapshots(transaction);
        return (int) pole.ledger().receiveFe(amount);
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        return 0;
    }
}
