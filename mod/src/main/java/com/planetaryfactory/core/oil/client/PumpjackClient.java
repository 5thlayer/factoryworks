package com.planetaryfactory.core.oil.client;

import com.planetaryfactory.core.PFBlockEntities;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import rearth.oritech.client.renderers.blocks.MachineRenderer;

/** Oritech's Pump model, scaled from one block to the Pumpjack's three (ADR-0081). */
public final class PumpjackClient {

    private static final float SCALE = 3.0f;

    private PumpjackClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(PumpjackClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PFBlockEntities.PUMPJACK.get(),
                context -> new MachineRenderer<>(context, "models/pump").withScale(SCALE));
    }
}
