package com.planetaryfactory.core.assembler;

/**
 * The clock-hand shade over the craft under way, as Factorio's hand-craft queue draws it: the part of
 * the icon not yet done is shaded, and the shade clears clockwise from twelve o'clock.
 *
 * <p>A cell rule rather than a mesh because the GUI draws only rectangles; the screen fills the shaded
 * cells. Minecraft-free so the shape is a unit test.
 */
public final class RadialWipe {

    private RadialWipe() {
    }

    /**
     * Whether cell {@code (x, y)} of a {@code size} by {@code size} square is still shaded at
     * {@code progress}, judged at the cell's centre.
     */
    public static boolean shaded(int x, int y, int size, float progress) {
        double half = size / 2.0;
        double dx = x + 0.5 - half;
        double dy = y + 0.5 - half;
        // Clockwise from twelve: atan2 of (right, up), shifted into [0, 1).
        double turn = Math.atan2(dx, -dy) / (2 * Math.PI);
        if (turn < 0) turn += 1;
        return turn >= Math.clamp(progress, 0.0f, 1.0f);
    }
}
