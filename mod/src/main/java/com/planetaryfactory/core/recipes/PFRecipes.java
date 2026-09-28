package com.planetaryfactory.core.recipes;

import com.planetaryfactory.core.PlanetaryFactoryCore;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The pack's own recipe types: {@code planetaryfactory:smelting} (#155), and the three on
 * {@link AssemblingRecipe}'s shape, one per {@link AssemblingFamily}.
 *
 * <p>Smelting is the only type the three furnace tiers read. Vanilla's {@code minecraft:smelting} is not
 * read alongside it: ADR-0034's sweep removes every vanilla smelting recipe, so a dual read would
 * have no live consumer and would mean a recipe carrying vanilla's cook time and getting no tier
 * scaling. All four corpus smelting recipes are on this type, and anything re-admitted later is
 * authored on it with {@code count: 1}.
 */
public final class PFRecipes {

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, PlanetaryFactoryCore.NAMESPACE);

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, PlanetaryFactoryCore.NAMESPACE);

    public static final String SMELTING = "smelting";

    public static final DeferredHolder<RecipeType<?>, RecipeType<SmeltingRecipe>> SMELTING_TYPE =
            TYPES.register(SMELTING, () -> RecipeType.simple(
                    Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, SMELTING)));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SmeltingRecipe>> SMELTING_SERIALIZER =
            SERIALIZERS.register(SMELTING, SmeltingRecipe::serializer);

    public static final String ASSEMBLING = "assembling";

    public static final DeferredHolder<RecipeType<?>, RecipeType<AssemblingRecipe>> ASSEMBLING_TYPE =
            assemblingType(ASSEMBLING);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AssemblingRecipe>> ASSEMBLING_SERIALIZER =
            SERIALIZERS.register(ASSEMBLING, () -> AssemblingRecipe.serializer(AssemblingFamily.ASSEMBLING));

    public static final String CHEMISTRY = "chemistry";

    public static final DeferredHolder<RecipeType<?>, RecipeType<AssemblingRecipe>> CHEMISTRY_TYPE =
            assemblingType(CHEMISTRY);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AssemblingRecipe>> CHEMISTRY_SERIALIZER =
            SERIALIZERS.register(CHEMISTRY, () -> AssemblingRecipe.serializer(AssemblingFamily.CHEMISTRY));

    public static final String OIL_PROCESSING = "oil_processing";

    public static final DeferredHolder<RecipeType<?>, RecipeType<AssemblingRecipe>> OIL_PROCESSING_TYPE =
            assemblingType(OIL_PROCESSING);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AssemblingRecipe>> OIL_PROCESSING_SERIALIZER =
            SERIALIZERS.register(OIL_PROCESSING, () -> AssemblingRecipe.serializer(AssemblingFamily.OIL_PROCESSING));

    private PFRecipes() {
    }

    private static DeferredHolder<RecipeType<?>, RecipeType<AssemblingRecipe>> assemblingType(String path) {
        return TYPES.register(path, () -> RecipeType.simple(
                Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, path)));
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        SERIALIZERS.register(modBus);
    }
}
