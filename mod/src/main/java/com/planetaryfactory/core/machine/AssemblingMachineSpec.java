package com.planetaryfactory.core.machine;

import com.planetaryfactory.core.energy.ForgeEnergy;

/**
 * A crafting machine's rate from its {@link MachineSpec} (#328, #295, #489, ADR-0029): its
 * {@code crafting_speed} and {@code energy_usage}, on ADR-0060's 1 FE = 100 J.
 *
 * <p>The recipe carries {@code energy_required * 20} and nothing else; the machine divides by its
 * speed, so copper-cable's 0.5 s is observed at 1 s on tier 1, as a player's first assembler shows
 * it in Factorio. 75 kW is 37.5 FE a tick and 375 kW is 187.5 -- not whole numbers, so the energy
 * is priced <b>per craft</b> and spread over its ticks so that the sum is exact, the way the Steam
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

    private static final long TICKS_PER_SECOND = 20L;

    private AssemblingMachineSpec() {
    }

    /**
     * The recipe's ticks as {@code spec}'s machine observes them, under Oritech's speed multiplier. Never
     * zero: a craft the divisor would round away still costs the tick it takes to run.
     */
    public static int durationTicks(MachineSpec spec, int recipeTicks, float speedMultiplier) {
        return Math.max(1, (int) Math.ceil(recipeTicks / spec.craftingSpeed() * speedMultiplier - 1e-6));
    }

    /**
     * FE for one whole craft: {@code energy_usage} over the unmodified, unrounded duration, scaled by
     * Oritech's efficiency multiplier. The speed multiplier is deliberately absent -- it moves the
     * rate, not the amount.
     */
    public static long fePerCraft(MachineSpec spec, int recipeTicks, float efficiencyMultiplier) {
        double joules = spec.watts() / (double) TICKS_PER_SECOND * (recipeTicks / spec.craftingSpeed());
        return Math.round(joules / ForgeEnergy.JOULES_PER_FE * efficiencyMultiplier);
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

    /** The average draw over a craft, in tenths of an FE a tick: 75 kW is 37.5 FE/t. 0 with no craft. */
    public static long drawTenths(long totalFe, int durationTicks) {
        return durationTicks <= 0 ? 0L : Math.round(totalFe * 10.0 / durationTicks);
    }

    private static long share(int ticks, int durationTicks, long totalFe) {
        return totalFe * ticks / durationTicks;
    }
}
