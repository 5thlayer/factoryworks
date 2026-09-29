package com.factoryworks.core.energy;

import java.util.Locale;
import java.util.Optional;

/**
 * The state the accumulator's HUD line names (#468). Full, empty and idle are left to Jade's energy
 * row, so a tick with no flow under a pole names nothing.
 */
public enum AccumulatorStatus {
    NOT_IN_POLE_AREA,
    CHARGING,
    DISCHARGING;

    /** @param flow FE taken last tick, negative when given */
    public static Optional<AccumulatorStatus> of(boolean inPoleArea, long flow) {
        if (!inPoleArea) {
            return Optional.of(NOT_IN_POLE_AREA);
        }
        if (flow > 0L) {
            return Optional.of(CHARGING);
        }
        return flow < 0L ? Optional.of(DISCHARGING) : Optional.empty();
    }

    /** A synced ordinal back to its status; -1, or one out of range from a mismatched peer, names nothing. */
    public static Optional<AccumulatorStatus> fromOrdinal(int ordinal) {
        AccumulatorStatus[] all = values();
        return ordinal >= 0 && ordinal < all.length ? Optional.of(all[ordinal]) : Optional.empty();
    }

    public String langKey() {
        return "gui.factoryworks.accumulator.status." + name().toLowerCase(Locale.ROOT);
    }

    public boolean problem() {
        return this == NOT_IN_POLE_AREA;
    }
}
