package com.factoryworks.core.oil;

import com.factoryworks.core.PFBlockEntities;
import io.github._5thlayer.pipeworks.api.FluidPort;
import io.github._5thlayer.pipeworks.api.FluidPorts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A part of the Pumpjack's footprint in its segment: every face open, adding no capacity of its own. */
public class PumpjackPartBlockEntity extends BlockEntity implements FluidPort {

    public PumpjackPartBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.PUMPJACK_PART.get(), pos, state);
    }

    @Override
    public boolean connectsOn(Direction face) {
        return true;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        FluidPorts.join(this);
    }
}
