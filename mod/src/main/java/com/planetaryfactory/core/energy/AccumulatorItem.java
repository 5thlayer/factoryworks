package com.planetaryfactory.core.energy;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.machine.footprint.FootprintMachine;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.PlansPlacement;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.jspecify.annotations.Nullable;

/**
 * Places the whole accumulator (ADR-0069). A plain {@link BlockItem}, not a {@code FootprintItem}:
 * Oritech's Large Energy Storage has a block model and no GeckoLib one to draw in the hand.
 */
public class AccumulatorItem extends BlockItem implements PlansPlacement {

    public AccumulatorItem(Properties properties) {
        super(PFBlocks.ACCUMULATOR.get(), properties);
    }

    @Override
    public @Nullable PlacementPlan plan(BlockPlaceContext context) {
        return PFBlocks.ACCUMULATOR_FOOTPRINT.plan(context);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        return FootprintMachine.place(this, context) == null ? InteractionResult.FAIL : InteractionResult.SUCCESS;
    }
}
