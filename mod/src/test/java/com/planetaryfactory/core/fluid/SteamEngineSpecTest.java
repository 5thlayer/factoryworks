package com.planetaryfactory.core.fluid;

import java.util.function.DoubleUnaryOperator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Steam Engine's calibration (#282, ADR-0062): Oritech's engine, burning Factorio's rate.
 *
 * <p>Every rate is asserted over whole ticks rather than per tick, because 30 mB/s is 1.5 mB a
 * tick and the tank only moves whole millibuckets. Oritech's own {@code (long)} cast floors that to
 * 1, which is an engine at two thirds of its prototype with a plausible number on the gauge.
 */
class SteamEngineSpecTest {

    /** Oritech's curve, as its jar states it; the spec takes whichever curve it is handed. */
    private static final DoubleUnaryOperator ORITECH_CURVE = s ->
            0.5 - 0.1966667 * s + 0.09166666865348816 * s * s
                    - 0.007499999832361937 * s * s * s + 0.4;

    private static final SteamEngineSpec SPEC =
            SteamEngineSpec.fromCorpus(SteamChainCorpus.get(), ORITECH_CURVE);

    private static final int SECOND = 20;

    /** What N engines at speed {@code speed} burn and make over {@code ticks} ticks. */
    private static long[] run(double speed, int rowLength, int ticks) {
        SteamEngineSpec.Carry carry = SteamEngineSpec.Carry.NONE;
        long steam = 0;
        long energy = 0;
        for (int t = 0; t < ticks; t++) {
            SteamEngineSpec.Tick tick = SPEC.tick(speed, rowLength, carry);
            steam += tick.steam();
            energy += tick.energy();
            carry = tick.carry();
        }
        return new long[] {steam, energy};
    }

    @Test
    @DisplayName("one engine at speed 7 burns 30 mB/s and makes 450 FE/t, over whole ticks")
    void oneEngineAtPeak() {
        long[] second = run(SteamEngineSpec.PEAK_SPEED, 1, SECOND);
        assertEquals(30L, second[0]);
        assertEquals(450L * SECOND, second[1]);
    }

    @Test
    @DisplayName("a lone tick never floors the half millibucket away")
    void remainderIsCarried() {
        long[] minute = run(SteamEngineSpec.PEAK_SPEED, 1, 60 * SECOND);
        assertEquals(1_800L, minute[0]);
    }

    @Test
    @DisplayName("N engines at speed 7 burn N x 30 mB/s and make N x 450 FE/t")
    void rowsAreLinear() {
        for (int n = 1; n <= 5; n++) {
            long[] second = run(SteamEngineSpec.PEAK_SPEED, n, SECOND);
            assertEquals(30L * n, second[0], "steam for a row of " + n);
            assertEquals(450L * n * SECOND, second[1], "energy for a row of " + n);
        }
    }

    @Test
    @DisplayName("a starved or flooded engine makes less per millibucket than one at the peak")
    void offPeakDoesWorse() {
        long[] peak = run(SteamEngineSpec.PEAK_SPEED, 1, SECOND);
        for (double speed : new double[] {2.0, 10.0}) {
            long[] off = run(speed, 1, SECOND);
            assertTrue((double) off[1] / off[0] < (double) peak[1] / peak[0],
                    "FE per mB at speed " + speed);
        }
    }

    @Test
    @DisplayName("no steam comes back as water")
    void noWaterReturn() {
        assertEquals(0, SteamEngineSpec.WATER_RETURNED);
    }

    @Test
    @DisplayName("the tank is 200 mB and the buffer 450 FE per engine in the row")
    void capacitiesScaleWithRow() {
        for (int n = 1; n <= 5; n++) {
            assertEquals(200 * n, SPEC.tankCapacity(n));
            assertEquals(450L * n, SPEC.bufferCapacity(n));
        }
    }
}
