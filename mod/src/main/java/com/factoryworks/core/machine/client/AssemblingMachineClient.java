package com.factoryworks.core.machine.client;

import com.factoryworks.core.PFBlockEntities;
import com.factoryworks.core.PFMenus;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * The chassis machines' client half: Oritech's renderer, pointed at Oritech's centrifuge model and
 * refinery models (ADR-0096), and the pack's own screen (#327).
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
        event.registerBlockEntityRenderer(PFBlockEntities.OIL_REFINERY.get(), OilRefineryRenderer::new);
    }
}
