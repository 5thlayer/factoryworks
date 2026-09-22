package com.planetaryfactory.core.placement;

import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.jspecify.annotations.Nullable;
import rearth.belts.items.SplitterItem;

/**
 * The SimpleBelts fork's splitter, the one other mod's item with a plan (ADR-0069). The plan is
 * built from the two methods the fork's {@code place} calls, so the click executes it. Loaded only
 * when the fork is.
 */
final class SplitterPlans {

    private SplitterPlans() {
    }

    static boolean isSplitter(Item item) {
        return item instanceof SplitterItem;
    }

    @Nullable
    static PlacementPlan plan(Item item, BlockPlaceContext context) {
        if (!context.canPlace()) {
            return null;
        }
        SplitterItem splitter = (SplitterItem) item;
        List<SplitterItem.Half> halves = splitter.halves(context);
        List<PlacementPlan.Placed> blocks = halves.stream()
                .map(half -> new PlacementPlan.Placed(half.pos(), half.state()))
                .toList();
        return splitter.fits(context, halves)
                ? PlacementPlan.accepted(blocks)
                : PlacementPlan.refused(blocks, PlacementPlan.Refusal.FOOTPRINT_BLOCKED);
    }
}
