package com.factoryworks.core.fluid.client;

import com.factoryworks.core.PFBlockEntities;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import rearth.oritech.client.renderers.blocks.MachineRenderer;

/**
 * Oritech's steam engine model on the engine's own type, until #586 draws its own. The model
 * resolves into Oritech's jar, so the pack ships no art for it.
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
