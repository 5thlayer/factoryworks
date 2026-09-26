package com.planetaryfactory.core.energy;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.machine.footprint.FootprintAnchorBlock;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The accumulator's anchor block (#283), holding {@link AccumulatorBlockEntity}. */
public class AccumulatorBlock extends FootprintAnchorBlock {

    public AccumulatorBlock(Properties properties) {
        super(properties, () -> PFBlocks.ACCUMULATOR_FOOTPRINT);
    }

    /** Oritech's {@code MachineBlock.newBlockEntity} constructs this class by reflection. */
    @Override
    public Class<? extends BlockEntity> getBlockEntityType() {
        return AccumulatorBlockEntity.class;
    }

    /** Oritech draws the Large Energy Storage as a block model, not through GeckoLib. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
