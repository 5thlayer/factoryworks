package com.planetaryfactory.core.machine;

import java.util.Locale;

/**
 * The one state the Assembling Machine's screen shows (#332, ADR-0073), each worded as what is
 * wrong and what fixes it.
 *
 * <p>Derived, never stored: the screen asks each time, because a stored conclusion re-checked on
 * the wrong trigger goes stale. Where several hold, the first the craft cycle stops on wins, so
 * fixing the named problem is what lets the machine run --
 * which puts power last, since a stalled machine draws nothing (ADR-0041).
 *
 * <p>Pure: the block entity asks the world and hands the answers in.
 */
public enum AssemblingStatus {
    IDLE,
    PROCESSING,
    LOCKED,
    MISSING_INGREDIENTS,
    MISSING_FLUID,
    OUTPUT_FULL,
    NO_POWER;

    /**
     * @param stall {@link AssemblingStall#of}'s answer, which never spends anything
     * @param powered whether the buffer can pay the next tick's share
     */
    public static AssemblingStatus of(AssemblingStall stall, boolean powered) {
        return switch (stall) {
            case NO_RECIPE -> IDLE;
            case LOCKED -> LOCKED;
            case NO_INGREDIENTS -> MISSING_INGREDIENTS;
            case NO_FLUID -> MISSING_FLUID;
            case OUTPUT_FULL -> OUTPUT_FULL;
            case NO_POWER -> NO_POWER;
            case NONE -> powered ? PROCESSING : NO_POWER;
        };
    }

    /** A synced ordinal back to its status; one out of range, from a mismatched peer, reads as idle. */
    public static AssemblingStatus fromOrdinal(int ordinal) {
        AssemblingStatus[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : IDLE;
    }

    public String langKey() {
        return "gui.planetaryfactory.assembling_machine.status." + name().toLowerCase(Locale.ROOT);
    }

    /** Whether the player has something to fix; an unset machine is idle, not broken. */
    public boolean problem() {
        return this != PROCESSING && this != IDLE;
    }
}
