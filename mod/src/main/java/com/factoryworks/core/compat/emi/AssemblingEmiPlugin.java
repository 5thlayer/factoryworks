package com.factoryworks.core.compat.emi;

import com.factoryworks.core.PFItems;
import com.factoryworks.core.machine.client.AssemblingMachineScreen;
import com.factoryworks.core.PFMenus;
import com.factoryworks.core.FactoryWorksCore;
import com.factoryworks.core.recipes.AssemblingFamily;
import java.util.Set;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import net.minecraft.resources.Identifier;

/**
 * For the reason {@link SmeltingEmiPlugin} exists: EMI has never heard of the type, so without a
 * category its recipes are in no viewer at all. The Oil Refinery is Oil Processing's workstation and
 * its screen takes Fill Recipe (#330, ADR-0073).
 */
@EmiEntrypoint
public final class AssemblingEmiPlugin implements EmiPlugin {

    public static final EmiRecipeCategory OIL_PROCESSING = category(AssemblingFamily.OIL_PROCESSING,
            EmiStack.of(PFItems.OIL_REFINERY.get()));

    /** Every tab whose recipes a chassis machine holds, so Fill Recipe answers on each. */
    static final Set<EmiRecipeCategory> HELD_CATEGORIES = Set.of(OIL_PROCESSING);

    private static EmiRecipeCategory category(AssemblingFamily family, EmiStack icon) {
        return new EmiRecipeCategory(
                Identifier.fromNamespaceAndPath(FactoryWorksCore.NAMESPACE, family.path()), icon);
    }

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(OIL_PROCESSING);
        registry.addWorkstation(OIL_PROCESSING, EmiStack.of(PFItems.OIL_REFINERY.get()));
        // Recipe and usage keys on a tank bar's fluid.
        registry.addStackProvider(AssemblingMachineScreen.class, (screen, x, y) -> screen.fluidBarAt(x, y)
                .map(bar -> new EmiStackInteraction(EmiStack.of(bar.fluid().orElseThrow())))
                .orElse(EmiStackInteraction.EMPTY));
        registry.addRecipeHandler(PFMenus.ASSEMBLING_MACHINE.get(), new AssemblingMachineEmiHandler());
        addRecipes(registry, OIL_PROCESSING, AssemblingFamily.OIL_PROCESSING);
    }

    private static void addRecipes(EmiRegistry registry, EmiRecipeCategory category, AssemblingFamily family) {
        registry.getRecipeMap()
                .byType(family.type())
                .forEach(holder -> registry.addRecipe(new AssemblingEmiRecipe(category, holder)));
    }
}
