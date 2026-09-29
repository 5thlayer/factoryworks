package com.planetaryfactory.core.gametest;

import static com.planetaryfactory.core.gametest.ChassisFixture.expectMoved;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.energy.PoleTier;
import com.planetaryfactory.core.energy.SupplyAreaPoleBlockEntity;
import com.planetaryfactory.core.machine.AssemblingMachineBlockEntity;
import com.planetaryfactory.core.machine.AssemblingMachineMenu;
import com.planetaryfactory.core.machine.AssemblingMachineRecipes;
import com.planetaryfactory.core.machine.AssemblingStall;
import com.planetaryfactory.core.machine.AssemblingStatus;
import com.planetaryfactory.core.machine.AssemblingTier;
import com.planetaryfactory.core.machine.HeldRecipe;
import com.planetaryfactory.core.machine.HoldVerdict;
import com.planetaryfactory.core.machine.RecipeChoice;
import com.planetaryfactory.core.recipes.PFRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import rearth.oritech.util.ColorableMachine.ColorVariant;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import rearth.oritech.block.base.block.MultiblockMachine;

/**
 * The Assembling Machine's Held recipe (#327), where only a running server reaches it.
 *
 * <p>The codec's round trip is {@code HeldRecipeTest}'s. What is left is the seam around it: that
 * the block entity's save hook actually writes the field and its load hook reads it back into a
 * recipe the server resolves; that every assembling recipe the server loaded is one Fill Recipe
 * can set and the machine can hold -- the check that replaces #237 and #238; and that changing the
 * recipe hands the player back what the inputs held. And the craft cycle (#328): that a fed,
 * powered machine crafts at Factorio's rate, and that each of the three stalls draws nothing, takes
 * nothing and keeps its recipe. Checked against its defect: extracting the tick's energy before the
 * stall is asked turns all three stall tests red.
 *
 * <p>The anchor is set alone, without its parts: nothing here is about the footprint, which
 * {@code PlacementPlanTests} holds.
 */
final class AssemblingMachineTests {

    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final Direction FACING = Direction.NORTH;
    private static final ChassisFixture CHASSIS = ChassisFixture.assembling(AssemblingTier.ONE, ANCHOR, FACING);

