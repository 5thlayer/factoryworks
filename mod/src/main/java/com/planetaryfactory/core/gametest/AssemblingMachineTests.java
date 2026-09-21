package com.planetaryfactory.core.gametest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.machine.AssemblingMachineBlockEntity;
import com.planetaryfactory.core.machine.AssemblingMachineRecipes;
import com.planetaryfactory.core.machine.HeldRecipe;
import com.planetaryfactory.core.machine.RecipeChoice;
import com.planetaryfactory.core.recipes.PFRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
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
 * recipe hands the player back what the inputs held.
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
