package com.planetaryfactory.core.recipes;

import java.util.function.Supplier;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * The recipe types that share {@link AssemblingRecipe}'s record and JSON shape, one per machine so
 * each has its own EMI tab (ADR-0096).
 */
public enum AssemblingFamily {
    ASSEMBLING(PFRecipes.ASSEMBLING, () -> PFRecipes.ASSEMBLING_TYPE.get(),
            () -> PFRecipes.ASSEMBLING_SERIALIZER.get()),
    CHEMISTRY(PFRecipes.CHEMISTRY, () -> PFRecipes.CHEMISTRY_TYPE.get(),
            () -> PFRecipes.CHEMISTRY_SERIALIZER.get()),
    OIL_PROCESSING(PFRecipes.OIL_PROCESSING, () -> PFRecipes.OIL_PROCESSING_TYPE.get(),
            () -> PFRecipes.OIL_PROCESSING_SERIALIZER.get());

    private final String path;
    private final Supplier<RecipeType<AssemblingRecipe>> type;
    private final Supplier<RecipeSerializer<AssemblingRecipe>> serializer;

    AssemblingFamily(String path, Supplier<RecipeType<AssemblingRecipe>> type,
            Supplier<RecipeSerializer<AssemblingRecipe>> serializer) {
        this.path = path;
        this.type = type;
        this.serializer = serializer;
    }

    /** The type's id path, which is also the folder its recipes load from. */
    public String path() {
        return path;
    }

    public RecipeType<AssemblingRecipe> type() {
        return type.get();
    }

    public RecipeSerializer<AssemblingRecipe> serializer() {
        return serializer.get();
    }
}
