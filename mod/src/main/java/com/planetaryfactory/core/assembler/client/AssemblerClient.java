package com.planetaryfactory.core.assembler.client;

import com.planetaryfactory.core.PFMenus;
import com.planetaryfactory.core.PlanetaryFactoryCore;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The client half of the Personal Assembler: the Crafting Plan's screen, the queue on the inventory
 * screen and the queue beside the hotbar.
 *
 * <p>Called only on the client, from {@code PlanetaryFactoryCore}, so nothing here is loaded on a
 * dedicated server.
 */
public final class AssemblerClient {

    private AssemblerClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(AssemblerClient::registerScreens);
        modBus.addListener(AssemblerClient::registerHud);
        NeoForge.EVENT_BUS.addListener(InventoryQueue::onRender);
        NeoForge.EVENT_BUS.addListener(InventoryQueue::onTooltip);
        NeoForge.EVENT_BUS.addListener(InventoryQueue::onClick);
    }

    /** Above the hotbar in draw order, so the queue is not painted under it. */
    private static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "assembler_queue"),
                new AssemblerHud());
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(PFMenus.CRAFTING_PLAN.get(), CraftingPlanScreen::new);
    }
}
