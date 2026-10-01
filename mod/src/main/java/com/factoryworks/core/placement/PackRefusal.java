package com.factoryworks.core.placement;

import io.github._5thlayer.groundworks.Refusal;

/** Why one of the pack's own items would not place (ADR-0069). */
public enum PackRefusal implements Refusal {
    /** A multiblock footprint at least one of whose positions is not clear. */
    FOOTPRINT_BLOCKED,
    /** An Offshore Pump with no adjacent source to pump (#213, ADR-0050). */
    NO_FLUID_SOURCE,
    /** A Pumpjack anywhere but over an oil well (ADR-0081). */
    NOT_ON_WELL,
    /** A Fast Replace whose player has no room for what it hands back (ADR-0082). */
    NO_ROOM_TO_RETURN,
}
