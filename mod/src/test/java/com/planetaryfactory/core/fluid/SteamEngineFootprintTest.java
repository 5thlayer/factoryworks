package com.planetaryfactory.core.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.planetaryfactory.core.machine.footprint.Footprint.Local;
import org.junit.jupiter.api.Test;

/**
 * The Steam Engine stands where Oritech's model is drawn (ADR-0077): the offsets are in the frame
 * Oritech's renderer rotates by, so they are asserted against Oritech's own layout.
 */
class SteamEngineFootprintTest {

    @Test
    void itIsOritechsControllerAndCoresInOritechsOrder() {
        // SteamEngineEntity.getCorePositions() at 2.0.0-exp6, read off the jar, after the anchor.
        assertEquals(List.of(new Local(0, 0, 0), new Local(0, 1, 0), new Local(0, 0, -1), new Local(0, 1, -1)),
                SteamEngineFootprint.FOOTPRINT.offsets());
    }
}
