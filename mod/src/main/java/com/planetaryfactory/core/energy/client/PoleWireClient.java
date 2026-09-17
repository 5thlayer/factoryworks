package com.planetaryfactory.core.energy.client;

import com.planetaryfactory.core.PFBlockEntities;

import com.planetaryfactory.core.energy.ClientPoles;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The client half of the Electric Network (#281): the wire between linked poles.
 *
 * <p>Called only on the client, from {@code PlanetaryFactoryCore}.
 */
public final class PoleWireClient {

    private PoleWireClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(PoleWireClient::registerRenderers);
        NeoForge.EVENT_BUS.addListener(ClientPoles::onLevelUnload);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PFBlockEntities.SUPPLY_AREA_POLE.get(), context -> new PoleWireRenderer());
    }
}
