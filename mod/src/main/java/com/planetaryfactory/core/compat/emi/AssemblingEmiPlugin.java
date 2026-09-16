package com.planetaryfactory.core.compat.emi;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.recipes.PFRecipes;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

/**
 * Puts {@code planetaryfactory:assembling} in EMI (#279).
 *
 * <p>For the reason {@link SmeltingEmiPlugin} exists: EMI has never heard of the type, so without a
 * category its recipes are in no viewer at all -- and the Personal Assembler's Fill Recipe button is
 * drawn on a recipe EMI shows, so an unshown hand recipe is also an unplannable one.
 *
 * <p>No workstation. The Assembling Machine is #277's, and the Personal Assembler is the inventory screen rather
 * than a block; the icon is a stand-in until the machine exists.
 */
@EmiEntrypoint
public final class AssemblingEmiPlugin implements EmiPlugin {

    public static final EmiRecipeCategory ASSEMBLING = new EmiRecipeCategory(
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, PFRecipes.ASSEMBLING),
            EmiStack.of(Items.CRAFTING_TABLE));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(ASSEMBLING);
        registry.getRecipeMap()
                .byType(PFRecipes.ASSEMBLING_TYPE.get())
                .forEach(holder -> registry.addRecipe(new AssemblingEmiRecipe(ASSEMBLING, holder)));
    }
}
