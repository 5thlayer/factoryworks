package com.planetaryfactory.core.energy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Which wires a placed pole adds (ADR-0068): every pole in reach sharing no neighbour with it, at
 * most five, nearest first.
 */
class PoleWiringPlacementTest {

    private static PoleLinks.Pole small(int x, int y, int z) {
        return new PoleLinks.Pole(x, y, z, PoleTier.SMALL);
    }

    @Test
    void aPlacedPoleWiresToAPoleInReachAndNotToOneBeyond() {
        PoleLinks.Pole near = small(7, 0, 0);
        List<PoleLinks.Pole> wired = PoleWiring.onPlace(
                small(0, 0, 0), List.of(near, small(-8, 0, 0)), new WireSet());
        assertEquals(List.of(near), wired);
    }

    private static PoleLinks.Pos pos(PoleLinks.Pole p) {
        return new PoleLinks.Pos(p.x(), p.y(), p.z());
    }

    @Test
    void aPoleAlreadyWiredToOneJustChosenIsSkippedSoNoTriangleForms() {
        PoleLinks.Pole a = small(0, 0, 0);
        PoleLinks.Pole b = small(7, 0, 0);
        WireSet wires = new WireSet();
        wires.add(pos(a), pos(b));
        // Placed between them, nearer a: it wires to a, and b is a's neighbour.
        List<PoleLinks.Pole> wired = PoleWiring.onPlace(small(3, 0, 0), List.of(b, a), wires);
        assertEquals(List.of(a), wired);
    }

    @Test
    void aPlacedPoleWiresToAtMostFiveTakingTheNearest() {
        PoleLinks.Pole p6 = small(0, 0, 6);
        PoleLinks.Pole p1 = small(1, 0, 0);
        PoleLinks.Pole p2 = small(-2, 0, 0);
        PoleLinks.Pole p3 = small(0, 0, 3);
        PoleLinks.Pole p4 = small(0, 0, -4);
        PoleLinks.Pole p5 = small(5, 0, 0);
        List<PoleLinks.Pole> wired = PoleWiring.onPlace(
                small(0, 0, 0), List.of(p6, p5, p4, p3, p2, p1), new WireSet());
        assertEquals(List.of(p1, p2, p3, p4, p5), wired);
    }

    @Test
    void polesAtTheSameDistanceAreTakenInPositionOrderWhateverOrderTheyStand() {
        PoleLinks.Pole east = small(3, 0, 0);
        PoleLinks.Pole west = small(-3, 0, 0);
        PoleLinks.Pole south = small(0, 0, 3);
        List<PoleLinks.Pole> wired = PoleWiring.onPlace(
                small(0, 0, 0), List.of(east, south, west), new WireSet());
        assertEquals(List.of(west, south, east), wired);
    }
}
