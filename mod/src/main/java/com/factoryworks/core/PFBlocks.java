package com.factoryworks.core;

import com.factoryworks.core.machine.AssemblingMachineBlock;
import com.factoryworks.core.machine.AssemblingMachineFootprint;
import com.factoryworks.core.machine.AssemblingTier;
import com.factoryworks.core.machine.ChemicalPlantBlock;
import com.factoryworks.core.machine.ChemicalPlantFootprint;
import com.factoryworks.core.machine.OilRefineryBlock;
import com.factoryworks.core.machine.OilRefineryFootprint;
import com.factoryworks.core.machine.footprint.FootprintMachine;
import com.factoryworks.core.machine.footprint.FootprintPartBlock;
import com.factoryworks.core.mining.rig.RigBlock;
import com.factoryworks.core.mining.rig.RigPartBlock;
import com.factoryworks.core.mining.rig.RigTier;
import com.factoryworks.core.ore.OreBlock;
import com.factoryworks.core.oil.OilWellBlock;
import com.factoryworks.core.oil.PumpjackBlock;
import com.factoryworks.core.oil.PumpjackFootprint;
import com.factoryworks.core.oil.PumpjackPartBlock;
import com.factoryworks.core.radar.RadarBlock;
import com.factoryworks.core.radar.RadarFootprint;
import com.factoryworks.core.radar.RadarPartBlock;
import com.factoryworks.core.ore.OreResource;
import com.factoryworks.core.smelting.FurnaceBlock;
import com.factoryworks.core.smelting.FurnaceTier;
import com.factoryworks.core.fluid.BoilerBlock;
import com.factoryworks.core.fluid.BoilerFootprint;
import com.factoryworks.core.fluid.OffshorePumpBlock;
import com.factoryworks.core.energy.AccumulatorBlock;
import com.factoryworks.core.energy.AccumulatorFootprint;
import com.factoryworks.core.energy.SolarPanelBlock;
import com.factoryworks.core.energy.SolarPanelFootprint;
import com.factoryworks.core.fluid.SteamEngineBlock;
import com.factoryworks.core.fluid.SteamEngineFootprint;
import com.factoryworks.core.chest.ChestTier;
import com.factoryworks.core.chest.PackChestBlock;
import com.factoryworks.core.wreck.CargoHoldBlock;
import com.factoryworks.core.wreck.DebrisSize;
import com.factoryworks.core.wreck.WreckDebrisBlock;
import com.factoryworks.core.wreck.WreckHullBlock;
import com.factoryworks.core.wreck.WreckHullSlabBlock;
import com.factoryworks.core.wreck.WreckHullStairsBlock;
import com.factoryworks.core.wreck.WreckWindowBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The blocks the mod itself registers: the two saplings, the furnace and rig
 * ladders, the Boiler, the pump, the Assembling Machine, the Steam Engine, the Radar, the Pumpjack
 * and the oil well.
 *
 * <p>Everything else the trees are made of -- logs, leaves, stems, fruit -- is registered by
 * {@code kubejs/startup_scripts/blocks.js}, into this same namespace. The boundary is ADR-0015;
 * a comment at the top of that file restates it, because that is the file someone will have open
 * when they are about to collide with these two ids.
 */
