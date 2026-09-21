package com.planetaryfactory.core.mixin.minecraft;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla's y=-59 is its overworld floor plus five, typed as a literal. Terra's floor is y=0
 * (ADR-0019), so at -59 every test block lands outside the world and the batch never finishes
 * (#338). Measured from the floor instead, a vanilla overworld still gets -59.
 */
@Mixin(GameTestServer.class)
public abstract class GameTestServerMixin {

    @ModifyExpressionValue(method = "startTests", at = @At(value = "CONSTANT", args = "intValue=-59"))
    private int planetaryfactory$aboveFloor(int vanillaY, @Local(argsOnly = true) ServerLevel level) {
        return level.getMinY() + 5;
    }
}
