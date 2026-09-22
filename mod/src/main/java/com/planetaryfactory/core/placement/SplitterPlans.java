package com.planetaryfactory.core.placement;

import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.jspecify.annotations.Nullable;
import rearth.belts.items.SplitterItem;

/**
 * The SimpleBelts fork's splitter, the one other mod's item with a plan (ADR-0069). The fork's
 * {@code place} executes the fork's own plan, which also names the belts the splitter cuts (#361);
 * this is that plan's blocks and refusal. Loaded only when the fork is.
 */
final class SplitterPlans {

    private SplitterPlans() {
    }

    static boolean isSplitter(Item item) {
        return item instanceof SplitterItem;
    }

    @Nullable
    static PlacementPlan plan(Item item, BlockPlaceContext context) {
        SplitterItem.Plan plan = ((SplitterItem) item).plan(context);
        if (plan == null) {
            return null;
        }
        List<PlacementPlan.Placed> blocks = plan.halves().stream()
                .map(half -> new PlacementPlan.Placed(half.pos(), half.state()))
                .toList();
        if (plan.blocked()) {
            return PlacementPlan.refused(blocks, PlacementPlan.Refusal.FOOTPRINT_BLOCKED);
        }
        return plan.refused()
                ? PlacementPlan.refused(blocks, PlacementPlan.Refusal.BELT_CROSSING)
                : PlacementPlan.accepted(blocks);
    }
}
