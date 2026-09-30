package com.factoryworks.core.energy;

import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.timeline.Timeline;

/** How far into its day a dimension is, whatever the day's length (ADR-0099). */
public final class DayFraction {

    private DayFraction() {
    }

    /**
     * The fraction of the dimension's shortest periodic timeline, which is its day: the moon's is a
     * multiple of it. Clock time is read the same on either side.
     */
    public static double of(Level level) {
        Timeline day = day(level);
        if (day == null) {
            return SolarDayCurve.NOON_CLOCK_FRACTION;
        }
        return (double) day.getCurrentTicks(level.clockManager()) / day.periodTicks().get();
    }

    /** The day's length in clock ticks, or 0 when the dimension has none. */
    public static int periodTicks(Level level) {
        Timeline day = day(level);
        return day == null ? 0 : day.periodTicks().get();
    }

    private static Timeline day(Level level) {
        Timeline day = null;
        for (Holder<Timeline> holder : level.dimensionType().timelines()) {
            Timeline timeline = holder.value();
            if (timeline.periodTicks().isPresent()
                    && (day == null || timeline.periodTicks().get() < day.periodTicks().get())) {
                day = timeline;
            }
        }
        return day;
    }
}
