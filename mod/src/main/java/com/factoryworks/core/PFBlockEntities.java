package com.factoryworks.core;

import com.factoryworks.core.chest.PackChestBlockEntity;
import com.factoryworks.core.wreck.CargoHoldBlock;
import com.factoryworks.core.wreck.CargoHoldBlockEntity;
import com.factoryworks.core.wreck.CargoHoldItemHandler;
import com.factoryworks.core.fluid.BoilerBlockEntity;
import com.factoryworks.core.fluid.BoilerItemHandler;
import com.factoryworks.core.fluid.OffshorePumpBlockEntity;
import com.factoryworks.core.energy.AccumulatorBlockEntity;
import com.factoryworks.core.energy.SolarPanelBlockEntity;
import com.factoryworks.core.fluid.SteamEngineBlockEntity;
import com.factoryworks.core.machine.AssemblingMachineBlockEntity;
import com.factoryworks.core.machine.AssemblingMachineFluidHandler;
import com.factoryworks.core.machine.AssemblingMachineItemHandler;
import com.factoryworks.core.machine.AssemblingTier;
import com.factoryworks.core.machine.ChemicalPlantBlockEntity;
import com.factoryworks.core.machine.OilRefineryBlockEntity;
import com.factoryworks.core.machine.footprint.FootprintMachine;
import com.factoryworks.core.mining.rig.RigBlockEntity;
import com.factoryworks.core.mining.rig.RigItemHandler;
import com.factoryworks.core.mining.rig.RigPartBlockEntity;
import com.factoryworks.core.mining.rig.RigTier;
import com.factoryworks.core.radar.RadarBlockEntity;
import com.factoryworks.core.oil.OilWellBlockEntity;
import com.factoryworks.core.oil.PumpjackBlockEntity;
import com.factoryworks.core.oil.PumpjackPartBlockEntity;
import com.factoryworks.core.smelting.FurnaceBlockEntity;
import com.factoryworks.core.smelting.FurnaceItemHandler;
import com.factoryworks.core.smelting.FurnaceTier;
import java.util.function.BiFunction;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

/**
 * Block entities: the furnace ladder and the machines.
 *
 * <p>All three furnace tiers share one {@link BlockEntityType}: they differ in numbers their tier
 * enum carries and in nothing else, so there is one behaviour and several blocks pointing at it.
 */
