package com.planetaryfactory.core.energy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * When the Pick lets go of a wire's first end (ADR-0068): the anchor pole breaks, the Pick leaves
 * the main hand, the player changes dimension, or the player walks beyond the anchor's wire reach.
 */
class PendingEndTest {

    private static final PoleLinks.Pole ANCHOR = new PoleLinks.Pole(0, 64, 0, PoleTier.SMALL);

    @Test
    void anEndStaysHeldBesideAStandingAnchor() {
        assertTrue(PendingEnd.stillHeld(ANCHOR, 3.5, 64.0, 0.5, true, true, true));
    }

    @Test
    void anEndIsDroppedWhenTheAnchorBreaks() {
        assertFalse(PendingEnd.stillHeld(ANCHOR, 3.5, 64.0, 0.5, false, true, true));
    }

    @Test
    void anEndIsDroppedWhenThePickLeavesTheMainHand() {
        assertFalse(PendingEnd.stillHeld(ANCHOR, 3.5, 64.0, 0.5, true, false, true));
    }

    @Test
    void anEndIsDroppedWhenThePlayerChangesDimension() {
        assertFalse(PendingEnd.stillHeld(ANCHOR, 3.5, 64.0, 0.5, true, true, false));
    }

    @Test
    void anEndIsDroppedBeyondTheAnchorsWireReachAndHeldWithinIt() {
        // A small pole reaches 7.5, measured from its block centre (0.5, 64.5, 0.5).
        assertTrue(PendingEnd.stillHeld(ANCHOR, 7.5, 64.5, 0.5, true, true, true));
        assertFalse(PendingEnd.stillHeld(ANCHOR, 8.5, 64.5, 0.5, true, true, true));
    }
}
