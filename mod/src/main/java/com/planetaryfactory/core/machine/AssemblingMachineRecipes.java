package com.planetaryfactory.core.machine;

import java.util.List;
import java.util.Optional;

import com.planetaryfactory.core.PFServerConfig;
import com.planetaryfactory.core.compat.researchd.ResearchdMachineLocks;
import com.planetaryfactory.core.recipes.AssemblingFamily;
import com.planetaryfactory.core.recipes.AssemblingRecipe;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * What a crafting machine may hold, read off the server's recipe manager (#327, #489).
 *
 * <p>Every loaded recipe of the machine's type, whatever it needs. One needing a tank the machine
 * lacks is listed and then refused by {@link HoldVerdict}, so Fill Recipe on it says why rather than
 * doing nothing (#331). What is Locked is the server config's {@code lockSources} (#260).
 */
public final class AssemblingMachineRecipes {

    private AssemblingMachineRecipes() {
    }

    /** Whether {@code machine} may not make {@code id} yet: the screen marks it and the machine idles. */
    public static boolean isLocked(BlockEntity machine, String id) {
        Identifier recipe = Identifier.tryParse(id);
        return recipe != null && PFServerConfig.locksBy(PFServerConfig.LockSource.researchd)
                && ModList.get().isLoaded("researchd") && ResearchdMachineLocks.isLocked(machine, recipe);
    }

    /** Every recipe of the machine's type, as its screen names them. */
    public static List<RecipeChoice> choices(AssemblingMachineBlockEntity machine) {
        List<String> ids = ((ServerLevel) machine.getLevel()).getServer().getRecipeManager().recipeMap()
                .byType(AssemblingFamily.of(machine.spec().recipeType()).type()).stream()
                .map(holder -> holder.id().identifier().toString())
                .toList();
        return RecipeChoice.of(ids, id -> isLocked(machine, id));
    }

    /** The recipe {@code held} names if it is of {@code spec}'s type, looked up now rather than when the machine loaded. */
    public static Optional<RecipeHolder<AssemblingRecipe>> resolve(ServerLevel level, HeldRecipe held, MachineSpec spec) {
        return resolveAny(level, held).filter(holder -> ofType(holder.value(), spec));
    }

    /** The recipe {@code held} names, of any type the chassis runs. */
    public static Optional<RecipeHolder<AssemblingRecipe>> resolveAny(ServerLevel level, HeldRecipe held) {
        return held.id().map(Identifier::tryParse)
                .map(id -> level.getServer().getRecipeManager().recipeMap()
                        .byKey(ResourceKey.create(Registries.RECIPE, id)))
                .filter(holder -> holder.value() instanceof AssemblingRecipe)
                .map(AssemblingMachineRecipes::cast);
    }

    public static boolean ofType(AssemblingRecipe recipe, MachineSpec spec) {
        return recipe.family().id().equals(spec.recipeType());
    }

    /** Whether {@code spec}'s machine has a slot or tank for everything {@code recipe} takes and gives. */
    public static boolean fits(AssemblingRecipe recipe, MachineSpec spec) {
        return spec.fits(recipe.ingredients().size(), recipe.results().size(),
                recipe.fluidIngredients().size(), recipe.fluidResults().size());
    }

    /**
     * What the input slots take, in {@link AssemblingInputSlots}' order: nothing for a recipe the
     * machine has no tank to run.
     */
    public static List<SizedIngredient> slotIngredients(AssemblingRecipe recipe, MachineSpec spec) {
        return fits(recipe, spec) ? recipe.ingredients() : List.of();
    }

    /** The recipe's name as Factorio gives it: its own, or else its main product's (#490). */
    public static Component name(RecipeHolder<AssemblingRecipe> holder) {
        Identifier id = holder.id().identifier();
        return Component.translatable("recipe." + id.getNamespace() + "." + id.getPath().replace('/', '.'),
                productName(holder.value(), id));
    }

    private static Component productName(AssemblingRecipe recipe, Identifier id) {
        ItemStack product = recipe.assemble(null);
        if (!product.isEmpty()) {
            return product.getHoverName();
        }
        return recipe.fluidResults().stream().findFirst()
                .map(result -> AssemblingStatusText.name(FluidResource.of(result).getFluid()))
                .orElse(Component.literal(id.toString()));
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
