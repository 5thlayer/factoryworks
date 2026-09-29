package com.factoryworks.core.oil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.factoryworks.core.machine.footprint.Footprint.Local;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The Pumpjack stands on Factorio's 3x3 tiles, as tall as it is wide, over the well it pumps (ADR-0081). */
class PumpjackFootprintTest {

    @Test
    void isAThreeByThreeByThreeCubeAnchoredAtItsBottomCentre() {
        List<Local> offsets = PumpjackFootprint.FOOTPRINT.offsets();
        assertEquals(27, offsets.size());
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    assertTrue(offsets.contains(new Local(x, y, z)), "missing " + x + "," + y + "," + z);
                }
            }
        }
    }
}
