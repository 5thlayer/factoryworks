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

    /** What a second click with the Pick did. */
    public enum Click {
        WIRED, CUT, CANCELLED, REFUSED
    }

    private PoleWiring() {
    }

    /** Whether a second click on {@code target} would be refused; the slack wire turns red on it. */
    public static boolean refuses(PoleLinks.Pole anchor, PoleLinks.Pole target) {
        return !pos(anchor).equals(pos(target)) && !PoleLinks.linked(anchor, target);
    }

    /** Applies the Pick's second click, on {@code target}, to the wire set. */
    public static Click click(PoleLinks.Pole anchor, PoleLinks.Pole target, WireSet wires) {
        if (pos(anchor).equals(pos(target))) {
            return Click.CANCELLED;
        }
        if (refuses(anchor, target)) {
            return Click.REFUSED;
        }
        if (wires.contains(pos(anchor), pos(target))) {
            wires.remove(pos(anchor), pos(target));
            return Click.CUT;
        }
        wires.add(pos(anchor), pos(target));
        return Click.WIRED;
    }

    /**
     * The wires the Placement Preview draws for a held pole (#298): the same
     * {@link #onPlace} the server runs, asked of a hypothetical pole at the aimed spot, and nothing
     * where the placement only grows a column, since a column that grew adds no wire (#309).
     *
     * @param joinsAColumn the placement would extend or join a standing column rather than start one
     */
    public static List<PoleLinks.Pole> wouldAdd(PoleLinks.Pole placed, Collection<PoleLinks.Pole> standing,
                                                WireSet wires, boolean joinsAColumn) {
        return joinsAColumn ? List.of() : onPlace(placed, standing, wires);
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
