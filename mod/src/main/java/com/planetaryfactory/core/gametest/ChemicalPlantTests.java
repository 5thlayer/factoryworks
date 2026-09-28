package com.planetaryfactory.core.gametest;

import java.util.List;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.energy.PoleTier;
import com.planetaryfactory.core.energy.SupplyAreaPoleBlockEntity;
import com.planetaryfactory.core.machine.AssemblingMachineBlockEntity;
import com.planetaryfactory.core.machine.AssemblingMachineMenu;
import com.planetaryfactory.core.machine.AssemblingStall;
import com.planetaryfactory.core.machine.HeldRecipe;
import com.planetaryfactory.core.machine.HoldVerdict;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;
import rearth.oritech.util.Geometry;

/**
 * The Chemical Plant (#490, ADR-0096): the Assembling Machine's chassis on the Centrifuge's 1x1x2.
 * Its placement is {@code PlacementPlanTests}' and its break {@code FootprintBreakTests}'.
 *
 * <p>Plastic is the fixture: 20 mB of Petroleum Gas and 1 coal into 2 plastic bars in 1 s, which
 * speed 1 runs in 20 ticks for 210 kW's 2,100 FE. Typed, for {@code BoilerTests}' reason.
 */
final class ChemicalPlantTests {

    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final Direction FACING = Direction.NORTH;

    private static final String PLASTIC = "planetaryfactory:chemistry/plastic_bar";
    private static final String CRACKING = "planetaryfactory:chemistry/heavy_oil_cracking";
    private static final String CABLE = "planetaryfactory:assembling/copper_cable";
    private static final String BASIC_OIL = "planetaryfactory:oil_processing/basic_oil_processing";

    private static final String PETROLEUM_GAS = "oritech:still_diesel";
    private static final String HEAVY_OIL = "oritech:still_heavy_oil";
    private static final String LIGHT_OIL = "oritech:still_naphtha";

    private static final int TICKS_PER_CRAFT = 20;
    private static final long FE_PER_CRAFT = 2_100L;
    private static final int GAS_PER_CRAFT = 20;
    private static final long CHARGE = 20_000L;
    private static final int OUTPUT = AssemblingMachineBlockEntity.OUTPUT;

    /** A pole rescans at most this many ticks after a machine appears (EnergyFaceTests' figure). */
    private static final int RESCAN_INTERVAL = 40;

