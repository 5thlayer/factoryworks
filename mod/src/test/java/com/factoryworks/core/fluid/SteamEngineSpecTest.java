package com.factoryworks.core.fluid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Steam Engine's calibration (#282, ADR-0062, ADR-0116): Factorio's rate.
 *
 * <p>Every rate is asserted over whole ticks rather than per tick, because 30 mB/s is 1.5 mB a
 * tick and a segment only moves whole millibuckets.
 */
class SteamEngineSpecTest {

    private static final SteamEngineSpec SPEC = SteamEngineSpec.fromCorpus(SteamChainCorpus.get());

    private static final int SECOND = 20;

    /** What one engine burns and makes over {@code ticks} ticks. */
    private static long[] run(int ticks) {
        SteamEngineSpec.Carry carry = SteamEngineSpec.Carry.NONE;
        long steam = 0;
        long energy = 0;
        for (int t = 0; t < ticks; t++) {
            SteamEngineSpec.Tick tick = SPEC.tick(carry, Long.MAX_VALUE);
            steam += tick.steam();
            energy += tick.energy();
            carry = tick.carry();
        }
        return new long[] {steam, energy};
    }

    @Test
    @DisplayName("one engine burns 30 mB/s and makes 450 FE/t, over whole ticks")
    void oneEngine() {
        long[] second = run(SECOND);
        assertEquals(30L, second[0]);
        assertEquals(450L * SECOND, second[1]);
    }

    @Test
    @DisplayName("a lone tick never floors the half millibucket away")
    void remainderIsCarried() {
        long[] minute = run(60 * SECOND);
        assertEquals(1_800L, minute[0]);
    }

    @Test
    @DisplayName("an engine drained by a pole every tick still makes 450 FE/t (#292)")
    void drainedBufferSustainsTheRate() {
        // The buffer is one tick of output and a millibucket is 300 FE, so the tick that burns the
        // carried half millibucket overshoots the room. Cutting that burn to whole millibuckets
        // held a lone engine at 300 FE/t on a pole that drew it dry.
        long room = SPEC.bufferCapacity();
        SteamEngineSpec.Carry carry = SPEC.tick(SteamEngineSpec.Carry.NONE, room).carry();
        long steam = 0;
        for (int t = 0; t < SECOND; t++) {
            SteamEngineSpec.Request asked = SPEC.request(carry, room);
            SteamEngineSpec.Tick made = SPEC.burn(asked.steam(), asked.carry(), room);
            assertEquals(450L, made.energy(), "tick " + t);
            steam += made.steam();
            carry = made.carry();
        }
        assertEquals(30L, steam);
    }

    @Test
    @DisplayName("a buffer with no room burns no steam")
    void fullBufferBurnsNothing() {
        SteamEngineSpec.Request asked = SPEC.request(new SteamEngineSpec.Carry(0.5, 0.0), 0L);
        assertEquals(0, asked.steam());
    }

    @Test
    @DisplayName("a buffer with partial room burns no millibucket whose energy starts past it")
    void partialRoomCutsTheBurn() {
        // 300 FE a millibucket: 250 FE of room takes one, not the two a carry asks for.
        SteamEngineSpec.Request asked = SPEC.request(new SteamEngineSpec.Carry(0.5, 0.0), 250L);
        assertEquals(1, asked.steam());
        assertEquals(0.0, asked.carry().steam());
        SteamEngineSpec.Tick made = SPEC.burn(asked.steam(), asked.carry(), 250L);
        assertEquals(250L, made.energy());
        assertEquals(50.0, made.carry().energy(), 1e-6, "the overshoot is owed, not destroyed");
    }

    @Test
    @DisplayName("the port is 200 mB and the buffer 450 FE")
    void capacities() {
        assertEquals(200, SPEC.portCapacity());
        assertEquals(450L, SPEC.bufferCapacity());
    }
}
