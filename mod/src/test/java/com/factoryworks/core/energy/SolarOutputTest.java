package com.factoryworks.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SolarOutputTest {

    @Test
    void peakPaysWholeAndCarriesNothing() {
        SolarOutput.Tick t = SolarOutput.tick(30, 1.0, 0.0);
        assertEquals(30L, t.fe());
        assertEquals(0.0, t.carry(), 1e-9);
    }

    @Test
    void nothingAtNight() {
        assertEquals(0L, SolarOutput.tick(30, 0.0, 0.0).fe());
    }

    @Test
    void aFractionalRateIsCarriedSoTheTotalIsExact() {
        double carry = 0.0;
        long total = 0;
        for (int i = 0; i < 1000; i++) {
            SolarOutput.Tick t = SolarOutput.tick(30, 0.55, carry);
            total += t.fe();
            carry = t.carry();
        }
        assertEquals(16500L, total);
    }

    @Test
    void neverPaysMoreThanPeak() {
        double carry = 0.0;
        for (int i = 0; i < 1000; i++) {
            SolarOutput.Tick t = SolarOutput.tick(30, 0.999, carry);
            carry = t.carry();
            if (t.fe() > 30) {
                throw new AssertionError("paid " + t.fe());
            }
        }
    }
}
