package com.factoryworks.core.gametest;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.factoryworks.core.machine.AssemblingMachineRecipes;
import com.factoryworks.core.machine.AssemblingTier;
import com.factoryworks.core.machine.HeldRecipe;
import com.factoryworks.core.recipes.AssemblingFamily;
import com.factoryworks.core.recipes.AssemblingRecipe;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * The three types on {@link AssemblingRecipe}'s shape, as the server loaded them (#488, ADR-0096).
 *
 * <p>A GameTest rather than a unit test because the codec is built from NeoForge's ingredient codecs,
 * which the unit-test classpath does not have. The family is the serializer's and not the JSON's, so
 * the round trip goes through {@link Recipe#CODEC}, whose {@code type} key is what picks it.
 */
final class AssemblingFamilyTests {

    private AssemblingFamilyTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("each_family_round_trips_its_recipes", 20, AssemblingFamilyTests::eachFamilyRoundTrips);
        tests.test("an_assembling_machine_holds_no_chemistry", 20, AssemblingFamilyTests::noChemistryHeld);
    }

    private static void eachFamilyRoundTrips(GameTestHelper helper) {
        DynamicOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        for (AssemblingFamily family : AssemblingFamily.values()) {
            var holders = helper.getLevel().getServer().getRecipeManager().recipeMap().byType(family.type());
            if (holders.isEmpty()) {
                helper.fail("the server loaded no factoryworks:" + family.path() + " recipe");
                return;
            }
            for (RecipeHolder<AssemblingRecipe> holder : holders) {
                String id = holder.id().identifier().toString();
                if (!id.startsWith("factoryworks:" + family.path() + "/")) {
                    helper.fail(id + " loaded as factoryworks:" + family.path());
                    return;
                }
                if (holder.value().family() != family) {
                    helper.fail(id + " decoded as " + holder.value().family() + ", not " + family);
                    return;
                }
                JsonElement encoded = Recipe.CODEC.encodeStart(ops, holder.value()).getOrThrow();
                Recipe<?> back = Recipe.CODEC.parse(ops, encoded).getOrThrow();
                if (!(back instanceof AssemblingRecipe recipe) || recipe.family() != family
                        || !encoded.equals(Recipe.CODEC.encodeStart(ops, back).getOrThrow())) {
                    helper.fail(id + " does not survive the round trip: " + encoded);
                    return;
                }
            }
        }
        helper.succeed();
    }

    private static void noChemistryHeld(GameTestHelper helper) {
        String plastic = "factoryworks:chemistry/plastic_bar";
        if (AssemblingMachineRecipes.resolve(helper.getLevel(), HeldRecipe.of(plastic), AssemblingTier.ONE.spec()).isPresent()) {
            helper.fail("an Assembling Machine resolves " + plastic);
            return;
        }
        helper.succeed();
    }
}
