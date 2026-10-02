package com.factoryworks.core.fluid;

import com.factoryworks.core.PFBlockEntities;
import com.factoryworks.core.machine.footprint.FootprintPartBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** A Boiler part that is a port: the front row's water ends and the back middle's steam (ADR-0114). */
public class BoilerPartBlockEntity extends BoilerPortBlockEntity {

    public BoilerPartBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.BOILER_PART.get(), pos, state);
    }

    @Override
    BoilerFootprint.Port port() {
        return portOf(getBlockState());
    }

    static BoilerFootprint.Port portOf(BlockState state) {
        return BoilerFootprint.portAt(BoilerFootprint.FOOTPRINT.offsetOfPart(state.getValue(FootprintPartBlock.PART)));
    }
}
