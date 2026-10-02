package com.factoryworks.core.oil;

import com.factoryworks.core.PFBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** A part of the Pumpjack's footprint in its segment, adding no capacity of its own. */
public class PumpjackPartBlockEntity extends PumpjackPortBlockEntity {

    public PumpjackPartBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.PUMPJACK_PART.get(), pos, state);
    }
}