    private AssemblingMachineTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("assembling_machine_keeps_its_recipe_over_a_reload", 20,
                AssemblingMachineTests::keepsItsRecipeOverAReload);
        tests.test("assembling_machine_holds_every_assembling_recipe", 20,
                AssemblingMachineTests::holdsEveryAssemblingRecipe);
        tests.test("assembling_machine_refuses_what_it_cannot_hold", 20,
                AssemblingMachineTests::refusesWhatItCannotHold);
        tests.test("assembling_machine_hands_back_ingredients_on_a_change", 20,
                AssemblingMachineTests::handsBackIngredientsOnAChange);
        tests.test("assembling_machine_crafts_at_factorios_rate", 100,
                AssemblingMachineTests::craftsAtFactoriosRate);
        tests.test("assembling_machine_2_crafts_at_factorios_rate", 100,
                AssemblingMachineTests::tierTwoCraftsAtFactoriosRate);
        for (AssemblingTier tier : AssemblingTier.values()) {
            tests.test(tier.blockName() + "_ignores_paint", 20, helper -> ignoresPaint(helper, tier));
        }
        tests.test("assembling_machine_stalls_on_a_full_output", 100,
                AssemblingMachineTests::stallsOnAFullOutput);
        tests.test("assembling_machine_stalls_unfed", 100,
                AssemblingMachineTests::stallsUnfed);
        tests.test("assembling_machine_status_is_derived_on_each_ask", 20,
                AssemblingMachineTests::statusIsDerivedOnEachAsk);
        // Counted once: a hull block has no block entity and answers the anchor's face (#328).
        tests.test("assembling_machine_is_powered_by_a_pole", 100,
                helper -> CHASSIS.isFedByAPole(helper, placeWhole(helper), ANCHOR.south(2), "one whole machine"));
        tests.test("assembling_machine_is_found_through_a_hull_block", 100,
                AssemblingMachineTests::poleReachingOnlyAHullBlockFindsIt);
        tests.test("assembling_machine_filters_inputs_to_its_recipe", 20,
                AssemblingMachineTests::filtersInputsToItsRecipe);
        tests.test("assembling_machine_with_no_recipe_takes_nothing", 20,
                AssemblingMachineTests::withNoRecipeTakesNothing);
        tests.test("assembling_machine_input_mode_stays_pinned", 20,
                AssemblingMachineTests::inputModeStaysPinned);
        tests.test("assembling_machine_holds_automated_input_to_the_overload_limit", 20,
                helper -> holdsInputToTheOverloadLimit(helper, AssemblingTier.ONE, 3));
        tests.test("assembling_machine_3_holds_automated_input_to_the_overload_limit", 20,
                helper -> holdsInputToTheOverloadLimit(helper, AssemblingTier.THREE, 4));
        tests.test("assembling_machine_screen_places_a_full_stack", 20,
                AssemblingMachineTests::screenPlacesAFullStack);
    }

    /** Two ingredients, so slots 2 and 3 are unused. */
    private static final String BOILER = "planetaryfactory:assembling/boiler";

    /** Through the capability, on the anchor and a hull block, on both overloads (ADR-0073). */
    private static void filtersInputsToItsRecipe(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = placeWhole(helper);
        machine.setHeldRecipe(HeldRecipe.of(BOILER), player(helper));
        if (!machine.heldRecipeResolves()) {
            helper.fail(BOILER + " is not loaded, so the filter has no recipe to filter to", ANCHOR);
            return;
        }
        BlockPos part = CHASSIS.hullBlock();
        ItemResource furnace = ItemResource.of(item("planetaryfactory:stone_furnace"));
        ItemResource fluidPipe = ItemResource.of(item("oritech:fluid_pipe"));
        ItemResource stone = ItemResource.of(Items.STONE);
        for (BlockPos at : List.of(ANCHOR, part)) {
            ResourceHandler<ItemResource> face = CHASSIS.itemFace(helper, at);
            if (face == null) {
                helper.fail("no item face at " + at, at);
                return;
            }
            clearInputs(machine);
            expectMoved(helper, at, "stone, slot-less", 0, face, (f, tx) -> f.insert(stone, 8, tx));
            expectMoved(helper, at, "stone into slot 0", 0, face, (f, tx) -> f.insert(0, stone, 8, tx));
            expectMoved(helper, at, "fluid pipe into the furnace's slot 0", 0, face, (f, tx) -> f.insert(0, fluidPipe, 8, tx));
            expectMoved(helper, at, "furnace into unused slot 2", 0, face, (f, tx) -> f.insert(2, furnace, 8, tx));
            expectMoved(helper, at, "furnace into unused slot 3", 0, face, (f, tx) -> f.insert(3, furnace, 8, tx));
            expectMoved(helper, at, "furnace into the output", 0, face,
                    (f, tx) -> f.insert(AssemblingMachineBlockEntity.OUTPUT, furnace, 8, tx));
            // Tier 1's Overload Limit for the boiler is 3 crafts (#517).
            expectMoved(helper, at, "furnace, slot-less", 3, face, (f, tx) -> f.insert(furnace, 8, tx));
            expectMoved(helper, at, "fluid pipe, slot-less", 8, face, (f, tx) -> f.insert(fluidPipe, 8, tx));
            if (!machine.inventory.getResource(0).equals(furnace) || !machine.inventory.getResource(1).equals(fluidPipe)
                    || !machine.inventory.getResource(2).isEmpty() || !machine.inventory.getResource(3).isEmpty()) {
                helper.fail("through " + at + " the inputs hold " + inputs(machine)
                        + ", expected the furnace, the fluid pipes and two empty slots", at);
                return;
            }
            expectMoved(helper, at, "extracting a furnace the craft is waiting on", 0, face,
                    (f, tx) -> f.extract(furnace, 1, tx));
            machine.inventory.set(AssemblingMachineBlockEntity.OUTPUT,
                    ItemResource.of(item("planetaryfactory:boiler")), 2);
            expectMoved(helper, at, "extracting the product", 2, face,
                    (f, tx) -> f.extract(ItemResource.of(item("planetaryfactory:boiler")), 2, tx));
        }
        helper.succeed();
    }

    /** No Held recipe: no slot takes anything, on either overload. */
    private static void withNoRecipeTakesNothing(GameTestHelper helper) {
        place(helper);
        ResourceHandler<ItemResource> face = CHASSIS.itemFace(helper, ANCHOR);
        ItemResource plate = ItemResource.of(item("ftbmaterials:iron_plate"));
        expectMoved(helper, ANCHOR, "plate, slot-less, with no recipe", 0, face, (f, tx) -> f.insert(plate, 8, tx));
        for (int slot = 0; slot < AssemblingMachineBlockEntity.INPUTS; slot++) {
            int named = slot;
            expectMoved(helper, ANCHOR, "plate into slot " + slot + " with no recipe", 0, face,
                    (f, tx) -> f.insert(named, plate, 8, tx));
        }
        helper.succeed();
    }

    /** Oritech's mode button must not reach {@code FILL_EVENLY} (ADR-0074). */
    private static void inputModeStaysPinned(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = place(helper);
        machine.setHeldRecipe(HeldRecipe.of(BOILER), player(helper));
        machine.cycleInputMode();
        ResourceHandler<ItemResource> face = CHASSIS.itemFace(helper, ANCHOR);
        ItemResource fluidPipe = ItemResource.of(item("oritech:fluid_pipe"));
        expectMoved(helper, ANCHOR, "fluid pipes into slot 1 after a mode cycle", 8, face,
                (f, tx) -> f.insert(1, fluidPipe, 8, tx));
        if (machine.inventory.getAmountAsInt(1) != 8) {
            helper.fail("after Oritech's mode button the inputs hold " + inputs(machine)
                    + ", expected all 8 fluid pipes in slot 1", ANCHOR);
            return;
        }
        helper.succeed();
    }

    /** Copper cable's Overload Limit, typed from the Factorio probe: 3 plates on tier 1, 4 on tier 3 (#517). */
    private static void holdsInputToTheOverloadLimit(GameTestHelper helper, AssemblingTier tier, int limit) {
        AssemblingMachineBlockEntity machine = place(helper, tier);
        machine.setHeldRecipe(HeldRecipe.of(CABLE), player(helper));
        ResourceHandler<ItemResource> face = CHASSIS.itemFace(helper, ANCHOR);
        ItemResource plate = ItemResource.of(item("ftbmaterials:copper_plate"));
        expectMoved(helper, ANCHOR, "plates, slot-less, into an empty machine", limit, face,
                (f, tx) -> f.insert(plate, 8, tx));
        expectMoved(helper, ANCHOR, "plates into slot 0 at the limit", 0, face, (f, tx) -> f.insert(0, plate, 8, tx));
        machine.inventory.set(0, plate, limit - 1);
        expectMoved(helper, ANCHOR, "plates into slot 0 one short of the limit", 1, face,
                (f, tx) -> f.insert(0, plate, 8, tx));
        helper.succeed();
    }

    /** The hand is not held to the Overload Limit: a shift-click moves the whole stack (#517). */
    private static void screenPlacesAFullStack(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = place(helper);
        Player player = player(helper);
        machine.setHeldRecipe(HeldRecipe.of(CABLE), player);
        player.getInventory().setItem(0, new ItemStack(item("ftbmaterials:copper_plate"), 64));
        AssemblingMachineMenu menu = AssemblingMachineMenu.open(0, player.getInventory(), machine);
        int hotbarFirst = AssemblingMachineBlockEntity.INPUTS + 1 + 27;
        menu.quickMoveStack(player, hotbarFirst);
        if (machine.inventory.getAmountAsInt(0) != 64) {
            helper.fail("a shift-click placed " + machine.inventory.getItem(0) + ", expected 64 copper plates", ANCHOR);
            return;
        }
        helper.succeed();
    }

    private static void clearInputs(AssemblingMachineBlockEntity machine) {
        for (int slot = 0; slot <= AssemblingMachineBlockEntity.OUTPUT; slot++) {
            machine.inventory.set(slot, ItemResource.EMPTY, 0);
        }
    }

    private static String inputs(AssemblingMachineBlockEntity machine) {
        StringBuilder out = new StringBuilder();
        for (int slot = 0; slot < AssemblingMachineBlockEntity.INPUTS; slot++) {
            out.append(slot == 0 ? "" : ", ").append(machine.inventory.getItem(slot));
        }
        return out.toString();
    }

    /**
     * A small pole whose 5x5 reaches a hull block and not the anchor still finds the machine.
     * Which block is the anchor is not visible to a player, so a pole placed against the far side
     * must not miss it.
     */
    private static void poleReachingOnlyAHullBlockFindsIt(GameTestHelper helper) {
        placeWhole(helper);
        BlockPos part = PFBlocks.assemblingFootprint(AssemblingTier.ONE).positions(ANCHOR, FACING).stream()
                .filter(pos -> pos.getY() == ANCHOR.getY() && !pos.equals(ANCHOR))
                .findFirst().orElseThrow();
        BlockPos step = part.subtract(ANCHOR);
        // Two blocks past the hull block: inside a small pole's +-2, and the anchor at 3 is not.
        BlockPos pole = part.offset(step.multiply(2));
        helper.startSequence()
                .thenExecute(() -> helper.setBlock(pole, PFBlocks.pole(PoleTier.SMALL).get()))
                .thenIdle(ChassisFixture.RESCAN_INTERVAL + 5)
                .thenExecute(() -> {
                    int found = helper.getBlockEntity(pole, SupplyAreaPoleBlockEntity.class).machineCount();
                    if (found != 1) {
                        helper.fail("a small pole reaching only a hull block counts " + found
                                + " machines, expected 1", pole);
                    }
                })
                .thenSucceed();
    }

    private static AssemblingMachineBlockEntity placeWhole(GameTestHelper helper) {
        return CHASSIS.placeWhole(helper, AssemblingMachineBlockEntity.class);
    }

    /** copper-cable: one copper plate makes two wire in Factorio's 0.5 s. */
    private static final String CABLE = "planetaryfactory:assembling/copper_cable";


    private static final String CIRCUIT = "planetaryfactory:assembling/electronic_circuit";

    private static final String CONCRETE = "planetaryfactory:assembling/concrete";

    /**
     * copper-cable's 0.5 s at {@code assembling-machine-1}'s speed 0.5, in ticks. Typed rather than
     * read off {@code AssemblingMachineSpec}, for {@code BoilerTests}' reason: read off the spec, the
     * test would agree with it by construction.
     */
    private static final int TICKS_PER_CRAFT = 20;

    /** 75 kW for one second at 100 J per FE. Typed for the same reason. */
    private static final long FE_PER_CRAFT = 750L;

    /** Two whole crafts. Any window this long holds exactly two completions and two crafts' energy. */
    private static final int WINDOW = 2 * TICKS_PER_CRAFT;

    private static final long CHARGE = 20_000L;

    /**
     * Fed, powered and with room, it crafts; measured over a window of whole crafts so the phase the
     * first tick lands on does not matter. The energy is asserted exactly: 37.5 FE a tick is not a
     * whole number, and a floor or a ceiling on it misses by a craft's worth of rounding.
     */
    private static void craftsAtFactoriosRate(GameTestHelper helper) {
        craftsCableAt(helper, AssemblingTier.ONE, TICKS_PER_CRAFT, FE_PER_CRAFT);
    }

    /**
     * copper-cable's 0.5 s at {@code assembling-machine-2}'s speed 0.75 is 13.3 ticks, run in 14; its
     * 150 kW over the unrounded 2/3 s is 1,000 FE. Typed, for the tier 1 figures' reason.
     */
    private static void tierTwoCraftsAtFactoriosRate(GameTestHelper helper) {
        craftsCableAt(helper, AssemblingTier.TWO, 14, 1000L);
    }

    private static void craftsCableAt(GameTestHelper helper, AssemblingTier tier, int ticksPerCraft, long fePerCraft) {
        AssemblingMachineBlockEntity machine = place(helper, tier);
        long[] energy = new long[1];
        int[] wire = new int[1];
        helper.startSequence()
                .thenExecute(() -> {
                    machine.setHeldRecipe(HeldRecipe.of(CABLE), player(helper));
                    machine.inventory.set(0, ItemResource.of(item("ftbmaterials:copper_plate")), 8);
                    machine.energyStorage.set(CHARGE);
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    energy[0] = machine.energyStorage.getAmountAsLong();
                    wire[0] = machine.inventory.getItem(OUTPUT).getCount();
                })
                .thenIdle(2 * ticksPerCraft)
                .thenExecute(() -> {
                    long spent = energy[0] - machine.energyStorage.getAmountAsLong();
                    if (spent != 2 * fePerCraft) {
                        helper.fail("two crafts' window on " + tier + " drew " + spent + " FE, expected "
                                + (2 * fePerCraft), ANCHOR);
                    }
                    int made = machine.inventory.getItem(OUTPUT).getCount() - wire[0];
                    if (made != 4) {
                        helper.fail("two crafts' window on " + tier + " made " + made + " copper wire, expected 4",
                                ANCHOR);
                    }
                    if (machine.inventory.getItem(0).getCount() + machine.inventory.getItem(OUTPUT).getCount() / 2
                            != 8) {
                        helper.fail("plates and wire do not account for the 8 plates put in", ANCHOR);
                    }
                })
                .thenSucceed();
    }

    /**
     * A shift-click with another tier's paint, through the player's game mode as a real click goes,
     * leaves the machine its tier's colour and the cartridge unspent (ADR-0075). The colours are
     * typed: read off the tier, a swapped pair would agree with itself.
     */
    private static void ignoresPaint(GameTestHelper helper, AssemblingTier tier) {
        AssemblingMachineBlockEntity machine = place(helper, tier);
        ColorVariant expected = switch (tier) {
            case ONE -> ColorVariant.ORANGE;
            case TWO -> ColorVariant.DIAMOND;
            case THREE -> ColorVariant.INDUSTRIAL;
        };
        if (machine.getCurrentColor() != expected) {
            helper.fail(tier + " wears " + machine.getCurrentColor() + ", expected " + expected, ANCHOR);
            return;
        }
        // No tier wears sculk, so this is always another tier's colour or none.
        Item paint = item("oritech:sculk_paint");
        // Not makeMockServerPlayerInLevel: joining the level fires KubeJS's login sync, which
        // refuses the mock connection.
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        ItemStack stack = new ItemStack(paint, 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos absolute = helper.absolutePos(ANCHOR);
        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
        player.setShiftKeyDown(false);
        if (machine.getCurrentColor() != expected) {
            helper.fail("painting " + tier + " turned it " + machine.getCurrentColor(), ANCHOR);
            return;
        }
        int left = player.getItemInHand(InteractionHand.MAIN_HAND).getCount();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        if (left != 4) {
            helper.fail("painting " + tier + " spent " + (4 - left) + " paint on a colour it cannot take", ANCHOR);
            return;
        }
        helper.succeed();
    }

    /** Room for one wire, and a craft makes two: it holds, draws nothing and takes no plate. */
    private static void stallsOnAFullOutput(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = place(helper);
        helper.startSequence()
                .thenExecute(() -> {
                    machine.setHeldRecipe(HeldRecipe.of(CABLE), player(helper));
                    machine.inventory.set(0, ItemResource.of(item("ftbmaterials:copper_plate")), 4);
                    machine.inventory.set(OUTPUT, ItemResource.of(item("ftbmaterials:copper_wire")), 63);
                    machine.energyStorage.set(CHARGE);
                })
                .thenIdle(WINDOW)
                .thenExecute(() -> assertStalled(helper, machine, AssemblingStall.OUTPUT_FULL, 4, CABLE))
                .thenSucceed();
    }

    /** Holding a recipe with nothing to make it from: it idles, and keeps the recipe. */
    private static void stallsUnfed(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = place(helper);
        helper.startSequence()
                .thenExecute(() -> {
                    machine.setHeldRecipe(HeldRecipe.of(CABLE), player(helper));
                    machine.energyStorage.set(CHARGE);
                })
                .thenIdle(WINDOW)
                .thenExecute(() -> assertStalled(helper, machine, AssemblingStall.NO_INGREDIENTS, 0, CABLE))
                .thenSucceed();
    }

    /**
     * The screen's status (#332), asked with no tick between the changes, since it is recomputed on
     * each ask rather than read off the last craft tick. An empty buffer is only reported once
     * nothing earlier in the craft cycle stops the machine.
     */
    private static void statusIsDerivedOnEachAsk(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = place(helper);
        helper.startSequence()
                .thenExecute(() -> {
                    machine.energyStorage.set(0L);
                    assertStatus(helper, machine, AssemblingStatus.IDLE);
                    machine.setHeldRecipe(HeldRecipe.of(CABLE), player(helper));
                    assertStatus(helper, machine, AssemblingStatus.MISSING_INGREDIENTS);
                    machine.inventory.set(0, ItemResource.of(item("ftbmaterials:copper_plate")), 4);
                    assertStatus(helper, machine, AssemblingStatus.NO_POWER);
                    machine.energyStorage.set(CHARGE);
                    assertStatus(helper, machine, AssemblingStatus.PROCESSING);
                })
                .thenSucceed();
    }

    private static void assertStatus(GameTestHelper helper, AssemblingMachineBlockEntity machine,
            AssemblingStatus expected) {
        if (machine.status() != expected) {
            helper.fail("the screen's status is " + machine.status() + ", expected " + expected, ANCHOR);
        }
    }

    /** A stall also takes no input. */
    private static void assertStalled(GameTestHelper helper, AssemblingMachineBlockEntity machine,
            AssemblingStall expected, int inputs, String held) {
        CHASSIS.assertStalled(helper, machine, expected, CHARGE, held);
        if (machine.inventory.getItem(0).getCount() != inputs) {
            helper.fail("a machine stalled on " + expected + " took input: " + machine.inventory.getItem(0),
                    ANCHOR);
        }
    }

    private static final int OUTPUT = AssemblingMachineBlockEntity.INPUTS;

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    private static void keepsItsRecipeOverAReload(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = place(helper);
        CHASSIS.keepsItsRecipeOverAReload(helper, machine, someRecipe(machine));
    }

    /**
     * Every {@code planetaryfactory:assembling} recipe in the server's manager of a category tier 1
     * crafts is one Fill Recipe's
     * setter -- {@code AssemblingMachineMenu.request}, which EMI's packet lands on (#330) -- holds,
     * and it resolves back once held. The recipe viewer is the only picker (ADR-0073, #336), so a
     * recipe this refuses is one no machine can ever make. Against the manager, not the emitted
     * files: a recipe the game rejected at load is {@code check-datapack-load.py}'s.
     */
    private static void holdsEveryAssemblingRecipe(GameTestHelper helper) {
        Set<String> loaded = helper.getLevel().getServer().getRecipeManager().recipeMap()
                .byType(PFRecipes.ASSEMBLING_TYPE.get()).stream()
                .filter(holder -> AssemblingTier.ONE.crafts(holder.value().category()))
                .map(holder -> holder.id().identifier().toString())
                .collect(Collectors.toSet());
        if (!loaded.contains(CABLE)) {
            helper.fail(CABLE + " is not among tier 1's recipes, so this proves nothing");
            return;
        }
        // Its result is a KubeJS item: absent, the harness has stopped loading the pack's registry (#338).
        if (!loaded.contains(CIRCUIT)) {
            helper.fail(CIRCUIT + " is not among tier 1's recipes, so KubeJS's items are not in this world");
            return;
        }
        AssemblingMachineBlockEntity machine = place(helper);
        Player player = player(helper);
        AssemblingMachineMenu menu = AssemblingMachineMenu.open(0, player.getInventory(), machine);
        for (String id : loaded) {
            HoldVerdict verdict = menu.request(player, id);
            if (!verdict.held() || !machine.heldRecipe().equals(HeldRecipe.of(id))) {
                helper.fail("Fill Recipe on " + id + " was answered " + verdict + " and left the machine holding "
                        + machine.heldRecipe(), ANCHOR);
                return;
            }
            if (!machine.heldRecipeResolves()) {
                helper.fail("the machine cannot hold " + id + ": it does not resolve once held", ANCHOR);
                return;
            }
        }
        helper.succeed();
    }

    /**
     * Fill Recipe on a recipe the machine may not hold is refused and changes nothing (#330): an id
     * that is not an assembling recipe and a {@code crafting-with-fluid} one tier 1 has no fluid box for.
     * Left to {@code setHeldRecipe}, each is held and then idles,
     * and the press looked like it worked.
     */
    private static void refusesWhatItCannotHold(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = place(helper);
        Player player = player(helper);
        AssemblingMachineMenu menu = AssemblingMachineMenu.open(0, player.getInventory(), machine);
        String held = CABLE;
        menu.request(player, held);
        HoldVerdict unknown = menu.request(player, "minecraft:stone_bricks");
        if (unknown != HoldVerdict.NOT_ASSEMBLING || !machine.heldRecipe().equals(HeldRecipe.of(held))) {
            helper.fail("a non-assembling id was answered " + unknown + " and left " + machine.heldRecipe(), ANCHOR);
            return;
        }
        HoldVerdict fluid = menu.request(player, CONCRETE);
        if (fluid != HoldVerdict.NOT_THIS_MACHINE || !machine.heldRecipe().equals(HeldRecipe.of(held))) {
            helper.fail("a crafting-with-fluid recipe was answered " + fluid + " and left " + machine.heldRecipe(), ANCHOR);
            return;
        }
        helper.succeed();
    }

    /** What the inputs held goes back to the player; re-picking the same recipe moves nothing. */
    private static void handsBackIngredientsOnAChange(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = place(helper);
        Player player = player(helper);
        List<RecipeChoice> choices = AssemblingMachineRecipes.choices(machine);
        if (choices.size() < 2) {
            helper.fail("fewer than two assembling recipes are loaded");
            return;
        }
        machine.setHeldRecipe(HeldRecipe.of(choices.get(0).id()), player);
        machine.inventory.set(0, ItemResource.of(Items.IRON_INGOT), 7);

        machine.setHeldRecipe(HeldRecipe.of(choices.get(0).id()), player);
        if (machine.inventory.getItem(0).getCount() != 7) {
            helper.fail("re-picking the recipe already held moved the ingredients", ANCHOR);
            return;
        }

        machine.setHeldRecipe(HeldRecipe.of(choices.get(1).id()), player);
        if (!machine.inventory.getItem(0).isEmpty()) {
            helper.fail("a changed recipe left " + machine.inventory.getItem(0) + " in the input", ANCHOR);
            return;
        }
        int returned = player.getInventory().countItem(Items.IRON_INGOT);
        if (returned != 7) {
            helper.fail("a changed recipe handed back " + returned + " of 7 iron ingots", ANCHOR);
            return;
        }
        helper.succeed();
    }

    private static AssemblingMachineBlockEntity place(GameTestHelper helper) {
        return place(helper, AssemblingTier.ONE);
    }

    private static AssemblingMachineBlockEntity place(GameTestHelper helper, AssemblingTier tier) {
        BlockState anchor = PFBlocks.assemblingMachine(tier).get().defaultBlockState()
                .setValue(MultiblockMachine.ASSEMBLED, true);
        helper.setBlock(ANCHOR, anchor);
        return (AssemblingMachineBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(ANCHOR));
    }

    private static Player player(GameTestHelper helper) {
        return helper.makeMockPlayer(GameType.SURVIVAL);
    }

    private static String someRecipe(AssemblingMachineBlockEntity machine) {
        return AssemblingMachineRecipes.choices(machine).getFirst().id();
    }
}
