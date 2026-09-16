package com.planetaryfactory.core.assembler.client;

import com.planetaryfactory.core.assembler.RadialWipe;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Draws {@link RadialWipe} over a 16-pixel item icon, in half-pixel cells so the hand's edge is not a
 * staircase at GUI scale.
 */
final class RadialWipeRenderer {

    private static final int CELLS = 32;
    private static final int SHADE = 0x90000000;

    private RadialWipeRenderer() {
    }

    /**
     * Over the icon at {@code (x, y)}. Starts a new stratum first: items are drawn in a later pass than
     * fills, so a fill in the same stratum would land under the icon it is meant to cover.
     */
    static void over(GuiGraphicsExtractor graphics, int x, int y, float progress) {
        if (progress >= 1.0f) return;
        graphics.nextStratum();
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(0.5f, 0.5f);
        for (int row = 0; row < CELLS; row++) {
            // One fill per run of shaded cells, not one per cell.
            int start = -1;
            for (int col = 0; col <= CELLS; col++) {
                boolean shaded = col < CELLS && RadialWipe.shaded(col, row, CELLS, progress);
                if (shaded && start < 0) start = col;
                if (!shaded && start >= 0) {
                    graphics.fill(start, row, col, row + 1, SHADE);
                    start = -1;
                }
            }
        }
        graphics.pose().popMatrix();
    }
}
