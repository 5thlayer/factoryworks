package com.planetaryfactory.core.machine;

/**
 * Whether an Assembling Machine takes a recipe it was asked to hold (#330, ADR-0073).
 *
 * <p>Every setter asks this -- EMI's Fill Recipe and the screen's own press -- so a refusal is one
 * rule and one message rather than a button that silently does nothing. A recipe that is not an
 * assembling recipe the server has loaded is refused before its lock is asked, so an unknown id is
 * never reported as merely unresearched.
 *
 * <p>Pure: the menu asks the world and hands the answers in.
 */
public enum HoldVerdict {
    HELD(null),
    /** The id names no assembling recipe the server has loaded. */
    NOT_ASSEMBLING("planetaryfactory_core.assembling_machine.refused.not_assembling"),
    /** An assembling recipe of a category this tier does not craft: tier 1 has no fluid box. */
    NOT_THIS_MACHINE("planetaryfactory_core.assembling_machine.refused.not_this_machine"),
    /** Research has not unlocked it. */
    LOCKED("planetaryfactory_core.assembling_machine.refused.locked");

    private final String messageKey;

    HoldVerdict(String messageKey) {
        this.messageKey = messageKey;
    }

    public static HoldVerdict of(boolean resolves, boolean crafts, boolean locked) {
        if (!resolves) {
            return NOT_ASSEMBLING;
        }
        if (!crafts) {
            return NOT_THIS_MACHINE;
        }
        return locked ? LOCKED : HELD;
    }

    public boolean held() {
        return this == HELD;
    }

    /** The lang key the player is told, or null when the recipe was held. */
    public String messageKey() {
        return messageKey;
    }
}
