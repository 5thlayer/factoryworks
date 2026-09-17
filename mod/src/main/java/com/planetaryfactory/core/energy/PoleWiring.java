package com.planetaryfactory.core.energy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * The wiring rules (ADR-0068). Pure: no Minecraft types.
 */
public final class PoleWiring {

    /**
     * Factorio's {@code auto_connect_up_to_n_wires} default. It caps only the wires placement adds;
     * a wire made by hand has no cap, as in Factorio 2.0.7 and later.
     */
    public static final int AUTO_WIRES = 5;

    private PoleWiring() {
    }

    /** The standing poles a newly placed pole wires itself to. */
    public static List<PoleLinks.Pole> onPlace(PoleLinks.Pole placed, Collection<PoleLinks.Pole> standing,
                                               WireSet wires) {
        List<PoleLinks.Pole> candidates = new ArrayList<>(standing);
        candidates.sort(Comparator.<PoleLinks.Pole>comparingLong(other -> distanceSquared(placed, other))
                .thenComparingInt(PoleLinks.Pole::x)
                .thenComparingInt(PoleLinks.Pole::y)
                .thenComparingInt(PoleLinks.Pole::z));
        List<PoleLinks.Pole> wired = new ArrayList<>();
        for (PoleLinks.Pole other : candidates) {
            if (wired.size() == AUTO_WIRES) {
                break;
            }
            if (PoleLinks.linked(placed, other) && !sharesANeighbour(other, wired, wires)) {
                wired.add(other);
            }
        }
        return wired;
    }

    /** Whether {@code other} is already wired to a pole the placed one has just wired to. */
    private static boolean sharesANeighbour(PoleLinks.Pole other, List<PoleLinks.Pole> wired, WireSet wires) {
        for (PoleLinks.Pole chosen : wired) {
            if (wires.contains(pos(chosen), pos(other))) {
                return true;
            }
        }
        return false;
    }

    private static PoleLinks.Pos pos(PoleLinks.Pole p) {
        return new PoleLinks.Pos(p.x(), p.y(), p.z());
    }

    private static long distanceSquared(PoleLinks.Pole a, PoleLinks.Pole b) {
        long dx = a.x() - b.x();
        long dy = a.y() - b.y();
        long dz = a.z() - b.z();
        return dx * dx + dy * dy + dz * dz;
    }
}
