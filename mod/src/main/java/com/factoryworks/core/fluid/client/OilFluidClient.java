package com.factoryworks.core.fluid.client;

import com.factoryworks.core.FactoryWorksCore;
import com.factoryworks.core.fluid.PFFluids;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;

/** How crude oil draws in a tank, a pipe or Jade: malcolmriley's unused-textures sprite, tinted (ADR-0109). */
public final class OilFluidClient {

    private static final Material CRUDE_OIL_SPRITE =
            new Material(Identifier.fromNamespaceAndPath(FactoryWorksCore.NAMESPACE, "block/fluid/crude_oil"));

    // test_fluid_tints.py holds this to Factorio's crude oil colour.
    private static final int CRUDE_OIL_TINT = 0xFF7A7A7A;

    private OilFluidClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(OilFluidClient::registerFluidModels);
    }

    private static void registerFluidModels(RegisterFluidModelsEvent event) {
        event.register(new FluidModel.Unbaked(CRUDE_OIL_SPRITE, CRUDE_OIL_SPRITE, null,
                FluidTintSources.constant(CRUDE_OIL_TINT)), PFFluids.CRUDE_OIL_SOURCE, PFFluids.CRUDE_OIL_FLOWING);
    }
}
