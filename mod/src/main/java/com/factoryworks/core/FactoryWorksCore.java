package com.factoryworks.core;

import com.factoryworks.core.machine.PaintLock;
import com.factoryworks.core.dismantle.PipeFamily;
import com.factoryworks.core.stretch.PipeworksPipeLegs;
import com.factoryworks.core.felling.TreeFelling;
import com.factoryworks.core.gametest.PFGameTests;
import com.factoryworks.core.fluid.PFFluidTypes;
import com.factoryworks.core.fluid.PFFluids;
import com.factoryworks.core.fluid.WaterConservation;
import io.github._5thlayer.groundworks.FastReplace;
import net.minecraft.resources.Identifier;
import io.github._5thlayer.wireworks.PoleColumnReplace;
import com.factoryworks.core.placement.ReplaceGroups;
import com.factoryworks.core.placement.Oriented;
import com.factoryworks.core.placement.client.PlacementPreviewClient;
import com.factoryworks.core.fluid.client.BoilerClient;
import com.factoryworks.core.fluid.client.OilFluidClient;
import com.factoryworks.core.fluid.client.SteamFluidClient;
import com.factoryworks.core.network.PFNetwork;
import com.factoryworks.core.recipes.PFRecipes;
import com.factoryworks.core.smelting.PFFuel;
import com.factoryworks.core.smelting.client.FuelTooltip;
import com.factoryworks.core.mining.rig.client.RigClient;
import com.factoryworks.core.smelting.client.FurnaceClient;
import com.factoryworks.core.ore.OreMining;
import com.factoryworks.core.start.StartingKitGrant;
import com.factoryworks.core.worldgen.PFWorldgen;
import com.factoryworks.core.worldgen.TerraStartingArea;
import com.factoryworks.core.worldgen.VanillaSpawning;
import com.factoryworks.core.radar.ChartDeliveries;
import com.factoryworks.core.reach.Reach;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.Rotate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The pack's first-party mod (ADR-0014).
 *
 * <p>Its remit is the mechanism, never the content (ADR-0015). What lives here is what no
 * scripting API in the pack reaches: right now that is {@link net.minecraft.world.level.block.SaplingBlock}
 * backed by a {@link net.minecraft.world.level.block.grower.TreeGrower}. Every value a designer would
 * tune -- tree shape, drop counts, growth chance, display names, models, textures -- is data in the
 * pack, not a constant in this jar.
 *
 * <p>Note the deliberate split between the mod id and the registry namespace: this mod is
 * {@code factoryworks_core}, and it registers into {@code factoryworks} alongside KubeJS.
 */
@Mod(FactoryWorksCore.MOD_ID)
public final class FactoryWorksCore {
    public static final String MOD_ID = "factoryworks_core";

    /** The shared registry namespace. Not the mod id. See ADR-0014. */
    public static final String NAMESPACE = "factoryworks";

