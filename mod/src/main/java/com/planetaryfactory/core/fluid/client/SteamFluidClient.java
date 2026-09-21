package com.planetaryfactory.core.fluid.client;

import com.planetaryfactory.core.fluid.PFFluids;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;

/**
 * How Terra's two steam fluids draw in a tank, a pipe or Jade (#189, ADR-0048): vanilla's water
 * sprites, which are greyscale, under a constant tint rather than a texture of the pack's own.
 */
public final class SteamFluidClient {

    private static final Material WATER_STILL =
            new Material(Identifier.withDefaultNamespace("block/water_still"));
    private static final Material WATER_FLOW =
            new Material(Identifier.withDefaultNamespace("block/water_flow"));

    /** Factorio's steam {@code base_color}, 0.5 grey, divided by the water sprite's 0.69 mean. */
    private static final int STEAM_TINT = 0xFFB8B8B8;

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
        return new FluidModel.Unbaked(WATER_STILL, WATER_FLOW, null, FluidTintSources.constant(tint));
    }
}
