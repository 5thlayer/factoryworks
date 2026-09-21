package com.planetaryfactory.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why an Assembling Machine does not run this tick (#328, ADR-0041): a Held recipe it cannot run is
 * held and idled on, never cleared, and the answer is reached before any energy is asked for.
 */
class AssemblingStallTest {

    @Test
    void aFedResearchedMachineWithRoomRuns() {
        assertEquals(AssemblingStall.NONE, AssemblingStall.of(true, false, true, true));
        assertFalse(AssemblingStall.NONE.stalled());
    }

    @Test
    void aMachineHoldingNothingIdles() {
        assertEquals(AssemblingStall.NO_RECIPE, AssemblingStall.of(false, false, true, true));
    }

    @Test
    void anUnresearchedRecipeDoesNotRunEvenWhenFed() {
        assertEquals(AssemblingStall.LOCKED, AssemblingStall.of(true, true, true, true));
    }

    @Test
    void anUnfedMachineIdles() {
        assertEquals(AssemblingStall.NO_INGREDIENTS, AssemblingStall.of(true, false, false, true));
    }

    @Test
    void aFullOutputStopsAFedMachine() {
        assertEquals(AssemblingStall.OUTPUT_FULL, AssemblingStall.of(true, false, true, false));
        assertTrue(AssemblingStall.OUTPUT_FULL.stalled());
    }
}
