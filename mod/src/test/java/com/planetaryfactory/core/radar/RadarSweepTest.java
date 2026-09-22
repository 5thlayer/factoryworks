package com.planetaryfactory.core.radar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Nearest uncharted sector first within the reach, then the pass repeats (#368). */
class RadarSweepTest {

    private static final RadarSweep SWEEP = RadarSweep.of(14);
    private static final Sector ORIGIN = new Sector(-3, 7);

    private static double distance(Sector a, Sector b) {
        return Math.hypot(a.x() - b.x(), a.z() - b.z());
    }

    @Test
    void aPassIsEverySectorWithinFourteenOnce() {
        Set<Sector> seen = new HashSet<>();
        for (int cursor = 0; cursor < SWEEP.size(); cursor++) {
            Sector sector = SWEEP.sectorAt(ORIGIN, cursor);
            assertTrue(seen.add(sector), "charted twice in one pass: " + sector);
            assertTrue(Math.abs(sector.x() - ORIGIN.x()) <= 14 && Math.abs(sector.z() - ORIGIN.z()) <= 14,
                    "out of reach: " + sector);
        }
        assertEquals(29 * 29, seen.size());
    }

    @Test
    void theRadarsOwnSectorIsFirstAndEachNextIsNoNearer() {
        assertEquals(ORIGIN, SWEEP.sectorAt(ORIGIN, 0));
        for (int cursor = 1; cursor < SWEEP.size(); cursor++) {
            assertTrue(distance(ORIGIN, SWEEP.sectorAt(ORIGIN, cursor - 1))
                            <= distance(ORIGIN, SWEEP.sectorAt(ORIGIN, cursor)),
                    "sector " + cursor + " is nearer than the one before it");
        }
    }

    @Test
    void aFullReachStartsTheNextPassAtTheRadar() {
        int cursor = 0;
        for (int i = 0; i < SWEEP.size(); i++) {
            cursor = SWEEP.next(cursor);
        }
        assertEquals(0, cursor);
        assertEquals(ORIGIN, SWEEP.sectorAt(ORIGIN, cursor));
    }

    @Test
    void aCursorFromALargerReachStartsOver() {
        assertEquals(0, SWEEP.resume(SWEEP.size() + 40));
        assertEquals(0, SWEEP.resume(-1));
        assertEquals(17, SWEEP.resume(17));
    }

    @Test
    void aSectorIsThirtyTwoBlocksOnAThirtyTwoBlockGrid() {
        assertEquals(new Sector(0, 0), Sector.ofBlock(31, 0));
        assertEquals(new Sector(1, -1), Sector.ofBlock(32, -1));
        assertEquals(new Sector(-1, -2), Sector.ofBlock(-32, -33));
        assertEquals(-64, new Sector(-2, 0).minBlockX());
        assertEquals(new Sector(-5, 9), Sector.unpack(new Sector(-5, 9).pack()));
    }
}
