package com.factoryworks.core.mixin.oritech;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.factoryworks.core.fluid.FluidTintCorpus;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rearth.oritech.client.init.FluidModelContent;

/**
 * Oritech's fluids, drawn in Factorio's colours where the pack borrows them (#277, ADR-0067).
 *
 * <p>Oritech states each fluid's tint as a constructor argument, and NeoForge's
 * {@link RegisterFluidModelsEvent} throws on a second registration for the same fluid, so the tint
 * cannot be overridden from outside: the core wraps each of Oritech's {@code register} calls and
 * swaps the tint for {@link FluidTintCorpus}'s where it has one. Sprites, overlay and renderer are
 * Oritech's, unchanged.
 *
 * <p>The target was read off the installed 2.0.0-exp6 jar with {@code javap}. The config is
 * {@code required: false}, so a renamed target is a warning and Oritech's colours, not a crash.
 */
@Mixin(FluidModelContent.class)
public abstract class FluidModelContentMixin {

    @WrapOperation(
            method = "registerFluidModels",
            at = @At(value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/client/event/RegisterFluidModelsEvent;register(Lnet/minecraft/client/renderer/block/FluidModel$Unbaked;Ljava/util/function/Supplier;Ljava/util/function/Supplier;)V"))
    private static void factoryworks$factorioTint(
            RegisterFluidModelsEvent event,
            FluidModel.Unbaked model,
            Supplier<? extends Fluid> still,
            Supplier<? extends Fluid> flowing,
            Operation<Void> original) {
        OptionalInt tint = FluidTintCorpus.get()
                .tint(BuiltInRegistries.FLUID.getKey(still.get()).toString());
        FluidModel.Unbaked tinted = tint.isEmpty() ? model : new FluidModel.Unbaked(
                model.stillMaterial(),
                model.flowingMaterial(),
                model.overlayMaterial(),
                FluidTintSources.constant(tint.getAsInt()),
                model.customRenderer());
        original.call(event, tinted, still, flowing);
    }
}
