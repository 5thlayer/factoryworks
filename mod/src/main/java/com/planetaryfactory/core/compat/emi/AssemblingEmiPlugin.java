package com.planetaryfactory.core.compat.emi;

import com.planetaryfactory.core.PFItems;
import com.planetaryfactory.core.machine.AssemblingTier;
import com.planetaryfactory.core.PFMenus;
import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.recipes.PFRecipes;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.Identifier;

/**
 * Puts {@code planetaryfactory:assembling} in EMI (#279).
 *
 * <p>For the reason {@link SmeltingEmiPlugin} exists: EMI has never heard of the type, so without a
 * category its recipes are in no viewer at all.
 *
 * <p>The Assembling Machine is the category's workstation and icon, and its screen takes Fill Recipe
 * (#330, ADR-0073).
 */
@EmiEntrypoint
public final class AssemblingEmiPlugin implements EmiPlugin {

    public static final EmiRecipeCategory ASSEMBLING = new EmiRecipeCategory(
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, PFRecipes.ASSEMBLING),
            EmiStack.of(PFItems.assemblingMachine(AssemblingTier.ONE).get()));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(ASSEMBLING);
        for (AssemblingTier tier : AssemblingTier.values()) {
            registry.addWorkstation(ASSEMBLING, EmiStack.of(PFItems.assemblingMachine(tier).get()));
        }
        registry.addRecipeHandler(PFMenus.ASSEMBLING_MACHINE.get(), new AssemblingMachineEmiHandler());
        registry.getRecipeMap()
                .byType(PFRecipes.ASSEMBLING_TYPE.get())
                .forEach(holder -> registry.addRecipe(new AssemblingEmiRecipe(ASSEMBLING, holder)));
    }
}
