package com.factoryworks.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The Solar Panel in FE: Factorio's 60 kW peak at 100 J per FE, read from the resource (#529). */
class SolarPanelSpecTest {

    @Test
    void peaksAtThirtyFePerTick() {
        assertEquals(30L, SolarPanelSpec.get().peakFePerTick());
    }

    @Test
    void holdsOneTickOfItsOutput() {
        assertEquals(30L, SolarPanelSpec.get().bufferFe());
    }
}
