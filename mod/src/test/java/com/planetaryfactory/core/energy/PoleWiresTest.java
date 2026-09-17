package com.planetaryfactory.core.energy;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which wires a pole draws (#281): one per pole it links to, and each linked pair drawn by exactly
 * one of its two ends, so a wire is neither missing nor doubled.
 */
class PoleWiresTest {

    private static PoleLinks.Pole small(int x, int y, int z) {
        return new PoleLinks.Pole(x, y, z, PoleTier.SMALL);
    }

    @Test
    void aLinkedPairIsDrawnByExactlyOneEnd() {
        PoleLinks.Pole a = small(0, 0, 0);
        PoleLinks.Pole b = small(7, 0, 0);
        List<PoleLinks.Pole> all = List.of(a, b);
        assertEquals(1, PoleWires.drawnBy(a, all).size() + PoleWires.drawnBy(b, all).size());
    }

    @Test
    void aPoleBeyondReachGetsNoWire() {
        PoleLinks.Pole a = small(0, 0, 0);
        PoleLinks.Pole b = small(8, 0, 0);
        List<PoleLinks.Pole> all = List.of(a, b);
        assertTrue(PoleWires.drawnBy(a, all).isEmpty());
        assertTrue(PoleWires.drawnBy(b, all).isEmpty());
    }

    @Test
    void aPoleDoesNotWireItself() {
        PoleLinks.Pole a = small(0, 0, 0);
        assertTrue(PoleWires.drawnBy(a, List.of(a)).isEmpty());
    }

    @Test
    void wiresFollowLinksNotNetworks() {
        // A chain is one network, but its ends are out of each other's reach: two wires, not three.
        List<PoleLinks.Pole> all = List.of(small(0, 0, 0), small(7, 0, 0), small(14, 0, 0));
        List<PoleLinks.Pole[]> wires = new ArrayList<>();
        for (PoleLinks.Pole pole : all) {
            for (PoleLinks.Pole other : PoleWires.drawnBy(pole, all)) {
                wires.add(new PoleLinks.Pole[] {pole, other});
            }
        }
        assertEquals(2, wires.size());
    }

    @Test
    void everyLinkedPairIsDrawnOnceWhateverTheLayout() {
        List<PoleLinks.Pole> all = List.of(small(0, 0, 0), small(3, 1, 0), small(0, 0, 5),
                small(-4, 2, -4), new PoleLinks.Pole(10, 0, 10, PoleTier.SUBSTATION));
        for (PoleLinks.Pole a : all) {
            for (PoleLinks.Pole b : all) {
                if (a == b) {
                    continue;
                }
                int drawn = (PoleWires.drawnBy(a, all).contains(b) ? 1 : 0)
                        + (PoleWires.drawnBy(b, all).contains(a) ? 1 : 0);
                assertEquals(PoleLinks.linked(a, b) ? 1 : 0, drawn, a + " / " + b);
            }
        }
    }
}
