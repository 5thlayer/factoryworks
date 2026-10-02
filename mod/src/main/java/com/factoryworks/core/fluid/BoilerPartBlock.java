package com.factoryworks.core.fluid;

import java.util.function.Supplier;

import com.factoryworks.core.machine.footprint.FootprintMachine;
import com.factoryworks.core.machine.footprint.FootprintPartBlock;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/** A Boiler part drawn as an iron block until the 3x2 model replaces it (#595). */
public class BoilerPartBlock extends FootprintPartBlock {

    public BoilerPartBlock(Properties properties, Supplier<FootprintMachine> machine) {
        super(properties, machine);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
