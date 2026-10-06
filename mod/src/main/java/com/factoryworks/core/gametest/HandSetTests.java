package com.factoryworks.core.gametest;

import io.github._5thlayer.craftworks.planner.AssemblingRecipe;
import io.github._5thlayer.craftworks.planner.AssemblingRecipeSet;
import io.github._5thlayer.craftworks.recipe.CraftworksRecipes;
import io.github._5thlayer.craftworks.recipe.RuntimeAssemblingRecipes;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * That Craftworks' Personal Assembler plans the pack's hand-craftable recipes (ADR-0118).
 *
 * <p>The flag's JSON is {@code test_hand_recipes.py}'s. What no static check reaches is whether
 * Craftworks reads them: a recipe it refuses, such as one whose tag names no item, only prints a
 * warning. The wooden chest is asserted for its tag ingredient, and concrete and the engine unit for
 * being left out, since one needs a fluid and the other is not hand-craftable. The planner is
 * Craftworks' internals until craftworks#37 names a Consumer API.
 */
final class HandSetTests {

    private static final String PREFIX = "factoryworks:assembling/";
    private static final String WOODEN_CHEST = PREFIX + "wooden_chest";
    private static final String CONCRETE = PREFIX + "concrete";
    private static final String ENGINE_UNIT = PREFIX + "engine_unit";

    private HandSetTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("craftworks_plans_the_hand_craftable_recipes", 20, HandSetTests::craftworksPlansTheHandSet);
    }

    private static void craftworksPlansTheHandSet(GameTestHelper helper) {
        AssemblingRecipeSet recipes = RuntimeAssemblingRecipes.recipes(helper.getLevel());
        long handCraftable = helper.getLevel().getServer().getRecipeManager().recipeMap()
                .byType(CraftworksRecipes.ASSEMBLING_TYPE.get()).stream()
                .filter(holder -> holder.id().identifier().toString().startsWith(PREFIX))
                .map(RecipeHolder::value)
                .filter(recipe -> recipe.handCraftable() && recipe.results().size() == 1
                        && recipe.fluidIngredients().isEmpty() && recipe.fluidResults().isEmpty())
                .count();
        long planned = recipes.ids().stream().filter(id -> id.startsWith(PREFIX)).count();
        if (handCraftable == 0 || planned != handCraftable) {
            helper.fail("Craftworks plans " + planned + " of the " + handCraftable
                    + " hand-craftable recipes the server loaded");
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
            helper.fail(CONCRETE + " needs a fluid and is planned");
            return;
        }
        if (recipes.byId(ENGINE_UNIT) != null) {
            helper.fail(ENGINE_UNIT + " is not hand-craftable and is planned");
            return;
        }
        helper.succeed();
    }
}
