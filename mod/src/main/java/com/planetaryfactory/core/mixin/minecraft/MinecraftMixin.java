package com.planetaryfactory.core.mixin.minecraft;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.planetaryfactory.core.reach.Reach;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Terrain beyond a break's reach is attacked as a miss, which swings once, where a cancelled start
 * leaves the client swinging and cracking it every tick (#413). The hit result itself stays a
 * block, since a placement against that terrain is still in Reach.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow
    public HitResult hitResult;

    @Shadow
    public LocalPlayer player;

    @ModifyExpressionValue(method = {"startAttack", "continueAttack"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/HitResult;getType()Lnet/minecraft/world/phys/HitResult$Type;"))
    private HitResult.Type planetaryfactory$missBeyondBreakReach(HitResult.Type type) {
        return type == HitResult.Type.BLOCK
                && Reach.refusesBreak(player, ((BlockHitResult) hitResult).getBlockPos())
                ? HitResult.Type.MISS : type;
    }
}
