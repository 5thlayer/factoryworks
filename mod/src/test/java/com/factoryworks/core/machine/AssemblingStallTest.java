package com.factoryworks.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Why an Assembling Machine does not run this tick (#328, ADR-0041): a Held recipe it cannot run is
 * held and idled on, never cleared, and the answer is reached before any energy is asked for.
 */
class AssemblingStallTest {

    @Test
    void aFedResearchedMachineWithRoomRuns() {
        assertEquals(AssemblingStall.NONE, AssemblingStall.of(true, false, true, true, true));
        assertFalse(AssemblingStall.NONE.stalled());
    }

    @Test
    void aMachineHoldingNothingIdles() {
        assertEquals(AssemblingStall.NO_RECIPE, AssemblingStall.of(false, false, true, true, true));
    }

    @Test
    void anUnresearchedRecipeDoesNotRunEvenWhenFed() {
        assertEquals(AssemblingStall.LOCKED, AssemblingStall.of(true, true, true, true, true));
    }

    @Test
    void anUnfedMachineIdles() {
        assertEquals(AssemblingStall.NO_INGREDIENTS, AssemblingStall.of(true, false, false, true, true));
    }

    @Test
    void aFullOutputStopsAFedMachine() {
        assertEquals(AssemblingStall.OUTPUT_FULL, AssemblingStall.of(true, false, true, true, false));
        assertTrue(AssemblingStall.OUTPUT_FULL.stalled());
    }

    /** A tank short of the recipe's fluid is its own stall, answered before any energy is asked. */
    @Test
    void aTankShortOfItsFluidIdles() {
        assertEquals(AssemblingStall.NO_FLUID, AssemblingStall.of(true, false, true, false, true));
        assertTrue(AssemblingStall.NO_FLUID.stalled());
    }

    /** Missing items are named first: both are inputs, and the slots are what a player sees first. */
    @Test
    void missingItemsOutrankAShortTank() {
        assertEquals(AssemblingStall.NO_INGREDIENTS, AssemblingStall.of(true, false, false, false, true));
    }

    /** A short tank outranks a full output, as missing items do: the craft would never start. */
    @Test
    void aShortTankOutranksAFullOutput() {
        assertEquals(AssemblingStall.NO_FLUID, AssemblingStall.of(true, false, true, false, false));
    }

    /** One full fraction of three stops a refinery: nothing may be voided (ADR-0096). */
    @Test
    void oneFullOutputTankOfSeveralStopsTheMachine() {
        assertEquals(AssemblingStall.OUTPUT_FULL,
                AssemblingStall.of(true, false, true, true, true, List.of(true, false, true)));
        assertEquals(AssemblingStall.OUTPUT_FULL,
                AssemblingStall.of(true, false, true, true, true, List.of(false, false, false)));
    }

    @Test
    void everyOutputTankWithRoomRuns() {
        assertEquals(AssemblingStall.NONE,
                AssemblingStall.of(true, false, true, true, true, List.of(true, true, true)));
    }

    /** No item result and three tanks with room: a refinery has no output slot to be full. */
    @Test
    void fullItemsStopAMachineWhoseTanksHaveRoom() {
        assertEquals(AssemblingStall.OUTPUT_FULL,
                AssemblingStall.of(true, false, true, true, false, List.of(true)));
    }

    /** A full tank never outranks a reason the craft would not start. */
    @Test
    void aFullTankIsNamedAfterEveryOtherStall() {
        List<Boolean> oneFull = List.of(true, false, true);
        assertEquals(AssemblingStall.NO_RECIPE, AssemblingStall.of(false, false, true, true, true, oneFull));
        assertEquals(AssemblingStall.LOCKED, AssemblingStall.of(true, true, true, true, true, oneFull));
        assertEquals(AssemblingStall.NO_INGREDIENTS, AssemblingStall.of(true, false, false, true, true, oneFull));
        assertEquals(AssemblingStall.NO_FLUID, AssemblingStall.of(true, false, true, false, true, oneFull));
    }
}
