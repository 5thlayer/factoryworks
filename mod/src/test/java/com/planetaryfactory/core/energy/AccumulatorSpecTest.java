package com.planetaryfactory.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The accumulator in FE: Factorio's 5 MJ and 300 kW at 100 J per FE (#283, ADR-0062). */
class AccumulatorSpecTest {

    @Test
    void holdsFiftyThousandFe() {
        assertEquals(50_000L, AccumulatorSpec.capacityFe());
    }

    @Test
    void movesOneHundredFiftyFePerTickEachWay() {
        assertEquals(150L, AccumulatorSpec.inputFePerTick());
        assertEquals(150L, AccumulatorSpec.outputFePerTick());
    }
}
