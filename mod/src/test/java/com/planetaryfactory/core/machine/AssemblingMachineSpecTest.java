package com.planetaryfactory.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

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

    private static final MachineSpec ONE = AssemblingTier.ONE.spec();
    private static final MachineSpec TWO = AssemblingTier.TWO.spec();
    private static final MachineSpec THREE = AssemblingTier.THREE.spec();

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
        assertTrue(AssemblingTier.ONE.crafts("crafting"));
        assertTrue(AssemblingTier.ONE.crafts("advanced-crafting"));
        assertFalse(AssemblingTier.ONE.crafts("crafting-with-fluid"));
    }

    /** Tiers 2 and 3 add {@code crafting-with-fluid}, and neither takes a chemical plant's category. */
    @Test
    void tiersTwoAndThreeCraftWithAFluid() {
        for (AssemblingTier tier : new AssemblingTier[] {AssemblingTier.TWO, AssemblingTier.THREE}) {
            assertTrue(tier.crafts("crafting"), tier + " crafting");
            assertTrue(tier.crafts("advanced-crafting"), tier + " advanced-crafting");
            assertTrue(tier.crafts("crafting-with-fluid"), tier + " crafting-with-fluid");
            assertFalse(tier.crafts("chemistry"), tier + " chemistry");
        }
    }

    /** Only the fluid tiers have a tank, of 1,000 mB, and no tier has an output tank. */
    @Test
    void onlyTiersTwoAndThreeHaveATank() {
        assertEquals(List.of(), ONE.fluidInputs());
        assertEquals(List.of(1000), TWO.fluidInputs());
        assertEquals(List.of(1000), THREE.fluidInputs());
        for (MachineSpec tier : List.of(ONE, TWO, THREE)) {
            assertEquals(List.of(), tier.fluidOutputs(), tier.name());
        }
    }

    /** ADR-0071's four input slots and one output, on every tier. */
    @Test
    void everyTierHasFourInputsAndOneOutput() {
        for (MachineSpec tier : List.of(ONE, TWO, THREE)) {
            assertEquals(4, tier.itemInputs(), tier.name());
            assertEquals(1, tier.itemOutputs(), tier.name());
        }
    }

    /** Each tier's {@code crafting_speed}, {@code energy_usage} and {@code drain}, in watts. */
    @Test
    void eachTierReadsItsSpeedAndPowerFromTheCorpus() {
        assertEquals(0.5, ONE.craftingSpeed());
        assertEquals(0.75, TWO.craftingSpeed());
        assertEquals(1.25, THREE.craftingSpeed());
        assertEquals(75_000L, ONE.watts());
        assertEquals(150_000L, TWO.watts());
        assertEquals(375_000L, THREE.watts());
        assertEquals(2_500L, ONE.drainWatts());
        assertEquals(5_000L, TWO.drainWatts());
        assertEquals(12_500L, THREE.drainWatts());
    }

    @Test
    void theThreeTiersHoldAssemblingRecipesAndReplaceEachOther() {
        for (MachineSpec tier : List.of(ONE, TWO, THREE)) {
            assertEquals("planetaryfactory:assembling", tier.recipeType(), tier.name());
            assertEquals("assembling-machine", tier.replaceGroup(), tier.name());
        }
    }

    /** 2 items and 2 fluids in, 1 of each out: the entity's second output box is one no recipe fills (ADR-0096). */
    @Test
    void theChemicalPlantsTanksFollowItsRecipes() {
        MachineSpec plant = MachineSpecs.get().spec("chemical-plant");
        assertEquals("planetaryfactory:chemistry", plant.recipeType());
        assertEquals(Set.of("chemistry"), plant.categories());
        assertEquals(2, plant.itemInputs());
        assertEquals(1, plant.itemOutputs());
        assertEquals(List.of(1000, 1000), plant.fluidInputs());
        assertEquals(List.of(100), plant.fluidOutputs());
        assertEquals(1.0, plant.craftingSpeed());
        assertEquals(210_000L, plant.watts());
        assertEquals(7_000L, plant.drainWatts());
        assertEquals("chemical-plant", plant.replaceGroup());
    }

    /** 2 fluids in and always 3 out, with no item slots at all (ADR-0096). */
    @Test
    void theOilRefineryHasThreeOutputTanksAndNoSlots() {
        MachineSpec refinery = MachineSpecs.get().spec("oil-refinery");
        assertEquals("planetaryfactory:oil_processing", refinery.recipeType());
        assertEquals(0, refinery.itemInputs());
        assertEquals(0, refinery.itemOutputs());
        assertEquals(List.of(1000, 1000), refinery.fluidInputs());
        assertEquals(List.of(100, 100, 100), refinery.fluidOutputs());
        assertEquals(420_000L, refinery.watts());
        assertEquals(14_000L, refinery.drainWatts());
        assertEquals("oil-refinery", refinery.replaceGroup());
    }

    /** 420 kW for basic oil processing's 5 s is 2.1 MJ, 21,000 FE. */
    @Test
    void aRefineryCraftCostsFourHundredTwentyKilowattsOverItsDuration() {
        MachineSpec refinery = MachineSpecs.get().spec("oil-refinery");
        assertEquals(100, AssemblingMachineSpec.durationTicks(refinery, 100, 1.0f));
        assertEquals(21_000, AssemblingMachineSpec.fePerCraft(refinery, 100, 1.0f));
    }

    /** Tanks are laid out for the widest machine: two in, three out. */
    @Test
    void theTankLayoutIsTheWidestMachines() {
        assertEquals(2, MachineSpecs.get().maxFluidInputs());
        assertEquals(3, MachineSpecs.get().maxFluidOutputs());
    }

    @Test
    void aTankPastTheMachinesLastHasNoRoom() {
        assertEquals(0, TWO.fluidInputVolume(1));
        assertEquals(0, ONE.fluidInputVolume(0));
        assertEquals(100, MachineSpecs.get().spec("oil-refinery").fluidOutputVolume(2));
        assertEquals(0, MachineSpecs.get().spec("chemical-plant").fluidOutputVolume(1));
    }

    /** A recipe fits when the machine has a slot or tank for every input and output. */
    @Test
    void aRecipeFitsOnlyWhereEveryInputAndOutputHasASlotOrTank() {
        assertTrue(ONE.fits(4, 1, 0, 0));
        assertFalse(ONE.fits(1, 1, 1, 0), "concrete's water on tier 1");
        assertTrue(TWO.fits(1, 1, 1, 0));
        assertFalse(TWO.fits(5, 1, 0, 0));
        assertFalse(TWO.fits(1, 0, 0, 1), "a fluid result on an assembler");
        MachineSpec refinery = MachineSpecs.get().spec("oil-refinery");
        assertTrue(refinery.fits(0, 0, 2, 3));
        assertFalse(refinery.fits(1, 0, 2, 3));
    }

    /** The ids the item map names, derived from the tier. */
    @Test
    void eachTierIsItsOwnBlock() {
        assertEquals("assembling_machine", AssemblingTier.ONE.blockName());
        assertEquals("assembling_machine_2", AssemblingTier.TWO.blockName());
        assertEquals("assembling_machine_3", AssemblingTier.THREE.blockName());
        assertEquals("assembling_machine_2_part", AssemblingTier.TWO.partBlockName());
    }
}
