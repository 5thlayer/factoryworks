package com.factoryworks.core.gametest;

import static com.factoryworks.core.gametest.Faces.expectMoved;
import static com.factoryworks.core.gametest.ChassisFixture.fluid;

import java.util.List;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.machine.AssemblingMachineBlockEntity;
import com.factoryworks.core.machine.AssemblingStall;
import com.factoryworks.core.machine.OilRefineryBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;
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

    private static final String ADVANCED = "factoryworks:oil_processing/advanced_oil_processing";
    private static final String BASIC = "factoryworks:oil_processing/basic_oil_processing";
    private static final String PLASTIC = "factoryworks:chemistry/plastic_bar";

    private static final String CRUDE = "factoryworks:crude_oil";
    private static final String HEAVY_OIL = "factoryworks:heavy_oil";
    private static final String LIGHT_OIL = "factoryworks:light_oil";
    private static final String PETROLEUM_GAS = "factoryworks:petroleum_gas";

    private static final int TICKS_PER_CRAFT = 100;
    private static final long FE_PER_CRAFT = 21_000L;
    private static final int WATER_PER_CRAFT = 50;
    private static final int CRUDE_PER_CRAFT = 100;
    private static final List<Integer> MADE_PER_CRAFT = List.of(25, 45, 55);
    /** Advanced oil processing's output boxes in Factorio 2.1.20 (#520), typed from the probe. */
    private static final List<Integer> OUTPUT_TANK_SIZES = List.of(100, 135, 165);
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
        tests.test("oil_refinery_output_tanks_are_sized_by_the_recipe", 20,
                OilRefineryTests::outputTanksAreSizedByTheRecipe);
        tests.test("oil_refinery_stalls_without_water", 200, OilRefineryTests::stallsWithoutWater);
        tests.test("oil_refinery_fluid_face_routes_by_the_held_recipe", 20,
                OilRefineryTests::fluidFaceRoutesByTheHeldRecipe);
        tests.test("oil_refinery_refuses_another_machines_recipe", 20,
                helper -> CHASSIS.refusesOtherRecipes(helper, placeWhole(helper), ADVANCED, List.of(PLASTIC)));
        tests.test("oil_refinery_keeps_its_recipe_over_a_reload", 20,
                helper -> CHASSIS.keepsItsRecipeOverAReload(helper, placeWhole(helper), ADVANCED));
        tests.test("oil_refinery_is_fed_through_a_chamber", 100,
                helper -> CHASSIS.isFedByAPole(helper, placeWhole(helper),
                        ANCHOR.above(3 + ChassisFixture.POLE_VERTICAL_REACH), "only a chamber"));
        tests.test("oil_refinery_is_counted_once_by_a_pole", 100,
                helper -> CHASSIS.isFedByAPole(helper, placeWhole(helper), ANCHOR.east(2), "several blocks"));
    }

    /** Fed for one craft and a half, it makes one at the recipe's rate and cost, then stops on its water. */
    private static void runsAdvancedOilProcessing(GameTestHelper helper) {
        OilRefineryBlockEntity machine = placeWhole(helper);
        feed(helper, machine, WATER_PER_CRAFT + WATER_PER_CRAFT / 2, 2 * CRUDE_PER_CRAFT);
        machine.energyStorage.set(CHARGE);
        helper.runAfterDelay(TICKS_PER_CRAFT + 1, () -> {
            for (int output = 0; output < MADE_PER_CRAFT.size(); output++) {
                long made = machine.outputTankAmount(output);
                if (made != MADE_PER_CRAFT.get(output)) {
                    helper.fail("output tank " + output + " holds " + made + " mB after one craft's time, expected "
                            + MADE_PER_CRAFT.get(output), ANCHOR);
                }
            }
            if (machine.tank().getAmountAsLong(0) != WATER_PER_CRAFT / 2
                    || machine.tank().getAmountAsLong(1) != CRUDE_PER_CRAFT) {
                helper.fail("one craft left " + machine.tank().getAmountAsLong(0) + " mB of water and "
                        + machine.tank().getAmountAsLong(1) + " of crude", ANCHOR);
            }
            long spent = CHARGE - machine.energyStorage.getAmountAsLong();
            if (spent != FE_PER_CRAFT) {
                helper.fail("one craft drew " + spent + " FE, expected " + FE_PER_CRAFT, ANCHOR);
            }
            if (machine.stall() != AssemblingStall.NO_FLUID) {
                helper.fail("after one craft 25 mB of water is short of a craft, yet the machine reports "
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
        int filled = OUTPUT_TANK_SIZES.get(output) - MADE_PER_CRAFT.get(output) + 1;
        ((FluidStacksResourceHandler) machine.tank()).set(CHASSIS.outputTank(machine, output), fluid, filled);
        machine.energyStorage.set(CHARGE);
        helper.runAfterDelay(TICKS_PER_CRAFT + 1, () -> {
            assertStalled(helper, machine, AssemblingStall.OUTPUT_FULL, 100, 200);
            helper.succeed();
        });
    }

    /** What each output tank takes, through the tank itself, with advanced and the pinned basic held. */
    private static void outputTanksAreSizedByTheRecipe(GameTestHelper helper) {
        OilRefineryBlockEntity machine = placeWhole(helper);
        CHASSIS.hold(helper, machine, ADVANCED);
        List<String> fluids = List.of(HEAVY_OIL, LIGHT_OIL, PETROLEUM_GAS);
        for (int output = 0; output < fluids.size(); output++) {
            expectOutputTank(helper, machine, output, fluids.get(output), OUTPUT_TANK_SIZES.get(output));
        }
        CHASSIS.hold(helper, machine, BASIC);
        expectOutputTank(helper, machine, 0, PETROLEUM_GAS, 135);
        helper.succeed();
    }

    private static void expectOutputTank(GameTestHelper helper, OilRefineryBlockEntity machine, int output,
            String fluid, int size) {
        int tank = CHASSIS.outputTank(machine, output);
        ((FluidStacksResourceHandler) machine.tank()).set(tank, FluidResource.EMPTY, 0);
        int took = Faces.simulate(machine.tank(), (f, tx) -> f.insert(tank, FluidResource.of(fluid(fluid)), 1000, tx));
        if (took != size) {
            helper.fail("output tank " + output + " took " + took + " mB of " + fluid + ", expected " + size, ANCHOR);
        }
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
            ResourceHandler<FluidResource> face = Faces.fluid(helper, at);
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
        ResourceHandler<FluidResource> face = Faces.fluid(helper, ANCHOR);
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
