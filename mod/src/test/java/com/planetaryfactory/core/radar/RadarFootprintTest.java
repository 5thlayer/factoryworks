package com.planetaryfactory.core.radar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.planetaryfactory.core.machine.footprint.Footprint.Local;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The Radar stands on a 3x3x3 cube, its anchor at the bottom centre (#368). */
class RadarFootprintTest {

    @Test
    void isAThreeByThreeByThreeCubeAnchoredAtItsBottomCentre() {
        List<Local> offsets = RadarFootprint.FOOTPRINT.offsets();
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
