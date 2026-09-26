package com.planetaryfactory.core.mixin.oritech;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.oritech.block.base.entity.ExpandableEnergyStorageBlockEntity;
import rearth.oritech.block.entity.storage.LargeStorageBlockEntity;

/**
 * The accumulator pushes nothing out of its front face: the pole network is the only carrier
 * (ADR-0062), and a push would feed a neighbour outside the network's balance.
 */
@Mixin(ExpandableEnergyStorageBlockEntity.class)
public abstract class ExpandableEnergyStorageBlockEntityMixin {

    @Inject(method = "outputEnergy", at = @At("HEAD"), cancellable = true)
    private void planetaryfactory$noPush(CallbackInfo ci) {
        if ((Object) this instanceof LargeStorageBlockEntity) {
            ci.cancel();
        }
    }
}
