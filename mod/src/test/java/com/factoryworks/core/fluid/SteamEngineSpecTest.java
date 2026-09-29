package com.factoryworks.core.fluid;

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
            SteamEngineSpec.Tick tick = SPEC.tick(speed, rowLength, carry, Long.MAX_VALUE);
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
    @DisplayName("a row drained by a pole every tick still makes N x 450 FE/t (#292)")
    void drainedBufferSustainsTheRate() {
        // The buffer is one tick of output and a millibucket is 300 FE, so the tick that burns the
        // carried half millibucket overshoots the room. Cutting that burn to whole millibuckets
        // held a row of three at 1,200 FE/t and a lone engine at 300 on a pole that drew it dry.
        for (int n = 1; n <= 5; n++) {
            // The first tick owes nothing yet, so it is left out; every tick after it is exact.
            long room = SPEC.bufferCapacity(n);
            SteamEngineSpec.Carry carry = SPEC.tick(SteamEngineSpec.PEAK_SPEED, n,
                    SteamEngineSpec.Carry.NONE, room).carry();
            long steam = 0;
            for (int t = 0; t < SECOND; t++) {
                SteamEngineSpec.Request asked = SPEC.request(SteamEngineSpec.PEAK_SPEED, n, carry, room);
                SteamEngineSpec.Tick made =
                        SPEC.burn(asked.steam(), SteamEngineSpec.PEAK_SPEED, asked.carry(), room);
                assertEquals(450L * n, made.energy(), "tick " + t + " for a row of " + n);
                steam += made.steam();
                carry = made.carry();
            }
            assertEquals(30L * n, steam, "steam for a row of " + n);
        }
    }

    @Test
    @DisplayName("a buffer with no room burns no steam")
    void fullBufferBurnsNothing() {
        SteamEngineSpec.Request asked = SPEC.request(SteamEngineSpec.PEAK_SPEED, 1,
                new SteamEngineSpec.Carry(0.5, 0.0), 0L);
        assertEquals(0, asked.steam());
    }

    @Test
    @DisplayName("a buffer with partial room burns no millibucket whose energy starts past it")
    void partialRoomCutsTheBurn() {
        // 300 FE a millibucket at the peak: 250 FE of room takes one, not the two a carry asks for.
        SteamEngineSpec.Request asked = SPEC.request(SteamEngineSpec.PEAK_SPEED, 1,
                new SteamEngineSpec.Carry(0.5, 0.0), 250L);
        assertEquals(1, asked.steam());
        assertEquals(0.0, asked.carry().steam());
        SteamEngineSpec.Tick made =
                SPEC.burn(asked.steam(), SteamEngineSpec.PEAK_SPEED, asked.carry(), 250L);
        assertEquals(250L, made.energy());
        assertEquals(50.0, made.carry().energy(), 1e-6, "the overshoot is owed, not destroyed");
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
