package com.factoryworks.core.radar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * A long-range sector per 10 MJ and a nearby pulse per 250 kJ, both counted from the same draw of
 * up to 150 FE/t (ADR-0079).
 */
class RadarEnergyTest {

    private static final RadarSpec SPEC = new RadarSpec(150L, 100_000L, 2_500L, 4, 14);

    /** Fills the buffer as a pole would, then ticks; returns the tick the first sector fell due on. */
    private static int ticksToFirstSector(RadarEnergy energy, long fedPerTick) {
        for (int tick = 1; tick <= 100_000; tick++) {
            energy.insert(fedPerTick);
            if (energy.tick().sector()) {
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
            sectors += energy.tick().sector() ? 1 : 0;
        }
        assertEquals(3, sectors);
        assertEquals(2_000L * 150L - 3L * 100_000L, energy.progress());
    }

    @Test
    void aFullyFedRadarPulsesTheNearbyAreaOnTick17AndTheSectorStillFallsDueOn667() {
        // 2,500 / 150 = 16.7. The pulse does not spend the sector's energy: Factorio's 33.3 s holds.
        RadarEnergy energy = new RadarEnergy(SPEC);
        int firstPulse = 0;
        int pulses = 0;
        for (int tick = 1; tick <= 667; tick++) {
            energy.insert(150L);
            RadarEnergy.Scans scans = energy.tick();
            if (scans.nearby()) {
                pulses++;
                if (firstPulse == 0) {
                    firstPulse = tick;
                }
            }
            assertEquals(tick == 667, scans.sector());
        }
        assertEquals(17, firstPulse);
        assertEquals(40, pulses);
    }

    @Test
    void aStarvedRadarChartsNothingAndDrawsNothing() {
        RadarEnergy energy = new RadarEnergy(SPEC);
        for (int tick = 0; tick < 2_000; tick++) {
            RadarEnergy.Scans scans = energy.tick();
            assertFalse(scans.sector());
            assertFalse(scans.nearby());
        }
        assertEquals(0L, energy.progress());
        assertEquals(0L, energy.nearbyProgress());
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
        assertEquals(150L, energy.nearbyProgress());
    }
}
