package com.factoryworks.core.compat.jei;

import java.util.Optional;

import com.factoryworks.core.FactoryWorksCore;
import com.factoryworks.core.machine.client.AssemblingMachineScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The pack's JEI plugin. Loaded by JEI's own annotation scan, so nothing in the mod references this
 * class and it never loads when JEI is absent.
 */
@JeiPlugin
public final class FactoryWorksJeiPlugin implements IModPlugin {

    private static final Identifier UID =
            Identifier.fromNamespaceAndPath(FactoryWorksCore.MOD_ID, "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    /** Recipe and usage keys on a chassis machine's tank bar. */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(AssemblingMachineScreen.class, new IGuiContainerHandler<>() {
            @Override
            public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
                    IClickableIngredientFactory factory, AssemblingMachineScreen screen, double mouseX, double mouseY) {
                return screen.fluidBarAt(mouseX, mouseY).flatMap(bar -> factory
                        .createBuilder(NeoForgeTypes.FLUID_STACK, new FluidStack(bar.fluid().orElseThrow(), 1000))
                        .buildWithArea(bar.x(), bar.y(), bar.width(), bar.height()));
            }
        });
    }
}
