package com.planetaryfactory.core.gametest;

import static com.planetaryfactory.core.gametest.ChassisFixture.expectMoved;
import static com.planetaryfactory.core.gametest.ChassisFixture.fluid;

import java.util.List;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.energy.PoleTier;
import com.planetaryfactory.core.machine.AssemblingMachineBlockEntity;
import com.planetaryfactory.core.machine.AssemblingStall;
import com.planetaryfactory.core.machine.OilRefineryBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

/**
 * The Oil Refinery (#491, ADR-0096): the chassis on Oritech's Refinery and its two chamber layers.
 * Its placement is {@code PlacementPlanTests}' and its break {@code FootprintBreakTests}'.
 *
 * <p>The figures are typed, for {@code BoilerTests}' reason. A second advanced craft has no room for
 * its gas in a 100 mB tank, so a fed machine makes exactly one and stops.
 */
final class OilRefineryTests {

    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final Direction FACING = Direction.NORTH;
    private static final ChassisFixture CHASSIS =
            new ChassisFixture("Oil Refinery", PFBlocks.OIL_REFINERY_FOOTPRINT, ANCHOR, FACING);

    private static final String ADVANCED = "planetaryfactory:oil_processing/advanced_oil_processing";
    private static final String BASIC = "planetaryfactory:oil_processing/basic_oil_processing";
    private static final String PLASTIC = "planetaryfactory:chemistry/plastic_bar";
    private static final String CABLE = "planetaryfactory:assembling/copper_cable";

    private static final String CRUDE = "oritech:still_oil";
    private static final String HEAVY_OIL = "oritech:still_heavy_oil";
    private static final String LIGHT_OIL = "oritech:still_naphtha";
    private static final String PETROLEUM_GAS = "oritech:still_diesel";

    private static final int TICKS_PER_CRAFT = 100;
    private static final long FE_PER_CRAFT = 21_000L;
    private static final int WATER_PER_CRAFT = 50;
    private static final int CRUDE_PER_CRAFT = 100;
    private static final List<Integer> MADE_PER_CRAFT = List.of(25, 45, 55);
    private static final int OUTPUT_TANK_SIZE = 100;
    private static final long CHARGE = 50_000L;

