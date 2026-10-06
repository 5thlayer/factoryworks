package com.factoryworks.core.oil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.factoryworks.core.machine.footprint.Footprint.Local;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The Pumpjack stands on Factorio's 3x3 tiles, two blocks tall, over the well it pumps (ADR-0081). */
class PumpjackFootprintTest {

    @Test
    void isAThreeByThreeByTwoBoxAnchoredAtItsBottomCentre() {
        List<Local> offsets = PumpjackFootprint.FOOTPRINT.offsets();
        assertEquals(18, offsets.size());
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    assertTrue(offsets.contains(new Local(x, y, z)), "missing " + x + "," + y + "," + z);
                }
            }
        }
        assertTrue(offsets.stream().noneMatch(offset -> offset.y() > 1), "a third layer");
    }
}
