package com.planetaryfactory.core.gametest;

import static com.planetaryfactory.core.gametest.ChassisFixture.expectMoved;

import java.util.List;

import com.planetaryfactory.core.machine.AssemblingMachineBlockEntity;
import com.planetaryfactory.core.machine.AssemblingMachineMenu;
import com.planetaryfactory.core.machine.AssemblingStall;
import com.planetaryfactory.core.machine.AssemblingTier;
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
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Tiers 2 and 3's input tank (#295, ADR-0075): a fluid recipe crafts from it and stalls without
 * it, its face takes only the Held recipe's fluid and never gives it back, a changed recipe voids
 * it, and it survives the save hook. Tier 1 has no face at all.
 *
 * <p>concrete is the fixture: 5 stone bricks, 1 raw iron and 100 mB of water into 10 grey concrete
 * in 10 s, which tier 2's speed 0.75 runs in 267 ticks. The figures are typed, for
 * {@code BoilerTests}' reason.
 */
final class AssemblingFluidTests {

    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final Direction FACING = Direction.NORTH;
    private static final ChassisFixture TIER_TWO = ChassisFixture.assembling(AssemblingTier.TWO, ANCHOR, FACING);

    private static final String CONCRETE = "planetaryfactory:assembling/concrete";
    private static final String CABLE = "planetaryfactory:assembling/copper_cable";

    private static final int CONCRETE_TICKS = 267;
    private static final int WATER_PER_CRAFT = 100;
    private static final long CHARGE = 50_000L;

