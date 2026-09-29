package com.factoryworks.core.energy;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The level's stored wires (ADR-0068). {@code JsonOps} stands in for NBT: the set holds only ints
 * and lists.
 */
class WireSetTest {

    private static PoleLinks.Pos at(int x, int y, int z) {
        return new PoleLinks.Pos(x, y, z);
    }

    @Test
    void wiresSurviveTheRoundTrip() {
        WireSet wires = new WireSet();
        wires.add(at(0, 64, 0), at(7, 64, 0));
        wires.add(at(7, 64, 0), at(7, 70, 5));

        JsonElement written = WireSet.CODEC.encodeStart(JsonOps.INSTANCE, wires).getOrThrow();
        WireSet read = WireSet.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow();

        assertEquals(wires.all(), read.all());
    }

    @Test
    void aWireAddedFromEitherEndIsStoredOnce() {
        WireSet wires = new WireSet();
        wires.add(at(0, 64, 0), at(7, 64, 0));
        wires.add(at(7, 64, 0), at(0, 64, 0));
        assertEquals(1, wires.all().size());
        assertTrue(wires.contains(at(7, 64, 0), at(0, 64, 0)));
    }

    @Test
    void aWireIsCutFromEitherEnd() {
        WireSet wires = new WireSet();
        wires.add(at(0, 64, 0), at(7, 64, 0));
        wires.remove(at(7, 64, 0), at(0, 64, 0));
        assertFalse(wires.contains(at(0, 64, 0), at(7, 64, 0)));
    }

    @Test
    void breakingAPoleCutsEveryWireItHoldsAndNoOther() {
        WireSet wires = new WireSet();
        wires.add(at(0, 64, 0), at(7, 64, 0));
        wires.add(at(14, 64, 0), at(7, 64, 0));
        wires.add(at(14, 64, 0), at(21, 64, 0));
        wires.removeAllOf(at(7, 64, 0));
        assertEquals(1, wires.all().size());
        assertTrue(wires.contains(at(21, 64, 0), at(14, 64, 0)));
    }

    @Test
    void aChunkCarriesEveryWireWithAnEndInsideIt() {
        WireSet wires = new WireSet();
        wires.add(at(1, 64, 1), at(8, 64, 1));      // both ends in chunk (0, 0)
        wires.add(at(12, 64, 1), at(18, 64, 1));    // crosses into chunk (1, 0)
        wires.add(at(20, 64, 1), at(26, 64, 1));    // chunk (1, 0) only
        assertEquals(2, wires.touching(0, 0).size());
        assertEquals(2, wires.touching(1, 0).size());
    }

    @Test
    void replacingAChunksWiresDropsOnesCutWhileItWasUnwatched() {
        WireSet client = new WireSet();
        client.add(at(1, 64, 1), at(8, 64, 1));
        client.add(at(12, 64, 1), at(18, 64, 1));
        client.add(at(20, 64, 1), at(26, 64, 1));
        // The server has since cut the first wire and made a new one.
        WireSet server = new WireSet();
        server.add(at(12, 64, 1), at(18, 64, 1));
        server.add(at(2, 64, 2), at(3, 64, 3));

        client.replaceTouching(0, 0, server.touching(0, 0));

        assertFalse(client.contains(at(1, 64, 1), at(8, 64, 1)));
        assertTrue(client.contains(at(2, 64, 2), at(3, 64, 3)));
        assertTrue(client.contains(at(20, 64, 1), at(26, 64, 1)));
        assertEquals(3, client.all().size());
    }

    /** A column that grew downwards or was trimmed from the bottom keeps its wires (#309). */
    @Test
    void rekeyingMovesEveryWireOfAPoleToTheNewBase() {
        WireSet wires = new WireSet();
        PoleLinks.Pos base = new PoleLinks.Pos(0, 64, 0);
        PoleLinks.Pos lower = new PoleLinks.Pos(0, 63, 0);
        PoleLinks.Pos other = new PoleLinks.Pos(5, 64, 0);
        wires.add(base, other);
        wires.rekey(base, lower);
        assertFalse(wires.contains(base, other));
        assertTrue(wires.contains(lower, other));
    }

    @Test
    void rekeyingDropsAWireThatWouldJoinTheColumnToItself() {
        WireSet wires = new WireSet();
        PoleLinks.Pos joined = new PoleLinks.Pos(0, 64, 0);
        PoleLinks.Pos base = new PoleLinks.Pos(0, 63, 0);
        wires.add(joined, base);
        wires.rekey(joined, base);
        assertTrue(wires.all().isEmpty());
    }

    @Test
    void rekeyingCollapsesTwoWiresToTheSameThirdPole() {
        WireSet wires = new WireSet();
        PoleLinks.Pos joined = new PoleLinks.Pos(0, 64, 0);
        PoleLinks.Pos base = new PoleLinks.Pos(0, 63, 0);
        PoleLinks.Pos third = new PoleLinks.Pos(5, 64, 0);
        wires.add(joined, third);
        wires.add(base, third);
        wires.rekey(joined, base);
        assertEquals(1, wires.all().size());
        assertTrue(wires.contains(base, third));
    }

    @Test
    void rekeyingAPoleWithNoWiresChangesNothing() {
        WireSet wires = new WireSet();
        PoleLinks.Pos a = new PoleLinks.Pos(0, 64, 0);
        PoleLinks.Pos b = new PoleLinks.Pos(5, 64, 0);
        wires.add(a, b);
        wires.rekey(new PoleLinks.Pos(9, 64, 9), new PoleLinks.Pos(9, 63, 9));
        assertEquals(1, wires.all().size());
        assertTrue(wires.contains(a, b));
    }
}
