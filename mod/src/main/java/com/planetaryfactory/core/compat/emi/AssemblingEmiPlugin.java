package com.planetaryfactory.core.compat.emi;

import com.planetaryfactory.core.PFItems;
import com.planetaryfactory.core.machine.AssemblingTier;
import com.planetaryfactory.core.PFMenus;
import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.recipes.AssemblingFamily;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * For the reason {@link SmeltingEmiPlugin} exists: EMI has never heard of the types, so without a
 * category their recipes are in no viewer at all.
 *
 * <p>The Assembling Machine is its category's workstation and icon, and its screen takes Fill Recipe
 * (#330, ADR-0073). The Chemical Plant and Oil Refinery are not blocks yet (#486), so their tabs wear
 * the Oritech models ADR-0096 puts them on and have no workstation.
 */
@EmiEntrypoint
public final class AssemblingEmiPlugin implements EmiPlugin {

    public static final EmiRecipeCategory ASSEMBLING = category(AssemblingFamily.ASSEMBLING,
            EmiStack.of(PFItems.assemblingMachine(AssemblingTier.ONE).get()));

    public static final EmiRecipeCategory CHEMISTRY = category(AssemblingFamily.CHEMISTRY, oritechIcon("centrifuge"));

    public static final EmiRecipeCategory OIL_PROCESSING =
            category(AssemblingFamily.OIL_PROCESSING, oritechIcon("refinery"));

    private static EmiRecipeCategory category(AssemblingFamily family, EmiStack icon) {
        return new EmiRecipeCategory(
                Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, family.path()), icon);
    }

    private static EmiStack oritechIcon(String path) {
        return EmiStack.of(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("oritech", path)));
    }

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(ASSEMBLING);
        registry.addCategory(CHEMISTRY);
        registry.addCategory(OIL_PROCESSING);
        for (AssemblingTier tier : AssemblingTier.values()) {
            registry.addWorkstation(ASSEMBLING, EmiStack.of(PFItems.assemblingMachine(tier).get()));
        }
        registry.addRecipeHandler(PFMenus.ASSEMBLING_MACHINE.get(), new AssemblingMachineEmiHandler());
        addRecipes(registry, ASSEMBLING, AssemblingFamily.ASSEMBLING);
        addRecipes(registry, CHEMISTRY, AssemblingFamily.CHEMISTRY);
        addRecipes(registry, OIL_PROCESSING, AssemblingFamily.OIL_PROCESSING);
    }

    private static void addRecipes(EmiRegistry registry, EmiRecipeCategory category, AssemblingFamily family) {
        registry.getRecipeMap()
                .byType(family.type())
                .forEach(holder -> registry.addRecipe(new AssemblingEmiRecipe(category, holder)));
    }
}