public final class PFBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(FactoryWorksCore.NAMESPACE);

    public static final DeferredHolder<Block, SaplingBlock> YUMAKO_SAPLING =
            sapling("yumako_sapling", PFTrees.YUMAKO);
    public static final DeferredHolder<Block, SaplingBlock> JELLYSTEM_SAPLING =
            sapling("jellystem_sapling", PFTrees.JELLYSTEM);

    /**
     * Factorio's Offshore Pump (#213, ADR-0050): the single point at which water enters the
     * factory. One block rather than a ladder -- Factorio has one pump and so does this pack.
     */
    public static final DeferredHolder<Block, OffshorePumpBlock> OFFSHORE_PUMP =
            BLOCKS.registerBlock("offshore_pump", OffshorePumpBlock::new);

    /**
     * Terra's Boiler (#224, ADR-0048): fuel and water in, low-temperature steam out.
     *
     * <p>One machine, not a ladder, and a 3x2 footprint (ADR-0114). ADR-0033 has the reactor emitting superheated steam directly
     * with no heat layer, so Factorio's second boiler tier has nothing to be in this pack.
     */
    public static final DeferredHolder<Block, BoilerBlock> BOILER =
            BLOCKS.registerBlock("boiler", BoilerBlock::new);

    public static final DeferredHolder<Block, FootprintPartBlock> BOILER_PART =
            BLOCKS.registerBlock("boiler_part",
                    props -> new FootprintPartBlock(machineProperties(props).noLootTable(),
                            () -> PFBlocks.BOILER_FOOTPRINT));

    public static final FootprintMachine BOILER_FOOTPRINT = new FootprintMachine(
            BoilerFootprint.FOOTPRINT, BOILER, BOILER_PART, () -> PFItems.BOILER.get());

    /**
     * The Assembling Machine ladder (#326, #295, ADR-0071, ADR-0075): per tier, an Oritech machine
     * anchor and the invisible parts its footprint is made of, one part block per tier so a part
     * tears down its own tier's anchor.
     */
    private static final Map<AssemblingTier, DeferredHolder<Block, AssemblingMachineBlock>> ASSEMBLING_MACHINES =
            new EnumMap<>(AssemblingTier.class);
    private static final Map<AssemblingTier, FootprintMachine> ASSEMBLING_FOOTPRINTS =
            new EnumMap<>(AssemblingTier.class);

    static {
        for (AssemblingTier tier : AssemblingTier.values()) {
            DeferredHolder<Block, AssemblingMachineBlock> anchor = BLOCKS.registerBlock(tier.blockName(),
                    props -> new AssemblingMachineBlock(tier, machineProperties(props)));
            DeferredHolder<Block, FootprintPartBlock> part = BLOCKS.registerBlock(tier.partBlockName(),
                    props -> new FootprintPartBlock(machineProperties(props).noLootTable(),
                            () -> PFBlocks.assemblingFootprint(tier)));
            ASSEMBLING_MACHINES.put(tier, anchor);
            ASSEMBLING_FOOTPRINTS.put(tier, new FootprintMachine(AssemblingMachineFootprint.FOOTPRINT,
                    anchor, part, () -> PFItems.assemblingMachine(tier).get()));
        }
    }

    public static final DeferredHolder<Block, ChemicalPlantBlock> CHEMICAL_PLANT =
            BLOCKS.registerBlock("chemical_plant", props -> new ChemicalPlantBlock(machineProperties(props)));

    public static final DeferredHolder<Block, FootprintPartBlock> CHEMICAL_PLANT_PART =
            BLOCKS.registerBlock("chemical_plant_part",
                    props -> new FootprintPartBlock(machineProperties(props).noLootTable(),
                            () -> PFBlocks.CHEMICAL_PLANT_FOOTPRINT));

    public static final FootprintMachine CHEMICAL_PLANT_FOOTPRINT = new FootprintMachine(
            ChemicalPlantFootprint.FOOTPRINT, CHEMICAL_PLANT, CHEMICAL_PLANT_PART,
            () -> PFItems.CHEMICAL_PLANT.get());

    public static final DeferredHolder<Block, OilRefineryBlock> OIL_REFINERY =
            BLOCKS.registerBlock("oil_refinery", props -> new OilRefineryBlock(machineProperties(props)));

    public static final DeferredHolder<Block, FootprintPartBlock> OIL_REFINERY_PART =
            BLOCKS.registerBlock("oil_refinery_part",
                    props -> new FootprintPartBlock(machineProperties(props).noLootTable(),
                            () -> PFBlocks.OIL_REFINERY_FOOTPRINT));

    public static final FootprintMachine OIL_REFINERY_FOOTPRINT = new FootprintMachine(
            OilRefineryFootprint.FOOTPRINT, OIL_REFINERY, OIL_REFINERY_PART,
            () -> PFItems.OIL_REFINERY.get());

    /** Terra's Steam Engine (ADR-0077): Oritech's engine entity, on the Assembling Machine's footprint seam. */
    public static final DeferredHolder<Block, SteamEngineBlock> STEAM_ENGINE =
            BLOCKS.registerBlock("steam_engine", props -> new SteamEngineBlock(machineProperties(props)));

    public static final DeferredHolder<Block, FootprintPartBlock> STEAM_ENGINE_PART =
            BLOCKS.registerBlock("steam_engine_part",
                    props -> new FootprintPartBlock(machineProperties(props).noLootTable(),
                            () -> PFBlocks.STEAM_ENGINE_FOOTPRINT));

    public static final FootprintMachine STEAM_ENGINE_FOOTPRINT = new FootprintMachine(
            SteamEngineFootprint.FOOTPRINT, STEAM_ENGINE, STEAM_ENGINE_PART,
            () -> PFItems.STEAM_ENGINE.get());

    /** The Solar Panel (#529): Oritech's Big Solar Panel entity, on the footprint seam. */
    public static final DeferredHolder<Block, SolarPanelBlock> SOLAR_PANEL =
            BLOCKS.registerBlock("solar_panel", props -> new SolarPanelBlock(machineProperties(props)));

    public static final DeferredHolder<Block, FootprintPartBlock> SOLAR_PANEL_PART =
            BLOCKS.registerBlock("solar_panel_part",
                    props -> new FootprintPartBlock(machineProperties(props).noLootTable(),
                            () -> PFBlocks.SOLAR_PANEL_FOOTPRINT));

    public static final FootprintMachine SOLAR_PANEL_FOOTPRINT = new FootprintMachine(
            SolarPanelFootprint.FOOTPRINT, SOLAR_PANEL, SOLAR_PANEL_PART,
            () -> PFItems.SOLAR_PANEL.get());

    /** The accumulator (#283): Oritech's Large Energy Storage entity, on the footprint seam. */
    public static final DeferredHolder<Block, AccumulatorBlock> ACCUMULATOR =
            BLOCKS.registerBlock("accumulator", props -> new AccumulatorBlock(machineProperties(props)));

    public static final DeferredHolder<Block, FootprintPartBlock> ACCUMULATOR_PART =
            BLOCKS.registerBlock("accumulator_part",
                    props -> new FootprintPartBlock(machineProperties(props).noLootTable(),
                            () -> PFBlocks.ACCUMULATOR_FOOTPRINT));

    public static final FootprintMachine ACCUMULATOR_FOOTPRINT = new FootprintMachine(
            AccumulatorFootprint.FOOTPRINT, ACCUMULATOR, ACCUMULATOR_PART,
            () -> PFItems.ACCUMULATOR.get());

    /** The Radar (#368): a pack anchor on the footprint seam, its parts drawn so the whole cube shows. */
    public static final DeferredHolder<Block, RadarBlock> RADAR =
            BLOCKS.registerBlock("radar", props -> new RadarBlock(radarProperties(props)));

    public static final DeferredHolder<Block, RadarPartBlock> RADAR_PART =
            BLOCKS.registerBlock("radar_part",
                    props -> new RadarPartBlock(radarProperties(props).noLootTable(),
                            () -> PFBlocks.RADAR_FOOTPRINT));

    public static final FootprintMachine RADAR_FOOTPRINT = new FootprintMachine(
            RadarFootprint.FOOTPRINT, RADAR, RADAR_PART, () -> PFItems.RADAR.get());

    /** The Pumpjack (ADR-0081): a pack anchor on the footprint seam, drawn whole by Oritech's Pump model. */
    public static final DeferredHolder<Block, PumpjackBlock> PUMPJACK =
            BLOCKS.registerBlock("pumpjack", props -> new PumpjackBlock(machineProperties(props)));

    public static final DeferredHolder<Block, PumpjackPartBlock> PUMPJACK_PART =
            BLOCKS.registerBlock("pumpjack_part",
                    props -> new PumpjackPartBlock(machineProperties(props).noLootTable(),
                            () -> PFBlocks.PUMPJACK_FOOTPRINT));

    public static final FootprintMachine PUMPJACK_FOOTPRINT = new FootprintMachine(
            PumpjackFootprint.FOOTPRINT, PUMPJACK, PUMPJACK_PART, () -> PFItems.PUMPJACK.get());

    /** The wreck's blocks (ADR-0107): none has an item, and nothing breaks them. */
    public static final DeferredHolder<Block, WreckHullBlock> WRECK_HULL =
            BLOCKS.registerBlock("wreck_hull", WreckHullBlock::new);

    public static final DeferredHolder<Block, WreckHullStairsBlock> WRECK_HULL_STAIRS =
            BLOCKS.registerBlock("wreck_hull_stairs",
                    props -> new WreckHullStairsBlock(WRECK_HULL.get().defaultBlockState(), props));

    public static final DeferredHolder<Block, WreckHullSlabBlock> WRECK_HULL_SLAB =
            BLOCKS.registerBlock("wreck_hull_slab", WreckHullSlabBlock::new);

    public static final DeferredHolder<Block, WreckWindowBlock> WRECK_WINDOW =
            BLOCKS.registerBlock("wreck_window", WreckWindowBlock::new);

    public static final DeferredHolder<Block, CargoHoldBlock> CARGO_HOLD =
            BLOCKS.registerBlock("cargo_hold", CargoHoldBlock::new);

    /** The wreck's Debris (#550): breakable, with no item and nothing to drop. */
    public static final DeferredHolder<Block, WreckDebrisBlock> WRECK_DEBRIS_BIG =
            BLOCKS.registerBlock("wreck_debris_big", props -> new WreckDebrisBlock(DebrisSize.BIG, props));

    public static final DeferredHolder<Block, WreckDebrisBlock> WRECK_DEBRIS_MEDIUM =
            BLOCKS.registerBlock("wreck_debris_medium",
                    props -> new WreckDebrisBlock(DebrisSize.MEDIUM, props));

    public static final DeferredHolder<Block, WreckDebrisBlock> WRECK_DEBRIS_SMALL =
            BLOCKS.registerBlock("wreck_debris_small",
                    props -> new WreckDebrisBlock(DebrisSize.SMALL, props));

    /** An oil well (ADR-0081): only worldgen places one, and nothing breaks it. */
    public static final DeferredHolder<Block, OilWellBlock> OIL_WELL =
            BLOCKS.registerBlock("oil_well", OilWellBlock::new);

    /**
     * One block per {@link OreResource}: Terra's five ore blocks (ADR-0041).
     *
     * <p>Pack-authored rather than GregTech's because they carry an amount and a sprite stage, and
     * GregTech models its ore blocks at runtime -- the ADR has the cost comparison. They still drop
     * GregTech's raw ore, so nothing downstream of the item can tell.
     */
    private static final Map<OreResource, DeferredHolder<Block, OreBlock>> ORES =
            new EnumMap<>(OreResource.class);

    static {
        for (OreResource resource : OreResource.values()) {
            ORES.put(resource, BLOCKS.registerBlock(resource.blockName(),
                    props -> new OreBlock(resource, props)));
        }
    }

    /**
     * Factorio's three furnace tiers (#155), keyed by tier: the id comes from the tier rather than
     * being typed out twice.
     */
    private static final Map<FurnaceTier, DeferredHolder<Block, FurnaceBlock>> FURNACES =
            new EnumMap<>(FurnaceTier.class);


    static {
        for (FurnaceTier tier : FurnaceTier.values()) {
            FURNACES.put(tier, BLOCKS.registerBlock(tier.blockName(),
                    props -> new FurnaceBlock(tier, props)));
        }
    }

    /**
     * The two rigs' anchors, and the parts that surround them (#192, ADR-0043). One anchor and one
     * part block per tier, the way the furnace ladder is one class per tier -- the
     * anchor holds the block entity and every part forwards a break to it.
     */
    private static final Map<RigTier, DeferredHolder<Block, RigBlock>> RIGS = new EnumMap<>(RigTier.class);
    private static final Map<RigTier, DeferredHolder<Block, RigPartBlock>> RIG_PARTS =
            new EnumMap<>(RigTier.class);

    static {
        for (RigTier tier : RigTier.values()) {
            RIGS.put(tier, BLOCKS.registerBlock(tier.blockName(), props -> new RigBlock(tier, props)));
            RIG_PARTS.put(tier, BLOCKS.registerBlock(tier.partBlockName(),
                    props -> new RigPartBlock(tier, props)));
        }
    }

    /**
     * A footprint machine's anchor and parts. {@code noOcclusion}: they render nothing themselves
     * (Oritech's renderer draws the model from the anchor), so a neighbour that culled its face
     * against one would show a hole straight through the machine.
     */
    private static BlockBehaviour.Properties machineProperties(BlockBehaviour.Properties props) {
        return props
                .mapColor(net.minecraft.world.level.material.MapColor.METAL)
                .strength(3.5F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .noOcclusion()
                // A piston moving one block would strand the rest of the footprint.
                .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK);
    }

    /** A footprint machine's, but occluding: every block of the Radar draws itself. */
    private static BlockBehaviour.Properties radarProperties(BlockBehaviour.Properties props) {
        return props
                .mapColor(net.minecraft.world.level.material.MapColor.METAL)
                .strength(3.5F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK);
    }

    private PFBlocks() {
    }

    public static DeferredHolder<Block, OreBlock> ore(OreResource resource) {
        return ORES.get(resource);
    }


    public static DeferredHolder<Block, AssemblingMachineBlock> assemblingMachine(AssemblingTier tier) {
        return ASSEMBLING_MACHINES.get(tier);
    }

    public static FootprintMachine assemblingFootprint(AssemblingTier tier) {
        return ASSEMBLING_FOOTPRINTS.get(tier);
    }

    /** The three anchors, for the block entity type that serves all of them. */
    public static Set<Block> assemblingMachineBlocks() {
        return ASSEMBLING_MACHINES.values().stream().map(DeferredHolder::get)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static final Map<ChestTier, DeferredHolder<Block, PackChestBlock>> CHESTS =
            new EnumMap<>(ChestTier.class);

    static {
        for (ChestTier tier : ChestTier.values()) {
            CHESTS.put(tier, BLOCKS.registerBlock(tier.blockName(),
                    props -> new PackChestBlock(tier, props)));
        }
    }

    public static DeferredHolder<Block, PackChestBlock> chest(ChestTier tier) {
        return CHESTS.get(tier);
    }

    public static Set<Block> chestBlocks() {
        return CHESTS.values().stream().map(DeferredHolder::get).collect(Collectors.toUnmodifiableSet());
    }

    public static DeferredHolder<Block, FurnaceBlock> furnace(FurnaceTier tier) {
        return FURNACES.get(tier);
    }

    /** The three furnace blocks, for the block entity type that serves all of them. */
    public static Set<Block> furnaceBlocks() {
        return FURNACES.values().stream().map(DeferredHolder::get).collect(Collectors.toUnmodifiableSet());
    }

    public static DeferredHolder<Block, RigBlock> rig(RigTier tier) {
        return RIGS.get(tier);
    }

    public static DeferredHolder<Block, RigPartBlock> rigPart(RigTier tier) {
        return RIG_PARTS.get(tier);
    }

    /** Both rigs' anchor blocks, for the block entity type that serves both. */
    public static Set<Block> rigBlocks() {
        return RIGS.values().stream().map(DeferredHolder::get).collect(Collectors.toUnmodifiableSet());
    }

    /** Both rigs' part blocks, for the block entity type that serves both. */
    public static Set<Block> rigPartBlocks() {
        return RIG_PARTS.values().stream().map(DeferredHolder::get).collect(Collectors.toUnmodifiableSet());
    }

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }

    private static DeferredHolder<Block, SaplingBlock> sapling(String name, TreeGrower grower) {
        return BLOCKS.registerBlock(name, props -> new SaplingBlock(
                grower,
                props
                        .mapColor(net.minecraft.world.level.material.MapColor.PLANT)
                        .noCollision()
                        .randomTicks()
                        .instabreak()
                        .sound(SoundType.GRASS)
                        .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
    }
}