public final class PFBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FactoryWorksCore.NAMESPACE);

    /**
     * All three furnace tiers share one type (#155). They differ in speed and in where their
     * energy comes from, both of which are on {@link FurnaceTier}, so there is one behaviour and
     * three blocks pointing at it.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FurnaceBlockEntity>>
            FURNACE = BLOCK_ENTITIES.register("furnace",
                    () -> new BlockEntityType<>(FurnaceBlockEntity::new, PFBlocks.furnaceBlocks()));

    /**
     * Both rigs' anchors share one type (#192, ADR-0043), the same arrangement as the furnace
     * above. It carries no fields yet -- #192 is an inert footprint -- so both tiers are
     * genuinely identical here; #193/#194 are what will need the tier on this entity.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RigBlockEntity>>
            RIG = BLOCK_ENTITIES.register("rig",
                    () -> new BlockEntityType<>(RigBlockEntity::new, PFBlocks.rigBlocks()));

    /** Both rigs' parts share one type; each part's only field is its anchor's position. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RigPartBlockEntity>>
            RIG_PART = BLOCK_ENTITIES.register("rig_part",
                    () -> new BlockEntityType<>(RigPartBlockEntity::new, PFBlocks.rigPartBlocks()));

    /**
     * The Offshore Pump (#213, ADR-0050). One block, so one type with one block in it -- the
     * ladders above share a type because they are ladders, not because sharing is the idiom.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OffshorePumpBlockEntity>>
            OFFSHORE_PUMP = BLOCK_ENTITIES.register("offshore_pump",
                    () -> new BlockEntityType<>(OffshorePumpBlockEntity::new,
                            java.util.Set.of(PFBlocks.OFFSHORE_PUMP.get())));

    /**
     * Terra's Boiler (#224, ADR-0048). One block, so one type with one block in it -- the same
     * reason the pump's type has one.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BoilerBlockEntity>>
            BOILER = BLOCK_ENTITIES.register("boiler",
                    () -> new BlockEntityType<>(BoilerBlockEntity::new,
                            java.util.Set.of(PFBlocks.BOILER.get())));

    /**
     * The Assembling Machine's anchor (#326, ADR-0071), one type for every tier (ADR-0075). Its own
     * type, not Oritech's {@code ASSEMBLER}: that is the reason the block entity extends Oritech's
     * abstract base rather than its concrete assembler. The parts have no block entity at all.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AssemblingMachineBlockEntity>>
            ASSEMBLING_MACHINE = BLOCK_ENTITIES.register("assembling_machine",
                    () -> new BlockEntityType<>(AssemblingMachineBlockEntity::new,
                            PFBlocks.assemblingMachineBlocks()));

    /** The Chemical Plant's anchor (ADR-0096): its own type so its renderer is the Centrifuge's model. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChemicalPlantBlockEntity>>
            CHEMICAL_PLANT = BLOCK_ENTITIES.register("chemical_plant",
                    () -> new BlockEntityType<>(ChemicalPlantBlockEntity::new,
                            java.util.Set.of(PFBlocks.CHEMICAL_PLANT.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OilRefineryBlockEntity>>
            OIL_REFINERY = BLOCK_ENTITIES.register("oil_refinery",
                    () -> new BlockEntityType<>(OilRefineryBlockEntity::new,
                            java.util.Set.of(PFBlocks.OIL_REFINERY.get())));

    /**
     * The Steam Engine's anchor (ADR-0077): Oritech's engine entity under the pack's own type, which
     * {@link SteamEngineBlockEntity#getType} answers in place of the one Oritech's constructor names.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamEngineBlockEntity>>
            STEAM_ENGINE = BLOCK_ENTITIES.register("steam_engine",
                    () -> new BlockEntityType<>(SteamEngineBlockEntity::new,
                            java.util.Set.of(PFBlocks.STEAM_ENGINE.get())));

    /** The Solar Panel's anchor (#529): Oritech's panel entity under the pack's own type. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>>
            SOLAR_PANEL = BLOCK_ENTITIES.register("solar_panel",
                    () -> new BlockEntityType<>(SolarPanelBlockEntity::new,
                            java.util.Set.of(PFBlocks.SOLAR_PANEL.get())));

    /** The accumulator's anchor (#283): Oritech's storage entity under the pack's own type. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AccumulatorBlockEntity>>
            ACCUMULATOR = BLOCK_ENTITIES.register("accumulator",
                    () -> new BlockEntityType<>(AccumulatorBlockEntity::new,
                            java.util.Set.of(PFBlocks.ACCUMULATOR.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadarBlockEntity>>
            RADAR = BLOCK_ENTITIES.register("radar",
                    () -> new BlockEntityType<>(RadarBlockEntity::new,
                            java.util.Set.of(PFBlocks.RADAR.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PumpjackBlockEntity>>
            PUMPJACK = BLOCK_ENTITIES.register("pumpjack",
                    () -> new BlockEntityType<>(PumpjackBlockEntity::new,
                            java.util.Set.of(PFBlocks.PUMPJACK.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PumpjackPartBlockEntity>>
            PUMPJACK_PART = BLOCK_ENTITIES.register("pumpjack_part",
                    () -> new BlockEntityType<>(PumpjackPartBlockEntity::new,
                            java.util.Set.of(PFBlocks.PUMPJACK_PART.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OilWellBlockEntity>>
            OIL_WELL = BLOCK_ENTITIES.register("oil_well",
                    () -> new BlockEntityType<>(OilWellBlockEntity::new,
                            java.util.Set.of(PFBlocks.OIL_WELL.get())));

    /** Both chests share one type (#540); neither is a vanilla chest type, so NeoForge gives neither a face. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PackChestBlockEntity>>
            CHEST = BLOCK_ENTITIES.register("chest",
                    () -> new BlockEntityType<>(PackChestBlockEntity::new, PFBlocks.chestBlocks()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CargoHoldBlockEntity>>
            CARGO_HOLD = BLOCK_ENTITIES.register("cargo_hold",
                    () -> new BlockEntityType<>(CargoHoldBlockEntity::new,
                            java.util.Set.of(PFBlocks.CARGO_HOLD.get())));

    private PFBlockEntities() {
    }

    static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }

    static void registerCapabilities(RegisterCapabilitiesEvent event) {
        registerFurnaceCapabilities(event);
        registerChestCapabilities(event);
        registerCargoHoldCapabilities(event);
        registerRigCapabilities(event);
        registerPumpCapabilities(event);
        registerBoilerCapabilities(event);
        registerAssemblingMachineCapabilities(event);
        registerChemicalPlantCapabilities(event);
        registerOilRefineryCapabilities(event);
        registerSteamEngineCapabilities(event);
        registerSolarPanelCapabilities(event);
        registerAccumulatorCapabilities(event);
        registerRadarCapabilities(event);
        registerPumpjackCapabilities(event);
    }

    /**
     * The pump's fluid face, on every side. Extract-only -- see
     * {@link OffshorePumpBlockEntity#fluidHandler()} -- so a pipe can take water from it and
     * nothing can push water into it.
     */
    private static void registerPumpCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.Fluid.BLOCK,
                (level, pos, state, blockEntity, side) ->
                        blockEntity instanceof OffshorePumpBlockEntity pump
                                ? pump.fluidHandler() : null,
                PFBlocks.OFFSHORE_PUMP.get());
    }

    /**
     * The Boiler's two faces (#224), both answered on every direction and on the null side.
     *
     * <p>Fluid: water in through tank 0, steam out of tank 1, and neither reachable the other way
     * round -- see {@link BoilerBlockEntity#fluidHandler()}. Item: fuel in and nothing out at all.
     *
     * <p>Unsided, for the reason the furnace's and the rig's are: Factorio decides in-or-out by the
     * inserter's direction rather than by the machine's face, and a nominated-face inventory
     * answers a Create funnel on any other face with silence and no diagnosis.
     */
    private static void registerBoilerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.Fluid.BLOCK,
                (level, pos, state, blockEntity, side) ->
                        blockEntity instanceof BoilerBlockEntity boiler ? boiler.fluidHandler() : null,
                PFBlocks.BOILER.get());
        event.registerBlock(
                Capabilities.Item.BLOCK,
                (level, pos, state, blockEntity, side) ->
                        blockEntity instanceof BoilerBlockEntity boiler
                                ? new BoilerItemHandler(boiler) : null,
                PFBlocks.BOILER.get());
    }

    /**
     * The furnace's two faces (#155), both answered on every direction and on the null side.
     *
     * <p><b>The item handler is unsided on purpose.</b> Direction never decides in or out -- the
     * item does, and {@link FurnaceItemHandler} routes it. That is Factorio's arrangement, and it
     * is what makes a Create funnel work on whichever face a player put it on: funnels reach a
     * neighbour through a {@code BlockCapability<IItemHandler, Direction>}, so a nominated-face
     * inventory answers a funnel on any other face with silence and no diagnosis.
     *
     * <p>The energy face is the Electric tier's alone. {@code energySide()} is null on the two
     * burner tiers, so a supply-area pole does not count a Stone Furnace as a machine it is
     * failing to power.
     */
    private static void registerChestCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, CHEST.get(),
                (chest, side) -> VanillaContainerWrapper.of(chest));
    }

    /**
     * Unsided, on the block rather than the anchor's type so a part answers too, with the anchor's
     * inventory (ADR-0107, #548).
     */
    private static void registerCargoHoldCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.Item.BLOCK, (level, pos, state, blockEntity, side) -> {
            CargoHoldBlockEntity hold = CargoHoldBlock.anchorOf(level, pos);
            return hold == null ? null : new CargoHoldItemHandler(hold);
        }, PFBlocks.CARGO_HOLD.get());
    }

    private static void registerFurnaceCapabilities(RegisterCapabilitiesEvent event) {
        for (FurnaceTier tier : FurnaceTier.values()) {
            Block block = PFBlocks.furnace(tier).get();
            event.registerBlock(
                    Capabilities.Item.BLOCK,
                    (level, pos, state, blockEntity, side) ->
                            blockEntity instanceof FurnaceBlockEntity furnace
                                    ? new FurnaceItemHandler(furnace) : null,
                    block);
            event.registerBlock(
                    Capabilities.Energy.BLOCK,
                    (level, pos, state, blockEntity, side) ->
                            blockEntity instanceof FurnaceBlockEntity furnace
                                    ? furnace.energySide() : null,
                    block);
        }
    }

    /**
     * The rig's item face (#193): fuel in, ore out, on every side and on the null side.
     *
     * <p>Unsided for the reason the furnace's is -- Factorio decides in-or-out by the inserter's
     * direction rather than by the machine's face, and a nominated-face inventory answers a Create
     * funnel on any other face with silence and no diagnosis.
     *
     * <p><b>Registered on the part blocks as well as the anchors.</b> Three quarters of a 2x2 is
     * part, so a hopper under the corner a player happened to build against would otherwise find
     * nothing, and which corner holds the anchor is not visible. The part forwards to its anchor's
     * block entity, the same as its break and its right-click do.
     *
     * <p>The energy face is the electric rig's alone, and forwarded from the parts for the same
     * reason: {@code energySide()} is null on the burner rig.
     */
    private static void registerRigCapabilities(RegisterCapabilitiesEvent event) {
        for (RigTier tier : RigTier.values()) {
            event.registerBlock(
                    Capabilities.Item.BLOCK,
                    (level, pos, state, blockEntity, side) ->
                            blockEntity instanceof RigBlockEntity rig ? new RigItemHandler(rig) : null,
                    PFBlocks.rig(tier).get());
            event.registerBlock(
                    Capabilities.Item.BLOCK,
                    (level, pos, state, blockEntity, side) -> {
                        RigBlockEntity rig = rigOf(level, blockEntity);
                        return rig == null ? null : new RigItemHandler(rig);
                    },
                    PFBlocks.rigPart(tier).get());
            event.registerBlock(
                    Capabilities.Energy.BLOCK,
                    (level, pos, state, blockEntity, side) ->
                            blockEntity instanceof RigBlockEntity rig ? rig.energySide() : null,
                    PFBlocks.rig(tier).get());
            event.registerBlock(
                    Capabilities.Energy.BLOCK,
                    (level, pos, state, blockEntity, side) -> {
                        RigBlockEntity rig = rigOf(level, blockEntity);
                        return rig == null ? null : rig.energySide();
                    },
                    PFBlocks.rigPart(tier).get());
        }
    }

    /** The anchor a rig part forwards to, or {@code null}. */
    @Nullable
    private static RigBlockEntity rigOf(Level level, @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof RigPartBlockEntity part && part.anchorPos() != null
                && level.getBlockEntity(part.anchorPos()) instanceof RigBlockEntity rig) {
            return rig;
        }
        return null;
    }

    /**
     * The Assembling Machine's energy face (#328), which the craft cycle draws from, its item face
     * (#329), and on the tiers with a tank its fluid face (ADR-0075), each on every block of the
     * footprint.
     */
    private static void registerAssemblingMachineCapabilities(RegisterCapabilitiesEvent event) {
        for (AssemblingTier tier : AssemblingTier.values()) {
            FootprintMachine footprint = PFBlocks.assemblingFootprint(tier);
            registerOnFootprint(event, Capabilities.Energy.BLOCK, footprint,
                    (blockEntity, side) -> blockEntity instanceof AssemblingMachineBlockEntity machine
                            ? machine.getEnergyLookup(side) : null);
            registerOnFootprint(event, Capabilities.Item.BLOCK, footprint,
                    (blockEntity, side) -> blockEntity instanceof AssemblingMachineBlockEntity machine
                            ? new AssemblingMachineItemHandler(machine) : null);
            if (tier.spec().hasTanks()) {
                registerOnFootprint(event, Capabilities.Fluid.BLOCK, footprint,
                        (blockEntity, side) -> blockEntity instanceof AssemblingMachineBlockEntity machine
                                ? new AssemblingMachineFluidHandler(machine) : null);
            }
        }
    }

    private static void registerChemicalPlantCapabilities(RegisterCapabilitiesEvent event) {
        FootprintMachine footprint = PFBlocks.CHEMICAL_PLANT_FOOTPRINT;
        registerOnFootprint(event, Capabilities.Energy.BLOCK, footprint,
                (blockEntity, side) -> blockEntity instanceof AssemblingMachineBlockEntity machine
                        ? machine.getEnergyLookup(side) : null);
        registerOnFootprint(event, Capabilities.Item.BLOCK, footprint,
                (blockEntity, side) -> blockEntity instanceof AssemblingMachineBlockEntity machine
                        ? new AssemblingMachineItemHandler(machine) : null);
        registerOnFootprint(event, Capabilities.Fluid.BLOCK, footprint,
                (blockEntity, side) -> blockEntity instanceof AssemblingMachineBlockEntity machine
                        ? new AssemblingMachineFluidHandler(machine) : null);
    }

    /** Energy and fluid on every block; the Refinery has no item slot, so no item face (ADR-0096). */
    private static void registerOilRefineryCapabilities(RegisterCapabilitiesEvent event) {
        FootprintMachine footprint = PFBlocks.OIL_REFINERY_FOOTPRINT;
        registerOnFootprint(event, Capabilities.Energy.BLOCK, footprint,
                (blockEntity, side) -> blockEntity instanceof AssemblingMachineBlockEntity machine
                        ? machine.getEnergyLookup(side) : null);
        registerOnFootprint(event, Capabilities.Fluid.BLOCK, footprint,
                (blockEntity, side) -> blockEntity instanceof AssemblingMachineBlockEntity machine
                        ? new AssemblingMachineFluidHandler(machine) : null);
    }

    /**
     * The Steam Engine's faces (ADR-0077), Oritech's own: the energy a pole pulls and the steam tank
     * a pipe fills, which on a slave is its master's.
     */
    private static void registerSteamEngineCapabilities(RegisterCapabilitiesEvent event) {
        registerOnFootprint(event, Capabilities.Energy.BLOCK, PFBlocks.STEAM_ENGINE_FOOTPRINT,
                (blockEntity, side) -> blockEntity instanceof SteamEngineBlockEntity engine
                        ? engine.getEnergyLookup(side) : null);
        registerOnFootprint(event, Capabilities.Fluid.BLOCK, PFBlocks.STEAM_ENGINE_FOOTPRINT,
                (blockEntity, side) -> blockEntity instanceof SteamEngineBlockEntity engine
                        ? engine.getFluidLookup(side) : null);
    }

    /** The Solar Panel's energy face (#529), on every block, so a pole reaching any of it draws it. */
    private static void registerSolarPanelCapabilities(RegisterCapabilitiesEvent event) {
        registerOnFootprint(event, Capabilities.Energy.BLOCK, PFBlocks.SOLAR_PANEL_FOOTPRINT,
                (blockEntity, side) -> blockEntity instanceof SolarPanelBlockEntity panel
                        ? panel.getEnergyLookup(side) : null);
    }

    /** The accumulator's energy face (#283), on every block, so a pole reaching any of it finds it. */
    private static void registerAccumulatorCapabilities(RegisterCapabilitiesEvent event) {
        registerOnFootprint(event, Capabilities.Energy.BLOCK, PFBlocks.ACCUMULATOR_FOOTPRINT,
                (blockEntity, side) -> blockEntity instanceof AccumulatorBlockEntity accumulator
                        ? accumulator.getEnergyLookup(null) : null);
    }

    /** The Radar's energy face (#368), on every block, so a pole reaching any of it feeds it. */
    private static void registerRadarCapabilities(RegisterCapabilitiesEvent event) {
        registerOnFootprint(event, Capabilities.Energy.BLOCK, PFBlocks.RADAR_FOOTPRINT,
                (blockEntity, side) -> blockEntity instanceof RadarBlockEntity radar ? radar.energySide() : null);
    }

    /** The Pumpjack's energy face, on every block. Its crude leaves through Pipeworks' segment, not a fluid face (ADR-0110). */
    private static void registerPumpjackCapabilities(RegisterCapabilitiesEvent event) {
        registerOnFootprint(event, Capabilities.Energy.BLOCK, PFBlocks.PUMPJACK_FOOTPRINT,
                (blockEntity, side) -> blockEntity instanceof PumpjackBlockEntity pumpjack ? pumpjack.energySide() : null);
    }

    /**
     * A face on the anchor and on every part, the part forwarding to its anchor's block entity: a
     * pole's area or a pipe that reaches only part of the machine must still find it.
     */
    private static <T> void registerOnFootprint(RegisterCapabilitiesEvent event,
            BlockCapability<T, Direction> capability, FootprintMachine machine,
            BiFunction<BlockEntity, Direction, T> face) {
        event.registerBlock(capability,
                (level, pos, state, blockEntity, side) -> face.apply(blockEntity, side),
                machine.anchor().get());
        event.registerBlock(capability,
                (level, pos, state, blockEntity, side) ->
                        face.apply(level.getBlockEntity(machine.anchorOf(pos, state)), side),
                machine.part().get());
    }
}
