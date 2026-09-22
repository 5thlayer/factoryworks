package com.planetaryfactory.core.radar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** A sector per 10 MJ, drawn at up to 150 FE/t (#368). */
class RadarEnergyTest {

    private static final RadarSpec SPEC = new RadarSpec(150L, 100_000L, 14);

    /** Fills the buffer as a pole would, then ticks; returns the tick the first sector fell due on. */
    private static int ticksToFirstSector(RadarEnergy energy, long fedPerTick) {
        for (int tick = 1; tick <= 100_000; tick++) {
            energy.insert(fedPerTick);
            if (energy.tick() > 0) {
                return tick;
            }
        }
        throw new AssertionError("no sector in 100,000 ticks");
    }

    @Test
    void aFullyFedRadarChartsItsFirstSectorOnTick667() {
        // 100,000 / 150 = 666.7: 666 ticks leave it 100 FE short.
        assertEquals(667, ticksToFirstSector(new RadarEnergy(SPEC), 150L));
    }

    @Test
    void theOverspillCarriesIntoTheNextSector() {
        RadarEnergy energy = new RadarEnergy(SPEC);
        int sectors = 0;
        for (int tick = 0; tick < 2_000; tick++) {
            energy.insert(150L);
            sectors += energy.tick();
        }
        assertEquals(3, sectors);
        assertEquals(2_000L * 150L - 3L * 100_000L, energy.progress());
    }

    @Test
    void aStarvedRadarChartsNothingAndDrawsNothing() {
        RadarEnergy energy = new RadarEnergy(SPEC);
        for (int tick = 0; tick < 2_000; tick++) {
            assertEquals(0, energy.tick());
        }
        assertEquals(0L, energy.progress());
    }

    @Test
    void aHalfFedRadarTakesTwiceAsLong() {
        assertEquals(1_334, ticksToFirstSector(new RadarEnergy(SPEC), 75L));
    }

    @Test
    void theBufferHoldsOneTickAndTakesNoMore() {
        RadarEnergy energy = new RadarEnergy(SPEC);
        assertEquals(150L, energy.insert(1_000L));
        assertEquals(0L, energy.insert(1L));
        energy.tick();
        assertEquals(0L, energy.buffered());
        assertEquals(150L, energy.progress());
    }
}
