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
        assertEquals(HoldVerdict.HELD, HoldVerdict.of(true, false));
        assertTrue(HoldVerdict.HELD.held());
    }

    @Test
    void anIdTheMachineCannotResolveIsRefused() {
        assertEquals(HoldVerdict.NOT_ASSEMBLING, HoldVerdict.of(false, false));
        assertFalse(HoldVerdict.NOT_ASSEMBLING.held());
    }

    @Test
    void aLockedRecipeIsRefused() {
        assertEquals(HoldVerdict.LOCKED, HoldVerdict.of(true, true));
    }

    @Test
    void anUnknownIdIsNotReportedAsLocked() {
        assertEquals(HoldVerdict.NOT_ASSEMBLING, HoldVerdict.of(false, true));
    }

    @Test
    void everyRefusalNamesAMessage() {
        for (HoldVerdict verdict : HoldVerdict.values()) {
            assertEquals(!verdict.held(), verdict.messageKey() != null, verdict.name());
        }
    }
}
