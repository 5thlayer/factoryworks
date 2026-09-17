package com.planetaryfactory.core.energy;

/**
 * Whether a wire's first end is still held by the Engineer's Pick (ADR-0068).
 *
 * <p>Pure: no Minecraft types.
 */
public final class PendingEnd {

    private PendingEnd() {
    }

    /**
     * @param x                the player's position, measured to the anchor's block centre
     * @param anchorStanding   the anchor pole is still in the world
     * @param pickInMainHand   the Pick that holds the end is still in the main hand
     * @param sameDimension    the player is in the anchor's dimension
     */
    public static boolean stillHeld(PoleLinks.Pole anchor, double x, double y, double z,
                                    boolean anchorStanding, boolean pickInMainHand, boolean sameDimension) {
        double dx = x - (anchor.x() + 0.5);
        double dy = y - (anchor.y() + 0.5);
        double dz = z - (anchor.z() + 0.5);
        double reach = anchor.tier().wireReach();
        boolean inReach = dx * dx + dy * dy + dz * dz <= reach * reach;
        return anchorStanding && pickInMainHand && sameDimension && inReach;
    }
}
