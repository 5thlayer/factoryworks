package com.planetaryfactory.core.placement;

import io.github._5thlayer.placementpreview.Refusal;

/** Why one of the pack's own items would not place (ADR-0069). */
public enum PackRefusal implements Refusal {
    /** A pole aimed at a pole column outside its Replace Group (ADR-0082). */
    OTHER_REPLACE_GROUP,
    /** A pole column already at {@code PoleColumn.MAX_SEGMENTS}. */
    COLUMN_FULL,
    /** A pole column whose next segment's position is occupied. */
    BLOCKED_TOP,
    /** A multiblock footprint at least one of whose positions is not clear. */
    FOOTPRINT_BLOCKED,
    /** An Offshore Pump with no adjacent source to pump (#213, ADR-0050). */
    NO_FLUID_SOURCE,
    /** A Pumpjack anywhere but over an oil well (ADR-0081). */
    NOT_ON_WELL,
    /** A Fast Replace whose player has no room for what it hands back (ADR-0082). */
    NO_ROOM_TO_RETURN,
}
