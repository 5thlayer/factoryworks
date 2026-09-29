package com.factoryworks.core.radar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The Radar's corpus figures in the pack's units (#368). Expected values typed, not derived. */
class RadarSpecTest {

    private final RadarSpec spec = RadarSpec.fromCorpus();

    @Test
    void drawsThreeHundredKilowattsAsOneHundredFiftyFePerTick() {
        assertEquals(150L, spec.fePerTick());
    }

    @Test
    void aSectorCostsTenMegajoulesAsOneHundredThousandFe() {
        assertEquals(100_000L, spec.fePerSector());
    }

    @Test
    void reachesFourteenSectors() {
        assertEquals(14, spec.reach());
    }

    @Test
    void aNearbyPulseCostsTwoHundredFiftyKilojoulesAsTwoThousandFiveHundredFe() {
        assertEquals(2_500L, spec.fePerNearbyScan());
    }

    @Test
    void theNearbyAreaReachesFourSectors() {
        assertEquals(4, spec.nearReach());
    }

    @Test
    void theFootprintIsFactoriosThreeByThree() {
        assertEquals(3, RadarCorpus.get().tileWidth());
        assertEquals(3, RadarCorpus.get().tileHeight());
    }
}
