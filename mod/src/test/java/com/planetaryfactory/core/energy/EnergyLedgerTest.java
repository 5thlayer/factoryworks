package com.planetaryfactory.core.energy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The pole's books, which are FE in and FE out (ADR-0060: with GregTech, Power Grid and Create
 * gone, FE is the pack's energy currency and there is nothing left for the pole to convert to).
 * The ledger no longer converts anything; what is left of its job is to respect a capacity and to
 * not invent energy while doing it. EU survives elsewhere in the mod -- see {@link EnergyLedger}'s
 * note on the furnace ladder, which #266 re-derives.
 *
 * <p>The conversion this class used to hold -- GregTech's {@code feToEuRatio} of 4, and the retained
 * remainder that kept a trickle from being rounded away -- went with the EU side it existed for.
 */
class EnergyLedgerTest {

    private static final long CAP = 1_000_000L;

    @Test
    void whatIsReceivedIsWhatIsAvailable() {
        EnergyLedger ledger = new EnergyLedger(CAP);
        ledger.receiveFe(4);
        assertEquals(4, ledger.availableFe());
    }

    @Test
    void drainingRemovesExactlyWhatItHandedOut() {
        EnergyLedger ledger = new EnergyLedger(CAP);
        ledger.receiveFe(10);
        assertEquals(2, ledger.drainFe(2));
        assertEquals(8, ledger.storedFe());
    }

    @Test
    void drainingMoreThanIsStoredYieldsOnlyWhatIsStored() {
        EnergyLedger ledger = new EnergyLedger(CAP);
        ledger.receiveFe(8);
        assertEquals(8, ledger.drainFe(50));
        assertEquals(0, ledger.storedFe());
    }

    @Test
    void receiveIsCappedAndReportsWhatItTook() {
        EnergyLedger ledger = new EnergyLedger(10);
        assertEquals(10, ledger.receiveFe(25));
        assertEquals(0, ledger.receiveFe(25));
        assertEquals(10, ledger.storedFe());
    }

    @Test
    void simulatingChangesNothing() {
        EnergyLedger ledger = new EnergyLedger(CAP);
        assertEquals(7, ledger.simulateReceiveFe(7));
        assertEquals(0, ledger.storedFe());
    }

    @Test
    void energyIsNeverCreated() {
        EnergyLedger ledger = new EnergyLedger(CAP);
        long fed = 0;
        for (int i = 0; i < 1000; i++) {
            fed += ledger.receiveFe(7);
        }
        long drained = ledger.drainFe(Long.MAX_VALUE);
        assertEquals(fed, drained + ledger.storedFe());
    }

    @Test
    void negativeAmountsAreRefused() {
        EnergyLedger ledger = new EnergyLedger(CAP);
        assertEquals(0, ledger.receiveFe(-5));
        assertEquals(0, ledger.drainFe(-5));
    }
}
