package com.factoryworks.core.mixin.oritech;

import com.factoryworks.core.energy.AccumulatorSpec;
import java.util.List;
import net.minecraft.core.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rearth.oritech.block.entity.storage.LargeStorageBlockEntity;

/**
 * Oritech's Large Energy Storage at Factorio's accumulator figures (#283, ADR-0062), in place of
 * Oritech's config values. The base class sizes its storage from these three in its constructor.
 * It takes no addons, since each adds its bonus on top of them.
 */
@Mixin(LargeStorageBlockEntity.class)
public abstract class LargeStorageBlockEntityMixin {

    @Inject(method = "getDefaultCapacity", at = @At("HEAD"), cancellable = true)
    private void factoryworks$capacity(CallbackInfoReturnable<Long> cir) {
        cir.setReturnValue(AccumulatorSpec.capacityFe());
    }

    @Inject(method = "getDefaultInsertRate", at = @At("HEAD"), cancellable = true)
    private void factoryworks$insertRate(CallbackInfoReturnable<Long> cir) {
        cir.setReturnValue(AccumulatorSpec.inputFePerTick());
    }

    @Inject(method = "getDefaultExtractionRate", at = @At("HEAD"), cancellable = true)
    private void factoryworks$extractionRate(CallbackInfoReturnable<Long> cir) {
        cir.setReturnValue(AccumulatorSpec.outputFePerTick());
    }

    @Inject(method = "getAddonSlots", at = @At("HEAD"), cancellable = true)
    private void factoryworks$noAddons(CallbackInfoReturnable<List<Vec3i>> cir) {
        cir.setReturnValue(List.of());
    }
}
