package com.planetaryfactory.core.fluid;

import java.util.List;

import com.planetaryfactory.core.PFBlockEntities;
import com.planetaryfactory.core.energy.ElectricNetworks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import rearth.oritech.block.entity.generators.SteamEngineEntity;

/**
 * The Steam Engine's anchor (ADR-0077): Oritech's own engine entity, so {@code SteamEngineEntityMixin}
 * reaches it through inheritance and ADR-0062's arithmetic, chaining and curve are unchanged.
 *
 * <p><b>Its type is the pack's.</b> Oritech's one constructor hard-codes {@code STEAM_ENGINE}, whose
 * valid blocks are Oritech's, so the constructor's own block-state check would throw and a save
 * would reload the entity as Oritech's class. The type field is read only through {@link #getType}
 * and {@link #typeHolder}, so answering both is enough.
 *
 * <p><b>A footprint, not Oritech's multiblock</b>, for {@code AssemblingMachineBlockEntity}'s
 * reasons: no cores, assembled by construction, never rescanned.
 */
public class SteamEngineBlockEntity extends SteamEngineEntity {

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public BlockEntityType<?> getType() {
        return PFBlockEntities.STEAM_ENGINE.get();
    }

    @Override
    public Holder<BlockEntityType<?>> typeHolder() {
        return getType().builtInRegistryHolder();
    }

    @Override
    public boolean isAssembled(BlockState state) {
        return true;
    }

    @Override
    public boolean initMultiblock(BlockState state) {
        return true;
    }

    @Override
    public void rescanMultiblock() {
    }

    @Override
    public List<Vec3i> getCorePositions() {
        return List.of();
    }

    @Override
    public ResourceHandler<FluidResource> getFluidLookup(Direction direction) {
        return new SteamEngineFluidHandler(source());
    }

    /** The engine whose tank and buffer this one's figures are: its master's when it is a slave. */
    public SteamEngineEntity source() {
        return inSlaveMode() ? master : this;
    }

    public SteamEngineStatus status() {
        SteamEngineEntity source = source();
        return SteamEngineStatus.of(steam(source) > 0,
                source.energyStorage.getAmountAsLong() >= source.energyStorage.getCapacityAsLong(),
                ElectricNetworks.of(level).drawsFrom(source.getBlockPos()));
    }

    public long steam() {
        return steam(source());
    }

    public long steamCapacity() {
        ResourceHandler<FluidResource> tank = source().boilerStorage.getInputContainer();
        FluidResource held = tank.getResource(0);
        return tank.getCapacityAsLong(0, held.isEmpty() ? FluidResource.of(PFFluids.STEAM_SOURCE.get()) : held);
    }

    /** Steam burnt over the last tick, as a rate: what the row actually drew, a full buffer's cut included. */
    public double consumptionPerSecond() {
        SteamEngineEntity source = source();
        return workedLastTick(source) ? source.clientStats.steamConsumed() * 20.0 : 0.0;
    }

    /** Factorio's rated consumption: the row at the curve's peak. */
    public double maxConsumptionPerSecond() {
        return readout().planetaryfactory$readSpec().steamPerSecond(SteamEngineSpec.PEAK_SPEED, rowLength());
    }

    /** FE made on the last tick: what the network drew, since the burn stops at the buffer's room. */
    public long outputPerTick() {
        SteamEngineEntity source = source();
        return workedLastTick(source) ? source.clientStats.energyProduced() : 0L;
    }

    /** Factorio's rated output: the row at the curve's peak. */
    public long maxOutputPerTick() {
        return Math.round(readout().planetaryfactory$readSpec().powerPerTick(SteamEngineSpec.PEAK_SPEED, rowLength()));
    }

    /** What the steam in the tank could make this tick if the network drew all of it. */
    public long availablePerTick() {
        SteamEngineReadout readout = readout();
        long available = Math.round(readout.planetaryfactory$readSpec()
                .powerPerTick(readout.planetaryfactory$readSpeed(), rowLength()));
        return Math.min(available, maxOutputPerTick());
    }

    private int rowLength() {
        return readout().planetaryfactory$readRowLength();
    }

    /** The mixin is what sizes and burns the engine, so an engine without it is not one this pack can run. */
    private SteamEngineReadout readout() {
        return (SteamEngineReadout) source();
    }

    /**
     * The mixin stamps {@code lastWorkedAt} and fills {@code clientStats} only on a tick that burnt,
     * so a stamp older than a tick means nothing was made.
     */
    private boolean workedLastTick(SteamEngineEntity source) {
        return source.clientStats != null && level.getGameTime() - source.lastWorkedAt <= 1;
    }

    private static long steam(SteamEngineEntity engine) {
        return engine.boilerStorage.getInputContainer().getAmountAsLong(0);
    }
}
