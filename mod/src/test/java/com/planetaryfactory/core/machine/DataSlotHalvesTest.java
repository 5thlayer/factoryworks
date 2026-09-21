package com.planetaryfactory.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * A menu data slot crosses the wire as a signed short, so a figure past 32,767 -- the Assembling
 * Machine's 50,000 FE buffer -- is sent as two halves (#332).
 */
class DataSlotHalvesTest {

    /** What {@code ClientboundContainerSetDataPacket} does to a value: write a short, read it back signed. */
    private static int wire(int value) {
        return (short) value;
    }

    private static long roundTrip(long value) {
        return DataSlotHalves.join(wire(DataSlotHalves.low(value)), wire(DataSlotHalves.high(value)));
    }

    @Test
    void aValueWhoseLowHalfIsNegativeAsAShortSurvives() {
        assertEquals(50_000L, roundTrip(50_000L));
        assertEquals(0xFFFFL, roundTrip(0xFFFFL));
    }

    @Test
    void smallAndWideValuesSurvive() {
        assertEquals(0L, roundTrip(0L));
        assertEquals(37L, roundTrip(37L));
        assertEquals(1_234_567_890L, roundTrip(1_234_567_890L));
    }

    @Test
    void aValuePastTwoHalvesSaturatesRatherThanWrapping() {
        assertEquals(0xFFFF_FFFFL, roundTrip(Long.MAX_VALUE));
        assertEquals(0L, roundTrip(-5L));
    }
}
