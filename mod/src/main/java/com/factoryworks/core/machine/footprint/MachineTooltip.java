package com.factoryworks.core.machine.footprint;

import com.factoryworks.core.energy.ForgeEnergy;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * A footprint machine's item tooltip: two lines in the pack's own words, every figure in the pack's
 * units and read from the machine's spec (#515).
 *
 * <p>Pure: no Minecraft types.
 */
public final class MachineTooltip {

    private static final String PREFIX = "tooltip.factoryworks.";
    private static final double TICKS_PER_SECOND = 20.0;

    public record Line(String key, List<String> args) {
        public Line {
            args = List.copyOf(args);
        }
    }

    private MachineTooltip() {
    }

    public static List<Line> crafting(double craftingSpeed, long watts) {
        return List.of(line("speed", number(craftingSpeed)),
                line("draws", number(watts / TICKS_PER_SECOND / ForgeEnergy.JOULES_PER_FE)));
    }

    public static List<Line> steamEngine(double steamPerSecond, double fePerTick) {
        return List.of(line("burns_steam", number(steamPerSecond)), line("makes_up_to", number(fePerTick)));
    }

    public static List<Line> accumulator(long capacityFe, long flowFePerTick) {
        return List.of(line("holds", number(capacityFe)), line("in_and_out", number(flowFePerTick)));
    }

    public static List<Line> radar(long fePerTick, int nearReach, int reach) {
        return List.of(line("draws", number(fePerTick)), line("charts", number(nearReach), number(reach)));
    }

    private static Line line(String key, String... args) {
        return new Line(PREFIX + key, List.of(args));
    }

    private static String number(double value) {
        return new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(value);
    }
}
