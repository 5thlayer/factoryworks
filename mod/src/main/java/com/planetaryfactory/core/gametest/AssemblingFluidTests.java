package com.planetaryfactory.core.gametest;

import java.util.List;

import com.planetaryfactory.core.PFBlocks;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

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
                    hold(helper, machine, CONCRETE);
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
                    hold(helper, machine, CONCRETE);
                    feedConcrete(machine);
                    fill(helper, machine, 50);
                    machine.energyStorage.set(CHARGE);
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    if (machine.stall() != AssemblingStall.NO_FLUID) {
                        helper.fail("a machine short of water reports " + machine.stall(), ANCHOR);
                    }
                    long spent = CHARGE - machine.energyStorage.getAmountAsLong();
                    if (spent != 0) {
                        helper.fail("a machine short of water drew " + spent + " FE", ANCHOR);
                    }
                    if (machine.progress.get() != 0) {
                        helper.fail("a machine short of water made progress " + machine.progress.get(), ANCHOR);
                    }
                    if (machine.inventory.getItem(0).getCount() != 5 || machine.inventory.getItem(1).getCount() != 1) {
                        helper.fail("a machine short of water took input", ANCHOR);
                    }
                    if (machine.tank().getAmountAsLong(0) != 50) {
                        helper.fail("a machine short of water took water: " + machine.tank().getAmountAsLong(0),
                                ANCHOR);
                    }
                    if (!machine.heldRecipe().equals(HeldRecipe.of(CONCRETE))) {
                        helper.fail("a machine short of water let go of its recipe", ANCHOR);
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
        for (BlockPos at : List.of(ANCHOR, hullBlock())) {
            ResourceHandler<FluidResource> face = fluidFace(helper, at);
            if (face == null) {
                helper.fail("tier 2 has no fluid face at " + at, at);
                return;
            }
            machine.setHeldRecipe(HeldRecipe.NONE, player(helper));
            expectMoved(helper, at, "water with no recipe held", 0, face, (f, tx) -> f.insert(water, 100, tx));
            hold(helper, machine, CONCRETE);
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

    /** Tier 1 has no tank, so no pipe finds one, on the anchor or a hull block. */
    private static void tierOneHasNoFluidFace(GameTestHelper helper) {
        placeWhole(helper, AssemblingTier.ONE);
        for (BlockPos at : List.of(ANCHOR, hullBlock())) {
            if (fluidFace(helper, at) != null) {
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
        hold(helper, machine, CONCRETE);
        fill(helper, machine, 300);
        hold(helper, machine, CONCRETE);
        if (machine.tank().getAmountAsLong(0) != 300) {
            helper.fail("re-picking the recipe already held emptied the tank", ANCHOR);
            return;
        }
        hold(helper, machine, CABLE);
        if (machine.tank().getAmountAsLong(0) != 0 || !machine.tank().getResource(0).isEmpty()) {
            helper.fail("a changed recipe left " + machine.tank().getAmountAsLong(0) + " mB in the tank", ANCHOR);
            return;
        }
        helper.succeed();
    }

    /** Saved and loaded through the tag a chunk save writes. */
    private static void keepsItsTankOverAReload(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = placeWhole(helper, AssemblingTier.TWO);
        hold(helper, machine, CONCRETE);
        fill(helper, machine, 420);
        CompoundTag saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(machine.getBlockPos(), machine.getBlockState(), saved,
                helper.getLevel().registryAccess());
        if (!(loaded instanceof AssemblingMachineBlockEntity reloaded)) {
            helper.fail("the saved machine reloaded as " + loaded, ANCHOR);
            return;
        }
        if (reloaded.tank().getAmountAsLong(0) != 420 || !reloaded.tank().getResource(0).equals(FluidResource.of(Fluids.WATER))) {
            helper.fail("420 mB of water reloaded as " + reloaded.tank().getAmountAsLong(0) + " mB of "
                    + reloaded.tank().getResource(0), ANCHOR);
            return;
        }
        helper.succeed();
    }

    private static void feedConcrete(AssemblingMachineBlockEntity machine) {
        machine.inventory.set(0, ItemResource.of(item("minecraft:stone_bricks")), 5);
        machine.inventory.set(1, ItemResource.of(item("minecraft:raw_iron")), 1);
    }

    private static void hold(GameTestHelper helper, AssemblingMachineBlockEntity machine, String id) {
        machine.setHeldRecipe(HeldRecipe.of(id), player(helper));
        if (!machine.heldRecipeResolves()) {
            helper.fail(id + " is not loaded, so this proves nothing", ANCHOR);
        }
    }

    /** Through the machine's own face, so a fill the face refuses fails here rather than later. */
    private static void fill(GameTestHelper helper, AssemblingMachineBlockEntity machine, int millibuckets) {
        ResourceHandler<FluidResource> face = fluidFace(helper, ANCHOR);
        expectMoved(helper, ANCHOR, "filling the tank", millibuckets, face,
                (f, tx) -> f.insert(FluidResource.of(Fluids.WATER), millibuckets, tx));
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

    private static BlockPos hullBlock() {
        return PFBlocks.assemblingFootprint(AssemblingTier.TWO).positions(ANCHOR, FACING).stream()
                .filter(pos -> !pos.equals(ANCHOR)).findFirst().orElseThrow();
    }

    private static AssemblingMachineBlockEntity placeWhole(GameTestHelper helper, AssemblingTier tier) {
        PFBlocks.assemblingFootprint(tier).placeAll(helper.getLevel(), helper.absolutePos(ANCHOR), FACING);
        return (AssemblingMachineBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(ANCHOR));
    }

    private static Player player(GameTestHelper helper) {
        return helper.makeMockPlayer(GameType.SURVIVAL);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }
}
