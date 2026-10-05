package com.factoryworks.core.fluid;

import com.factoryworks.core.FactoryWorksCore;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Terra's two pack-owned steam fluids, made real: still, flowing and a block each
 * (#223, ADR-0048).
 *
 * <p>Two registries, kept together because the three objects for one fluid are mutually
 * referential -- the fluid needs the block to convert to on placement, the block needs the fluid's
 * source, and both are resolved lazily through {@link DeferredHolder#get()}, never eagerly.
 * Vanilla's own {@code FLUID} and {@code BLOCK} registries fire their {@code RegisterEvent} in that
 * declared order (see {@code net.minecraft.core.registries.BuiltInRegistries}), which is what makes
 * it safe for the block suppliers below to call {@code .get()} on a fluid holder from inside their
 * own registration lambda.
 *
 * <p><b>Neither fluid has a bucket, deliberately.</b> ADR-0037 already answered portable fluid for
 * this pack: {@code factoryworks:barrel}, which takes any fluid at Factorio's own 50 mB. That
 * ADR states the capacity as a rule rather than as a fact about one item -- a portable container
 * holds the Factorio number under the converter's 1:1 unit rule, and "does not get to be re-argued
 * from Minecraft's bucket". A 1 000 mB bucket of steam is exactly the twentyfold dose it rejects,
 * and it would hand the player a hand-carry route around the Boiler-pipe-Engine chain that rung 0
 * exists to teach. A {@link net.neoforged.neoforge.fluids.FluidType} needs no bucket to register.
 *
 * <p>A liquid block is drawn by its fluid model, which
 * {@link com.factoryworks.core.fluid.client.SteamFluidClient} registers; its blockstate names
 * vanilla's water model only for the particle.
 */
public final class PFFluids {
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, FactoryWorksCore.NAMESPACE);
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(FactoryWorksCore.NAMESPACE);

    // ---- Steam ----------------------------------------------------------------------------

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STEAM_SOURCE =
            FLUIDS.register("steam", () -> new BaseFlowingFluid.Source(steamProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> STEAM_FLOWING =
            FLUIDS.register("flowing_steam", () -> new BaseFlowingFluid.Flowing(steamProperties()));

    public static final DeferredHolder<Block, PFLiquidBlock> STEAM_BLOCK =
            BLOCKS.registerBlock("steam",
                    props -> new PFLiquidBlock(STEAM_SOURCE.get(), liquidProperties(props)));


    // ---- Superheated Steam ------------------------------------------------------------------

    /**
     * ADR-0048: no producer and no consumer, on purpose. The reactor is #135, the Turbine is
     * ADR-0033; registering the fluid now is what stops the second consumer being retrofitted
     * into a mechanism that assumed one.
     */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SUPERHEATED_STEAM_SOURCE =
            FLUIDS.register(
                    "superheated_steam", () -> new BaseFlowingFluid.Source(superheatedSteamProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> SUPERHEATED_STEAM_FLOWING =
            FLUIDS.register(
                    "flowing_superheated_steam",
                    () -> new BaseFlowingFluid.Flowing(superheatedSteamProperties()));

    public static final DeferredHolder<Block, PFLiquidBlock> SUPERHEATED_STEAM_BLOCK = BLOCKS.registerBlock(
            "superheated_steam",
            props -> new PFLiquidBlock(SUPERHEATED_STEAM_SOURCE.get(), liquidProperties(props)));

    // ---- Crude Oil --------------------------------------------------------------------------

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> CRUDE_OIL_SOURCE =
            FLUIDS.register("crude_oil", () -> new BaseFlowingFluid.Source(crudeOilProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> CRUDE_OIL_FLOWING =
            FLUIDS.register("flowing_crude_oil", () -> new BaseFlowingFluid.Flowing(crudeOilProperties()));

    public static final DeferredHolder<Block, PFLiquidBlock> CRUDE_OIL_BLOCK =
            BLOCKS.registerBlock("crude_oil",
                    props -> new PFLiquidBlock(CRUDE_OIL_SOURCE.get(), liquidProperties(props)));

    // ---- The refinery's and chemical plant's fluids (ADR-0109) --------------------------------

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> HEAVY_OIL_SOURCE =
            FLUIDS.register("heavy_oil", () -> new BaseFlowingFluid.Source(heavyOilProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> HEAVY_OIL_FLOWING =
            FLUIDS.register("flowing_heavy_oil", () -> new BaseFlowingFluid.Flowing(heavyOilProperties()));
    public static final DeferredHolder<Block, PFLiquidBlock> HEAVY_OIL_BLOCK =
            BLOCKS.registerBlock("heavy_oil",
                    props -> new PFLiquidBlock(HEAVY_OIL_SOURCE.get(), liquidProperties(props)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIGHT_OIL_SOURCE =
            FLUIDS.register("light_oil", () -> new BaseFlowingFluid.Source(lightOilProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> LIGHT_OIL_FLOWING =
            FLUIDS.register("flowing_light_oil", () -> new BaseFlowingFluid.Flowing(lightOilProperties()));
    public static final DeferredHolder<Block, PFLiquidBlock> LIGHT_OIL_BLOCK =
            BLOCKS.registerBlock("light_oil",
                    props -> new PFLiquidBlock(LIGHT_OIL_SOURCE.get(), liquidProperties(props)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> PETROLEUM_GAS_SOURCE =
            FLUIDS.register("petroleum_gas", () -> new BaseFlowingFluid.Source(petroleumGasProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> PETROLEUM_GAS_FLOWING =
            FLUIDS.register("flowing_petroleum_gas", () -> new BaseFlowingFluid.Flowing(petroleumGasProperties()));
    public static final DeferredHolder<Block, PFLiquidBlock> PETROLEUM_GAS_BLOCK =
            BLOCKS.registerBlock("petroleum_gas",
                    props -> new PFLiquidBlock(PETROLEUM_GAS_SOURCE.get(), liquidProperties(props)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LUBRICANT_SOURCE =
            FLUIDS.register("lubricant", () -> new BaseFlowingFluid.Source(lubricantProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> LUBRICANT_FLOWING =
            FLUIDS.register("flowing_lubricant", () -> new BaseFlowingFluid.Flowing(lubricantProperties()));
    public static final DeferredHolder<Block, PFLiquidBlock> LUBRICANT_BLOCK =
            BLOCKS.registerBlock("lubricant",
                    props -> new PFLiquidBlock(LUBRICANT_SOURCE.get(), liquidProperties(props)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SULFURIC_ACID_SOURCE =
            FLUIDS.register("sulfuric_acid", () -> new BaseFlowingFluid.Source(sulfuricAcidProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> SULFURIC_ACID_FLOWING =
            FLUIDS.register("flowing_sulfuric_acid", () -> new BaseFlowingFluid.Flowing(sulfuricAcidProperties()));
    public static final DeferredHolder<Block, PFLiquidBlock> SULFURIC_ACID_BLOCK =
            BLOCKS.registerBlock("sulfuric_acid",
                    props -> new PFLiquidBlock(SULFURIC_ACID_SOURCE.get(), liquidProperties(props)));

    private PFFluids() {
    }

    private static BaseFlowingFluid.Properties steamProperties() {
        return new BaseFlowingFluid.Properties(PFFluidTypes.STEAM, STEAM_SOURCE, STEAM_FLOWING)
                .block(STEAM_BLOCK);
    }

    private static BaseFlowingFluid.Properties crudeOilProperties() {
        return new BaseFlowingFluid.Properties(PFFluidTypes.CRUDE_OIL, CRUDE_OIL_SOURCE, CRUDE_OIL_FLOWING)
                .block(CRUDE_OIL_BLOCK);
    }

    private static BaseFlowingFluid.Properties heavyOilProperties() {
        return new BaseFlowingFluid.Properties(PFFluidTypes.HEAVY_OIL, HEAVY_OIL_SOURCE, HEAVY_OIL_FLOWING)
                .block(HEAVY_OIL_BLOCK);
    }

    private static BaseFlowingFluid.Properties lightOilProperties() {
        return new BaseFlowingFluid.Properties(PFFluidTypes.LIGHT_OIL, LIGHT_OIL_SOURCE, LIGHT_OIL_FLOWING)
                .block(LIGHT_OIL_BLOCK);
    }

    private static BaseFlowingFluid.Properties petroleumGasProperties() {
        return new BaseFlowingFluid.Properties(PFFluidTypes.PETROLEUM_GAS, PETROLEUM_GAS_SOURCE, PETROLEUM_GAS_FLOWING)
                .block(PETROLEUM_GAS_BLOCK);
    }

    private static BaseFlowingFluid.Properties lubricantProperties() {
        return new BaseFlowingFluid.Properties(PFFluidTypes.LUBRICANT, LUBRICANT_SOURCE, LUBRICANT_FLOWING)
                .block(LUBRICANT_BLOCK);
    }

    private static BaseFlowingFluid.Properties sulfuricAcidProperties() {
        return new BaseFlowingFluid.Properties(PFFluidTypes.SULFURIC_ACID, SULFURIC_ACID_SOURCE, SULFURIC_ACID_FLOWING)
                .block(SULFURIC_ACID_BLOCK);
    }

    private static BaseFlowingFluid.Properties superheatedSteamProperties() {
        return new BaseFlowingFluid.Properties(
                PFFluidTypes.SUPERHEATED_STEAM, SUPERHEATED_STEAM_SOURCE, SUPERHEATED_STEAM_FLOWING)
                .block(SUPERHEATED_STEAM_BLOCK);
    }

    /**
     * Vanilla's own water/lava shape: replaceable, no collision, no loot table of its own.
     *
     * <p>Takes the properties rather than making them: 26.1 carries the block's registry id on the
     * Properties, so a fresh {@code Properties.of()} here is an id-less block and a
     * {@code Block id not set} at registration -- which is a load failure, not a compile one.
     */
    private static BlockBehaviour.Properties liquidProperties(BlockBehaviour.Properties props) {
        return props
                .mapColor(MapColor.WATER)
                .replaceable()
                .noCollision()
                .strength(100.0F)
                .pushReaction(PushReaction.DESTROY)
                .noLootTable()
                .liquid()
                .sound(SoundType.EMPTY);
    }

    public static void register(IEventBus modBus) {
        FLUIDS.register(modBus);
        BLOCKS.register(modBus);
    }

}
