package com.planetaryfactory.core.mixin.oritech;

import java.util.HashSet;

import com.planetaryfactory.core.machine.footprint.FootprintItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.oritech.client.renderers.BlockOutlineRenderer;

/**
 * Oritech's multiblock outline is not drawn for a footprint machine's item (#326).
 *
 * <p>Oritech outlines the controller plus the cores of any held item whose block entity is a
 * {@code MultiblockMachineController}. The pack's machines have no cores, so that outline is one
 * white box inside the pack's own placement preview (ADR-0069), which already draws the whole
 * footprint -- two previews for one gesture, one of them wrong about its size.
 *
 * <p>Target read off the 2.0.0-exp6 jar with {@code javap}. The config is {@code required: false}:
 * a renamed target is a warning and a stray white box, not a crash.
 */
@Mixin(BlockOutlineRenderer.class)
public abstract class BlockOutlineRendererMixin {

    @Inject(method = "addBlockPreviewOutlines", at = @At("HEAD"), cancellable = true)
    private static void planetaryfactory$notForAFootprintMachine(
            ClientLevel level, LocalPlayer player, ItemStack stack, BlockPos pos, BlockHitResult hit,
            HashSet<?> outlines, CallbackInfo ci) {
        if (stack.getItem() instanceof FootprintItem) {
            ci.cancel();
        }
    }
}