    private ChemicalPlantTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("chemical_plant_crafts_plastic_at_factorios_rate", 100, ChemicalPlantTests::craftsPlastic);
        tests.test("chemical_plant_stalls_on_a_full_output", 100, ChemicalPlantTests::stallsOnAFullOutput);
        tests.test("chemical_plant_stalls_unfed", 100, ChemicalPlantTests::stallsUnfed);
        tests.test("chemical_plant_fluid_face_routes_by_the_held_recipe", 20,
                ChemicalPlantTests::fluidFaceRoutesByTheHeldRecipe);
        tests.test("chemical_plant_refuses_another_machines_recipe", 20,
                ChemicalPlantTests::refusesAnotherMachinesRecipe);
        tests.test("chemical_plant_keeps_its_recipe_over_a_reload", 20, ChemicalPlantTests::keepsItsRecipeOverAReload);
        tests.test("chemical_plant_is_fed_through_its_part", 100,
                helper -> isFedByAPole(helper, ANCHOR.above(PoleTier.VERTICAL_RADIUS + 1).east(), "only the part"));
        tests.test("chemical_plant_is_counted_once_by_a_pole", 100,
                helper -> isFedByAPole(helper, ANCHOR.east(2), "both blocks"));
        tests.test("chemical_plant_refuses_the_fluid_addon", 20, ChemicalPlantTests::refusesTheFluidAddon);
        if (ModList.get().isLoaded("researchd")) {
            tests.test("chemical_plant_stalls_on_a_locked_recipe", 100, ChemicalPlantTests::stallsOnALockedRecipe);
        }
    }

    /** Over a window of two whole crafts, so the phase the first tick lands on does not matter. */
    private static void craftsPlastic(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = placeWhole(helper);
        long[] energy = new long[1];
        int[] plastic = new int[1];
        long[] gas = new long[1];
        helper.startSequence()
                .thenExecute(() -> {
                    feedPlastic(helper, machine, 8, 200);
                    machine.energyStorage.set(CHARGE);
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    energy[0] = machine.energyStorage.getAmountAsLong();
                    plastic[0] = machine.inventory.getItem(OUTPUT).getCount();
                    gas[0] = machine.tank().getAmountAsLong(0);
                })
                .thenIdle(2 * TICKS_PER_CRAFT)
                .thenExecute(() -> {
                    long spent = energy[0] - machine.energyStorage.getAmountAsLong();
                    if (spent != 2 * FE_PER_CRAFT) {
                        helper.fail("two crafts' window drew " + spent + " FE, expected " + 2 * FE_PER_CRAFT, ANCHOR);
                    }
                    int made = machine.inventory.getItem(OUTPUT).getCount() - plastic[0];
                    if (made != 4) {
                        helper.fail("two crafts' window made " + made + " plastic, expected 4", ANCHOR);
                    }
                    long burned = gas[0] - machine.tank().getAmountAsLong(0);
                    if (burned != 2 * GAS_PER_CRAFT) {
                        helper.fail("two crafts' window took " + burned + " mB of gas, expected "
                                + 2 * GAS_PER_CRAFT, ANCHOR);
                    }
                })
                .thenSucceed();
    }

    /** Room for one bar, and a craft makes two. */
    private static void stallsOnAFullOutput(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = placeWhole(helper);
        feedPlastic(helper, machine, 4, 100);
        machine.inventory.set(OUTPUT, ItemResource.of(item("planetaryfactory:plastic_bar")), 63);
        machine.energyStorage.set(CHARGE);
        helper.runAfterDelay(2 * TICKS_PER_CRAFT, () -> {
            assertStalled(helper, machine, AssemblingStall.OUTPUT_FULL, 4, 100);
            helper.succeed();
        });
    }

    /** Gas and power, and no coal. */
    private static void stallsUnfed(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = placeWhole(helper);
        feedPlastic(helper, machine, 0, 100);
        machine.energyStorage.set(CHARGE);
        helper.runAfterDelay(2 * TICKS_PER_CRAFT, () -> {
            assertStalled(helper, machine, AssemblingStall.NO_INGREDIENTS, 0, 100);
            helper.succeed();
        });
    }

    /** No research unlocks plastic for a new team. */
    private static void stallsOnALockedRecipe(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = placeWhole(helper);
        ResearchTeams.placedBy(machine, ResearchTeams.create(helper));
        feedPlastic(helper, machine, 4, 100);
        machine.energyStorage.set(CHARGE);
        helper.runAfterDelay(2 * TICKS_PER_CRAFT, () -> {
            assertStalled(helper, machine, AssemblingStall.LOCKED, 4, 100);
            helper.succeed();
        });
    }

    /**
     * Heavy oil cracking names two fluids: through the anchor and the part, on the slot-less
     * overloads a pipe uses, each goes to its own input tank, a fluid it does not name is refused,
     * the inputs never come back out and the output does.
     */
    private static void fluidFaceRoutesByTheHeldRecipe(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = placeWhole(helper);
        hold(helper, machine, CRACKING);
        FluidResource water = FluidResource.of(Fluids.WATER);
        FluidResource heavy = FluidResource.of(fluid(HEAVY_OIL));
        FluidResource light = FluidResource.of(fluid(LIGHT_OIL));
        FluidResource lava = FluidResource.of(Fluids.LAVA);
        int outputTank = outputTank(machine);
        ((FluidStacksResourceHandler) machine.tank()).set(outputTank, light, 90);
        int step = 0;
        for (BlockPos at : List.of(ANCHOR, ANCHOR.above())) {
            ResourceHandler<FluidResource> face = fluidFace(helper, at);
            if (face == null) {
                helper.fail("no fluid face at " + at, at);
                return;
            }
            step++;
            expectMoved(helper, at, "water", 10, face, (f, tx) -> f.insert(water, 10, tx));
            expectMoved(helper, at, "heavy oil", 10, face, (f, tx) -> f.insert(heavy, 10, tx));
            expectMoved(helper, at, "lava", 0, face, (f, tx) -> f.insert(lava, 10, tx));
            expectMoved(helper, at, "light oil in", 0, face, (f, tx) -> f.insert(light, 10, tx));
            expectMoved(helper, at, "water back out", 0, face, (f, tx) -> f.extract(water, 10, tx));
            expectMoved(helper, at, "heavy oil back out", 0, face, (f, tx) -> f.extract(heavy, 10, tx));
            expectMoved(helper, at, "light oil out", 30, face, (f, tx) -> f.extract(light, 30, tx));
            if (!machine.tank().getResource(0).equals(water) || machine.tank().getAmountAsLong(0) != 10L * step
                    || !machine.tank().getResource(1).equals(heavy) || machine.tank().getAmountAsLong(1) != 10L * step) {
                helper.fail("through " + at + " the input tanks hold " + machine.tank().getAmountAsLong(0) + " mB of "
                        + machine.tank().getResource(0) + " and " + machine.tank().getAmountAsLong(1) + " mB of "
                        + machine.tank().getResource(1), at);
                return;
            }
        }
        helper.succeed();
    }

    /** Fill Recipe's setter refuses a recipe of either other type, with a message, and keeps plastic. */
    private static void refusesAnotherMachinesRecipe(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = placeWhole(helper);
        ListeningPlayer player = new ListeningPlayer(helper);
        AssemblingMachineMenu menu = AssemblingMachineMenu.open(0, player.getInventory(), machine);
        HoldVerdict plastic = menu.request(player, PLASTIC);
        if (plastic != HoldVerdict.HELD) {
            helper.fail("plastic was answered " + plastic, ANCHOR);
            return;
        }
        for (String other : List.of(CABLE, BASIC_OIL)) {
            player.heard.clear();
            HoldVerdict verdict = menu.request(player, other);
            if (verdict != HoldVerdict.NOT_THIS_TYPE || !machine.heldRecipe().equals(HeldRecipe.of(PLASTIC))
                    || !player.heard.equals(List.of(HoldVerdict.NOT_THIS_TYPE.messageKey()))) {
                helper.fail(other + " was answered " + verdict + " with " + player.heard + " and left "
                        + machine.heldRecipe(), ANCHOR);
                return;
            }
        }
        helper.succeed();
    }

    private static void keepsItsRecipeOverAReload(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = placeWhole(helper);
        hold(helper, machine, PLASTIC);
        CompoundTag saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(machine.getBlockPos(), machine.getBlockState(), saved,
                helper.getLevel().registryAccess());
        if (!(loaded instanceof AssemblingMachineBlockEntity reloaded)
                || !reloaded.heldRecipe().equals(HeldRecipe.of(PLASTIC))) {
            helper.fail("the machine held plastic and reloaded as " + loaded, ANCHOR);
            return;
        }
        reloaded.setLevel(helper.getLevel());
        if (!reloaded.heldRecipeResolves()) {
            helper.fail("the reloaded plastic does not resolve against the recipe manager", ANCHOR);
            return;
        }
        helper.succeed();
    }

    /**
     * A pole reaching {@code reach} counts one machine and fills it. Three blocks above the anchor
     * a pole reaches only the part, at two.
     */
    private static void isFedByAPole(GameTestHelper helper, BlockPos pole, String reach) {
        AssemblingMachineBlockEntity machine = placeWhole(helper);
        helper.startSequence()
                .thenExecute(() -> {
                    machine.energyStorage.set(0L);
                    helper.setBlock(pole, PFBlocks.CREATIVE_POLE.get());
                })
                .thenIdle(RESCAN_INTERVAL + 5)
                .thenExecute(() -> {
                    int found = helper.getBlockEntity(pole, SupplyAreaPoleBlockEntity.class).machineCount();
                    if (found != 1) {
                        helper.fail("a pole reaching " + reach + " counts " + found + " machines", pole);
                    }
                    if (machine.energyStorage.getAmountAsLong() <= 0L) {
                        helper.fail("a pole reaching " + reach + " left the machine unpowered", ANCHOR);
                    }
                })
                .thenSucceed();
    }

    /** The Fluid addon stays unused where a speed addon, the control, attaches. */
    private static void refusesTheFluidAddon(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = placeWhole(helper);
        BlockPos fluidAddon = addonSlot(helper, machine, 0);
        BlockPos speedAddon = addonSlot(helper, machine, 1);
        helper.getLevel().setBlockAndUpdate(fluidAddon, BuiltInRegistries.BLOCK
                .getValue(Identifier.parse("oritech:machine_fluid_addon")).defaultBlockState());
        helper.getLevel().setBlockAndUpdate(speedAddon, BuiltInRegistries.BLOCK
                .getValue(Identifier.parse("oritech:machine_speed_addon")).defaultBlockState());
        machine.initAddons();
        if (!machine.getConnectedAddons().contains(speedAddon)) {
            helper.fail("the speed addon did not attach, so this proves nothing", helper.relativePos(speedAddon));
            return;
        }
        if (machine.getConnectedAddons().contains(fluidAddon)
                || helper.getLevel().getBlockState(fluidAddon).getValue(MachineAddonBlock.ADDON_USED)) {
            helper.fail("the Fluid addon attached to a machine that has its tanks", helper.relativePos(fluidAddon));
            return;
        }
        helper.succeed();
    }

    private static BlockPos addonSlot(GameTestHelper helper, AssemblingMachineBlockEntity machine, int index) {
        return new BlockPos(Geometry.offsetToWorldPosition(FACING, machine.getAddonSlots().get(index),
                helper.absolutePos(ANCHOR)));
    }

    /** A stall draws no FE, takes no coal or gas, makes no progress and keeps its recipe. */
    private static void assertStalled(GameTestHelper helper, AssemblingMachineBlockEntity machine,
            AssemblingStall expected, int coal, int gas) {
        if (machine.stall() != expected) {
            helper.fail("the machine reports " + machine.stall() + ", expected " + expected, ANCHOR);
        }
        long spent = CHARGE - machine.energyStorage.getAmountAsLong();
        if (spent != 0) {
            helper.fail("a machine stalled on " + expected + " drew " + spent + " FE", ANCHOR);
        }
        if (machine.progress.get() != 0) {
            helper.fail("a machine stalled on " + expected + " made progress " + machine.progress.get(), ANCHOR);
        }
        if (machine.inventory.getItem(0).getCount() != coal || machine.tank().getAmountAsLong(0) != gas) {
            helper.fail("a machine stalled on " + expected + " took input: " + machine.inventory.getItem(0) + ", "
                    + machine.tank().getAmountAsLong(0) + " mB", ANCHOR);
        }
        if (!machine.heldRecipe().equals(HeldRecipe.of(PLASTIC))) {
            helper.fail("a machine stalled on " + expected + " let go of its recipe", ANCHOR);
        }
    }

    /** Coal into slot 0 directly, and the gas through the face, so a face refusing it fails here. */
    private static void feedPlastic(GameTestHelper helper, AssemblingMachineBlockEntity machine, int coal, int gas) {
        hold(helper, machine, PLASTIC);
        if (coal > 0) {
            machine.inventory.set(0, ItemResource.of(item("minecraft:coal")), coal);
        }
        expectMoved(helper, ANCHOR, "petroleum gas", gas, fluidFace(helper, ANCHOR),
                (f, tx) -> f.insert(FluidResource.of(fluid(PETROLEUM_GAS)), gas, tx));
    }

    private static int outputTank(AssemblingMachineBlockEntity machine) {
        for (int index = 0; index < machine.tank().size(); index++) {
            if (machine.isOutputTank(index)) {
                return index;
            }
        }
        throw new IllegalStateException("the Chemical Plant has no output tank");
    }

    private static void hold(GameTestHelper helper, AssemblingMachineBlockEntity machine, String id) {
        machine.setHeldRecipe(HeldRecipe.of(id), player(helper));
        if (!machine.heldRecipeResolves()) {
            helper.fail(id + " does not resolve on the Chemical Plant, so this proves nothing", ANCHOR);
        }
    }

    private interface Move {
        int apply(ResourceHandler<FluidResource> face, Transaction tx);
    }

    private static void expectMoved(GameTestHelper helper, BlockPos at, String what, int expected,
            ResourceHandler<FluidResource> face, Move move) {
        int moved;
        try (Transaction tx = Transaction.openRoot()) {
            moved = move.apply(face, tx);
            tx.commit();
        }
        if (moved != expected) {
            helper.fail(what + " moved " + moved + " mB, expected " + expected, at);
        }
    }

    private static ResourceHandler<FluidResource> fluidFace(GameTestHelper helper, BlockPos at) {
        return helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(at), null);
    }

    private static AssemblingMachineBlockEntity placeWhole(GameTestHelper helper) {
        PFBlocks.CHEMICAL_PLANT_FOOTPRINT.placeAll(helper.getLevel(), helper.absolutePos(ANCHOR), FACING);
        return (AssemblingMachineBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(ANCHOR));
    }

    private static Player player(GameTestHelper helper) {
        return helper.makeMockPlayer(GameType.SURVIVAL);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    private static Fluid fluid(String id) {
        return BuiltInRegistries.FLUID.getValue(Identifier.parse(id));
    }
}
