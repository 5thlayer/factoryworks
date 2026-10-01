package com.factoryworks.core.chest.client;

import com.factoryworks.core.PFBlockEntities;

import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Vanilla's chest renderer on the pack's chest type; without it the block draws nothing (#540). */
public final class ChestClient {

    private ChestClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ChestClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PFBlockEntities.CHEST.get(), ChestRenderer::new);
    }
}
