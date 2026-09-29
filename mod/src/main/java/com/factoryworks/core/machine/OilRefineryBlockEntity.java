package com.factoryworks.core.machine;

import com.factoryworks.core.PFBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** The Oil Refinery (ADR-0096): the chassis under a type of its own, so its renderer can draw the Refinery. */
public class OilRefineryBlockEntity extends AssemblingMachineBlockEntity {

    public OilRefineryBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.OIL_REFINERY.get(), pos, state);
    }
}
