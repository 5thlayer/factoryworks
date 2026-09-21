package com.planetaryfactory.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * The one state the Assembling Machine's screen shows (#332). Where two hold at once, the machine
 * reports the first its craft cycle would stop on, so fixing what it names is what lets it run.
 */
class AssemblingStatusTest {

    @Test
    void aRunnablePoweredMachineIsProcessing() {
        assertEquals(AssemblingStatus.PROCESSING, AssemblingStatus.of(false, AssemblingStall.NONE, true));
    }

    @Test
    void eachStallIsItsOwnStatus() {
        assertEquals(AssemblingStatus.IDLE, AssemblingStatus.of(false, AssemblingStall.NO_RECIPE, true));
        assertEquals(AssemblingStatus.LOCKED, AssemblingStatus.of(false, AssemblingStall.LOCKED, true));
        assertEquals(AssemblingStatus.MISSING_INGREDIENTS,
                AssemblingStatus.of(false, AssemblingStall.NO_INGREDIENTS, true));
        assertEquals(AssemblingStatus.OUTPUT_FULL, AssemblingStatus.of(false, AssemblingStall.OUTPUT_FULL, true));
    }

    @Test
    void aRunnableMachineThatCannotPayItsTickHasNoPower() {
        assertEquals(AssemblingStatus.NO_POWER, AssemblingStatus.of(false, AssemblingStall.NONE, false));
        assertEquals(AssemblingStatus.NO_POWER, AssemblingStatus.of(false, AssemblingStall.NO_POWER, true));
    }

    @Test
    void everyStallOutranksNoPower() {
        assertEquals(AssemblingStatus.MISSING_INGREDIENTS,
                AssemblingStatus.of(false, AssemblingStall.NO_INGREDIENTS, false));
        assertEquals(AssemblingStatus.OUTPUT_FULL, AssemblingStatus.of(false, AssemblingStall.OUTPUT_FULL, false));
        assertEquals(AssemblingStatus.LOCKED, AssemblingStatus.of(false, AssemblingStall.LOCKED, false));
        assertEquals(AssemblingStatus.IDLE, AssemblingStatus.of(false, AssemblingStall.NO_RECIPE, false));
    }

    /** The stalls' own order is {@link AssemblingStall#of}'s, which {@code AssemblingStallTest} holds. */
    @Test
    void aRedstoneSignalOutranksEverything() {
        for (AssemblingStall stall : AssemblingStall.values()) {
            assertEquals(AssemblingStatus.DISABLED, AssemblingStatus.of(true, stall, false));
        }
    }

    @Test
    void everyStatusHasItsOwnLangKey() {
        assertEquals(AssemblingStatus.values().length,
                Arrays.stream(AssemblingStatus.values()).map(AssemblingStatus::langKey).distinct().count());
        assertEquals("gui.planetaryfactory.assembling_machine.status.missing_ingredients",
                AssemblingStatus.MISSING_INGREDIENTS.langKey());
    }
}
