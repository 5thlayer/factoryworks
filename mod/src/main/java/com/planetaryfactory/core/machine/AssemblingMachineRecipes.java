package com.planetaryfactory.core.machine;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import com.planetaryfactory.core.recipes.AssemblingRecipe;
import com.planetaryfactory.core.recipes.PFRecipes;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * What an Assembling Machine may hold, read off the server's recipe manager (#327).
 *
 * <p>Every loaded {@code planetaryfactory:assembling} recipe, whatever its category. A
 * {@code crafting-with-fluid} one is listed and then refused by {@link HoldVerdict}, so Fill Recipe
 * on it says why rather than doing nothing (#331).
 *
 * <p><b>Nothing is Locked yet</b>, for {@code RuntimePlanSource}'s reason: the research predicate
 * asks Researchd, whose fork is not on 26.1.2 (#260). The annotation is wired all the way to the
 * screen and into the craft cycle (#328), so restoring the predicate is {@link #locked}'s one line.
 */
public final class AssemblingMachineRecipes {

    /** Which recipe ids research has not unlocked. Nothing, until Researchd returns (#260). */
    private static final Predicate<String> locked = id -> false;

    /** Per id, because a batch's GameTests run side by side and must not unlock each other's recipes. */
    private static final Set<String> lockedForTest = ConcurrentHashMap.newKeySet();

    private AssemblingMachineRecipes() {
    }

    /** Whether research has yet to unlock {@code id}: the screen marks it and no machine makes it. */
    public static boolean isLocked(String id) {
        return locked.test(id) || lockedForTest.contains(id);
    }

    public static void lockForTest(String id) {
        lockedForTest.add(id);
    }

    public static void unlockForTest(String id) {
        lockedForTest.remove(id);
    }

    public static boolean isLockedForTest(String id) {
        return lockedForTest.contains(id);
    }

    /** Every assembling recipe, as the screen names them. */
    public static List<RecipeChoice> choices(ServerLevel level) {
        List<String> ids = level.getServer().getRecipeManager().recipeMap()
                .byType(PFRecipes.ASSEMBLING_TYPE.get()).stream()
                .map(holder -> holder.id().identifier().toString())
                .toList();
        return RecipeChoice.of(ids, AssemblingMachineRecipes::isLocked);
    }

    /** The recipe {@code held} names, looked up now rather than when the machine loaded. */
    public static Optional<RecipeHolder<AssemblingRecipe>> resolve(ServerLevel level, HeldRecipe held) {
        return held.id().map(Identifier::tryParse)
                .map(id -> level.getServer().getRecipeManager().recipeMap()
                        .byKey(ResourceKey.create(Registries.RECIPE, id)))
                .filter(holder -> holder.value() instanceof AssemblingRecipe)
                .map(AssemblingMachineRecipes::cast);
    }

    /**
     * What the input slots take, in {@link AssemblingInputSlots}' order: nothing for a fluid recipe
     * on a tier with no tank to run it.
     */
    public static List<SizedIngredient> slotIngredients(AssemblingRecipe recipe, AssemblingTier tier) {
        return recipe.fluidIngredients().isEmpty() || tier.hasFluidInput() ? recipe.ingredients() : List.of();
    }

    /** Whether input {@code slot} takes {@code stack}, on the server's face and the client's slot alike. */
    public static boolean accepts(int slot, List<SizedIngredient> slotIngredients, ItemStack stack) {
        return AssemblingInputSlots.accepts(slot, slotIngredients, stack,
                (SizedIngredient sized, ItemStack item) -> sized.ingredient().test(item));
    }

    @SuppressWarnings("unchecked")
    private static RecipeHolder<AssemblingRecipe> cast(RecipeHolder<?> holder) {
        return (RecipeHolder<AssemblingRecipe>) holder;
    }
}
