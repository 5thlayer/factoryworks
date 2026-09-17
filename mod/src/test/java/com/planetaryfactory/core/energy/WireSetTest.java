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
}
