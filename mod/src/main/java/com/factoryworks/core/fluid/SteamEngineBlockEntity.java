package com.factoryworks.core.fluid;

import java.util.List;
import java.util.Optional;

import com.factoryworks.core.PFBlockEntities;
import com.factoryworks.core.machine.footprint.FootprintPartBlock;
import io.github._5thlayer.pipeworks.api.FluidPort;
import io.github._5thlayer.pipeworks.api.FluidPorts;
import io.github._5thlayer.wireworks.ElectricNetworks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.util.Geometry;
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
public class SteamEngineBlockEntity extends SteamEngineEntity implements FluidPort {

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
    public boolean connectsOn(Direction face) {
        return true;
    }

    @Override
    public long capacity() {
        return SteamChainCorpus.get().steamEngineFluidBoxVolume();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        FluidPorts.join(this);
    }

    static void leave(ServerLevel level, BlockPos pos) {
        FluidPorts.leave(level, pos);
    }

    @Override
    public void serverTick(ServerLevel world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        drawSteam();
        super.serverTick(world, pos, state, blockEntity);
    }

    /**
     * Tops the row's tank up to its peak fill from this port's segment. An engine with an empty tank
     * and another engine behind it waits to be that engine's slave, or every engine of a row would
     * hold steam and none could chain (ADR-0062).
     */
    private void drawSteam() {
        SteamEngineEntity source = source();
        if (source == this && source.boilerStorage.getInStack().isEmpty() && !isRowHead()) {
            return;
        }
        ResourceHandler<FluidResource> segment = FluidPorts.segment(level, worldPosition);
        if (segment == null) {
            return;
        }
        ResourceHandler<FluidResource> tank = source.boilerStorage.getInputContainer();
        FluidResource steam = FluidResource.of(PFFluids.STEAM_SOURCE.get());
        long missing = SteamEngineSpec.peakFill(tank.getCapacityAsLong(0, steam)) - tank.getAmountAsLong(0);
        if (missing <= 0L) {
            return;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int drawn = segment.extract(steam, (int) missing, transaction);
            if (drawn > 0 && tank.insert(steam, drawn, transaction) == drawn) {
                transaction.commit();
            }
        }
    }

    private boolean isRowHead() {
        BlockPos behind = new BlockPos(Geometry.offsetToWorldPosition(getFacing(), new Vec3i(-1, 0, 0), worldPosition));
        BlockState state = level.getBlockState(behind);
        if (state.getBlock() instanceof FootprintPartBlock part) {
            behind = part.machine().anchorOf(behind, state);
        }
        return !(level.getBlockEntity(behind) instanceof SteamEngineEntity);
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
