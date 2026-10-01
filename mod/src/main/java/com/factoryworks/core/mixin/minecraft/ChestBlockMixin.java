package com.factoryworks.core.mixin.minecraft;

import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No chest pairs: placement and neighbour updates both ask this, so answering false keeps every
 * {@code ChestBlock} single (ADR-0106).
 */
@Mixin(ChestBlock.class)
public abstract class ChestBlockMixin {

    @Inject(method = "chestCanConnectTo", at = @At("HEAD"), cancellable = true)
    private void factoryworks$neverConnect(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
