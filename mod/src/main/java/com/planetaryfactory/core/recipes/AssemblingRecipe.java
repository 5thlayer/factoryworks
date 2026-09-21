package com.planetaryfactory.core.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * One Factorio assembling recipe: {@code crafting}, {@code advanced-crafting} or
 * {@code crafting-with-fluid} (#279).
 *
 * <p>GregTech's {@code gtceu:assembling} left with ADR-0060, and every recipe the converter emitted
 * onto it became a file nothing read. This is the pack's own replacement, and the one type both the
 * Personal Assembler's hand set and the Assembling Machine #277 decides are to read. The Factorio
 * category rides on the recipe, so ADR-0038's hand set stays a predicate over this type rather than
 * a type of its own.
 *
 * <p>Every field is a NeoForge or vanilla codec rather than one of the pack's: a sized item
 * ingredient, a sized fluid ingredient, and item and fluid <em>templates</em> for the results. A
 * template, not a stack, for the reason {@link SmeltingRecipe} gives: {@code ItemStack.CODEC} refuses
 * an item whose components are not bound yet, which they are not during the datapack load that reads
 * recipes.
 *
 * <p>{@code time} is Factorio's {@code energy_required * 20}; the machine or the Assembler applies
 * its own speed (ADR-0029).
 *
 * <p>{@link #matches} is false: nothing looks one up by its inputs. The hand set plans over it, and
 * the Assembling Machine runs the one recipe it holds by id (#328, ADR-0071).
 */
public record AssemblingRecipe(
        String category,
        List<SizedIngredient> ingredients,
        List<SizedFluidIngredient> fluidIngredients,
        List<ItemStackTemplate> results,
        List<FluidStackTemplate> fluidResults,
        int time) implements Recipe<RecipeInput> {

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input) {
        return results.isEmpty() ? ItemStack.EMPTY : results.getFirst().create();
    }

    /** Not a grid recipe: nothing places ingredients for it (#97). */
    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    /** No toast on unlock: research says so itself. */
    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<AssemblingRecipe> getSerializer() {
        return PFRecipes.ASSEMBLING_SERIALIZER.get();
    }

    @Override
    public RecipeType<AssemblingRecipe> getType() {
        return PFRecipes.ASSEMBLING_TYPE.get();
    }

    public static RecipeSerializer<AssemblingRecipe> serializer() {
        return new RecipeSerializer<>(CODEC, STREAM_CODEC);
    }

    private static final MapCodec<AssemblingRecipe> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.STRING.fieldOf("category").forGetter(AssemblingRecipe::category),
                    SizedIngredient.NESTED_CODEC.listOf().optionalFieldOf("ingredients", List.of())
                            .forGetter(AssemblingRecipe::ingredients),
                    SizedFluidIngredient.CODEC.listOf().optionalFieldOf("fluid_ingredients", List.of())
                            .forGetter(AssemblingRecipe::fluidIngredients),
                    ItemStackTemplate.CODEC.listOf().optionalFieldOf("results", List.of())
                            .forGetter(AssemblingRecipe::results),
                    FluidStackTemplate.CODEC.listOf().optionalFieldOf("fluid_results", List.of())
                            .forGetter(AssemblingRecipe::fluidResults),
                    Codec.INT.fieldOf("time").forGetter(AssemblingRecipe::time))
                    .apply(instance, AssemblingRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, AssemblingRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, AssemblingRecipe::category,
                    SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), AssemblingRecipe::ingredients,
                    SizedFluidIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), AssemblingRecipe::fluidIngredients,
                    ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list()), AssemblingRecipe::results,
                    FluidStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list()), AssemblingRecipe::fluidResults,
                    ByteBufCodecs.VAR_INT, AssemblingRecipe::time,
                    AssemblingRecipe::new);
}
