package com.planetaryfactory.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Whether a request to hold a recipe is taken (#330, ADR-0073): the one rule Fill Recipe and the
 * screen share, so a refusal is said rather than left as a button that did nothing.
 */
class HoldVerdictTest {

    @Test
    void anUnlockedAssemblingRecipeIsHeld() {
        assertEquals(HoldVerdict.HELD, HoldVerdict.of(true, true, false));
        assertTrue(HoldVerdict.HELD.held());
    }

    @Test
    void anIdTheMachineCannotResolveIsRefused() {
        assertEquals(HoldVerdict.NOT_ASSEMBLING, HoldVerdict.of(false, true, false));
        assertFalse(HoldVerdict.NOT_ASSEMBLING.held());
    }

    @Test
    void aLockedRecipeIsRefused() {
        assertEquals(HoldVerdict.LOCKED, HoldVerdict.of(true, true, true));
    }

    @Test
    void anUnknownIdIsNotReportedAsLocked() {
        assertEquals(HoldVerdict.NOT_ASSEMBLING, HoldVerdict.of(false, true, true));
    }

    @Test
    void aRecipeOfACategoryTheMachineDoesNotCraftIsRefused() {
        assertEquals(HoldVerdict.NOT_THIS_MACHINE, HoldVerdict.of(true, false, false));
        assertFalse(HoldVerdict.NOT_THIS_MACHINE.held());
    }

    /** Factorio lists no such recipe in tier 1 at all, so researching it changes nothing here. */
    @Test
    void aLockedRecipeOfTheWrongCategoryIsNotReportedAsLocked() {
        assertEquals(HoldVerdict.NOT_THIS_MACHINE, HoldVerdict.of(true, false, true));
    }

    @Test
    void everyRefusalNamesAMessage() {
        for (HoldVerdict verdict : HoldVerdict.values()) {
            assertEquals(!verdict.held(), verdict.messageKey() != null, verdict.name());
        }
    }
}
