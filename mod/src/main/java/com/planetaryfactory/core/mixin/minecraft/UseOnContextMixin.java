package com.planetaryfactory.core.mixin.minecraft;

import com.planetaryfactory.core.placement.HeldTurn;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A placement's look, turned by the held stack's Rotate offset, so every block that places by the
 * look turns with no code of its own (ADR-0083). Only a {@link BlockPlaceContext}'s: using an item
 * on a block is not placing it.
 */
@Mixin(UseOnContext.class)
public abstract class UseOnContextMixin {

    @Shadow
    public abstract ItemStack getItemInHand();

    @Inject(method = "getHorizontalDirection", at = @At("RETURN"), cancellable = true)
    private void planetaryfactory$turnHorizontal(CallbackInfoReturnable<Direction> cir) {
        if ((Object) this instanceof BlockPlaceContext) {
            cir.setReturnValue(HeldTurn.turn(getItemInHand(), cir.getReturnValue()));
        }
    }

    @Inject(method = "getRotation", at = @At("RETURN"), cancellable = true)
    private void planetaryfactory$turnRotation(CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof BlockPlaceContext) {
            cir.setReturnValue(HeldTurn.of(getItemInHand()).turnYaw(cir.getReturnValue()));
        }
    }
}
