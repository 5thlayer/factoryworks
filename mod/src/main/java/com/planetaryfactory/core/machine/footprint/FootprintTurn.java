package com.planetaryfactory.core.machine.footprint;

import com.planetaryfactory.core.placement.PlacedTurn;
import com.planetaryfactory.core.placement.TurnsInPlace;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block of a footprint machine, refused Rotate: vanilla's turn would turn this block alone and
 * split the machine, which turns whole with #406.
 */
public interface FootprintTurn extends TurnsInPlace {

    String REFUSAL = "message.planetaryfactory.rotate.footprint";

    @Override
    default PlacedTurn.Verdict<BlockState> turnInPlace(BlockState state, Level level, BlockPos pos, boolean reverse) {
        return PlacedTurn.refused(REFUSAL);
    }
}
