package com.planetaryfactory.core.radar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The Radar's order (ADR-0079): an 8x8-sector nearby area as one pulse, centred on the Radar's
 * block, and beyond it Factorio's long range in square rings, each from its top-left sector
 * clockwise, unexplored sectors first.
 */
class RadarSweepTest {

    /** Sector (-3, 7) is blocks -96..-65 by 224..255; its low half starts at -96 and 224. */
    private static final Sector OWN = new Sector(-3, 7);
    private static final RadarSweep LOW = RadarSweep.around(-90, 230, 8, 14);
    private static final RadarSweep HIGH = RadarSweep.around(-70, 250, 8, 14);

    private static int ring(Sector sector) {
        return Math.max(Math.abs(sector.x() - OWN.x()), Math.abs(sector.z() - OWN.z()));
    }

    private static Set<Sector> square(int from, int to) {
        Set<Sector> square = new HashSet<>();
        for (int dx = from; dx <= to; dx++) {
            for (int dz = from; dz <= to; dz++) {
                square.add(OWN.offset(dx, dz));
            }
        }
        return square;
    }

    @Test
    void theNearbyAreaIsSixteenBySixteenChunksCentredOnTheRadar() {
        assertEquals(square(-4, 3), new HashSet<>(LOW.nearby()));
        assertEquals(square(-3, 4), new HashSet<>(HIGH.nearby()));
        assertEquals(64, LOW.nearby().size());
        assertEquals(OWN, LOW.nearby().get(0));
    }

    @Test
    void aPassIsEverySectorBeyondTheNearbyAreaWithinFourteenOnce() {
        Set<Sector> nearby = new HashSet<>(LOW.nearby());
        Set<Sector> seen = new HashSet<>();
        for (int cursor = 0; cursor < LOW.size(); cursor++) {
            Sector sector = LOW.sectorAt(cursor);
            assertTrue(seen.add(sector), "scanned twice in one pass: " + sector);
            assertFalse(nearby.contains(sector), "the nearby area is not the long range: " + sector);
            assertTrue(ring(sector) <= 14, "out of reach: " + sector);
        }
        assertEquals(29 * 29 - 8 * 8, seen.size());
    }

    @Test
    void eachRingStartsTopLeftAndRunsClockwise() {
        assertEquals(OWN.offset(-4, -4), HIGH.sectorAt(0));
        assertEquals(OWN.offset(-3, -4), HIGH.sectorAt(1));
        assertEquals(OWN.offset(4, -4), HIGH.sectorAt(8));
        assertEquals(OWN.offset(-4, 4), HIGH.sectorAt(9));
        assertEquals(OWN.offset(-4, -3), HIGH.sectorAt(16));
        assertEquals(OWN.offset(-5, -5), HIGH.sectorAt(17));
        assertEquals(OWN.offset(4, -4), LOW.sectorAt(0));
        assertEquals(OWN.offset(4, 4), LOW.sectorAt(8));
        assertEquals(OWN.offset(-4, 4), LOW.sectorAt(16));
        for (int cursor = 1; cursor < LOW.size(); cursor++) {
            assertTrue(ring(LOW.sectorAt(cursor - 1)) <= ring(LOW.sectorAt(cursor)),
                    "sector " + cursor + " is in a nearer ring than the one before it");
        }
    }

    @Test
    void anUnexploredSectorIsScannedBeforeAnyRescan() {
        Set<Sector> charted = new HashSet<>();
        for (int cursor = 0; cursor < LOW.size(); cursor++) {
            charted.add(LOW.sectorAt(cursor));
        }
        charted.remove(LOW.sectorAt(500));

        assertEquals(500, LOW.pick(charted::contains, 3));
    }

    @Test
    void theFirstUnexploredSectorInOrderComesFirst() {
        Set<Sector> charted = Set.of(LOW.sectorAt(0), LOW.sectorAt(1));

        assertEquals(2, LOW.pick(charted::contains, 40));
    }

    @Test
    void withEverythingChartedTheCursorRescansInTurn() {
        assertEquals(40, LOW.pick(sector -> true, 40));
    }

    @Test
    void aFullReachStartsTheNextPassAtTheFirstRing() {
        int cursor = 0;
        for (int i = 0; i < LOW.size(); i++) {
            cursor = LOW.next(cursor);
        }
        assertEquals(0, cursor);
    }

    @Test
    void aCursorFromALargerReachStartsOver() {
        assertEquals(0, LOW.resume(LOW.size() + 40));
        assertEquals(0, LOW.resume(-1));
        assertEquals(17, LOW.resume(17));
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
