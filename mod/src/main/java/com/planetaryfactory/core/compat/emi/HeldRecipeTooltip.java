package com.planetaryfactory.core.compat.emi;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.render.EmiTooltipComponents;
import dev.emi.emi.api.stack.EmiStack;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The Assembling Machine screen's Held recipe as EMI draws a recipe-bearing stack (#336): the
 * result's tooltip with the recipe beneath it, and a click that opens the recipe. Reached only
 * when EMI is loaded; without it the screen falls back to the item's own tooltip.
 */
public final class HeldRecipeTooltip {

    private HeldRecipeTooltip() {
    }

    /** The result's tooltip and the recipe drawn under it, or the result's alone if EMI lacks it. */
    public static List<ClientTooltipComponent> components(ItemStack result, String recipeId) {
        List<ClientTooltipComponent> lines = new ArrayList<>(EmiStack.of(result).getTooltip());
        EmiRecipe recipe = recipe(recipeId);
        if (recipe != null) {
            lines.add(EmiTooltipComponents.getRecipeTooltipComponent(recipe));
        }
        return lines;
    }

    /** Opens the recipe in EMI; false if EMI does not know it. */
    public static boolean display(String recipeId) {
        EmiRecipe recipe = recipe(recipeId);
        if (recipe == null) {
            return false;
        }
        EmiApi.displayRecipe(recipe);
        return true;
    }

    private static EmiRecipe recipe(String recipeId) {
        Identifier id = Identifier.tryParse(recipeId);
        return id == null ? null : EmiApi.getRecipeManager().getRecipe(id);
    }
}
