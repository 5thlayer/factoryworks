package com.planetaryfactory.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.planetaryfactory.core.machine.footprint.Footprint.Local;

class OilRefineryFootprintTest {

    private static final Set<Local> BLOCKS = Set.copyOf(OilRefineryFootprint.FOOTPRINT.offsets());

    @Test
    void theBaseIsOritechsRefineryAndBothChambersAreWhole() {
        assertEquals(10 + 6 + 6, BLOCKS.size());
    }

    @Test
    void itStandsFourLayersTall() {
        assertEquals(Set.of(0, 1, 2, 3), BLOCKS.stream().map(Local::y).collect(Collectors.toSet()));
    }

    @Test
    void eachChamberLayerCoversTheWholeThreeByTwo() {
        for (int y = 2; y <= 3; y++) {
            for (int x = 0; x <= 2; x++) {
                for (int z = -1; z <= 0; z++) {
                    assertTrue(BLOCKS.contains(new Local(x, y, z)), x + "," + y + "," + z);
                }
            }
        }
    }

    @Test
    void theBaseLeavesOritechsNotchOpen() {
        assertFalse(BLOCKS.contains(new Local(2, 0, 0)));
        assertFalse(BLOCKS.contains(new Local(2, 1, 0)));
    }
}
