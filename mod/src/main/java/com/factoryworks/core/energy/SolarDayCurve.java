package com.factoryworks.core.energy;

/**
 * A Solar Panel's output against the day, as Factorio's surface has it: {@code daytime} 0 is noon,
 * full to dusk at 0.25, linear to nothing at evening 0.45, nothing to morning 0.55, linear to full
 * at dawn 0.75 (Factorio Lua API, {@code LuaSurface.dusk/evening/morning/dawn}). The mean over a day
 * is 0.7.
 *
 * <p>Read against the fraction of the dimension's day, so it holds at any period (ADR-0099).
 * Pure: no Minecraft types.
 */
public final class SolarDayCurve {

    public static final double DUSK = 0.25;
    public static final double EVENING = 0.45;
    public static final double MORNING = 0.55;
    public static final double DAWN = 0.75;

    /** Minecraft's noon is this far into its clock's day. */
    public static final double NOON_CLOCK_FRACTION = 0.25;

    /** Oritech's fold reads a time of day in (0, 12500) as day (ADR-0099). */
    private static final long DAY_SPAN = 12000L;
    private static final long NIGHT_TIME = 18000L;

    private SolarDayCurve() {
    }

    /** The curve's daytime for a clock fraction of the day, where Minecraft's noon is 0. */
    public static double fromClockFraction(double clockFraction) {
        return wrap(clockFraction - NOON_CLOCK_FRACTION);
    }

    /** 0 to 1 for a daytime fraction; periodic. */
    public static double multiplier(double daytime) {
        double d = wrap(daytime);
        if (d <= DUSK) {
            return 1.0;
        }
        if (d < EVENING) {
            return (EVENING - d) / (EVENING - DUSK);
        }
        if (d <= MORNING) {
            return 0.0;
        }
        if (d < DAWN) {
            return (d - MORNING) / (DAWN - MORNING);
        }
        return 1.0;
    }

    /**
     * The time of day Oritech's panel animation is told: inside (0, 12500) exactly when the multiplier
     * is above zero, spread so noon is 6000 and the sun tracks across the sky, and 18000 otherwise.
     */
    public static long animationTimeOfDay(double daytime) {
        double d = wrap(daytime);
        if (multiplier(d) <= 0.0) {
            return NIGHT_TIME;
        }
        double sinceMorning = wrap(d - MORNING);
        long time = Math.round(sinceMorning / (1.0 - (MORNING - EVENING)) * DAY_SPAN);
        return Math.max(1L, time);
    }

    private static double wrap(double x) {
        return x - Math.floor(x);
    }
}
