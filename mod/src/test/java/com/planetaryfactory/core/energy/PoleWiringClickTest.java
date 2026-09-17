package com.planetaryfactory.core.energy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Engineer's Pick's second click on a pole (ADR-0068): it wires an unwired pair, cuts a wired
 * one, cancels on the anchor itself, and refuses a pair out of reach without changing anything.
 */
class PoleWiringClickTest {

    private static PoleLinks.Pole small(int x, int y, int z) {
        return new PoleLinks.Pole(x, y, z, PoleTier.SMALL);
    }

    private static PoleLinks.Pos pos(PoleLinks.Pole p) {
        return new PoleLinks.Pos(p.x(), p.y(), p.z());
    }

    @Test
    void clickingAnUnwiredPoleInReachWiresThePair() {
        PoleLinks.Pole a = small(0, 0, 0);
        PoleLinks.Pole b = small(7, 0, 0);
        WireSet wires = new WireSet();
        assertEquals(PoleWiring.Click.WIRED, PoleWiring.click(a, b, wires));
        assertTrue(wires.contains(pos(a), pos(b)));
    }

    @Test
    void clickingAWiredPoleCutsTheWire() {
        PoleLinks.Pole a = small(0, 0, 0);
        PoleLinks.Pole b = small(7, 0, 0);
        WireSet wires = new WireSet();
        wires.add(pos(b), pos(a));
        assertEquals(PoleWiring.Click.CUT, PoleWiring.click(a, b, wires));
        assertFalse(wires.contains(pos(a), pos(b)));
    }

    @Test
    void clickingAPoleOutOfReachIsRefusedAndChangesNothing() {
        PoleLinks.Pole a = small(0, 0, 0);
        PoleLinks.Pole b = small(8, 0, 0);
        WireSet wires = new WireSet();
        assertEquals(PoleWiring.Click.REFUSED, PoleWiring.click(a, b, wires));
        assertTrue(wires.all().isEmpty());
    }

    @Test
    void clickingTheAnchorItselfCancelsAndChangesNothing() {
        PoleLinks.Pole a = small(0, 0, 0);
        WireSet wires = new WireSet();
        assertEquals(PoleWiring.Click.CANCELLED, PoleWiring.click(a, a, wires));
        assertTrue(wires.all().isEmpty());
    }

    @Test
    void wiringByHandHasNoCap() {
        PoleLinks.Pole hub = small(0, 0, 0);
        WireSet wires = new WireSet();
        for (int i = 1; i <= PoleWiring.AUTO_WIRES + 1; i++) {
            assertEquals(PoleWiring.Click.WIRED, PoleWiring.click(hub, small(i, 0, 0), wires));
        }
        assertEquals(PoleWiring.AUTO_WIRES + 1, wires.all().size());
    }

    /** The slack wire's red tint asks this before the click, so it must agree with the click. */
    @Test
    void refusesAgreesWithTheClick() {
        PoleLinks.Pole a = small(0, 0, 0);
        for (PoleLinks.Pole target : new PoleLinks.Pole[] {a, small(7, 0, 0), small(8, 0, 0)}) {
            boolean refused = PoleWiring.click(a, target, new WireSet()) == PoleWiring.Click.REFUSED;
            assertEquals(refused, PoleWiring.refuses(a, target));
        }
    }
}
