package com.planetaryfactory.core.energy;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the Placement Preview draws for a held pole (#298, ADR-0068): exactly the wires placing it
 * would add, and none where the placement only grows a column.
 */
class PoleWiringPreviewTest {

    private static PoleLinks.Pole small(int x, int y, int z) {
        return new PoleLinks.Pole(x, y, z, PoleTier.SMALL);
    }

    @Test
    void thePreviewNamesTheWiresAPlacementWouldAdd() {
        PoleLinks.Pole placed = small(0, 64, 0);
        List<PoleLinks.Pole> standing = List.of(small(5, 64, 0), small(0, 64, 5));
        WireSet wires = new WireSet();
        assertEquals(PoleWiring.onPlace(placed, standing, wires),
                PoleWiring.wouldAdd(placed, standing, wires, false));
    }

    @Test
    void aPlacementThatOnlyGrowsAColumnDrawsNoWires() {
        PoleLinks.Pole placed = small(0, 64, 0);
        List<PoleLinks.Pole> standing = List.of(small(5, 64, 0));
        assertTrue(PoleWiring.wouldAdd(placed, standing, new WireSet(), true).isEmpty());
    }

    @Test
    void thePreviewChangesNoWire() {
        PoleLinks.Pole placed = small(0, 64, 0);
        WireSet wires = new WireSet();
        PoleWiring.wouldAdd(placed, List.of(small(5, 64, 0)), wires, false);
        assertTrue(wires.all().isEmpty());
    }

    @Test
    void aPoleOutOfReachIsNotDrawn() {
        PoleLinks.Pole placed = small(0, 64, 0);
        assertTrue(PoleWiring.wouldAdd(placed, List.of(small(20, 64, 0)), new WireSet(), false).isEmpty());
    }
}
