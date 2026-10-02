package com.factoryworks.core.oil;

import io.github._5thlayer.pipeworks.api.FluidPort;
import io.github._5thlayer.pipeworks.api.FluidPorts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** What the Pumpjack's anchor and its parts share: every face open, joined to the segment on load (ADR-0110). */
abstract class PumpjackPortBlockEntity extends BlockEntity implements FluidPort {

    PumpjackPortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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

    static void leave(ServerLevel level, BlockPos pos) {
        FluidPorts.leave(level, pos);
    }
}
