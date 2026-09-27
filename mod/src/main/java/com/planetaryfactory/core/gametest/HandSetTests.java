package com.planetaryfactory.core.gametest;

import com.planetaryfactory.core.assembler.HandRecipe;
import com.planetaryfactory.core.assembler.RecipeGraph;
import com.planetaryfactory.core.assembler.RuntimeHandRecipes;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * That the Personal Assembler's hand set is read off {@code planetaryfactory:assembling} (#279).
 *
 * <p>The admission rule is {@code HandSetAdmissionTest}'s and the emitted JSON is
 * {@code check-datapack-load.py}'s. What neither reaches is the glue between them: that the server's
 * recipe manager holds the pack's recipes under the pack's type, that {@code RuntimeHandRecipes}
 * finds them there, and that a tag ingredient arrives as the items it names. A graph read off the
 * wrong type is empty with nothing logged, which is the state the port left the Assembler in.
 *
 * <p>The recipes asserted are the wooden chest, for its tag ingredient, and concrete, for its fluid.
 */
final class HandSetTests {

    private static final String WOODEN_CHEST = "planetaryfactory:assembling/wooden_chest";
    private static final String CONCRETE = "planetaryfactory:assembling/concrete";

    private HandSetTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("hand_set_reads_the_assembling_type", 20, HandSetTests::handSetReadsTheAssemblingType);
    }

    private static void handSetReadsTheAssemblingType(GameTestHelper helper) {
        RecipeGraph graph = RuntimeHandRecipes.graph(helper.getLevel());
        HandRecipe chest = graph.byId(WOODEN_CHEST);
        if (chest == null) {
            helper.fail("the hand set holds " + graph.size() + " recipe(s) and not " + WOODEN_CHEST);
            return;
        }
        if (!chest.inputs().getFirst().items().contains("minecraft:oak_log")) {
            helper.fail(WOODEN_CHEST + " takes " + chest.inputs() + "; #minecraft:logs should"
                    + " arrive as the logs it names");
            return;
        }
        if (graph.makerOf("minecraft:chest") != chest) {
            helper.fail("the graph does not know " + WOODEN_CHEST + " makes a chest");
            return;
        }
        // Loaded, but crafting-with-fluid: the machine's, never the hand's.
        boolean loaded = helper.getLevel().getServer().getRecipeManager().recipeMap().values().stream()
                .anyMatch(holder -> holder.id().identifier().toString().equals(CONCRETE));
        if (!loaded) {
            helper.fail(CONCRETE + " is not in the recipe manager, so its absence below proves nothing");
            return;
        }
        if (graph.byId(CONCRETE) != null) {
            helper.fail(CONCRETE + " is crafting-with-fluid and is in the hand set");
            return;
        }
        helper.succeed();
    }
}
