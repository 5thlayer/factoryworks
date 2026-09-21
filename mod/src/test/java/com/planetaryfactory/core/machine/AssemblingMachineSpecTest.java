package com.planetaryfactory.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The Assembling Machine's rate (#328, ADR-0029): {@code assembling-machine-1}'s
 * {@code crafting_speed} 0.5 and 75 kW, at ADR-0060's 1 FE = 100 J.
 *
 * <p>Every expected figure is typed from the corpus by hand rather than read off the spec, so the
 * test cannot agree with the implementation by construction.
 */
class AssemblingMachineSpecTest {

    /** copper-cable: 0.5 s in the corpus, emitted as 10 ticks, observed at 1 s on tier 1. */
    @Test
    void aHalfSecondRecipeTakesOneSecondOnTierOne() {
        assertEquals(20, AssemblingMachineSpec.durationTicks(10, 1.0f));
    }

    /** The 2 s median: 40 ticks emitted, 80 observed. */
    @Test
    void theMedianRecipeTakesFourSeconds() {
        assertEquals(80, AssemblingMachineSpec.durationTicks(40, 1.0f));
    }

    /** 75 kW for 1 s is 75 kJ, which is 750 FE. */
    @Test
    void aCraftCostsSeventyFiveKilowattsOverItsDuration() {
        assertEquals(750, AssemblingMachineSpec.fePerCraft(10, 1.0f));
        assertEquals(3000, AssemblingMachineSpec.fePerCraft(40, 1.0f));
    }

    /** 37.5 FE/t does not divide: the ticks alternate and the craft still pays exactly 750. */
    @Test
    void theDrawOverACraftSumsToItsCostExactly() {
        long total = 0;
        for (int tick = 0; tick < 20; tick++) {
            long fe = AssemblingMachineSpec.feForTick(tick, 20, 750);
            assertEquals(true, fe == 37 || fe == 38, "tick " + tick + " drew " + fe);
            total += fe;
        }
        assertEquals(750, total);
    }

    /** Oritech's speed addon multiplies the duration: 0.5 halves it, and the craft costs the same. */
    @Test
    void theDrawIsSeventyFiveKilowattsInTenthsOfAnFePerTick() {
        assertEquals(375, AssemblingMachineSpec.drawTenths(750, 20));
        assertEquals(375, AssemblingMachineSpec.drawTenths(3000, 80));
    }

    @Test
    void aFasterMachineDrawsHarderAndNoRecipeDrawsNothing() {
        assertEquals(750, AssemblingMachineSpec.drawTenths(750, 10));
        assertEquals(0, AssemblingMachineSpec.drawTenths(750, 0));
    }

    @Test
    void aSpeedAddonShortensTheCraftButNotItsCost() {
        assertEquals(10, AssemblingMachineSpec.durationTicks(10, 0.5f));
        assertEquals(750, AssemblingMachineSpec.fePerCraft(10, 1.0f));
        long total = 0;
        for (int tick = 0; tick < 10; tick++) {
            total += AssemblingMachineSpec.feForTick(tick, 10, 750);
        }
        assertEquals(750, total);
    }

    /** Oritech's efficiency multiplier scales the energy and leaves the time alone. */
    @Test
    void anEfficiencyAddonScalesTheCostOnly() {
        assertEquals(600, AssemblingMachineSpec.fePerCraft(10, 0.8f));
        assertEquals(20, AssemblingMachineSpec.durationTicks(10, 1.0f));
    }

    /** hazard-concrete at 10 ticks under a large speed bonus still costs one tick. */
    @Test
    void noCraftIsFasterThanOneTick() {
        assertEquals(1, AssemblingMachineSpec.durationTicks(1, 0.01f));
    }

    /** A tick past the craft's end draws nothing rather than a negative or a remainder. */
    @Test
    void progressPastTheEndDrawsNothing() {
        assertEquals(0, AssemblingMachineSpec.feForTick(20, 20, 750));
    }

    /** Tier 1's {@code crafting_categories}: no fluid box, so no {@code crafting-with-fluid}. */
    @Test
    void tierOneCraftsNoFluidRecipe() {
        assertTrue(AssemblingMachineSpec.crafts("crafting"));
        assertTrue(AssemblingMachineSpec.crafts("advanced-crafting"));
        assertFalse(AssemblingMachineSpec.crafts("crafting-with-fluid"));
    }
}
