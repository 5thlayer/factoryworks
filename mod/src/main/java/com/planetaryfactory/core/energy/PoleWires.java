package com.planetaryfactory.core.energy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Which wires a pole draws (#281). Cosmetic: ADR-0062's balance never reads this.
 *
 * <p>A wire is a {@link PoleLinks#linked} pair, not a network, so a chain shows its links and not
 * a wire between its ends. Each pair is drawn by the end that sorts first by position, which needs
 * no shared state between two renderers and so can neither miss a wire nor draw it twice. Like the
 * links, it is recomputed from the poles standing, so a broken link's wire is simply not drawn.
 *
 * <p>Pure: no Minecraft types.
 */
public final class PoleWires {

    private PoleWires() {
    }

    /** The poles {@code self} draws a wire to, out of every pole standing. */
    public static List<PoleLinks.Pole> drawnBy(PoleLinks.Pole self, Collection<PoleLinks.Pole> poles) {
        List<PoleLinks.Pole> drawn = new ArrayList<>();
        for (PoleLinks.Pole other : poles) {
            if (sortsFirst(self, other) && PoleLinks.linked(self, other)) {
                drawn.add(other);
            }
        }
        return drawn;
    }

    private static boolean sortsFirst(PoleLinks.Pole a, PoleLinks.Pole b) {
        if (a.x() != b.x()) {
            return a.x() < b.x();
        }
        if (a.y() != b.y()) {
            return a.y() < b.y();
        }
        return a.z() < b.z();
    }
}
