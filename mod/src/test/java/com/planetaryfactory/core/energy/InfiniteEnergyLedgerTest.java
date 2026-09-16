package com.planetaryfactory.core.energy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The creative pole's books (#272).
 *
 * <p>The shipped pole is insert-only and its buffer is one tick of ADR-0060's transmission rate, so
 * checking an energy face by hand means first building a power chain -- which makes a two-block
 * test depend on most of the port. A ledger that is always full removes that, and it is the
 * <em>only</em> thing the creative pole changes: the scan, the water-fill rationing, the Jade line
 * and the per-tick push are the shipped pole's, or the thing being tested is not the thing that
 * ships.
 *
 * <p>Two properties carry that. A drain never reduces it -- a ledger that debited would run dry
 * after one tick and read as an ordinary pole with a large buffer, which is the failure mode a
 * player would misread as "the machine is not drawing". And a full buffer still reports the demand
 * its area asked for, because {@code demandedFePerTick} is measured from the receivers rather than
 * from the ledger and is the half of the Jade line that says the machines were found at all.
 */
class InfiniteEnergyLedgerTest {

    @Test
    void drainingNeverReducesIt() {
        EnergyLedger ledger = new InfiniteEnergyLedger();
        long before = ledger.availableFe();
        assertEquals(5_000L, ledger.drainFe(5_000L));
        assertEquals(before, ledger.availableFe());
        assertEquals(before, ledger.storedFe());
    }

    @Test
    void everyDemandIsAnsweredWhole() {
        EnergyLedger ledger = new InfiniteEnergyLedger();
        // The pole hands availableFe() to the water-fill as the pot. A creative pole's pot has to
        // cover any demand the area can state, and a receiver's demand is an int insert.
        long[] grants = EnergyShare.waterFill(ledger.availableFe(),
                new long[] {Integer.MAX_VALUE, Integer.MAX_VALUE, 90L});
        assertEquals(Integer.MAX_VALUE, grants[0]);
        assertEquals(Integer.MAX_VALUE, grants[1]);
        assertEquals(90L, grants[2]);
    }

    @Test
    void itIsAlwaysFull() {
        EnergyLedger ledger = new InfiniteEnergyLedger();
        assertEquals(ledger.capacityFe(), ledger.storedFe());
        ledger.drainFe(Long.MAX_VALUE);
        assertEquals(ledger.capacityFe(), ledger.storedFe());
    }

    @Test
    void settingTheStoredAmountCannotEmptyIt() {
        // The block entity reads StoredFe back off the save on every load. A creative pole saved
        // empty -- or saved before it was creative -- must not come back as a dead pole.
        EnergyLedger ledger = new InfiniteEnergyLedger();
        ledger.setStoredFe(0L);
        assertEquals(ledger.capacityFe(), ledger.storedFe());
    }

    @Test
    void insertionIsAcceptedAndChangesNothing() {
        // The face is still registered, so a connector may still insert. It must not be told the
        // energy was refused -- a grid mod that is told nothing fits may log or back up -- and it
        // must not be able to make the ledger any fuller than it already is.
        EnergyLedger ledger = new InfiniteEnergyLedger();
        assertEquals(1_000L, ledger.simulateReceiveFe(1_000L));
        assertEquals(1_000L, ledger.receiveFe(1_000L));
        assertEquals(ledger.capacityFe(), ledger.storedFe());
    }

    @Test
    void theCapacityIsFiniteEnoughToArithmeticOn() {
        // Long.MAX_VALUE would overflow the moment anything summed it. The pot only has to cover
        // an 18x18x5 area of machines each demanding at most Integer.MAX_VALUE.
        EnergyLedger ledger = new InfiniteEnergyLedger();
        assertTrue(ledger.capacityFe() > 1620L * Integer.MAX_VALUE,
                "the pot has to cover a full substation area of maximally hungry machines");
        assertTrue(ledger.capacityFe() < Long.MAX_VALUE / 4L,
                "and still leave room for anything that sums or doubles it");
    }
}
