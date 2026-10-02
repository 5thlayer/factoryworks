package com.factoryworks.core.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.factoryworks.core.machine.footprint.Footprint.Local;
import org.junit.jupiter.api.Test;

/** The Boiler's three front blocks are water ports, its back middle the steam port, its back corners none (ADR-0114). */
class BoilerFootprintTest {

    @Test
    void theFrontRowIsWater() {
        for (int z = -1; z <= 1; z++) {
            assertEquals(BoilerFootprint.Port.WATER, BoilerFootprint.portAt(new Local(0, 0, z)));
        }
    }

    @Test
    void theBackMiddleIsTheOnlySteamPort() {
        assertEquals(BoilerFootprint.Port.STEAM, BoilerFootprint.portAt(new Local(1, 0, 0)));
        assertEquals(new Local(1, 0, 0), BoilerFootprint.FOOTPRINT.offsets().get(BoilerFootprint.STEAM_PART));
    }

    @Test
    void theBackCornersAreNoPort() {
        assertNull(BoilerFootprint.portAt(new Local(1, 0, -1)));
        assertNull(BoilerFootprint.portAt(new Local(1, 0, 1)));
    }

    @Test
    void everyBlockOfTheFootprintIsClassified() {
        long ports = BoilerFootprint.FOOTPRINT.offsets().stream()
                .filter(offset -> BoilerFootprint.portAt(offset) != null).count();
        assertEquals(4, ports);
    }
}
