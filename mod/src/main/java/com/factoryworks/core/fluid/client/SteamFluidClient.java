package com.factoryworks.core.fluid.client;

import com.factoryworks.core.fluid.PFFluids;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;

/**
 * How Terra's two steam fluids draw in a tank, a pipe or Jade (#189, ADR-0048): Oritech's animated
 * steam sprite, untinted as Oritech draws its own, still and flowing alike.
 */
public final class SteamFluidClient {

    private static final Material STEAM_SPRITE =
            new Material(Identifier.fromNamespaceAndPath("oritech", "block/fluid/fluid_steam"));

    private static final int STEAM_TINT = 0xFFFFFFFF;

    /** Orange, so the Turbine's fluid is not mistaken for the Engine's in a tank. */
    private static final int SUPERHEATED_STEAM_TINT = 0xFFFF8A3D;

    private SteamFluidClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(SteamFluidClient::registerFluidModels);
    }

    private static void registerFluidModels(RegisterFluidModelsEvent event) {
        event.register(tinted(STEAM_TINT), PFFluids.STEAM_SOURCE, PFFluids.STEAM_FLOWING);
        event.register(tinted(SUPERHEATED_STEAM_TINT),
                PFFluids.SUPERHEATED_STEAM_SOURCE, PFFluids.SUPERHEATED_STEAM_FLOWING);
    }

    private static FluidModel.Unbaked tinted(int tint) {
        return new FluidModel.Unbaked(STEAM_SPRITE, STEAM_SPRITE, null, FluidTintSources.constant(tint));
    }
}
