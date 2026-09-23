package com.planetaryfactory.core.placement;

import org.jspecify.annotations.Nullable;

/**
 * How far the held stack's placement is turned from the way the player looks, a quarter turn
 * clockwise per step (ADR-0083). {@link #NONE} is also what a stack with no offset reads as.
 */
public record QuarterTurn(int quarters) {

    public static final QuarterTurn NONE = new QuarterTurn(0);

    public QuarterTurn {
        if (quarters < 0 || quarters > 3) {
            throw new IllegalArgumentException("a quarter turn is 0 to 3, not " + quarters);
        }
    }

    public static QuarterTurn of(int quarters) {
        return new QuarterTurn(Math.floorMod(quarters, 4));
    }

    public static QuarterTurn orNone(@Nullable QuarterTurn turn) {
        return turn == null ? NONE : turn;
    }

    public QuarterTurn rotate() {
        return of(quarters + 1);
    }

    public QuarterTurn reverseRotate() {
        return of(quarters - 1);
    }

    public Heading turn(Heading heading) {
        return Heading.fromDataValue(heading.ordinal() + quarters);
    }

    public float turnYaw(float yaw) {
        return yaw + 90f * quarters;
    }

    /** The horizontal directions in Minecraft's 2D data value order, which runs clockwise. */
    public enum Heading {
        SOUTH, WEST, NORTH, EAST;

        public static Heading fromDataValue(int value) {
            return values()[Math.floorMod(value, 4)];
        }
    }
}
