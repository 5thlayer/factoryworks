package com.planetaryfactory.core.energy;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class PoleEnergyStorage implements EnergyHandler {

    private final SupplyAreaPoleBlockEntity pole;

    PoleEnergyStorage(SupplyAreaPoleBlockEntity pole) {
        this.pole = pole;
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
        long taken = pole.ledger().receiveFe(amount);
        if (taken > 0L) {
            transaction.addCloseCallback((ctx, result) -> {
                if (result.wasAborted()) {
                    pole.ledger().setStoredFe(pole.ledger().storedFe() - taken);
                } else {
                    pole.setChanged();
                }
            });
        }
        return (int) taken;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        return 0;
    }
}
