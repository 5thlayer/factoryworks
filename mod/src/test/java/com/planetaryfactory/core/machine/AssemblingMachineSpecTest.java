package com.planetaryfactory.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static com.planetaryfactory.core.machine.AssemblingTier.ONE;
import static com.planetaryfactory.core.machine.AssemblingTier.THREE;
import static com.planetaryfactory.core.machine.AssemblingTier.TWO;

import org.junit.jupiter.api.Test;

/**
 * The Assembling Machine's rate per tier (#328, #295, ADR-0029, ADR-0075): each
 * {@code assembling-machine-N}'s {@code crafting_speed} and {@code energy_usage}, at ADR-0060's
 * 1 FE = 100 J.
 *
 * <p>Every expected figure is typed from the corpus by hand rather than read off the spec, so the
 * test cannot agree with the implementation by construction.
 */
class AssemblingMachineSpecTest {

    /** copper-cable: 0.5 s in the corpus, emitted as 10 ticks, observed at 1 s on tier 1. */
    @Test
    void aHalfSecondRecipeTakesOneSecondOnTierOne() {
        assertEquals(20, AssemblingMachineSpec.durationTicks(ONE, 10, 1.0f));
    }

    /** The 2 s median: 40 ticks emitted, 80 observed. */
    @Test
    void theMedianRecipeTakesFourSeconds() {
        assertEquals(80, AssemblingMachineSpec.durationTicks(ONE, 40, 1.0f));
    }

    /** 75 kW for 1 s is 75 kJ, which is 750 FE. */
    @Test
    void aCraftCostsSeventyFiveKilowattsOverItsDuration() {
        assertEquals(750, AssemblingMachineSpec.fePerCraft(ONE, 10, 1.0f));
        assertEquals(3000, AssemblingMachineSpec.fePerCraft(ONE, 40, 1.0f));
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
        assertEquals(10, AssemblingMachineSpec.durationTicks(ONE, 10, 0.5f));
        assertEquals(750, AssemblingMachineSpec.fePerCraft(ONE, 10, 1.0f));
        long total = 0;
        for (int tick = 0; tick < 10; tick++) {
            total += AssemblingMachineSpec.feForTick(tick, 10, 750);
        }
        assertEquals(750, total);
    }

    /** Oritech's efficiency multiplier scales the energy and leaves the time alone. */
    @Test
    void anEfficiencyAddonScalesTheCostOnly() {
        assertEquals(600, AssemblingMachineSpec.fePerCraft(ONE, 10, 0.8f));
        assertEquals(20, AssemblingMachineSpec.durationTicks(ONE, 10, 1.0f));
    }

    /** hazard-concrete at 10 ticks under a large speed bonus still costs one tick. */
    @Test
    void noCraftIsFasterThanOneTick() {
        assertEquals(1, AssemblingMachineSpec.durationTicks(ONE, 1, 0.01f));
    }

    /** A tick past the craft's end draws nothing rather than a negative or a remainder. */
    @Test
    void progressPastTheEndDrawsNothing() {
        assertEquals(0, AssemblingMachineSpec.feForTick(20, 20, 750));
    }

    /** 0.5 s at speed 0.75 is 13.3 ticks, run in 14; 150 kW over the unrounded 2/3 s is 1,000 FE. */
    @Test
    void tierTwoRunsAtThreeQuartersAndOneHundredFiftyKilowatts() {
        assertEquals(14, AssemblingMachineSpec.durationTicks(TWO, 10, 1.0f));
        assertEquals(1000, AssemblingMachineSpec.fePerCraft(TWO, 10, 1.0f));
        assertEquals(4000, AssemblingMachineSpec.fePerCraft(TWO, 40, 1.0f));
    }

    /** 0.5 s at speed 1.25 is 8 ticks; 375 kW is 187.5 FE/t, 1,500 FE over the craft, exactly. */
    @Test
    void tierThreeCarriesItsHalfAnFeATickExactly() {
        assertEquals(8, AssemblingMachineSpec.durationTicks(THREE, 10, 1.0f));
        assertEquals(1500, AssemblingMachineSpec.fePerCraft(THREE, 10, 1.0f));
        long total = 0;
        for (int tick = 0; tick < 8; tick++) {
            long fe = AssemblingMachineSpec.feForTick(tick, 8, 1500);
            assertTrue(fe == 187 || fe == 188, "tick " + tick + " drew " + fe);
            total += fe;
        }
        assertEquals(1500, total);
        assertEquals(1875, AssemblingMachineSpec.drawTenths(1500, 8));
    }

    /** Tier 1's {@code crafting_categories}: no fluid box, so no {@code crafting-with-fluid}. */
    @Test
    void tierOneCraftsNoFluidRecipe() {
        assertTrue(ONE.crafts("crafting"));
        assertTrue(ONE.crafts("advanced-crafting"));
        assertFalse(ONE.crafts("crafting-with-fluid"));
    }

    /** Tiers 2 and 3 add {@code crafting-with-fluid}, and neither takes a chemical plant's category. */
    @Test
    void tiersTwoAndThreeCraftWithAFluid() {
        for (AssemblingTier tier : new AssemblingTier[] {TWO, THREE}) {
            assertTrue(tier.crafts("crafting"), tier + " crafting");
            assertTrue(tier.crafts("advanced-crafting"), tier + " advanced-crafting");
            assertTrue(tier.crafts("crafting-with-fluid"), tier + " crafting-with-fluid");
            assertFalse(tier.crafts("chemistry"), tier + " chemistry");
        }
    }

    /** Only the fluid tiers have a tank. */
    @Test
    void onlyTiersTwoAndThreeHaveATank() {
        assertFalse(ONE.hasFluidInput());
        assertTrue(TWO.hasFluidInput());
        assertTrue(THREE.hasFluidInput());
    }

    /** The ids the item map names, derived from the tier. */
    @Test
    void eachTierIsItsOwnBlock() {
        assertEquals("assembling_machine", ONE.blockName());
        assertEquals("assembling_machine_2", TWO.blockName());
        assertEquals("assembling_machine_3", THREE.blockName());
        assertEquals("assembling_machine_2_part", TWO.partBlockName());
    }
}
