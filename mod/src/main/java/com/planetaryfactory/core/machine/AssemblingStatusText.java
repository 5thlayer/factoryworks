package com.planetaryfactory.core.machine;

import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;

/** What the screen and Jade print for an {@link AssemblingStatus} and the tank: a missing fluid is named (#295). */
public final class AssemblingStatusText {

    private AssemblingStatusText() {
    }

    public static Component of(AssemblingStatus status, Optional<Fluid> fluid) {
        if (status != AssemblingStatus.MISSING_FLUID) {
            return Component.translatable(status.langKey());
        }
        return Component.translatable(status.langKey(), fluid.map(AssemblingStatusText::name)
                .orElse(Component.translatable("gui.planetaryfactory.assembling_machine.the_fluid")));
    }

    /** The tank's line: its fluid, fill and size, or empty when no recipe names a fluid. */
    public static Component tank(Optional<Fluid> fluid, long amount, long capacity) {
        if (fluid.isEmpty()) {
            return Component.translatable("gui.planetaryfactory.assembling_machine.tank_empty");
        }
        return Component.translatable("gui.planetaryfactory.assembling_machine.tank", name(fluid.get()),
                String.format("%,d", amount), String.format("%,d", capacity));
    }

    public static Component name(Fluid fluid) {
        return fluid.getFluidType().getDescription();
    }
}
