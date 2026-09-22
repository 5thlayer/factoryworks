package com.planetaryfactory.core;

import com.planetaryfactory.core.energy.PoleTier;
import com.planetaryfactory.core.machine.AssemblingMachineBlock;
import com.planetaryfactory.core.machine.AssemblingMachineFootprint;
import com.planetaryfactory.core.machine.footprint.FootprintMachine;
import com.planetaryfactory.core.machine.footprint.FootprintPartBlock;
import com.planetaryfactory.core.mining.rig.RigBlock;
import com.planetaryfactory.core.mining.rig.RigPartBlock;
import com.planetaryfactory.core.mining.rig.RigTier;
import com.planetaryfactory.core.ore.OreBlock;
import com.planetaryfactory.core.oil.OilWellBlock;
import com.planetaryfactory.core.radar.RadarBlock;
import com.planetaryfactory.core.radar.RadarFootprint;
import com.planetaryfactory.core.radar.RadarPartBlock;
import com.planetaryfactory.core.ore.OreResource;
import com.planetaryfactory.core.smelting.FurnaceBlock;
import com.planetaryfactory.core.smelting.FurnaceTier;
import com.planetaryfactory.core.energy.CreativeSupplyAreaPoleBlock;
import com.planetaryfactory.core.energy.SupplyAreaPoleBlock;
import com.planetaryfactory.core.fluid.BoilerBlock;
import com.planetaryfactory.core.fluid.OffshorePumpBlock;
import com.planetaryfactory.core.fluid.SteamEngineBlock;
import com.planetaryfactory.core.fluid.SteamEngineFootprint;
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
 * The blocks the mod itself registers: the two saplings, the pole blocks, the furnace and rig
 * ladders, the Boiler, the pump, the Assembling Machine, the Steam Engine, the Radar and
 * the oil well.
 *
 * <p>The supply-area poles are here (ADR-0036) -- the three tiers and the creative pole (#272),
 * which is one block beside the ladder rather than a row in it. They are mechanism -- a block
 * entity that scans and pushes energy -- so ADR-0015 puts them in the mod rather than in KubeJS,
 * while their models, textures and names stay data in the pack like everything else.
 *
 * <p>Everything else the trees are made of -- logs, leaves, stems, fruit -- is registered by
 * {@code kubejs/startup_scripts/blocks.js}, into this same namespace. The boundary is ADR-0015;
 * a comment at the top of that file restates it, because that is the file someone will have open
 * when they are about to collide with these two ids.
 */
public final class PFBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(PlanetaryFactoryCore.NAMESPACE);

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
     * <p>One block, not a ladder. ADR-0033 has the reactor emitting superheated steam directly
     * with no heat layer, so Factorio's second boiler tier has nothing to be in this pack.
     */
    public static final DeferredHolder<Block, BoilerBlock> BOILER =
            BLOCKS.registerBlock("boiler", BoilerBlock::new);

    /**
     * The Assembling Machine (#326, ADR-0071): an Oritech machine anchor and the invisible parts its
     * footprint is made of. One block, not a ladder -- whether tiers 2 and 3 are blocks at all is
     * #295's.
     */
    public static final DeferredHolder<Block, AssemblingMachineBlock> ASSEMBLING_MACHINE =
            BLOCKS.registerBlock("assembling_machine", props -> new AssemblingMachineBlock(machineProperties(props)));

    public static final DeferredHolder<Block, FootprintPartBlock> ASSEMBLING_MACHINE_PART =
            BLOCKS.registerBlock("assembling_machine_part",
                    props -> new FootprintPartBlock(machineProperties(props).noLootTable(),
                            () -> PFBlocks.ASSEMBLING_MACHINE_FOOTPRINT));

    public static final FootprintMachine ASSEMBLING_MACHINE_FOOTPRINT = new FootprintMachine(
            AssemblingMachineFootprint.FOOTPRINT, ASSEMBLING_MACHINE, ASSEMBLING_MACHINE_PART,
            () -> PFItems.ASSEMBLING_MACHINE.get());

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

    /** The Radar (#368): a pack anchor on the footprint seam, its parts drawn so the whole cube shows. */
    public static final DeferredHolder<Block, RadarBlock> RADAR =
            BLOCKS.registerBlock("radar", props -> new RadarBlock(radarProperties(props)));

    public static final DeferredHolder<Block, RadarPartBlock> RADAR_PART =
            BLOCKS.registerBlock("radar_part",
                    props -> new RadarPartBlock(radarProperties(props).noLootTable(),
                            () -> PFBlocks.RADAR_FOOTPRINT));

    public static final FootprintMachine RADAR_FOOTPRINT = new FootprintMachine(
            RadarFootprint.FOOTPRINT, RADAR, RADAR_PART, () -> PFItems.RADAR.get());

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
     * Factorio's three furnace tiers (#155), keyed the same way the poles are: the id comes from
     * the tier rather than being typed out twice.
     */
    private static final Map<FurnaceTier, DeferredHolder<Block, FurnaceBlock>> FURNACES =
            new EnumMap<>(FurnaceTier.class);

    /**
     * One block per {@link PoleTier}, in declaration order, so the four ids are derived from the
     * tier rather than typed out twice.
     */
    private static final Map<PoleTier, DeferredHolder<Block, SupplyAreaPoleBlock>> POLES =
            new EnumMap<>(PoleTier.class);

    static {
        for (PoleTier tier : PoleTier.values()) {
            POLES.put(tier, BLOCKS.registerBlock(tier.blockName(),
                    props -> new SupplyAreaPoleBlock(tier, props)));
        }
    }

    /**
     * The creative pole (#272): a supply-area pole with a ledger that is always full, so an energy
     * face can be checked by hand without first building a power chain.
     *
     * <p>Registered beside the ladder rather than in it. {@link PoleTier} is Factorio's footprint
     * ladder and three loops walk it; a creative row would reach the item map, the recipe sweep and
     * the mechanic ledger, none of which have a row to give a dev tool.
     */
    public static final DeferredHolder<Block, CreativeSupplyAreaPoleBlock> CREATIVE_POLE =
            BLOCKS.registerBlock(CreativeSupplyAreaPoleBlock.BLOCK_NAME,
                    CreativeSupplyAreaPoleBlock::new);

    static {
        for (FurnaceTier tier : FurnaceTier.values()) {
            FURNACES.put(tier, BLOCKS.registerBlock(tier.blockName(),
                    props -> new FurnaceBlock(tier, props)));
        }
    }

    /**
     * The two rigs' anchors, and the parts that surround them (#192, ADR-0043). One anchor and one
     * part block per tier, the way the furnace and pole ladders are one class per tier -- the
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

    public static DeferredHolder<Block, SupplyAreaPoleBlock> pole(PoleTier tier) {
        return POLES.get(tier);
    }

    /**
     * Every pole block, for the block entity type that serves all of them -- the three tiers and
     * the creative pole, which is one too and would have no block entity at all if it were left
     * out of this set.
     */
    public static Set<Block> poleBlocks() {
        return Stream.concat(POLES.values().stream(), Stream.of(CREATIVE_POLE))
                .map(DeferredHolder::get).collect(Collectors.toUnmodifiableSet());
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
