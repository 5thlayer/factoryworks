package com.factoryworks.core.oil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** A well's yield, a cycle's crude and the floor its depletion stops at (ADR-0081). */
class WellYieldTest {

    private static final WellYield YIELD = WellYield.fromCorpus();

    @Test
    void theCorpusFiguresAreFactorios() {
        assertEquals(300_000, YIELD.normal());
        assertEquals(60_000, YIELD.minimum());
        assertEquals(10, YIELD.depletion());
        assertEquals(10, YIELD.amountPerCycle());
    }

    @Test
    void yieldIsTheAmountOverNormal() {
        assertEquals(1.0, YIELD.yield(300_000), 1e-12);
        assertEquals(0.2, YIELD.yield(60_000), 1e-12);
        assertEquals(1.8, YIELD.yield(540_000), 1e-12);
    }

    @Test
    void aFullYieldWellGivesTenACycleAndLosesTen() {
        WellYield.Cycle cycle = YIELD.cycle(300_000, 300_000, 0);
        assertEquals(10, cycle.crude());
        assertEquals(299_990, cycle.amount());
        assertEquals(0, cycle.carry());
    }

    @Test
    void aFractionalYieldIsCarriedToTheNextCycle() {
        // 25% yield is 2.5 a cycle: 2, then 3.
        WellYield.Cycle first = YIELD.cycle(75_000, 75_000, 0);
        assertEquals(2, first.crude());
        WellYield.Cycle second = YIELD.cycle(75_000, 75_000, first.carry());
        assertEquals(3, second.crude());
        assertEquals(0, second.carry());
    }

    @Test
    void aCycleYieldsAtMostOneThousand() {
        WellYield.Cycle cycle = YIELD.cycle(40_000_000, 40_000_000, 0);
        assertEquals(1_000, cycle.crude());
    }

    @Test
    void theFloorIsTwentyPercentYieldForAnOrdinaryWell() {
        assertEquals(60_000, YIELD.floor(250_000));
        assertEquals(60_000, YIELD.cycle(60_005, 250_000, 0).amount());
        assertEquals(60_000, YIELD.cycle(60_000, 250_000, 0).amount());
    }

    @Test
    void aWellStartingAt180PercentStopsAt36() {
        long initial = 540_000;
        assertEquals(108_000, YIELD.floor(initial));
        long amount = initial;
        long carry = 0;
        for (int i = 0; i < 50_000; i++) {
            WellYield.Cycle cycle = YIELD.cycle(amount, initial, carry);
            amount = cycle.amount();
            carry = cycle.carry();
        }
        assertEquals(108_000, amount);
        assertEquals(0.36, YIELD.yield(amount), 1e-12);
        assertEquals(3, YIELD.cycle(amount, initial, 0).crude());
    }

    @Test
    void aWellBelowItsFloorIsNeverDepleted() {
        assertEquals(50_000, YIELD.cycle(50_000, 50_000, 0).amount());
    }
}
