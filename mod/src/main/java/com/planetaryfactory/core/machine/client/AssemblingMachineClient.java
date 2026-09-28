package com.planetaryfactory.core.machine.client;

import com.planetaryfactory.core.PFBlockEntities;
import com.planetaryfactory.core.PFMenus;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import rearth.oritech.client.renderers.blocks.MachineRenderer;

/**
 * The chassis machines' client half: Oritech's renderer, pointed at Oritech's assembler model
 * (#326) and centrifuge model (ADR-0096), and the pack's own screen (#327).
 *
 * <p>{@code "models/assembler"} and {@code false} are the arguments Oritech's {@code ModRenderers}
 * registers its assembler with, read off the 2.0.0-exp6 jar. The model and textures resolve into
 * Oritech's jar, so the pack ships no art for the machine.
 */
public final class AssemblingMachineClient {

    private AssemblingMachineClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(AssemblingMachineClient::registerRenderers);
        modBus.addListener(AssemblingMachineClient::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(PFMenus.ASSEMBLING_MACHINE.get(), AssemblingMachineScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PFBlockEntities.ASSEMBLING_MACHINE.get(),
                context -> new MachineRenderer<>(context, "models/assembler", false));
        // Not Oritech's CentrifugeRenderer, which is typed to its own block entity's tanks.
        event.registerBlockEntityRenderer(PFBlockEntities.CHEMICAL_PLANT.get(),
                context -> new MachineRenderer<>(context, "models/centrifuge", false));
    }
}