    private AssemblingFluidTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("assembling_machine_2_crafts_concrete_from_its_tank", CONCRETE_TICKS + 40,
                AssemblingFluidTests::craftsConcreteFromItsTank);
        tests.test("assembling_machine_2_stalls_short_of_water", 100,
                AssemblingFluidTests::stallsShortOfWater);
        tests.test("assembling_machine_2_fluid_face_takes_only_the_held_fluid", 20,
                AssemblingFluidTests::fluidFaceTakesOnlyTheHeldFluid);
        tests.test("assembling_machine_has_no_fluid_face", 20,
                AssemblingFluidTests::tierOneHasNoFluidFace);
        tests.test("assembling_machine_2_fluid_face_stops_at_the_overload_limit", 20,
                AssemblingFluidTests::fluidFaceStopsAtTheOverloadLimit);
        tests.test("assembling_machine_2_holds_a_fluid_recipe", 20,
                AssemblingFluidTests::holdsAFluidRecipe);
        tests.test("assembling_machine_2_voids_its_tank_on_a_change", 20,
                AssemblingFluidTests::voidsItsTankOnAChange);
        tests.test("assembling_machine_2_keeps_its_tank_over_a_reload", 20,
                AssemblingFluidTests::keepsItsTankOverAReload);
    }

    private static void craftsConcreteFromItsTank(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = placeWhole(helper, AssemblingTier.TWO);
        helper.startSequence()
                .thenExecute(() -> {
                    TIER_TWO.hold(helper, machine, CONCRETE);
                    feedConcrete(machine);
                    fill(helper, machine, 150);
                    machine.energyStorage.set(CHARGE);
                })
                .thenIdle(CONCRETE_TICKS + 5)
                .thenExecute(() -> {
                    int concrete = machine.inventory.getItem(AssemblingMachineBlockEntity.OUTPUT).getCount();
                    if (concrete != 10) {
                        helper.fail("one concrete craft made " + concrete + " concrete, expected 10", ANCHOR);
                    }
                    long water = machine.tank().getAmountAsLong(0);
                    if (water != 150 - WATER_PER_CRAFT) {
                        helper.fail("one concrete craft left " + water + " mB of 150, expected "
                                + (150 - WATER_PER_CRAFT), ANCHOR);
                    }
                    if (!machine.inventory.getItem(0).isEmpty() || !machine.inventory.getItem(1).isEmpty()) {
                        helper.fail("one craft left inputs behind: " + machine.inventory.getItem(0) + ", "
                                + machine.inventory.getItem(1), ANCHOR);
                    }
                })
                .thenSucceed();
    }

    /** Fed items, powered and with room, but 50 mB where the craft needs 100: nothing moves. */
    private static void stallsShortOfWater(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = placeWhole(helper, AssemblingTier.TWO);
        helper.startSequence()
                .thenExecute(() -> {
                    TIER_TWO.hold(helper, machine, CONCRETE);
                    feedConcrete(machine);
                    fill(helper, machine, 50);
                    machine.energyStorage.set(CHARGE);
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    TIER_TWO.assertStalled(helper, machine, AssemblingStall.NO_FLUID, CHARGE, CONCRETE);
                    if (machine.inventory.getItem(0).getCount() != 5 || machine.inventory.getItem(1).getCount() != 1) {
                        helper.fail("a machine short of water took input", ANCHOR);
                    }
                    if (machine.tank().getAmountAsLong(0) != 50) {
                        helper.fail("a machine short of water took water: " + machine.tank().getAmountAsLong(0),
                                ANCHOR);
                    }
                })
                .thenSucceed();
    }

    /**
     * Through the capability on the anchor and on a hull block, on the slot-less overloads a pipe
     * uses: water with concrete held goes in, lava does not, nothing comes out, and with no recipe
     * held nothing goes in.
     */
    private static void fluidFaceTakesOnlyTheHeldFluid(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = placeWhole(helper, AssemblingTier.TWO);
        FluidResource water = FluidResource.of(Fluids.WATER);
        FluidResource lava = FluidResource.of(Fluids.LAVA);
        for (BlockPos at : List.of(ANCHOR, TIER_TWO.hullBlock())) {
            ResourceHandler<FluidResource> face = TIER_TWO.fluidFace(helper, at);
            if (face == null) {
                helper.fail("tier 2 has no fluid face at " + at, at);
                return;
            }
            machine.setHeldRecipe(HeldRecipe.NONE, player(helper));
            expectMoved(helper, at, "water with no recipe held", 0, face, (f, tx) -> f.insert(water, 100, tx));
            TIER_TWO.hold(helper, machine, CONCRETE);
            expectMoved(helper, at, "lava with concrete held", 0, face, (f, tx) -> f.insert(lava, 100, tx));
            expectMoved(helper, at, "water with concrete held", 100, face, (f, tx) -> f.insert(water, 100, tx));
            expectMoved(helper, at, "water back out", 0, face, (f, tx) -> f.extract(water, 100, tx));
            if (machine.tank().getAmountAsLong(0) != 100) {
                helper.fail("through " + at + " the tank holds " + machine.tank().getAmountAsLong(0)
                        + " mB, expected 100", at);
                return;
            }
        }
        helper.succeed();
    }

    /** Four crafts' worth of concrete's 100 mB, typed from the Factorio probe (#519). */
    private static void fluidFaceStopsAtTheOverloadLimit(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = placeWhole(helper, AssemblingTier.TWO);
        TIER_TWO.hold(helper, machine, CONCRETE);
        FluidResource water = FluidResource.of(Fluids.WATER);
        for (BlockPos at : List.of(ANCHOR, TIER_TWO.hullBlock())) {
            ResourceHandler<FluidResource> face = TIER_TWO.fluidFace(helper, at);
            ((FluidStacksResourceHandler) machine.tank()).set(0, water, 150);
            expectMoved(helper, at, "water up to the limit", 250, face, (f, tx) -> f.insert(water, 1000, tx));
            expectMoved(helper, at, "water past the limit", 0, face, (f, tx) -> f.insert(water, 100, tx));
        }
        helper.succeed();
    }

    /** Tier 1 has no tank, so no pipe finds one, on the anchor or a hull block. */
    private static void tierOneHasNoFluidFace(GameTestHelper helper) {
        placeWhole(helper, AssemblingTier.ONE);
        for (BlockPos at : List.of(ANCHOR, TIER_TWO.hullBlock())) {
            if (TIER_TWO.fluidFace(helper, at) != null) {
                helper.fail("tier 1 answers a fluid face at " + at, at);
                return;
            }
        }
        helper.succeed();
    }

    /** Fill Recipe's setter holds concrete on tier 2, where tier 1 refuses it (#331). */
    private static void holdsAFluidRecipe(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = placeWhole(helper, AssemblingTier.TWO);
        Player player = player(helper);
        HoldVerdict verdict = AssemblingMachineMenu.open(0, player.getInventory(), machine).request(player, CONCRETE);
        if (verdict != HoldVerdict.HELD || !machine.heldRecipe().equals(HeldRecipe.of(CONCRETE))) {
            helper.fail("tier 2 answered concrete " + verdict + " and holds " + machine.heldRecipe(), ANCHOR);
            return;
        }
        helper.succeed();
    }

    private static void voidsItsTankOnAChange(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = placeWhole(helper, AssemblingTier.TWO);
        TIER_TWO.hold(helper, machine, CONCRETE);
        fill(helper, machine, 300);
        TIER_TWO.hold(helper, machine, CONCRETE);
        if (machine.tank().getAmountAsLong(0) != 300) {
            helper.fail("re-picking the recipe already held emptied the tank", ANCHOR);
            return;
        }
        TIER_TWO.hold(helper, machine, CABLE);
        if (machine.tank().getAmountAsLong(0) != 0 || !machine.tank().getResource(0).isEmpty()) {
            helper.fail("a changed recipe left " + machine.tank().getAmountAsLong(0) + " mB in the tank", ANCHOR);
            return;
        }
        helper.succeed();
    }

    /** Saved and loaded through the tag a chunk save writes. */
    private static void keepsItsTankOverAReload(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = placeWhole(helper, AssemblingTier.TWO);
        TIER_TWO.hold(helper, machine, CONCRETE);
        fill(helper, machine, 320);
        CompoundTag saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(machine.getBlockPos(), machine.getBlockState(), saved,
                helper.getLevel().registryAccess());
        if (!(loaded instanceof AssemblingMachineBlockEntity reloaded)) {
            helper.fail("the saved machine reloaded as " + loaded, ANCHOR);
            return;
        }
        if (reloaded.tank().getAmountAsLong(0) != 320 || !reloaded.tank().getResource(0).equals(FluidResource.of(Fluids.WATER))) {
            helper.fail("320 mB of water reloaded as " + reloaded.tank().getAmountAsLong(0) + " mB of "
                    + reloaded.tank().getResource(0), ANCHOR);
            return;
        }
        helper.succeed();
    }

    private static void feedConcrete(AssemblingMachineBlockEntity machine) {
        machine.inventory.set(0, ItemResource.of(item("minecraft:stone_bricks")), 5);
        machine.inventory.set(1, ItemResource.of(item("minecraft:raw_iron")), 1);
    }

    /** Through the machine's own face, so a fill the face refuses fails here rather than later. */
    private static void fill(GameTestHelper helper, AssemblingMachineBlockEntity machine, int millibuckets) {
        ResourceHandler<FluidResource> face = TIER_TWO.fluidFace(helper, ANCHOR);
        expectMoved(helper, ANCHOR, "filling the tank", millibuckets, face,
                (f, tx) -> f.insert(FluidResource.of(Fluids.WATER), millibuckets, tx));
    }

    private static AssemblingMachineBlockEntity placeWhole(GameTestHelper helper, AssemblingTier tier) {
        return ChassisFixture.assembling(tier, ANCHOR, FACING).placeWhole(helper, AssemblingMachineBlockEntity.class);
    }

    private static Player player(GameTestHelper helper) {
        return helper.makeMockPlayer(GameType.SURVIVAL);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }
}
