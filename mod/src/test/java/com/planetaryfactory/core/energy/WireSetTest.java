package com.planetaryfactory.core.energy;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The level's stored wires (ADR-0068). {@code JsonOps} stands in for NBT, as in
 * {@code AssemblerCodecsTest}: the set holds only ints and lists.
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
}
