package com.factoryworks.core.energy;

import java.util.List;
import java.util.Set;

import com.factoryworks.core.PFBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import rearth.oritech.block.entity.generators.BigSolarPanelEntity;

/**
 * The Solar Panel's anchor (#529): Oritech's Big Solar Panel entity under the pack's own type, placed
 * as a footprint (ADR-0077), so it has no cores and its core quality stays Oritech's default of 1.
 *
 * <p>It makes the spec's peak every tick into a one-tick buffer, and a pole pulls it through
 * {@code factoryworks:generators} (ADR-0062); Oritech's push into neighbours is switched off.
 */
public class SolarPanelBlockEntity extends BigSolarPanelEntity {

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
        sizeBuffer();
    }

    @Override
    public BlockEntityType<?> getType() {
        return PFBlockEntities.SOLAR_PANEL.get();
    }

    @Override
    public Holder<BlockEntityType<?>> typeHolder() {
        return getType().builtInRegistryHolder();
    }

    @Override
    public int getProductionRate() {
        return (int) SolarPanelSpec.get().peakFePerTick();
    }

    @Override
    public boolean isProducing() {
        return true;
    }

    @Override
    protected Set<Tuple<BlockPos, Direction>> getOutputTargets(BlockPos pos, Level level) {
        return Set.of();
    }

    @Override
    public List<Vec3i> getCorePositions() {
        return List.of();
    }

    /** Oritech saves the capacity with the energy, so a load would restore its 200,000 FE. */
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        sizeBuffer();
    }

    private void sizeBuffer() {
        long buffer = SolarPanelSpec.get().bufferFe();
        energyStorage.setCapacity(buffer);
        energyStorage.energy = Math.min(energyStorage.energy, buffer);
    }
}
