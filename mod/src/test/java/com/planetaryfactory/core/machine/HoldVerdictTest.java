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
        assertEquals(HoldVerdict.HELD, HoldVerdict.of(true, true, true, false));
        assertTrue(HoldVerdict.HELD.held());
    }

    @Test
    void anIdTheMachineCannotResolveIsRefused() {
        assertEquals(HoldVerdict.NOT_ASSEMBLING, HoldVerdict.of(false, true, true, false));
        assertFalse(HoldVerdict.NOT_ASSEMBLING.held());
    }

    @Test
    void aLockedRecipeIsRefused() {
        assertEquals(HoldVerdict.LOCKED, HoldVerdict.of(true, true, true, true));
    }

    @Test
    void anUnknownIdIsNotReportedAsLocked() {
        assertEquals(HoldVerdict.NOT_ASSEMBLING, HoldVerdict.of(false, true, true, true));
    }

    @Test
    void aRecipeNeedingATankTheMachineLacksIsRefused() {
        assertEquals(HoldVerdict.NOT_THIS_MACHINE, HoldVerdict.of(true, true, false, false));
        assertFalse(HoldVerdict.NOT_THIS_MACHINE.held());
    }

    /** Factorio lists no such recipe in tier 1 at all, so researching it changes nothing here. */
    @Test
    void aLockedRecipeOfTheWrongCategoryIsNotReportedAsLocked() {
        assertEquals(HoldVerdict.NOT_THIS_MACHINE, HoldVerdict.of(true, true, false, true));
    }

    @Test
    void everyRefusalNamesAMessage() {
        for (HoldVerdict verdict : HoldVerdict.values()) {
            assertEquals(!verdict.held(), verdict.messageKey() != null, verdict.name());
        }
    }

    /** A chemistry recipe on an Assembling Machine: Fill Recipe from another machine's tab (ADR-0096). */
    @Test
    void aRecipeOfAnotherMachinesTypeIsRefused() {
        assertEquals(HoldVerdict.NOT_THIS_TYPE, HoldVerdict.of(true, false, true, false));
        assertFalse(HoldVerdict.NOT_THIS_TYPE.held());
    }

    /** Another type is named before its fit or its lock: no research makes it this machine's. */
    @Test
    void anotherTypeOutranksFitAndLock() {
        assertEquals(HoldVerdict.NOT_THIS_TYPE, HoldVerdict.of(true, false, false, true));
    }

    @Test
    void anUnknownIdIsNotReportedAsAnotherType() {
        assertEquals(HoldVerdict.NOT_ASSEMBLING, HoldVerdict.of(false, false, false, false));
    }
}
