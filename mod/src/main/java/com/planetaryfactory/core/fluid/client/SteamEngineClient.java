package com.planetaryfactory.core.fluid.client;

import com.planetaryfactory.core.PFBlockEntities;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import rearth.oritech.client.renderers.blocks.MachineRenderer;

/**
 * Oritech's Steam Engine renderer on the pack's engine type (ADR-0077). {@code "models/steam_engine"}
 * is the argument Oritech's {@code ModRenderers} registers its own engine with, read off the
 * 2.0.0-exp6 jar; the model resolves into Oritech's jar, so the pack ships no art for it.
 */
public final class SteamEngineClient {

    private SteamEngineClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(SteamEngineClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PFBlockEntities.STEAM_ENGINE.get(),
                context -> new MachineRenderer<>(context, "models/steam_engine"));
    }
}
