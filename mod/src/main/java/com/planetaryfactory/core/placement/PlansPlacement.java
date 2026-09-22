package com.planetaryfactory.core.placement;

import net.minecraft.world.item.context.BlockPlaceContext;
import org.jspecify.annotations.Nullable;

/**
 * An item that answers for its own placement (#297, ADR-0069).
 *
 * <p>Implemented only by an item whose placement is not vanilla's. Everything else -- the Boiler,
 * a sapling -- is served by {@link Placements#vanillaPlan}, which defers to
 * {@code BlockPlaceContext} and so gets facing, replaceable blocks and "can this state survive
 * here" without the pack restating any of it.
 *
 * <p>The contract is that {@link #place} <em>executes</em> what {@link #plan} describes. A plan the
 * click then disagrees with is exactly the drift ADR-0069 forbids, and it is what
 * {@code PlacementPlanTests} asserts in a built world.
 */
public interface PlansPlacement {

    /**
     * What this item would do here, or {@code null} where there is nothing to draw at all.
     *
     * <p>{@code null} is the common case for most of vanilla's refusals: the ray hits a block and
     * placement simply picks another spot rather than refusing one. A {@link PlacementPlan} with a
     * {@link PlacementPlan.Refusal} is the other thing -- a spot the player is aiming at and would
     * be told no at, which is what draws red.
     *
     * <p>Called on both sides. The client builds its own plan for the preview; the server builds
     * one on the click and is the authority.
     */
    @Nullable
    PlacementPlan plan(BlockPlaceContext context);
}
