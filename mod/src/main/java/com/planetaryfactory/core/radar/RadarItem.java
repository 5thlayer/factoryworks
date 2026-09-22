package com.planetaryfactory.core.radar;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.machine.footprint.FootprintMachine;
import com.planetaryfactory.core.placement.PlacementPlan;
import com.planetaryfactory.core.placement.PlansPlacement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.jspecify.annotations.Nullable;

/** Places the whole Radar (ADR-0069) and makes the placing player its owner, whose team it charts for. */
public class RadarItem extends BlockItem implements PlansPlacement {

    public RadarItem(Properties properties) {
        super(PFBlocks.RADAR.get(), properties);
    }

    @Override
    public @Nullable PlacementPlan plan(BlockPlaceContext context) {
        return PFBlocks.RADAR_FOOTPRINT.plan(context);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        BlockPos anchor = FootprintMachine.place(this, context);
        if (anchor == null) {
            return InteractionResult.FAIL;
        }
        if (context.getPlayer() != null
                && context.getLevel().getBlockEntity(anchor) instanceof RadarBlockEntity radar) {
            radar.setOwner(context.getPlayer().getUUID());
        }
        return InteractionResult.SUCCESS;
    }
}
