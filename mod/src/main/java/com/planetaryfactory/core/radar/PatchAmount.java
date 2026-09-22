package com.planetaryfactory.core.radar;

/**
 * A patch's amount in Factorio's short form, or an oil field's yield, floored so a label never claims
 * more than is there (#370).
 */
public final class PatchAmount {

    private PatchAmount() {
    }

    public static String format(long amount) {
        if (amount < 1_000) {
            return Long.toString(amount);
        }
        if (amount < 1_000_000) {
            return shorten(amount, 1_000, "k");
        }
        return shorten(amount, 1_000_000, "M");
    }

    /** An oil field's summed yield, floored to a whole percent of {@code normal} (ADR-0081). */
    public static String yield(long amount, long normal) {
        return String.format(java.util.Locale.ROOT, "%,d%%", amount * 100 / normal);
    }

    private static String shorten(long amount, long unit, String suffix) {
        long whole = amount / unit;
        if (whole >= 100) {
            return whole + suffix;
        }
        long tenths = amount * 10 / unit % 10;
        return whole + "." + tenths + suffix;
    }
}
