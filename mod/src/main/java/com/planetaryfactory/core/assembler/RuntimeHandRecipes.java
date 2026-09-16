package com.planetaryfactory.core.assembler;

import com.mojang.logging.LogUtils;
import com.planetaryfactory.core.recipes.AssemblingRecipe;
import com.planetaryfactory.core.recipes.PFRecipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.slf4j.Logger;

/**
 * The hand-craftable set, read off what the server actually loaded.
 *
 * <p>ADR-0038 gives the Personal Assembler no recipe type of its own: the hand set is a predicate
 * over the Assembling Machine's {@code planetaryfactory:assembling} recipes (#279), and
 * {@link HandSetAdmission} is that predicate.
 *
 * <p>Reading the recipe manager rather than the corpus is deliberate. The corpus says what the pack
 * intends; the recipe manager says what the player can actually be handed, and a Factorio name whose
 * {@code item-map.json} row is still {@code undecided} is emitted by nothing and must be planned by
 * nothing. {@code tests/factorio/test_hand_resolver.py} is the other direction -- that the corpus
 * admits a terminating plan for all 113.
 *
 * <p>Cached against the {@link RecipeManager}'s identity: a datapack reload builds a fresh manager,
 * so its identity is an exact and free invalidation signal with no reload listener to register.
 */
public final class RuntimeHandRecipes {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile RecipeManager builtFrom;
    private static volatile RecipeGraph graph = RecipeGraph.empty();

    private RuntimeHandRecipes() {}

    /** The graph for the level's current datapack state, rebuilt when the recipes reload. */
    public static RecipeGraph graph(Level level) {
        RecipeManager manager = level.getServer().getRecipeManager();
        if (manager == builtFrom) return graph;
        return rebuild(manager, level.registryAccess());
    }

    private static synchronized RecipeGraph rebuild(RecipeManager manager, HolderLookup.Provider registries) {
        if (manager == builtFrom) return graph;
        RecipeGraph built = build(manager, registries);
        graph = built;
        builtFrom = manager;
        LOGGER.info("Personal Assembler: {} hand-craftable recipe(s) loaded.", built.size());
        return built;
    }

    private static RecipeGraph build(RecipeManager manager, HolderLookup.Provider registries) {
        RecipeGraph.Builder builder = RecipeGraph.builder();
        List<String> refused = new ArrayList<>();
        for (RecipeHolder<AssemblingRecipe> holder : manager.recipeMap().byType(PFRecipes.ASSEMBLING_TYPE.get())) {
            AssemblingRecipe recipe = holder.value();
            String id = holder.id().identifier().toString();
            // Another category is a machine's recipe, not a refusal: it is not logged.
            if (!HandSetAdmission.HAND_CATEGORY.equals(recipe.category())) continue;
            String reason = HandSetAdmission.refusal(recipe.category(), recipe.fluidIngredients().size(),
                    recipe.fluidResults().size(), recipe.results().size());
            HandRecipe read = reason == null ? read(id, recipe, registries, refused) : null;
            if (reason != null) refused.add(id + " (" + reason + ")");
            if (read != null) builder.add(read);
        }
        if (!refused.isEmpty()) {
            // Named, not counted. A recipe in the hand category that the Assembler will not plan
            // reaches the player as a Crafting Plan with nothing in it, and the only way to tell
            // which recipe and why is to say so here.
            LOGGER.warn("Personal Assembler: {} hand recipe(s) refused: {}", refused.size(), refused);
        }
        return builder.build();
    }

    /** The recipe as the resolver sees it, or null with the reason added to {@code refused}. */
    private static HandRecipe read(String id, AssemblingRecipe recipe, HolderLookup.Provider registries,
                                   List<String> refused) {
        List<Ingredient> inputs = new ArrayList<>();
        for (SizedIngredient sized : recipe.ingredients()) {
            List<String> items = new ArrayList<>();
            for (ItemStack match : matches(sized)) {
                // A key that names nothing is refused here and nowhere else: the resolver stays free
                // of any notion of resolvability, and the failure arrives as a named line rather
                // than as a queue that pauses forever with nothing in the log.
                String key = ItemKeys.of(match, registries);
                if (key == null) {
                    refused.add(id + " (an input with a component nothing can name on " + match.getItem() + ")");
                    return null;
                }
                items.add(key);
            }
            if (items.isEmpty()) {
                refused.add(id + " (an input no item satisfies)");
                return null;
            }
            inputs.add(new Ingredient(items, sized.count()));
        }
        List<ItemAmount> outputs = new ArrayList<>();
        for (ItemStackTemplate result : recipe.results()) {
            String key = ItemKeys.of(result.create(), registries);
            if (key == null) {
                refused.add(id + " (an output with a component nothing can name)");
                return null;
            }
            outputs.add(new ItemAmount(key, result.count()));
        }
        return new HandRecipe(id, inputs, outputs, recipe.time());
    }

    /**
     * One stack per item the ingredient accepts.
     *
     * <p>A component ingredient is read as its items carrying its components, which is the one case
     * the key format was widened for (ADR-0052): the science packs are one item and four keys. Any
     * other custom ingredient contributes its bare items.
     */
    private static List<ItemStack> matches(SizedIngredient sized) {
        DataComponentPatch components = sized.ingredient().getCustomIngredient() instanceof DataComponentIngredient data
                ? data.components()
                : DataComponentPatch.EMPTY;
        List<ItemStack> stacks = new ArrayList<>();
        for (Holder<Item> item : sized.ingredient().items().toList()) {
            stacks.add(new ItemStack(item, 1, components));
        }
        return stacks;
    }
}
