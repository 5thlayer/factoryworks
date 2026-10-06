package com.factoryworks.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * A crafting machine's rate from its spec (#328, ADR-0029): {@code crafting_speed} and
 * {@code energy_usage}, at ADR-0060's 1 FE = 100 J.
 *
 * <p>The arithmetic is what the Oil Refinery runs on, and it is exercised at
 * the three speeds Factorio's Assembling Machines have, which are Craftworks' now (ADR-0118), so each
 * is a local spec. Every expected figure is typed from the corpus by hand rather than read off a
 * spec, so the test cannot agree with the implementation by construction.
 */
class AssemblingMachineSpecTest {

    private static final MachineSpec ONE = rate("speed-0.5", 0.5, 75_000L);
    private static final MachineSpec TWO = rate("speed-0.75", 0.75, 150_000L);
    private static final MachineSpec THREE = rate("speed-1.25", 1.25, 375_000L);

    private static MachineSpec rate(String name, double craftingSpeed, long watts) {
        return new MachineSpec(name, "craftworks:assembling", Set.of("crafting"), craftingSpeed, watts, 0L, name,
                0, 0, List.of(), List.of(), List.of());
    }

    /** copper-cable: 0.5 s in the corpus, emitted as 10 ticks, observed at 1 s at speed 0.5. */
    @Test
    void aHalfSecondRecipeTakesOneSecondAtHalfSpeed() {
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
    void aThreeQuarterSpeedMachineDrawsOneHundredFiftyKilowatts() {
        assertEquals(14, AssemblingMachineSpec.durationTicks(TWO, 10, 1.0f));
        assertEquals(1000, AssemblingMachineSpec.fePerCraft(TWO, 10, 1.0f));
        assertEquals(4000, AssemblingMachineSpec.fePerCraft(TWO, 40, 1.0f));
    }

    /** 0.5 s at speed 1.25 is 8 ticks; 375 kW is 187.5 FE/t, 1,500 FE over the craft, exactly. */
    @Test
    void aFastMachineCarriesItsHalfAnFeATickExactly() {
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

    /** 2 fluids in and always 3 out, with no item slots at all (ADR-0096). */
    @Test
    void theOilRefineryHasThreeOutputTanksAndNoSlots() {
        MachineSpec refinery = MachineSpecs.get().spec("oil-refinery");
        assertEquals("factoryworks:oil_processing", refinery.recipeType());
        assertEquals(0, refinery.itemInputs());
        assertEquals(0, refinery.itemOutputs());
        assertEquals(List.of(1000, 1000), refinery.fluidInputs());
        assertEquals(List.of(100, 100, 100), refinery.fluidOutputs());
        assertEquals(List.of(100, 100, 100), refinery.fluidOutputBoxes());
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
        MachineSpec refinery = MachineSpecs.get().spec("oil-refinery");
        assertEquals(0, refinery.fluidInputVolume(2));
        assertEquals(100, refinery.fluidOutputVolume(2));
        assertEquals(0, refinery.fluidOutputVolume(3));
    }

    /** A recipe fits when the machine has a slot or tank for every input and output. */
    @Test
    void aRecipeFitsOnlyWhereEveryInputAndOutputHasASlotOrTank() {
        MachineSpec refinery = MachineSpecs.get().spec("oil-refinery");
        assertTrue(refinery.fits(0, 0, 2, 3));
        assertFalse(refinery.fits(1, 0, 2, 3), "an item ingredient");
        assertFalse(refinery.fits(0, 0, 3, 3), "a third fluid ingredient");
        assertFalse(refinery.fits(0, 0, 2, 4), "a fourth fluid result");
    }
}