    public FactoryWorksCore(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, PFServerConfig.SPEC);
        PFBlocks.register(modBus);
        PFAttachments.register(modBus);
        PFMenus.register(modBus);
        PFDataComponents.register(modBus);
        PFItems.register(modBus);
        PFBlockEntities.register(modBus);
        PFWorldgen.register(modBus);
        PFRecipes.register(modBus);
        // The GameTests and the one registry entry they need (#271). The test-instance TYPE is
        // registered unconditionally and so does ship in the production jar -- a registry entry
        // has to exist on every side that might decode one. The tests themselves do not: the
        // event that asks for them fires only on a run with game tests enabled.
        PFGameTests.register(modBus);
        // Terra's two pack-owned steam fluids (#223, ADR-0048). Fluid types before fluids before
        // blocks before items, matching the order BuiltInRegistries declares those registries in --
        // see PFFluids' own javadoc for why that order is load-bearing here.
        PFFluidTypes.register(modBus);
        PFFluids.register(modBus);
        Placements.optIn(block -> BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(NAMESPACE)
                || Oriented.is(block));
        // Every block turns in place, as it did before the library owned Rotate (ADR-0087, #451).
        Rotate.turnsInPlace(block -> true);
        modBus.addListener(PFItems::addToCreativeTabs);
        modBus.addListener(PFBlockEntities::registerCapabilities);
        modBus.addListener(PFItems::registerCapabilities);
        modBus.addListener(PFNetwork::register);
        modBus.addListener(Reach::onEntityAttributes);
        // Game bus, not the mod bus: this one fires per running server, not per mod load.
        NeoForge.EVENT_BUS.addListener(TerraStartingArea::onServerStarted);
        if (ModList.get().isLoaded("oritech") && ModList.get().isLoaded("beltworks")) {
            NeoForge.EVENT_BUS.addListener(com.factoryworks.core.showcase.ShowcaseCommand::onRegisterCommands);
        }
        // Wireworks states no Replace group; Factorio's pole group is the Pack's (ADR-0082).
        FastReplace.group(Identifier.fromNamespaceAndPath(NAMESPACE, "electric_poles"),
                block -> ReplaceGroups.get().isIn("electric-pole", BuiltInRegistries.BLOCK.getKey(block).toString()),
                PoleColumnReplace.BUILDER);
        // Water is extracted and transported, never created (ADR-0050): re-asserted every server
        // start rather than defaulted once, because a player's own /gamerule toggle would otherwise
        // survive a reload.
        NeoForge.EVENT_BUS.addListener(WaterConservation::onServerStarting);
        NeoForge.EVENT_BUS.addListener(VanillaSpawning::onCreateSpawnPosition);
        // The area is once per world; the kit that the spec's Opening opens on is once per player
        // (#203). Both are grants, and neither is once per join.
        NeoForge.EVENT_BUS.addListener(StartingKitGrant::onLogin);
        // What burns is datapack JSON generated from Factorio's own fuel values (ADR-0047), so
        // it reloads with the rest rather than being a table compiled into this jar.
        NeoForge.EVENT_BUS.addListener(PFFuel::register);
        NeoForge.EVENT_BUS.addListener(PFFuel::onDatapackSync);
        // One break gesture draws one unit, and the block stands until it is spent (ADR-0041).
        NeoForge.EVENT_BUS.addListener(OreMining::onBreak);
        // A tree is one entity, so mining its base fells it, and the gesture costs the whole tree's
        // time on that one block (ADR-0051). Both listeners survey the same shape on purpose.
        NeoForge.EVENT_BUS.addListener(TreeFelling::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(TreeFelling::onBreak);
        NeoForge.EVENT_BUS.addListener(TreeFelling::onLogout);
        NeoForge.EVENT_BUS.addListener(PaintLock::onRightClickBlock);
        PipeFamily.register();
        PipeworksPipeLegs.register();
        NeoForge.EVENT_BUS.addListener(Reach::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(ChartDeliveries::onServerTick);
        NeoForge.EVENT_BUS.addListener(ChartDeliveries::onLogout);
        NeoForge.EVENT_BUS.addListener(ChartDeliveries::onChunkSent);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            FurnaceClient.register(modBus);
            com.factoryworks.core.chest.client.ChestClient.register(modBus);
            RigClient.register(modBus);
            com.factoryworks.core.machine.client.AssemblingMachineClient.register(modBus);
            com.factoryworks.core.fluid.client.SteamEngineClient.register(modBus);
            com.factoryworks.core.oil.client.PumpjackClient.register(modBus);
            SteamFluidClient.register(modBus);
            OilFluidClient.register(modBus);
            BoilerClient.register(modBus);
            PlacementPreviewClient.register();
            // What an item is worth as fuel, on its own tooltip: the fuel table is default-deny,
            // so vanilla's intuitions about what burns are wrong in both directions.
            FuelTooltip.register();
            com.factoryworks.core.start.client.QuestBookTooltip.register();
            com.factoryworks.core.radar.client.RadarMapClient.register();
        }
    }
}
