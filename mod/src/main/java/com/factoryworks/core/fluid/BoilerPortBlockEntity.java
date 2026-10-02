package com.factoryworks.core.fluid;

import io.github._5thlayer.pipeworks.api.FluidPort;
import io.github._5thlayer.pipeworks.api.FluidPorts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** What the Boiler's anchor and its port parts share: one block in one segment, joined on load (ADR-0114). */
abstract class BoilerPortBlockEntity extends BlockEntity implements FluidPort {

    BoilerPortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    abstract BoilerFootprint.Port port();

    @Override
    public boolean connectsOn(Direction face) {
        return BoilerPorts.opens(port(), getBlockState().getValue(HorizontalDirectionalBlock.FACING), face);
    }

    @Override
    public long capacity() {
        return BoilerPorts.capacity(port());
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
