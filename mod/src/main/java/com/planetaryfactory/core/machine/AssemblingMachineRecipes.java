package com.planetaryfactory.core.machine;

import java.util.List;
import java.util.Optional;

import com.planetaryfactory.core.compat.researchd.ResearchdMachineLocks;
import com.planetaryfactory.core.recipes.AssemblingRecipe;
import com.planetaryfactory.core.recipes.PFRecipes;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * What an Assembling Machine may hold, read off the server's recipe manager (#327).
 *
 * <p>Every loaded {@code planetaryfactory:assembling} recipe, whatever its category. A
 * {@code crafting-with-fluid} one is listed and then refused by {@link HoldVerdict}, so Fill Recipe
 * on it says why rather than doing nothing (#331). What is Locked is Researchd's to say (#260).
 */
public final class AssemblingMachineRecipes {

    private AssemblingMachineRecipes() {
    }

    /** Whether {@code machine} may not make {@code id} yet: the screen marks it and the machine idles. */
    public static boolean isLocked(BlockEntity machine, String id) {
        Identifier recipe = Identifier.tryParse(id);
        return recipe != null && ModList.get().isLoaded("researchd") && ResearchdMachineLocks.isLocked(machine, recipe);
    }

    /** Every assembling recipe, as {@code machine}'s screen names them. */
    public static List<RecipeChoice> choices(BlockEntity machine) {
        List<String> ids = ((ServerLevel) machine.getLevel()).getServer().getRecipeManager().recipeMap()
                .byType(PFRecipes.ASSEMBLING_TYPE.get()).stream()
                .map(holder -> holder.id().identifier().toString())
                .toList();
        return RecipeChoice.of(ids, id -> isLocked(machine, id));
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

    /** The first fluid a recipe's fluid ingredients name, which is the one its tank holds. */
    public static Optional<Fluid> firstFluid(List<SizedFluidIngredient> fluidIngredients) {
        return fluidIngredients.stream()
                .flatMap(sized -> sized.ingredient().fluids().stream())
                .map(Holder::value)
                .findFirst();
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
