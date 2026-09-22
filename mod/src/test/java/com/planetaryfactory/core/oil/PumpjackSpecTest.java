package com.planetaryfactory.core.oil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The Pumpjack in the pack's units: 90 kW and a 3 kW drain, one cycle a second (ADR-0081). */
class PumpjackSpecTest {

    private static final PumpjackSpec SPEC = PumpjackSpec.fromCorpus();

    @Test
    void itDraws45FePerTickWhileWorking() {
        assertEquals(45, SPEC.fePerTick());
    }

    @Test
    void itsDrainIs150JoulesATick() {
        assertEquals(150, SPEC.drainJoulesPerTick());
    }

    @Test
    void aCycleIsOneSecondsDraw() {
        assertEquals(900, SPEC.fePerCycle());
        assertEquals(20, SPEC.fePerCycle() / SPEC.fePerTick());
    }

    @Test
    void itPumpsOritechsCrudeIntoAThousandMillibucketTank() {
        assertEquals("oritech:still_oil", SPEC.fluid());
        assertEquals(1_000, SPEC.tankMillibuckets());
    }
}
