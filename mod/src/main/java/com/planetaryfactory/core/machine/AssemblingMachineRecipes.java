package com.planetaryfactory.core.machine;

import java.util.List;
import java.util.Optional;

import com.planetaryfactory.core.recipes.AssemblingRecipe;
import com.planetaryfactory.core.recipes.PFRecipes;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * What an Assembling Machine may hold, read off the server's recipe manager (#327).
 *
 * <p>Every loaded {@code planetaryfactory:assembling} recipe, whatever its category: the machine
 * is Factorio's assembler, which takes all three crafting categories, and a recipe the widget
 * leaves out is a recipe no machine can ever make.
 *
 * <p><b>Nothing is Locked yet</b>, for {@code RuntimePlanSource}'s reason: the research predicate
 * asks Researchd, whose fork is not on 26.1.2 (#260). The annotation is wired all the way to the
 * screen, so restoring the predicate is this one line.
 */
public final class AssemblingMachineRecipes {

    private AssemblingMachineRecipes() {
    }

    /** Every assembling recipe, as the widget lists it. */
    public static List<RecipeChoice> choices(ServerLevel level) {
        List<String> ids = level.getServer().getRecipeManager().recipeMap()
                .byType(PFRecipes.ASSEMBLING_TYPE.get()).stream()
                .map(holder -> holder.id().identifier().toString())
                .toList();
        return RecipeChoice.of(ids, id -> false);
    }

    /** The recipe {@code held} names, looked up now rather than when the machine loaded. */
    public static Optional<RecipeHolder<AssemblingRecipe>> resolve(ServerLevel level, HeldRecipe held) {
        return held.id().map(Identifier::tryParse)
                .map(id -> level.getServer().getRecipeManager().recipeMap()
                        .byKey(ResourceKey.create(Registries.RECIPE, id)))
                .filter(holder -> holder.value() instanceof AssemblingRecipe)
                .map(AssemblingMachineRecipes::cast);
    }

    /** What the widget draws for a recipe: its first result. */
    public static ItemStack icon(ServerLevel level, String id) {
        return resolve(level, HeldRecipe.of(id))
                .map(holder -> holder.value().assemble(null))
                .orElse(ItemStack.EMPTY);
    }

    @SuppressWarnings("unchecked")
    private static RecipeHolder<AssemblingRecipe> cast(RecipeHolder<?> holder) {
        return (RecipeHolder<AssemblingRecipe>) holder;
    }
}
