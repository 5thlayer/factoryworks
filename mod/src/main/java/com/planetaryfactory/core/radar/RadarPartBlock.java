package com.planetaryfactory.core.radar;

import java.util.function.Supplier;

import com.planetaryfactory.core.machine.footprint.FootprintMachine;
import com.planetaryfactory.core.machine.footprint.FootprintPartBlock;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/** A Radar part draws its own block, so the placeholder shows the whole structure until #367's art. */
public class RadarPartBlock extends FootprintPartBlock {

    public RadarPartBlock(Properties properties, Supplier<FootprintMachine> machine) {
        super(properties, machine);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
