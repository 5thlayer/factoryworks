package com.planetaryfactory.core.mixin.oritech;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rearth.oritech.client.ui.OritechScreenHandler;

/** Oritech closes a machine's screen 8 blocks off, inside the player's Reach (#413). */
@Mixin(OritechScreenHandler.class)
public abstract class OritechScreenHandlerMixin {

    @Shadow
    public BlockEntity blockEntity;

    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    private void planetaryfactory$withinReach(Player player, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(blockEntity != null && Container.stillValidBlockEntity(blockEntity, player));
    }
}
