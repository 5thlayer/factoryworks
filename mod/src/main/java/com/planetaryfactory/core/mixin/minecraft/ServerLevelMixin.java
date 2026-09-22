package com.planetaryfactory.core.mixin.minecraft;

import com.planetaryfactory.core.gametest.BlockUpdateWatch;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets a GameTest see a block update being sent, which nothing else observes on a server (#351). */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "sendBlockUpdated", at = @At("HEAD"))
    private void planetaryfactory$watch(BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        BlockUpdateWatch.saw(pos);
    }
}
