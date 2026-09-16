package com.planetaryfactory.core.mixin.emi;

import com.planetaryfactory.core.compat.emi.FillClick;
import dev.emi.emi.api.widget.RecipeFillButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Notes which mouse button pressed EMI's {@code + Fill Recipe}, for the Assembler's handler (#288,
 * ADR-0065).
 *
 * <p>EMI's handler API carries no button -- {@code EmiCraftContext} has a type, a destination and an
 * amount -- and {@code mouseClicked} ignores its own {@code button} argument, so this is the one
 * place it can be read. It is read here rather than polled from GLFW because this is the button
 * Minecraft delivered to the screen, after its per-OS conversions.
 *
 * <p>It only records. The click is never altered, consumed or rerouted, and on any screen but the
 * Assembler's nothing reads what was recorded; the value is cleared when the call returns either way.
 */
@Mixin(value = RecipeFillButtonWidget.class, remap = false)
public abstract class RecipeFillButtonWidgetMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void planetaryfactory$recordButton(int mouseX, int mouseY, int button,
            CallbackInfoReturnable<Boolean> cir) {
        FillClick.press(button);
    }

    @Inject(method = "mouseClicked", at = @At("RETURN"))
    private void planetaryfactory$clearButton(int mouseX, int mouseY, int button,
            CallbackInfoReturnable<Boolean> cir) {
        FillClick.release();
    }
}
