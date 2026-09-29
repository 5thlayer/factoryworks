package com.factoryworks.core.mining.rig.client;

import com.factoryworks.core.PFMenus;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The client half of the two mining rigs: one screen for one tier-aware menu (#193), and the
 * mining-area overlay on a rig looked at (#195).
 *
 * <p>Called only on the client, from {@code FactoryWorksCore}.
 */
public final class RigClient {

    private RigClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(RigClient::registerScreens);
        NeoForge.EVENT_BUS.addListener(MiningAreaOverlay::onSubmitGeometry);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(PFMenus.RIG.get(), RigScreen::new);
    }
}
