package com.planetaryfactory.core.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The clock-hand shade over the craft under way: what is not yet done is shaded, and the shade
 * clears clockwise from twelve o'clock as the craft progresses, as Factorio's hand-craft queue does.
 */
class RadialWipeTest {

    private static final int SIZE = 32;

    private static int shadedCells(float progress) {
        int shaded = 0;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (RadialWipe.shaded(x, y, SIZE, progress)) shaded++;
            }
        }
        return shaded;
    }

    @Test
    void aCraftNotYetStartedIsShadedWhole() {
        assertEquals(SIZE * SIZE, shadedCells(0.0f));
    }

    @Test
    void aFinishedCraftIsNotShadedAtAll() {
        assertEquals(0, shadedCells(1.0f));
    }

    @Test
    void aQuarterDoneClearsTheTopRightQuadrantFirst() {
        assertFalse(RadialWipe.shaded(24, 8, SIZE, 0.26f), "top right clears first -- clockwise from twelve");
        assertTrue(RadialWipe.shaded(8, 8, SIZE, 0.26f), "top left clears last");
        assertTrue(RadialWipe.shaded(24, 24, SIZE, 0.24f), "bottom right is the second quarter");
    }

    @Test
    void halfDoneShadesTheLeftHalf() {
        assertEquals(SIZE * SIZE / 2, shadedCells(0.5f));
        assertTrue(RadialWipe.shaded(4, 16, SIZE, 0.5f));
        assertFalse(RadialWipe.shaded(28, 16, SIZE, 0.5f));
    }

    @Test
    void theShadeOnlyEverShrinks() {
        int previous = Integer.MAX_VALUE;
        for (int step = 0; step <= 20; step++) {
            int shaded = shadedCells(step / 20.0f);
            assertTrue(shaded <= previous);
            previous = shaded;
        }
    }

    @Test
    void progressOutsideZeroToOneIsClamped() {
        assertEquals(SIZE * SIZE, shadedCells(-0.5f));
        assertEquals(0, shadedCells(1.5f));
    }
}
