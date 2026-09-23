package com.planetaryfactory.core.mixin.minecraft;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.planetaryfactory.core.placement.HeldTurn;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The nearest looking directions, turned by the held stack's Rotate offset (ADR-0083). Turned where
 * they are read off the player, because {@code getNearestLookingDirections} then puts the clicked
 * face first, which is not the look and must not turn.
 */
@Mixin(BlockPlaceContext.class)
public abstract class BlockPlaceContextMixin {

    @ModifyExpressionValue(method = {"getNearestLookingDirection", "getNearestLookingDirections"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/Direction;orderedByNearest(Lnet/minecraft/world/entity/Entity;)[Lnet/minecraft/core/Direction;"))
    private Direction[] planetaryfactory$turnLook(Direction[] directions) {
        return HeldTurn.turn(((BlockPlaceContext) (Object) this).getItemInHand(), directions);
    }
}
