package com.factoryworks.core.energy.client;

import com.factoryworks.core.PFBlockEntities;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import rearth.oritech.client.renderers.blocks.SolarPanelRenderer;

/**
 * Oritech's Big Solar Panel renderer on the pack's panel type (#529). {@code "models/big_solar_panel"}
 * is the argument Oritech's {@code ModRenderers} registers its own panel with, read off the 2.0.0-exp6
 * jar.
 */
public final class SolarPanelClient {

    private SolarPanelClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(SolarPanelClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PFBlockEntities.SOLAR_PANEL.get(),
                context -> new SolarPanelRenderer<>(context, "models/big_solar_panel"));
    }
}