    private OilRefineryTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("oil_refinery_runs_advanced_oil_processing_at_factorios_rate", 200,
                OilRefineryTests::runsAdvancedOilProcessing);
        for (int output = 0; output < MADE_PER_CRAFT.size(); output++) {
            int full = output;
            tests.test("oil_refinery_stalls_on_full_output_" + output, 200,
                    helper -> stallsOnAFullOutput(helper, full));
        }
        tests.test("oil_refinery_stalls_without_water", 200, OilRefineryTests::stallsWithoutWater);
        tests.test("oil_refinery_fluid_face_routes_by_the_held_recipe", 20,
                OilRefineryTests::fluidFaceRoutesByTheHeldRecipe);
        tests.test("oil_refinery_refuses_another_machines_recipe", 20,
                helper -> CHASSIS.refusesOtherRecipes(helper, placeWhole(helper), ADVANCED, List.of(PLASTIC, CABLE)));
        tests.test("oil_refinery_keeps_its_recipe_over_a_reload", 20,
                helper -> CHASSIS.keepsItsRecipeOverAReload(helper, placeWhole(helper), ADVANCED));
        tests.test("oil_refinery_is_fed_through_a_chamber", 100,
                helper -> CHASSIS.isFedByAPole(helper, placeWhole(helper),
                        ANCHOR.above(3 + PoleTier.VERTICAL_RADIUS), "only a chamber"));
        tests.test("oil_refinery_is_counted_once_by_a_pole", 100,
                helper -> CHASSIS.isFedByAPole(helper, placeWhole(helper), ANCHOR.east(2), "several blocks"));
        if (ModList.get().isLoaded("researchd")) {
            tests.test("oil_refinery_stalls_on_a_locked_recipe", 200, OilRefineryTests::stallsOnALockedRecipe);
        }
    }

    /** Fed for two crafts, it makes one at the recipe's rate and cost, then stops on its gas. */
    private static void runsAdvancedOilProcessing(GameTestHelper helper) {
        OilRefineryBlockEntity machine = placeWhole(helper);
        feed(helper, machine, 2 * WATER_PER_CRAFT, 2 * CRUDE_PER_CRAFT);
        machine.energyStorage.set(CHARGE);
        helper.runAfterDelay(TICKS_PER_CRAFT + 1, () -> {
            for (int output = 0; output < MADE_PER_CRAFT.size(); output++) {
                long made = machine.outputTankAmount(output);
                if (made != MADE_PER_CRAFT.get(output)) {
                    helper.fail("output tank " + output + " holds " + made + " mB after one craft's time, expected "
                            + MADE_PER_CRAFT.get(output), ANCHOR);
                }
            }
            if (machine.tank().getAmountAsLong(0) != WATER_PER_CRAFT
                    || machine.tank().getAmountAsLong(1) != CRUDE_PER_CRAFT) {
                helper.fail("one craft left " + machine.tank().getAmountAsLong(0) + " mB of water and "
                        + machine.tank().getAmountAsLong(1) + " of crude", ANCHOR);
            }
            long spent = CHARGE - machine.energyStorage.getAmountAsLong();
            if (spent != FE_PER_CRAFT) {
                helper.fail("one craft drew " + spent + " FE, expected " + FE_PER_CRAFT, ANCHOR);
            }
            if (machine.stall() != AssemblingStall.OUTPUT_FULL) {
                helper.fail("after one craft the gas tank has no room for 55 mB, yet the machine reports "
                        + machine.stall(), ANCHOR);
            }
            helper.succeed();
        });
    }

    /** One output tank too full for a craft's share stalls the machine, whichever tank it is. */
    private static void stallsOnAFullOutput(GameTestHelper helper, int output) {
        OilRefineryBlockEntity machine = placeWhole(helper);
        feed(helper, machine, 100, 200);
        FluidResource fluid = FluidResource.of(fluid(List.of(HEAVY_OIL, LIGHT_OIL, PETROLEUM_GAS).get(output)));
        int filled = OUTPUT_TANK_SIZE - MADE_PER_CRAFT.get(output) + 1;
        ((FluidStacksResourceHandler) machine.tank()).set(CHASSIS.outputTank(machine, output), fluid, filled);
        machine.energyStorage.set(CHARGE);
        helper.runAfterDelay(TICKS_PER_CRAFT + 1, () -> {
            assertStalled(helper, machine, AssemblingStall.OUTPUT_FULL, 100, 200);
            helper.succeed();
        });
    }

    private static void stallsWithoutWater(GameTestHelper helper) {
        OilRefineryBlockEntity machine = placeWhole(helper);
        feed(helper, machine, 0, 200);
        machine.energyStorage.set(CHARGE);
        helper.runAfterDelay(TICKS_PER_CRAFT + 1, () -> {
            assertStalled(helper, machine, AssemblingStall.NO_FLUID, 0, 200);
            helper.succeed();
        });
    }

    /** Basic oil processing, the one a research locks (#206). */
    private static void stallsOnALockedRecipe(GameTestHelper helper) {
        OilRefineryBlockEntity machine = placeWhole(helper);
        ResearchTeams.placedBy(machine, ResearchTeams.create(helper));
        CHASSIS.hold(helper, machine, BASIC);
        expectMoved(helper, ANCHOR, "crude", 200, CHASSIS.fluidFace(helper, ANCHOR),
                (f, tx) -> f.insert(FluidResource.of(fluid(CRUDE)), 200, tx));
        machine.energyStorage.set(CHARGE);
        helper.runAfterDelay(TICKS_PER_CRAFT + 1, () -> {
            CHASSIS.assertStalled(helper, machine, AssemblingStall.LOCKED, CHARGE, BASIC);
            if (machine.tank().getAmountAsLong(0) != 200) {
                helper.fail("a locked machine took crude", ANCHOR);
            }
            helper.succeed();
        });
    }

    /**
     * Through the anchor and a top chamber block, on the slot-less overloads a pipe uses: water and
     * crude each reach their own tank, other fluids are refused, the inputs never come back out and
     * each output does.
     */
    private static void fluidFaceRoutesByTheHeldRecipe(GameTestHelper helper) {
        OilRefineryBlockEntity machine = placeWhole(helper);
        CHASSIS.hold(helper, machine, ADVANCED);
        FluidResource water = FluidResource.of(Fluids.WATER);
        FluidResource crude = FluidResource.of(fluid(CRUDE));
        FluidResource lava = FluidResource.of(Fluids.LAVA);
        List<FluidResource> outputs = List.of(FluidResource.of(fluid(HEAVY_OIL)), FluidResource.of(fluid(LIGHT_OIL)),
                FluidResource.of(fluid(PETROLEUM_GAS)));
        for (int output = 0; output < outputs.size(); output++) {
            ((FluidStacksResourceHandler) machine.tank()).set(CHASSIS.outputTank(machine, output), outputs.get(output), 90);
        }
        int step = 0;
        for (BlockPos at : List.of(ANCHOR, ANCHOR.above(3))) {
            ResourceHandler<FluidResource> face = CHASSIS.fluidFace(helper, at);
            if (face == null) {
                helper.fail("no fluid face at " + at, at);
                return;
            }
            step++;
            expectMoved(helper, at, "water", 10, face, (f, tx) -> f.insert(water, 10, tx));
            expectMoved(helper, at, "crude", 10, face, (f, tx) -> f.insert(crude, 10, tx));
            expectMoved(helper, at, "lava", 0, face, (f, tx) -> f.insert(lava, 10, tx));
            expectMoved(helper, at, "heavy oil in", 0, face, (f, tx) -> f.insert(outputs.get(0), 10, tx));
            expectMoved(helper, at, "water back out", 0, face, (f, tx) -> f.extract(water, 10, tx));
            expectMoved(helper, at, "crude back out", 0, face, (f, tx) -> f.extract(crude, 10, tx));
            for (FluidResource output : outputs) {
                expectMoved(helper, at, output + " out", 30, face, (f, tx) -> f.extract(output, 30, tx));
            }
            if (!machine.tank().getResource(0).equals(water) || machine.tank().getAmountAsLong(0) != 10L * step
                    || !machine.tank().getResource(1).equals(crude) || machine.tank().getAmountAsLong(1) != 10L * step) {
                helper.fail("through " + at + " the input tanks hold " + machine.tank().getAmountAsLong(0) + " mB of "
                        + machine.tank().getResource(0) + " and " + machine.tank().getAmountAsLong(1) + " mB of "
                        + machine.tank().getResource(1), at);
                return;
            }
        }
        helper.succeed();
    }

    /** A stall also takes no water or crude. */
    private static void assertStalled(GameTestHelper helper, AssemblingMachineBlockEntity machine,
            AssemblingStall expected, int water, int crude) {
        CHASSIS.assertStalled(helper, machine, expected, CHARGE, ADVANCED);
        if (machine.tank().getAmountAsLong(0) != water || machine.tank().getAmountAsLong(1) != crude) {
            helper.fail("a machine stalled on " + expected + " took input: " + machine.tank().getAmountAsLong(0)
                    + " mB of water, " + machine.tank().getAmountAsLong(1) + " of crude", ANCHOR);
        }
    }

    /** Both fluids through the face, so a face refusing either fails here. */
    private static void feed(GameTestHelper helper, AssemblingMachineBlockEntity machine, int water, int crude) {
        CHASSIS.hold(helper, machine, ADVANCED);
        ResourceHandler<FluidResource> face = CHASSIS.fluidFace(helper, ANCHOR);
        if (water > 0) {
            expectMoved(helper, ANCHOR, "water", water, face,
                    (f, tx) -> f.insert(FluidResource.of(Fluids.WATER), water, tx));
        }
        expectMoved(helper, ANCHOR, "crude", crude, face, (f, tx) -> f.insert(FluidResource.of(fluid(CRUDE)), crude, tx));
    }

    private static OilRefineryBlockEntity placeWhole(GameTestHelper helper) {
        return CHASSIS.placeWhole(helper, OilRefineryBlockEntity.class);
    }
}
