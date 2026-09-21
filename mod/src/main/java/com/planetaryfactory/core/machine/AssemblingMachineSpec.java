package com.planetaryfactory.core.machine;

import java.util.Set;

/**
 * The Assembling Machine's rate (#328, ADR-0029): {@code assembling-machine-1}'s own
 * {@code crafting_speed} and {@code energy_usage}, on ADR-0060's 1 FE = 100 J.
 *
 * <p>The recipe carries {@code energy_required * 20} and nothing else; the machine divides by its
 * speed, so copper-cable's 0.5 s is observed at 1 s, as a player's first assembler shows it in
 * Factorio. 75 kW is 3,750 J a tick, which is 37.5 FE -- not a whole number, so the energy is
 * priced <b>per craft</b> and spread over its ticks so that the sum is exact, the way the Steam
 * Engine carries its fraction (#292) rather than flooring it away.
 *
 * <p>Oritech's addons stay live on top, with Oritech's own meaning: the speed multiplier scales the
 * duration (below 1 is faster) and leaves a craft's cost alone, so a faster machine draws harder;
 * the efficiency multiplier scales the cost and leaves the time alone. Idle draw is not modelled
 * (ADR-0029, {@code excluded}).
 *
 * <p>Pure: no Minecraft types.
 */
public final class AssemblingMachineSpec {

    /** {@code assembling-machine-1}'s {@code crafting_speed}, from {@code machine.json}. */
    static final double CRAFTING_SPEED = 0.5;

    /** {@code assembling-machine-1}'s {@code energy_usage} in watts, from {@code machine.json}. */
    static final long WATTS = 75_000L;

    /** {@code assembling-machine-1}'s {@code crafting_categories}, from {@code machine.json}. */
    private static final Set<String> CRAFTING_CATEGORIES = Set.of("crafting", "advanced-crafting");

    private static final long TICKS_PER_SECOND = 20L;

    private static final long JOULES_PER_FE = 100L;

    private AssemblingMachineSpec() {
    }

    /** Whether this tier crafts a recipe of Factorio {@code category}. */
    public static boolean crafts(String category) {
        return CRAFTING_CATEGORIES.contains(category);
    }

    /**
     * The recipe's ticks as this machine observes them, under Oritech's speed multiplier. Never
     * zero: a craft the divisor would round away still costs the tick it takes to run.
     */
    public static int durationTicks(int recipeTicks, float speedMultiplier) {
        return Math.max(1, (int) Math.ceil(recipeTicks / CRAFTING_SPEED * speedMultiplier - 1e-6));
    }

    /**
     * FE for one whole craft: {@code energy_usage} over the unmodified duration, scaled by Oritech's
     * efficiency multiplier. The speed multiplier is deliberately absent -- it moves the rate, not the
     * amount.
     */
    public static long fePerCraft(int recipeTicks, float efficiencyMultiplier) {
        double joules = WATTS / (double) TICKS_PER_SECOND * (recipeTicks / CRAFTING_SPEED);
        return Math.round(joules / JOULES_PER_FE * efficiencyMultiplier);
    }

    /**
     * The FE the tick at {@code progress} draws: the share of {@code totalFe} that tick completes,
     * so a craft's ticks sum to its cost exactly and a tick past the end draws nothing.
     */
    public static long feForTick(int progress, int durationTicks, long totalFe) {
        if (progress < 0 || progress >= durationTicks) {
            return 0L;
        }
        return share(progress + 1, durationTicks, totalFe) - share(progress, durationTicks, totalFe);
    }

    private static long share(int ticks, int durationTicks, long totalFe) {
        return totalFe * ticks / durationTicks;
    }
}
