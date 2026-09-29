package com.factoryworks.core.machine.footprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import com.factoryworks.core.machine.footprint.Footprint.Local;
import org.junit.jupiter.api.Test;

class FootprintTest {

    private static final Footprint THREE = Footprint.of(new Local(0, 1, 0), new Local(0, 0, 1));

    @Test
    void theAnchorIsFirstAndThePartsFollowInOrder() {
        assertEquals(List.of(new Local(0, 0, 0), new Local(0, 1, 0), new Local(0, 0, 1)), THREE.offsets());
        assertEquals(2, THREE.partCount());
    }

    @Test
    void aPartIndexNamesItsOffset() {
        assertEquals(new Local(0, 1, 0), THREE.offsetOfPart(1));
        assertEquals(new Local(0, 0, 1), THREE.offsetOfPart(2));
    }

    @Test
    void theAnchorIsNotAPart() {
        assertThrows(IllegalArgumentException.class, () -> THREE.offsetOfPart(0));
        assertThrows(IllegalArgumentException.class, () -> THREE.offsetOfPart(3));
    }

    @Test
    void aPartCannotStandOnTheAnchorOrOnAnotherPart() {
        assertThrows(IllegalArgumentException.class, () -> Footprint.of(new Local(0, 0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> Footprint.of(new Local(0, 1, 0), new Local(0, 1, 0)));
    }
}
