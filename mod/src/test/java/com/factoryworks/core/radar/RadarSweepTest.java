package com.factoryworks.core.radar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Factorio's Radar order (ADR-0079): the 9x9 nearby area as one pulse, and beyond it the reach in
 * square rings, each from its top-left sector clockwise, unexplored sectors first.
 */
class RadarSweepTest {

    private static final RadarSweep SWEEP = RadarSweep.of(4, 14);
    private static final Sector ORIGIN = new Sector(-3, 7);

    private static int ring(Sector sector) {
        return Math.max(Math.abs(sector.x() - ORIGIN.x()), Math.abs(sector.z() - ORIGIN.z()));
    }

    @Test
    void theNearbyAreaIsTheNineByNineAroundTheRadar() {
        List<Sector> nearby = SWEEP.nearby(ORIGIN);
        assertEquals(81, new HashSet<>(nearby).size());
        assertEquals(81, nearby.size());
        assertTrue(nearby.stream().allMatch(sector -> ring(sector) <= 4));
        assertEquals(ORIGIN, nearby.get(0));
    }

    @Test
    void aPassIsEverySectorBeyondTheNearbyAreaWithinFourteenOnce() {
        Set<Sector> seen = new HashSet<>();
        for (int cursor = 0; cursor < SWEEP.size(); cursor++) {
            Sector sector = SWEEP.sectorAt(ORIGIN, cursor);
            assertTrue(seen.add(sector), "scanned twice in one pass: " + sector);
            assertTrue(ring(sector) > 4 && ring(sector) <= 14, "out of the long range: " + sector);
        }
        assertEquals(29 * 29 - 9 * 9, seen.size());
    }

    @Test
    void eachRingStartsTopLeftAndRunsClockwise() {
        assertEquals(ORIGIN.offset(-5, -5), SWEEP.sectorAt(ORIGIN, 0));
        assertEquals(ORIGIN.offset(-4, -5), SWEEP.sectorAt(ORIGIN, 1));
        assertEquals(ORIGIN.offset(5, -5), SWEEP.sectorAt(ORIGIN, 10));
        assertEquals(ORIGIN.offset(5, -4), SWEEP.sectorAt(ORIGIN, 11));
        assertEquals(ORIGIN.offset(5, 5), SWEEP.sectorAt(ORIGIN, 20));
        assertEquals(ORIGIN.offset(-5, 5), SWEEP.sectorAt(ORIGIN, 30));
        assertEquals(ORIGIN.offset(-5, -4), SWEEP.sectorAt(ORIGIN, 39));
        assertEquals(ORIGIN.offset(-6, -6), SWEEP.sectorAt(ORIGIN, 40));
        for (int cursor = 1; cursor < SWEEP.size(); cursor++) {
            assertTrue(ring(SWEEP.sectorAt(ORIGIN, cursor - 1)) <= ring(SWEEP.sectorAt(ORIGIN, cursor)),
                    "sector " + cursor + " is in a nearer ring than the one before it");
        }
    }

    @Test
    void anUnexploredSectorIsScannedBeforeAnyRescan() {
        Set<Sector> charted = new HashSet<>();
        for (int cursor = 0; cursor < SWEEP.size(); cursor++) {
            charted.add(SWEEP.sectorAt(ORIGIN, cursor));
        }
        Sector unexplored = SWEEP.sectorAt(ORIGIN, 500);
        charted.remove(unexplored);

        assertEquals(500, SWEEP.pick(ORIGIN, charted::contains, 3));
    }

    @Test
    void theFirstUnexploredSectorInOrderComesFirst() {
        Set<Sector> charted = Set.of(SWEEP.sectorAt(ORIGIN, 0), SWEEP.sectorAt(ORIGIN, 1));

        assertEquals(2, SWEEP.pick(ORIGIN, charted::contains, 40));
    }

    @Test
    void withEverythingChartedTheCursorRescansInTurn() {
        assertEquals(40, SWEEP.pick(ORIGIN, sector -> true, 40));
    }

    @Test
    void aFullReachStartsTheNextPassAtTheFirstRing() {
        int cursor = 0;
        for (int i = 0; i < SWEEP.size(); i++) {
            cursor = SWEEP.next(cursor);
        }
        assertEquals(0, cursor);
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
