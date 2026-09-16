package com.planetaryfactory.core.compat.emi;

import com.planetaryfactory.core.recipes.AssemblingRecipe;
import dev.emi.emi.api.neoforge.NeoForgeEmiIngredient;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * One assembling recipe as EMI draws it (#279): the inputs in a row, an arrow, the results.
 *
 * <p>Counts are the whole point, as for the smelts. Factorio's recipes take eight plates or ten
 * bricks, which no 3x3 grid display can say. Time is the recipe's own, at crafting speed 1.
 */
public class AssemblingEmiRecipe extends BasicEmiRecipe {

    private static final int SLOT = 18;

    private final int time;

    public AssemblingEmiRecipe(EmiRecipeCategory category, RecipeHolder<AssemblingRecipe> holder) {
        super(category, holder.id().identifier(), 0, 32);
        AssemblingRecipe recipe = holder.value();
        this.time = recipe.time();
        recipe.ingredients().forEach(sized -> inputs.add(NeoForgeEmiIngredient.of(sized)));
        recipe.fluidIngredients().forEach(sized -> inputs.add(NeoForgeEmiIngredient.of(sized)));
        recipe.results().forEach(result -> outputs.add(EmiStack.of(result.create())));
        recipe.fluidResults().forEach(result -> outputs.add(NeoForgeEmiStack.of(result.create())));
        this.width = inputs.size() * SLOT + 30 + outputs.size() * SLOT;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int x = 0;
        for (var input : inputs) {
            widgets.addSlot(input, x, 4);
            x += SLOT;
        }
        widgets.addTexture(EmiTexture.EMPTY_ARROW, x + 3, 5);
        widgets.addFillingArrow(x + 3, 5, Math.max(time, 1) * 50);
        widgets.addText(Component.translatable("emi.planetaryfactory.assembling.seconds",
                String.format("%.1f", time / 20F)), x + 3, 24, 0xFF808080, false);
        x += 30;
        for (EmiStack output : outputs) {
            widgets.addSlot(output, x, 4).recipeContext(this);
            x += SLOT;
        }
    }
}
