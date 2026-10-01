package com.factoryworks.core.mixin.minecraft;

import com.factoryworks.core.wreck.WreckSpawn;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla scatters a spawn by {@code respawn_radius} and lifts it to the heightmap, which is the
 * wreck's roof. Both a first join and a respawn with no bed come through here; a bed or anchor
 * never does (ADR-0107).
 */
@Mixin(PlayerSpawnFinder.class)
public abstract class PlayerSpawnFinderMixin {

    @Inject(method = "findSpawn", at = @At("HEAD"), cancellable = true)
    private static void factoryworks$wreckFloor(ServerLevel level, BlockPos suggestion,
            CallbackInfoReturnable<CompletableFuture<Vec3>> cir) {
        if (WreckSpawn.isWreckSpawn(level, suggestion)) {
            cir.setReturnValue(CompletableFuture.completedFuture(Vec3.atBottomCenterOf(suggestion)));
        }
    }
}
