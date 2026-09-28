package com.planetaryfactory.core;

import com.planetaryfactory.core.machine.PaintLock;
import com.planetaryfactory.core.dismantle.PipeFamily;
import com.planetaryfactory.core.stretch.OritechPipeLegs;
import com.planetaryfactory.core.felling.TreeFelling;
import com.planetaryfactory.core.gametest.PFGameTests;
import com.planetaryfactory.core.fluid.PFFluidTypes;
import com.planetaryfactory.core.fluid.PFFluids;
import com.planetaryfactory.core.fluid.WaterConservation;
import com.planetaryfactory.core.energy.client.PoleWireClient;
import com.planetaryfactory.core.placement.Oriented;
import com.planetaryfactory.core.placement.client.PlacementPreviewClient;
import com.planetaryfactory.core.fluid.client.BoilerClient;
import com.planetaryfactory.core.fluid.client.SteamFluidClient;
import com.planetaryfactory.core.network.PFNetwork;
import com.planetaryfactory.core.recipes.PFRecipes;
import com.planetaryfactory.core.smelting.PFFuel;
import com.planetaryfactory.core.smelting.client.FuelTooltip;
import com.planetaryfactory.core.mining.rig.client.RigClient;
import com.planetaryfactory.core.smelting.client.FurnaceClient;
import com.planetaryfactory.core.ore.OreMining;
import com.planetaryfactory.core.start.StartingKitGrant;
import com.planetaryfactory.core.worldgen.PFWorldgen;
import com.planetaryfactory.core.worldgen.TerraStartingArea;
import com.planetaryfactory.core.worldgen.VanillaSpawning;
import com.planetaryfactory.core.radar.ChartDeliveries;
import com.planetaryfactory.core.reach.Reach;
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
 * {@code planetaryfactory_core}, and it registers into {@code planetaryfactory} alongside KubeJS.
 */
@Mod(PlanetaryFactoryCore.MOD_ID)
public final class PlanetaryFactoryCore {
    public static final String MOD_ID = "planetaryfactory_core";

    /** The shared registry namespace. Not the mod id. See ADR-0014. */
    public static final String NAMESPACE = "planetaryfactory";

    public PlanetaryFactoryCore(IEventBus modBus, ModContainer container) {
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
        // Every Electric Network in a level settles once per level tick (ADR-0062). Poles only
        // report and scan; without this line no pole moves any energy at all.
        NeoForge.EVENT_BUS.addListener(com.planetaryfactory.core.energy.ElectricNetworks::onLevelTick);
        NeoForge.EVENT_BUS.addListener(com.planetaryfactory.core.energy.ElectricNetworks::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(com.planetaryfactory.core.energy.LevelWires::onChunkSent);
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
        if (ModList.get().isLoaded("oritech")) {
            OritechPipeLegs.register();
        }
        NeoForge.EVENT_BUS.addListener(Reach::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(ChartDeliveries::onServerTick);
        NeoForge.EVENT_BUS.addListener(ChartDeliveries::onLogout);
        NeoForge.EVENT_BUS.addListener(ChartDeliveries::onChunkSent);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            FurnaceClient.register(modBus);
            RigClient.register(modBus);
            com.planetaryfactory.core.machine.client.AssemblingMachineClient.register(modBus);
            com.planetaryfactory.core.fluid.client.SteamEngineClient.register(modBus);
            com.planetaryfactory.core.oil.client.PumpjackClient.register(modBus);
            SteamFluidClient.register(modBus);
            BoilerClient.register(modBus);
            // The wire between linked poles (#281); cosmetic, the balance never reads it.
            PoleWireClient.register(modBus);
            PlacementPreviewClient.register();
            // What an item is worth as fuel, on its own tooltip: the fuel table is default-deny,
            // so vanilla's intuitions about what burns are wrong in both directions.
            FuelTooltip.register();
            com.planetaryfactory.core.start.client.QuestBookTooltip.register();
            com.planetaryfactory.core.radar.client.RadarMapClient.register();
        }
    }
}
