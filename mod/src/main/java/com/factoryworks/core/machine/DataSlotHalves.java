package com.factoryworks.core.machine;

/**
 * A figure too wide for one menu data slot, which crosses the wire as a signed short (#332): sent
 * as two unsigned 16-bit halves, saturating at 32 bits rather than wrapping.
 */
public final class DataSlotHalves {

    private static final long MAX = 0xFFFF_FFFFL;

    private DataSlotHalves() {
    }

    public static int low(long value) {
        return (int) (clamp(value) & 0xFFFF);
    }

    public static int high(long value) {
        return (int) (clamp(value) >>> 16);
    }

    /** The client reads each half back sign-extended, hence the masks. */
    public static long join(int low, int high) {
        return ((long) (high & 0xFFFF) << 16) | (low & 0xFFFF);
    }

    private static long clamp(long value) {
        return Math.max(0L, Math.min(MAX, value));
    }
}
