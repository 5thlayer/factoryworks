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
 * <p>It makes the peak times {@link SolarDayCurve}'s multiplier into a one-tick buffer, and only with
 * the sky open above the footprint. A pole pulls it through {@code factoryworks:generators}
 * (ADR-0062); Oritech's push into neighbours is switched off.
 */
public class SolarPanelBlockEntity extends BigSolarPanelEntity {

    /** The footprint is two blocks tall, so the sky is asked one above its top. */
    private static final int SKY_PROBE_HEIGHT = 2;

    private double carry;
    private long tickFe;
    private long worked = Long.MIN_VALUE;

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

    /** Whole FE for this tick; the fraction owed is carried to the next (ADR-0062). */
    @Override
    public int getProductionRate() {
        return (int) tickFe;
    }

    /** Oritech asks this once a tick before the rate, so it is where the tick's energy is worked out. */
    @Override
    public boolean isProducing() {
        if (level != null && worked == level.getGameTime()) {
            return tickFe > 0;
        }
        worked = level == null ? Long.MIN_VALUE : level.getGameTime();
        double multiplier = level == null || !level.canSeeSky(worldPosition.above(SKY_PROBE_HEIGHT))
                ? 0.0
                : SolarDayCurve.multiplier(daytime());
        SolarOutput.Tick tick = SolarOutput.tick(SolarPanelSpec.get().peakFePerTick(), multiplier, carry);
        tickFe = tick.fe();
        carry = tick.carry();
        return tickFe > 0;
    }

    /** Oritech folds the panel outside 0-12,500, so this reports day exactly while the curve is above zero. */
    @Override
    public long getAdjustedTimeOfDay() {
        return level == null ? 18000L : SolarDayCurve.animationTimeOfDay(daytime());
    }

    /** What the panel made this tick, in whole FE; worked out on ask so a HUD never reads a stale tick. */
    public long currentOutputFe() {
        isProducing();
        return tickFe;
    }

    private double daytime() {
        return SolarDayCurve.fromClockFraction(DayFraction.of(level));
    }

    @Override
    protected Set<Tuple<BlockPos, Direction>> getOutputTargets(BlockPos pos, Level level) {
        return Set.of();
    }

    /** Oritech's animation reads its core-assembly flag, which a footprint never sets; false holds it packaged. */
    @Override
    public boolean isActive(BlockState state) {
        return true;
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
