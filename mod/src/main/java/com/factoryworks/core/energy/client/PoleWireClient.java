package com.factoryworks.core.energy.client;

import com.factoryworks.core.PFBlockEntities;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The client half of the Electric Network (#281): the wire between linked poles.
 *
 * <p>Called only on the client, from {@code FactoryWorksCore}.
 */
public final class PoleWireClient {

    private PoleWireClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(PoleWireClient::registerRenderers);
        // The Supply Area Box (#158): its line pipeline ignores depth, which no stock line type
        // does, so it has to be registered before the first frame that draws one.
        SupplyAreaBox.register(modBus);
        NeoForge.EVENT_BUS.addListener(com.factoryworks.core.energy.ClientWires::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(PoleWireClient::onLevelUnload);
    }

    /** The box's machine scan is cached, and a cached answer must not outlive its world. */
    private static void onLevelUnload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            SuppliedMachines.clear();
        }
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PFBlockEntities.SUPPLY_AREA_POLE.get(), context -> new PoleWireRenderer());
    }
}
