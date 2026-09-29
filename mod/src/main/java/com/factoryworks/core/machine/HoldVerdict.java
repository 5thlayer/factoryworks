package com.factoryworks.core.machine;

/**
 * Whether a crafting machine takes a recipe it was asked to hold (#330, #489, ADR-0073, ADR-0096).
 *
 * <p>Every setter asks this -- EMI's Fill Recipe and the screen's own press -- so a refusal is one
 * rule and one message rather than a button that silently does nothing. A recipe the machine could
 * never run is refused before its lock is asked, so it is never reported as merely unresearched.
 *
 * <p>Pure: the menu asks the world and hands the answers in.
 */
public enum HoldVerdict {
    HELD(null),
    /** The id names no recipe of the chassis's types the server has loaded. */
    NOT_ASSEMBLING("factoryworks_core.assembling_machine.refused.not_assembling"),
    /** A recipe of another machine's type: a chemistry recipe on an Assembling Machine. */
    NOT_THIS_TYPE("factoryworks_core.assembling_machine.refused.not_this_type"),
    /** A recipe of this type needing a slot or tank this machine lacks: tier 1 has no fluid box. */
    NOT_THIS_MACHINE("factoryworks_core.assembling_machine.refused.not_this_machine"),
    /** Research has not unlocked it. */
    LOCKED("factoryworks_core.assembling_machine.refused.locked");

    private final String messageKey;

    HoldVerdict(String messageKey) {
        this.messageKey = messageKey;
    }

    public static HoldVerdict of(boolean resolves, boolean ofThisType, boolean fits, boolean locked) {
        if (!resolves) {
            return NOT_ASSEMBLING;
        }
        if (!ofThisType) {
            return NOT_THIS_TYPE;
        }
        if (!fits) {
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
