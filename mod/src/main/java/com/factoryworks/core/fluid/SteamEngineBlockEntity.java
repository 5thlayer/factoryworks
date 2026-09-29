package com.factoryworks.core.fluid;

import java.util.List;
import java.util.Optional;

import com.factoryworks.core.PFBlockEntities;
import com.factoryworks.core.energy.ElectricNetworks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
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

    @Override
    public EnergyHandler getEnergyLookup(Direction direction) {
        return source().energyStorage;
    }

    /** The engine whose tank and buffer this one's figures are: its master's when it is a slave. */
    public SteamEngineEntity source() {
        return inSlaveMode() ? master : this;
    }

    public Optional<SteamEngineStatus> status() {
        SteamEngineEntity source = source();
        return SteamEngineStatus.of(steam(source) > 0,
                ElectricNetworks.of(level).drawsFrom(source.getBlockPos()));
    }

    private static long steam(SteamEngineEntity engine) {
        return engine.boilerStorage.getInputContainer().getAmountAsLong(0);
    }
}
