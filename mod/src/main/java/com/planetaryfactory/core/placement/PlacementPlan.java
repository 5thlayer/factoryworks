package com.planetaryfactory.core.placement;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * What a held item would do at an aimed spot (#297, ADR-0069): the positions it would fill, the
 * blockstate at each, and a refusal or none.
 *
 * <p>This is the whole of the pack's placement vocabulary. The preview draws a plan and the click
 * executes one, so there is exactly one rule and the preview cannot drift from what placing
 * actually does -- which is the defect ADR-0069 exists to prevent, because a player builds against
 * a preview and a preview that lies is worse than none.
 *
 * <p><b>A refused plan still carries its blocks.</b> That is deliberate: a refusal has to be drawn
 * somewhere, and the only honest place is where the blocks would have gone. A plan with no blocks
 * at all is not a refusal, it is the absence of a plan, and callers express that with {@code null}
 * rather than an empty one -- see {@link Placements#planFor}.
 *
 * <p><b>A multiblock is one plan and refuses whole</b> (ADR-0069). The rig's footprint is one plan
 * with one refusal, because the rig places every part in one gesture; drawing one part red and the
 * rest translucent would promise a partial placement the game never performs.
 */
public record PlacementPlan(List<Placed> blocks, @Nullable Refusal refusal) {

    /** One block the plan would put down. */
    public record Placed(BlockPos pos, BlockState state) {
    }

    /**
     * Why placing would fail.
     *
     * <p>An enum rather than a message, because a refusal is asked about by checks and by the
     * renderer, and neither wants to match on prose. What the player is told is the item's own
     * business -- the pump says so above the hotbar and nothing else says anything, which is
     * unchanged by this ticket.
     */
    public enum Refusal {
        /** Vanilla would refuse: no room, the state cannot survive, the context is not placeable. */
        VANILLA,
        /** A pole aimed at a pole of another tier, which is not fast replace (ADR-0069). */
        WRONG_TIER,
        /** A pole column already at {@code PoleColumn.MAX_SEGMENTS}. */
        COLUMN_FULL,
        /** A pole column whose next segment's position is occupied. */
        BLOCKED_TOP,
        /** A multiblock footprint at least one of whose positions is not clear. */
        FOOTPRINT_BLOCKED,
        /** An Offshore Pump with no adjacent source to pump (#213, ADR-0050). */
        NO_FLUID_SOURCE,
        /** A splitter over a belt it would not cut: against its facing, at an angle, through a side or on a curve (#361). */
        BELT_CROSSING,
        /** A Pumpjack anywhere but over an oil well (ADR-0081). */
        NOT_ON_WELL,
    }

    public PlacementPlan {
        blocks = List.copyOf(blocks);
    }

    /** A plan that would go through. */
    public static PlacementPlan accepted(List<Placed> blocks) {
        return new PlacementPlan(blocks, null);
    }

    /** A plan that would not, drawn where its blocks would have gone. */
    public static PlacementPlan refused(List<Placed> blocks, Refusal refusal) {
        return new PlacementPlan(blocks, refusal);
    }

    /** A one-block plan that would go through. */
    public static PlacementPlan accepted(BlockPos pos, BlockState state) {
        return accepted(List.of(new Placed(pos, state)));
    }

    /** A one-block plan that would not. */
    public static PlacementPlan refused(BlockPos pos, BlockState state, Refusal refusal) {
        return refused(List.of(new Placed(pos, state)), refusal);
    }

    public boolean isRefused() {
        return refusal != null;
    }
}
