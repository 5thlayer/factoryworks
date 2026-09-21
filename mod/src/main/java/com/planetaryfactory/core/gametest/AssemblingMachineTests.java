package com.planetaryfactory.core.gametest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.energy.PoleTier;
import com.planetaryfactory.core.energy.SupplyAreaPoleBlockEntity;
import com.planetaryfactory.core.machine.AssemblingMachineBlockEntity;
import com.planetaryfactory.core.machine.AssemblingMachineHull;
import com.planetaryfactory.core.machine.AssemblingMachineItem;
import com.planetaryfactory.core.machine.AssemblingMachineMenu;
import com.planetaryfactory.core.machine.AssemblingMachineRecipes;
import com.planetaryfactory.core.machine.AssemblingStall;
import com.planetaryfactory.core.machine.HeldRecipe;
import com.planetaryfactory.core.machine.HoldVerdict;
import com.planetaryfactory.core.machine.RecipeChoice;
import com.planetaryfactory.core.recipes.PFRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
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
        tests.test("assembling_machine_stalls_on_a_full_output", 100,
                AssemblingMachineTests::stallsOnAFullOutput);
        tests.test("assembling_machine_stalls_unfed", 100,
                AssemblingMachineTests::stallsUnfed);
        tests.test("assembling_machine_stalls_on_a_locked_recipe", 100,
                AssemblingMachineTests::stallsOnALockedRecipe);
        tests.test("assembling_machine_is_powered_by_a_pole", 100,
                AssemblingMachineTests::isPoweredByAPole);
        tests.test("assembling_machine_is_found_through_a_hull_block", 100,
                AssemblingMachineTests::poleReachingOnlyAHullBlockFindsIt);
        tests.test("assembling_machine_filters_inputs_to_its_recipe", 20,
                AssemblingMachineTests::filtersInputsToItsRecipe);
        tests.test("assembling_machine_with_no_recipe_takes_nothing", 20,
                AssemblingMachineTests::withNoRecipeTakesNothing);
        tests.test("assembling_machine_input_mode_stays_pinned", 20,
                AssemblingMachineTests::inputModeStaysPinned);
    }

    /**
     * Two ingredients, so slots 2 and 3 are unused. Not electronic-circuit: KubeJS registers its
     * item, and the harness has no KubeJS.
     */
    private static final String BOILER = "planetaryfactory:assembling/boiler";

    /** Through the capability, on the anchor and a hull block, on both overloads (ADR-0073). */
    private static void filtersInputsToItsRecipe(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = placeWhole(helper);
        machine.setHeldRecipe(HeldRecipe.of(BOILER), player(helper));
        if (!machine.heldRecipeResolves()) {
            helper.fail(BOILER + " is not loaded, so the filter has no recipe to filter to", ANCHOR);
            return;
        }
        BlockPos hull = AssemblingMachineHull.positions(ANCHOR, FACING).stream()
                .filter(pos -> !pos.equals(ANCHOR)).findFirst().orElseThrow();
        ItemResource furnace = ItemResource.of(item("planetaryfactory:stone_furnace"));
        ItemResource fluidPipe = ItemResource.of(item("oritech:fluid_pipe"));
        ItemResource stone = ItemResource.of(Items.STONE);
        for (BlockPos at : List.of(ANCHOR, hull)) {
            ResourceHandler<ItemResource> face = itemFace(helper, at);
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
            expectMoved(helper, at, "furnace, slot-less", 8, face, (f, tx) -> f.insert(furnace, 8, tx));
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
        ResourceHandler<ItemResource> face = itemFace(helper, ANCHOR);
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
        ResourceHandler<ItemResource> face = itemFace(helper, ANCHOR);
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

    /** One transfer through a face, inside the transaction {@link #expectMoved} opens. */
    private interface Move {
        int apply(ResourceHandler<ItemResource> face, Transaction tx);
    }

    /** Runs {@code move} in a committed transaction and fails unless it moved {@code expected}. */
    private static void expectMoved(GameTestHelper helper, BlockPos at, String what, int expected,
            ResourceHandler<ItemResource> face, Move move) {
        int moved;
        try (Transaction tx = Transaction.openRoot()) {
            moved = move.apply(face, tx);
            tx.commit();
        }
        if (moved != expected) {
            helper.fail(what + " moved " + moved + ", expected " + expected, at);
        }
    }

    private static ResourceHandler<ItemResource> itemFace(GameTestHelper helper, BlockPos at) {
        return helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(at), null);
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
     * A creative pole beside a whole machine -- anchor and three hull blocks, as the item places it
     * -- counts it once and fills it. Once, not four times: a hull block has no block entity and
     * answers the anchor's face, and a scan that did not resolve it to the anchor would offer and
     * draw one machine per block (#328 in-world: first "Machines in area: 0", with no face at all).
     */
    private static void isPoweredByAPole(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = placeWhole(helper);
        BlockPos pole = ANCHOR.south(2);
        helper.startSequence()
                .thenExecute(() -> {
                    machine.energyStorage.set(0L);
                    helper.setBlock(pole, PFBlocks.CREATIVE_POLE.get());
                })
                .thenIdle(RESCAN_INTERVAL + 5)
                .thenExecute(() -> {
                    int found = helper.getBlockEntity(pole, SupplyAreaPoleBlockEntity.class).machineCount();
                    if (found != 1) {
                        helper.fail("a pole reaching one whole Assembling Machine counts " + found
                                + " machines", ANCHOR);
                    }
                    if (machine.energyStorage.getAmountAsLong() <= 0L) {
                        helper.fail("a creative pole beside the machine left it unpowered", ANCHOR);
                    }
                })
                .thenSucceed();
    }

    /**
     * A small pole whose 5x5 reaches a hull block and not the anchor still finds the machine.
     * Which block is the anchor is not visible to a player, so a pole placed against the far side
     * must not miss it.
     */
    private static void poleReachingOnlyAHullBlockFindsIt(GameTestHelper helper) {
        placeWhole(helper);
        List<BlockPos> blocks = AssemblingMachineHull.positions(ANCHOR, FACING);
        BlockPos part = blocks.stream()
                .filter(pos -> pos.getY() == ANCHOR.getY() && !pos.equals(ANCHOR))
                .findFirst().orElseThrow();
        BlockPos step = part.subtract(ANCHOR);
        // Two blocks past the hull block: inside a small pole's +-2, and the anchor at 3 is not.
        BlockPos pole = part.offset(step.multiply(2));
        helper.startSequence()
                .thenExecute(() -> helper.setBlock(pole, PFBlocks.pole(PoleTier.SMALL).get()))
                .thenIdle(RESCAN_INTERVAL + 5)
                .thenExecute(() -> {
                    int found = helper.getBlockEntity(pole, SupplyAreaPoleBlockEntity.class).machineCount();
                    if (found != 1) {
                        helper.fail("a small pole reaching only a hull block counts " + found
                                + " machines, expected 1", pole);
                    }
                })
                .thenSucceed();
    }

    /** A pole rescans at most this many ticks after a machine appears (EnergyFaceTests' figure). */
    private static final int RESCAN_INTERVAL = 40;

    private static final Direction FACING = Direction.NORTH;

    /** The anchor and its three hull blocks, in the states the item places them in. */
    private static AssemblingMachineBlockEntity placeWhole(GameTestHelper helper) {
        List<BlockPos> blocks = AssemblingMachineHull.positions(ANCHOR, FACING);
        for (int i = 0; i < blocks.size(); i++) {
            helper.setBlock(blocks.get(i), AssemblingMachineItem.stateAt(i, FACING));
        }
        return (AssemblingMachineBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(ANCHOR));
    }

    /** copper-cable: one copper plate makes two wire in Factorio's 0.5 s. */
    private static final String CABLE = "planetaryfactory:assembling/copper_cable";

    /** The pipe recipe, which the refusal test locks and no other test crafts. */
    private static final String PIPE = "planetaryfactory:assembling/pipe";

    /** iron-gear-wheel, which the lock test locks and no other test crafts. */
    private static final String GEAR = "planetaryfactory:assembling/iron_gear_wheel";

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
        AssemblingMachineBlockEntity machine = place(helper);
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
                .thenIdle(WINDOW)
                .thenExecute(() -> {
                    long spent = energy[0] - machine.energyStorage.getAmountAsLong();
                    if (spent != 2 * FE_PER_CRAFT) {
                        helper.fail("two crafts' window drew " + spent + " FE, expected " + (2 * FE_PER_CRAFT),
                                ANCHOR);
                    }
                    int made = machine.inventory.getItem(OUTPUT).getCount() - wire[0];
                    if (made != 4) {
                        helper.fail("two crafts' window made " + made + " copper wire, expected 4", ANCHOR);
                    }
                    if (machine.inventory.getItem(0).getCount() + machine.inventory.getItem(OUTPUT).getCount() / 2
                            != 8) {
                        helper.fail("plates and wire do not account for the 8 plates put in", ANCHOR);
                    }
                })
                .thenSucceed();
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
     * Fed, powered and with room, but the recipe is locked. Nothing is locked until Researchd
     * returns (#260), so the lock is the stand-in {@link AssemblingMachineRecipes#lockForTest}, on a
     * recipe no other test crafts. It is lifted before the assertions run; a sequence that
     * times out before then leaves it set, which only this test's recipe notices.
     */
    private static void stallsOnALockedRecipe(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = place(helper);
        helper.startSequence()
                .thenExecute(() -> {
                    AssemblingMachineRecipes.lockForTest(GEAR);
                    machine.setHeldRecipe(HeldRecipe.of(GEAR), player(helper));
                    machine.inventory.set(0, ItemResource.of(item("ftbmaterials:iron_plate")), 4);
                    machine.energyStorage.set(CHARGE);
                })
                .thenIdle(WINDOW)
                .thenExecute(() -> {
                    AssemblingMachineRecipes.unlockForTest(GEAR);
                    assertStalled(helper, machine, AssemblingStall.LOCKED, 4, GEAR);
                })
                .thenSucceed();
    }

    /** A stall draws no FE, takes no input, makes no progress and keeps its recipe. */
    private static void assertStalled(GameTestHelper helper, AssemblingMachineBlockEntity machine,
            AssemblingStall expected, int inputs, String held) {
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
        if (machine.inventory.getItem(0).getCount() != inputs) {
            helper.fail("a machine stalled on " + expected + " took input: " + machine.inventory.getItem(0),
                    ANCHOR);
        }
        if (!machine.heldRecipe().equals(HeldRecipe.of(held))) {
            helper.fail("a machine stalled on " + expected + " let go of its recipe", ANCHOR);
        }
    }

    private static final int OUTPUT = AssemblingMachineBlockEntity.INPUTS;

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    /**
     * Saved and loaded through the same tag a chunk save writes. Checked against its defect:
     * dropping the {@code store} from {@code saveAdditional} turns it red.
     */
    private static void keepsItsRecipeOverAReload(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = place(helper);
        String id = someRecipe(helper);
        machine.setHeldRecipe(HeldRecipe.of(id), player(helper));

        CompoundTag saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(machine.getBlockPos(), machine.getBlockState(), saved,
                helper.getLevel().registryAccess());
        if (!(loaded instanceof AssemblingMachineBlockEntity reloaded)) {
            helper.fail("the saved machine reloaded as " + loaded, ANCHOR);
            return;
        }
        if (!reloaded.heldRecipe().equals(HeldRecipe.of(id))) {
            helper.fail("the machine held " + id + " and reloaded holding " + reloaded.heldRecipe(), ANCHOR);
            return;
        }
        reloaded.setLevel(helper.getLevel());
        if (!reloaded.heldRecipeResolves()) {
            helper.fail("the reloaded machine's " + id + " does not resolve against the recipe manager",
                    ANCHOR);
            return;
        }
        helper.succeed();
    }

    /**
     * Every {@code planetaryfactory:assembling} recipe in the server's manager is one Fill Recipe's
     * setter -- {@code AssemblingMachineMenu.request}, which EMI's packet lands on (#330) -- holds,
     * and it resolves back once held. The recipe viewer is the only picker (ADR-0073, #336), so a
     * recipe this refuses is one no machine can ever make. Against the manager, not the emitted
     * files: a recipe the game rejected at load is {@code check-datapack-load.py}'s.
     */
    private static void holdsEveryAssemblingRecipe(GameTestHelper helper) {
        Set<String> loaded = helper.getLevel().getServer().getRecipeManager().recipeMap()
                .byType(PFRecipes.ASSEMBLING_TYPE.get()).stream()
                .map(holder -> holder.id().identifier().toString())
                .collect(Collectors.toSet());
        if (loaded.isEmpty()) {
            helper.fail("the recipe manager holds no assembling recipe, so this proves nothing");
            return;
        }
        AssemblingMachineBlockEntity machine = place(helper);
        Player player = player(helper);
        AssemblingMachineMenu menu = AssemblingMachineMenu.open(0, player.getInventory(), machine);
        for (String id : loaded) {
            if (AssemblingMachineRecipes.isLockedForTest(id)) {
                continue;
            }
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
     * that is not an assembling recipe, and one research has not unlocked. Left to
     * {@code setHeldRecipe}, both are held -- the first then idles as "no recipe" and the second as
     * locked, and the press looked like it worked.
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
        // No other test crafts or locks this one; the lock set is shared by the batch.
        String other = PIPE;
        AssemblingMachineRecipes.lockForTest(other);
        try {
            HoldVerdict locked = menu.request(player, other);
            if (locked != HoldVerdict.LOCKED || !machine.heldRecipe().equals(HeldRecipe.of(held))) {
                helper.fail("a locked recipe was answered " + locked + " and left " + machine.heldRecipe(), ANCHOR);
                return;
            }
        } finally {
            AssemblingMachineRecipes.unlockForTest(other);
        }
        helper.succeed();
    }

    /** What the inputs held goes back to the player; re-picking the same recipe moves nothing. */
    private static void handsBackIngredientsOnAChange(GameTestHelper helper) {
        AssemblingMachineBlockEntity machine = place(helper);
        Player player = player(helper);
        List<RecipeChoice> choices = AssemblingMachineRecipes.choices(helper.getLevel());
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
        BlockState anchor = PFBlocks.ASSEMBLING_MACHINE.get().defaultBlockState()
                .setValue(MultiblockMachine.ASSEMBLED, true);
        helper.setBlock(ANCHOR, anchor);
        return (AssemblingMachineBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(ANCHOR));
    }

    private static Player player(GameTestHelper helper) {
        return helper.makeMockPlayer(GameType.SURVIVAL);
    }

    private static String someRecipe(GameTestHelper helper) {
        return AssemblingMachineRecipes.choices(helper.getLevel()).getFirst().id();
    }
}
