package com.planetaryfactory.core.gametest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.machine.AssemblingMachineBlockEntity;
import com.planetaryfactory.core.machine.AssemblingMachineRecipes;
import com.planetaryfactory.core.machine.AssemblingStall;
import com.planetaryfactory.core.machine.HeldRecipe;
import com.planetaryfactory.core.machine.RecipeChoice;
import com.planetaryfactory.core.recipes.PFRecipes;

import net.minecraft.core.BlockPos;
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
import net.neoforged.neoforge.transfer.item.ItemResource;
import rearth.oritech.block.base.block.MultiblockMachine;

/**
 * The Assembling Machine's Held recipe (#327), where only a running server reaches it.
 *
 * <p>The codec's round trip is {@code HeldRecipeTest}'s. What is left is the seam around it: that
 * the block entity's save hook actually writes the field and its load hook reads it back into a
 * recipe the server resolves; that every assembling recipe the server loaded is one the widget
 * offers and the machine can hold -- the check that replaces #237 and #238; and that changing the
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
        tests.test("assembling_machine_offers_every_assembling_recipe", 20,
                AssemblingMachineTests::offersEveryAssemblingRecipe);
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
    }

    /** copper-cable: one copper plate makes two wire in Factorio's 0.5 s. */
    private static final String CABLE = "planetaryfactory:assembling/copper_cable";

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
                    AssemblingMachineRecipes.lockForTest(GEAR::equals);
                    machine.setHeldRecipe(HeldRecipe.of(GEAR), player(helper));
                    machine.inventory.set(0, ItemResource.of(item("ftbmaterials:iron_plate")), 4);
                    machine.energyStorage.set(CHARGE);
                })
                .thenIdle(WINDOW)
                .thenExecute(() -> {
                    AssemblingMachineRecipes.lockForTest(null);
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
     * Every {@code planetaryfactory:assembling} recipe in the server's manager is in the widget's
     * list, and holding it resolves back to it. Against the manager, not the emitted files: a
     * recipe the game rejected at load is {@code check-datapack-load.py}'s.
     */
    private static void offersEveryAssemblingRecipe(GameTestHelper helper) {
        Set<String> loaded = helper.getLevel().getServer().getRecipeManager().recipeMap()
                .byType(PFRecipes.ASSEMBLING_TYPE.get()).stream()
                .map(holder -> holder.id().identifier().toString())
                .collect(Collectors.toSet());
        if (loaded.isEmpty()) {
            helper.fail("the recipe manager holds no assembling recipe, so this proves nothing");
            return;
        }
        Set<String> offered = AssemblingMachineRecipes.choices(helper.getLevel()).stream()
                .map(RecipeChoice::id)
                .collect(Collectors.toSet());
        if (!offered.equals(loaded)) {
            helper.fail("the widget offers " + offered.size() + " recipe(s) of " + loaded.size()
                    + "; missing " + loaded.stream().filter(id -> !offered.contains(id)).toList());
            return;
        }
        AssemblingMachineBlockEntity machine = place(helper);
        Player player = player(helper);
        for (String id : loaded) {
            machine.setHeldRecipe(HeldRecipe.of(id), player);
            if (!machine.heldRecipeResolves()) {
                helper.fail("the machine cannot hold " + id + ": it does not resolve once held", ANCHOR);
                return;
            }
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
