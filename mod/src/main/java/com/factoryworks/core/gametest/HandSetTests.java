package com.factoryworks.core.gametest;

import io.github._5thlayer.craftworks.planner.AssemblingRecipe;
import io.github._5thlayer.craftworks.planner.AssemblingRecipeSet;
import io.github._5thlayer.craftworks.recipe.RuntimeAssemblingRecipes;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * That Craftworks plans the pack's hand copies (#291, ADR-0089).
 *
 * <p>The copies' JSON is {@code test_hand_recipes.py}'s. What no static check reaches is whether
 * Craftworks reads them: a copy it refuses, such as one whose tag names no item, only prints a
 * warning. The wooden chest is asserted for its tag ingredient, and concrete for being left out,
 * since it needs a fluid.
 */
final class HandSetTests {

    private static final String WOODEN_CHEST = "factoryworks:hand/wooden_chest";
    private static final String CONCRETE = "factoryworks:hand/concrete";

    private HandSetTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("craftworks_plans_the_hand_copies", 20, HandSetTests::craftworksPlansTheHandCopies);
    }

    private static void craftworksPlansTheHandCopies(GameTestHelper helper) {
        AssemblingRecipeSet recipes = RuntimeAssemblingRecipes.recipes(helper.getLevel());
        long copies = helper.getLevel().getServer().getRecipeManager().recipeMap().values().stream()
                .filter(holder -> holder.id().identifier().toString().startsWith("factoryworks:hand/"))
                .count();
        long planned = recipes.ids().stream().filter(id -> id.startsWith("factoryworks:hand/")).count();
        if (copies == 0 || planned != copies) {
            helper.fail("Craftworks plans " + planned + " of the " + copies + " hand copies the server loaded");
            return;
        }
        AssemblingRecipe chest = recipes.byId(WOODEN_CHEST);
        if (chest == null) {
            helper.fail("Craftworks does not plan " + WOODEN_CHEST);
            return;
        }
        if (!chest.ingredients().getFirst().items().contains("minecraft:oak_log")) {
            helper.fail(WOODEN_CHEST + " takes " + chest.ingredients() + "; #minecraft:logs should"
                    + " arrive as the logs it names");
            return;
        }
        if (!recipes.routes("minecraft:chest").contains(chest)) {
            helper.fail("Craftworks does not know " + WOODEN_CHEST + " makes a chest");
            return;
        }
        if (recipes.byId(CONCRETE) != null) {
            helper.fail(CONCRETE + " needs a fluid and has a hand copy");
            return;
        }
        helper.succeed();
    }
}
