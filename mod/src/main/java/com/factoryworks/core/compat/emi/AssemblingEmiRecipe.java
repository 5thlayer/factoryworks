package com.factoryworks.core.compat.emi;

import com.factoryworks.core.recipes.AssemblingRecipe;
import dev.emi.emi.api.neoforge.NeoForgeEmiIngredient;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.TextWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.ArrayList;
import java.util.List;
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
    /** Parallel to {@code inputs} and {@code outputs}: a fluid's mB, or 0 for an item. */
    private final List<Integer> inputAmounts = new ArrayList<>();
    private final List<Integer> outputAmounts = new ArrayList<>();

    public AssemblingEmiRecipe(EmiRecipeCategory category, RecipeHolder<AssemblingRecipe> holder) {
        super(category, holder.id().identifier(), 0, 32);
        AssemblingRecipe recipe = holder.value();
        this.time = recipe.time();
        recipe.ingredients().forEach(sized -> {
            inputs.add(NeoForgeEmiIngredient.of(sized));
            inputAmounts.add(0);
        });
        recipe.fluidIngredients().forEach(sized -> {
            inputs.add(NeoForgeEmiIngredient.of(sized));
            inputAmounts.add(sized.amount());
        });
        recipe.results().forEach(result -> {
            outputs.add(EmiStack.of(result.create()));
            outputAmounts.add(0);
        });
        recipe.fluidResults().forEach(result -> {
            outputs.add(NeoForgeEmiStack.of(result.create()));
            outputAmounts.add(result.amount());
        });
        this.width = inputs.size() * SLOT + 30 + outputs.size() * SLOT;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int x = 0;
        for (int i = 0; i < inputs.size(); i++) {
            widgets.addSlot(inputs.get(i), x, 4);
            addAmount(widgets, inputAmounts.get(i), x);
            x += SLOT;
        }
        widgets.addTexture(EmiTexture.EMPTY_ARROW, x + 3, 5);
        widgets.addFillingArrow(x + 3, 5, Math.max(time, 1) * 50);
        widgets.addText(Component.translatable("emi.factoryworks.assembling.seconds",
                String.format("%.1f", time / 20F)), x + 3, 24, 0xFF808080, false);
        x += 30;
        for (int i = 0; i < outputs.size(); i++) {
            widgets.addSlot(outputs.get(i), x, 4).recipeContext(this);
            addAmount(widgets, outputAmounts.get(i), x);
            x += SLOT;
        }
    }

    /** EMI draws an item's count on its slot but a fluid's amount only in the tooltip. */
    private static void addAmount(WidgetHolder widgets, int amount, int slotX) {
        if (amount > 0) {
            widgets.addText(Component.literal(Integer.toString(amount)), slotX + 18, 14, 0xFFFFFFFF, true)
                    .horizontalAlign(TextWidget.Alignment.END);
        }
    }
}
