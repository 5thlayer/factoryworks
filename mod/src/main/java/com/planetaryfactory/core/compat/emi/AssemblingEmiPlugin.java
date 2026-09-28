package com.planetaryfactory.core.compat.emi;

import com.planetaryfactory.core.PFItems;
import com.planetaryfactory.core.machine.AssemblingTier;
import com.planetaryfactory.core.machine.client.AssemblingMachineScreen;
import com.planetaryfactory.core.PFMenus;
import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.recipes.AssemblingFamily;
import java.util.Set;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * For the reason {@link SmeltingEmiPlugin} exists: EMI has never heard of the types, so without a
 * category their recipes are in no viewer at all.
 *
 * <p>The Assembling Machine is its category's workstation and icon, and its screen takes Fill Recipe
 * (#330, ADR-0073), and the Chemical Plant is Chemistry's (ADR-0096).
 */
@EmiEntrypoint
public final class AssemblingEmiPlugin implements EmiPlugin {

    public static final EmiRecipeCategory ASSEMBLING = category(AssemblingFamily.ASSEMBLING,
            EmiStack.of(PFItems.assemblingMachine(AssemblingTier.ONE).get()));

    public static final EmiRecipeCategory CHEMISTRY = category(AssemblingFamily.CHEMISTRY,
            EmiStack.of(PFItems.CHEMICAL_PLANT.get()));

    public static final EmiRecipeCategory OIL_PROCESSING =
            category(AssemblingFamily.OIL_PROCESSING, oritechIcon("refinery"));

    /** Every tab whose recipes a chassis machine holds, so Fill Recipe answers on each. */
    static final Set<EmiRecipeCategory> HELD_CATEGORIES = Set.of(ASSEMBLING, CHEMISTRY);

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
        registry.addWorkstation(CHEMISTRY, EmiStack.of(PFItems.CHEMICAL_PLANT.get()));
        // Recipe and usage keys on a tank bar's fluid.
        registry.addStackProvider(AssemblingMachineScreen.class, (screen, x, y) -> screen.fluidBarAt(x, y)
                .map(bar -> new EmiStackInteraction(EmiStack.of(bar.fluid().orElseThrow())))
                .orElse(EmiStackInteraction.EMPTY));
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
