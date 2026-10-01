package com.factoryworks.core.chest.client;

import com.factoryworks.core.PFBlockEntities;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Without a renderer on the pack's chest type the block draws nothing (#540). */
public final class ChestClient {

    private ChestClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ChestClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PFBlockEntities.CHEST.get(), PackChestRenderer::new);
    }
}
