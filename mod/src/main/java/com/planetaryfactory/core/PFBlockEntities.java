package com.planetaryfactory.core;

import com.planetaryfactory.core.energy.PoleTier;
import com.planetaryfactory.core.energy.SupplyAreaPoleBlockEntity;
import com.planetaryfactory.core.fluid.BoilerBlockEntity;
import com.planetaryfactory.core.fluid.BoilerItemHandler;
import com.planetaryfactory.core.fluid.OffshorePumpBlockEntity;
import com.planetaryfactory.core.fluid.SteamEngineBlockEntity;
import com.planetaryfactory.core.machine.AssemblingMachineBlockEntity;
import com.planetaryfactory.core.machine.AssemblingMachineItemHandler;
import com.planetaryfactory.core.machine.footprint.FootprintMachine;
import com.planetaryfactory.core.mining.rig.RigBlockEntity;
import com.planetaryfactory.core.mining.rig.RigItemHandler;
import com.planetaryfactory.core.mining.rig.RigPartBlockEntity;
import com.planetaryfactory.core.mining.rig.RigTier;
import com.planetaryfactory.core.smelting.FurnaceBlockEntity;
import com.planetaryfactory.core.smelting.FurnaceItemHandler;
import com.planetaryfactory.core.smelting.FurnaceTier;
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

/**
 * Block entities: the supply-area pole and the furnace ladder.
 *
 * <p>All four pole tiers share one {@link BlockEntityType}, and so do all three furnace tiers:
 * each set differs in numbers its tier enum carries and in nothing else, so there is one behaviour
 * and several blocks pointing at it.
 */
public final class PFBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PlanetaryFactoryCore.NAMESPACE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SupplyAreaPoleBlockEntity>>
            SUPPLY_AREA_POLE = BLOCK_ENTITIES.register("supply_area_pole",
                    // No data fixer, which 26.1's constructor no longer has a slot for anyway.
                    // The pack is pre-release and carries no world forward, which is the standing
                    // position rather than an oversight here.
                    () -> new BlockEntityType<>(SupplyAreaPoleBlockEntity::new, PFBlocks.poleBlocks()));

    /**
     * All three furnace tiers share one type (#155). They differ in speed and in where their
     * energy comes from, both of which are on {@link FurnaceTier}, so there is one behaviour and
     * three blocks pointing at it -- the same arrangement as the pole above.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FurnaceBlockEntity>>
            FURNACE = BLOCK_ENTITIES.register("furnace",
                    () -> new BlockEntityType<>(FurnaceBlockEntity::new, PFBlocks.furnaceBlocks()));

    /**
     * Both rigs' anchors share one type (#192, ADR-0043), the same arrangement as the pole and
     * furnace above. It carries no fields yet -- #192 is an inert footprint -- so both tiers are
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
     * The Assembling Machine's anchor (#326, ADR-0071). Its own type, not Oritech's
     * {@code ASSEMBLER}: that is the reason the block entity extends Oritech's abstract base rather
     * than its concrete assembler. The parts have no block entity at all.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AssemblingMachineBlockEntity>>
            ASSEMBLING_MACHINE = BLOCK_ENTITIES.register("assembling_machine",
                    () -> new BlockEntityType<>(AssemblingMachineBlockEntity::new,
                            java.util.Set.of(PFBlocks.ASSEMBLING_MACHINE.get())));

    /**
     * The Steam Engine's anchor (ADR-0077): Oritech's engine entity under the pack's own type, which
     * {@link SteamEngineBlockEntity#getType} answers in place of the one Oritech's constructor names.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamEngineBlockEntity>>
            STEAM_ENGINE = BLOCK_ENTITIES.register("steam_engine",
                    () -> new BlockEntityType<>(SteamEngineBlockEntity::new,
                            java.util.Set.of(PFBlocks.STEAM_ENGINE.get())));

    private PFBlockEntities() {
    }

    static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }

    static void registerCapabilities(RegisterCapabilitiesEvent event) {
        registerFurnaceCapabilities(event);
        registerRigCapabilities(event);
        registerPumpCapabilities(event);
        registerBoilerCapabilities(event);
        registerAssemblingMachineCapabilities(event);
        registerSteamEngineCapabilities(event);
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
     * The Assembling Machine's energy face (#328), which the craft cycle draws from, and its item
     * face (#329), each on every block of the footprint.
     */
    private static void registerAssemblingMachineCapabilities(RegisterCapabilitiesEvent event) {
        registerOnFootprint(event, Capabilities.Energy.BLOCK, PFBlocks.ASSEMBLING_MACHINE_FOOTPRINT,
                (blockEntity, side) -> blockEntity instanceof AssemblingMachineBlockEntity machine
                        ? machine.getEnergyLookup(side) : null);
        registerOnFootprint(event, Capabilities.Item.BLOCK, PFBlocks.ASSEMBLING_MACHINE_FOOTPRINT,
                (blockEntity, side) -> blockEntity instanceof AssemblingMachineBlockEntity machine
                        ? new AssemblingMachineItemHandler(machine) : null);
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
