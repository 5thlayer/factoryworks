package com.factoryworks.core.fluid.client;

import com.factoryworks.core.FactoryWorksCore;
import com.factoryworks.core.fluid.FluidTintCorpus;
import com.factoryworks.core.fluid.PFFluids;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * How the oil and chemistry fluids draw in a tank, a pipe or Jade (ADR-0109).
 *
 * <p>Crude is malcolmriley's unused-textures sprite under a tint typed here. The other five draw
 * sprites under the tint {@code factoryworks_core/fluid/tints.json} holds for them.
 */
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

        registerCorpusTinted(event, "heavy_oil", "block/fluid/liquid_amber",
                PFFluids.HEAVY_OIL_SOURCE, PFFluids.HEAVY_OIL_FLOWING);
        registerCorpusTinted(event, "light_oil", "block/fluid/liquid_amber",
                PFFluids.LIGHT_OIL_SOURCE, PFFluids.LIGHT_OIL_FLOWING);
        registerCorpusTinted(event, "petroleum_gas", "block/fluid/steam",
                PFFluids.PETROLEUM_GAS_SOURCE, PFFluids.PETROLEUM_GAS_FLOWING);
        registerCorpusTinted(event, "lubricant", "block/fluid/liquid_pale",
                PFFluids.LUBRICANT_SOURCE, PFFluids.LUBRICANT_FLOWING);
        registerCorpusTinted(event, "sulfuric_acid", "block/fluid/liquid_pale",
                PFFluids.SULFURIC_ACID_SOURCE, PFFluids.SULFURIC_ACID_FLOWING);
    }

    private static void registerCorpusTinted(
            RegisterFluidModelsEvent event,
            String name,
            String sprite,
            DeferredHolder<Fluid, ? extends Fluid> still,
            DeferredHolder<Fluid, ? extends Fluid> flowing) {
        Material material = new Material(Identifier.fromNamespaceAndPath(FactoryWorksCore.NAMESPACE, sprite));
        int tint = FluidTintCorpus.get().tint(FactoryWorksCore.NAMESPACE + ":" + name).orElseThrow();
        event.register(new FluidModel.Unbaked(material, material, null, FluidTintSources.constant(tint)),
                still, flowing);
    }
}
