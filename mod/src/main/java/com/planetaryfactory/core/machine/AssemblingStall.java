package com.planetaryfactory.core.machine;

import java.util.List;

/**
 * Why an Assembling Machine makes no progress this tick (#328), or {@link #NONE}.
 *
 * <p>ADR-0041's rule, which the Boiler's stall follows too: a blocked machine starts nothing, burns
 * nothing and voids nothing. So every reason is answered <b>before</b> any energy is asked for, and
 * none of them clears the Held recipe -- ADR-0071 says a machine that cannot run its recipe holds it
 * and idles.
 *
 * <p>Pure: the block entity asks the world and hands the answers in.
 */
public enum AssemblingStall {
    NONE,
    /** Holds nothing, or an id the recipe manager no longer has. */
    NO_RECIPE,
    /** Holds a recipe research has not unlocked. */
    LOCKED,
    /** The inputs do not cover one craft. */
    NO_INGREDIENTS,
    /** The tank does not hold one craft's fluid. */
    NO_FLUID,
    /** The output slots or any output tank the Held recipe fills cannot take a craft's whole result. */
    OUTPUT_FULL,
    /**
     * Runnable, but the buffer cannot pay this tick's share. Found by the draw itself, inside a
     * transaction that then aborts, so it too spends nothing; {@link #of} never answers it.
     */
    NO_POWER;

    public static AssemblingStall of(boolean resolves, boolean locked, boolean fed, boolean fluidFed,
                                     boolean outputFits) {
        return of(resolves, locked, fed, fluidFed, outputFits, List.of());
    }

    /** {@code tanksFit} holds, per output tank the Held recipe fills, whether it takes a craft's share. */
    public static AssemblingStall of(boolean resolves, boolean locked, boolean fed, boolean fluidFed,
                                     boolean itemsFit, List<Boolean> tanksFit) {
        boolean outputFits = itemsFit && !tanksFit.contains(false);
        if (!resolves) {
            return NO_RECIPE;
        }
        if (locked) {
            return LOCKED;
        }
        if (!fed) {
            return NO_INGREDIENTS;
        }
        if (!fluidFed) {
            return NO_FLUID;
        }
        if (!outputFits) {
            return OUTPUT_FULL;
        }
        return NONE;
    }

    public boolean stalled() {
        return this != NONE;
    }
}
