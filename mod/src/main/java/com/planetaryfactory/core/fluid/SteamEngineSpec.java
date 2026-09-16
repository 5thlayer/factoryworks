package com.planetaryfactory.core.fluid;

import java.util.function.DoubleUnaryOperator;

/**
 * The Steam Engine's arithmetic, calibrated to Factorio (#282, ADR-0062).
 *
 * <p>The engine is Oritech's block, and Oritech's shape is kept: the speed follows the steam tank's
 * fill, the draw is proportional to that speed and to the row's length, and what a millibucket is
 * worth follows Oritech's efficiency curve over the speed. What this class replaces is the scale.
 * At the curve's peak, speed {@value #PEAK_SPEED}, one engine burns Factorio's own steam draw and
 * makes Factorio's own 900 kW; off the peak it burns proportionally and is worth less per unit.
 *
 * <p>No rate is typed here. The draw, the output and the steam box are read off the prototype by
 * {@link SteamChainCorpus}; the curve is handed in, because it is Oritech's and lives in its jar.
 *
 * <p><b>The trap is the half millibucket.</b> Factorio's 0.5 units a tick at 60 ticks a second is
 * 30 mB/s, which is 1.5 mB a Minecraft tick. A tank moves whole millibuckets, and Oritech's own
 * {@code (long)} cast floors 1.5 to 1: an engine at two thirds of its prototype with nothing on the
 * gauge to say so. The fraction is carried from tick to tick in a {@link Carry} instead, so the
 * rate is exact over any whole number of seconds. Energy is carried the same way.
 *
 * <p>Pure: no Minecraft types.
 */
public final class SteamEngineSpec {

    /** Where Oritech's efficiency curve peaks, and where the prototype's rates are met exactly. */
    public static final double PEAK_SPEED = 7.0;

    /**
     * Oritech returns 90% of spent steam as water; the pack returns none (ADR-0062). The Offshore
     * Pump never runs dry (ADR-0050), so a return would only add a stall on a full water tank.
     */
    public static final int WATER_RETURNED = 0;

    private static final int FACTORIO_TICKS_PER_SECOND = 60;
    private static final int MINECRAFT_TICKS_PER_SECOND = 20;

    /** ADR-0060's rate, the one the Electric Furnace's 90 FE/t is on too. */
    private static final double JOULES_PER_FE = 100.0;

    /** Guards a floor against a sum like 0.1 + 0.2 landing a hair under a whole number. */
    private static final double EPSILON = 1e-9;

    private final double steamPerTickAtPeak;
    private final double energyPerTickAtPeak;
    private final int fluidBoxVolume;
    private final DoubleUnaryOperator efficiency;

    SteamEngineSpec(double fluidUsagePerFactorioTick, double maxPowerOutputWatts,
            int fluidBoxVolume, DoubleUnaryOperator efficiency) {
        this.steamPerTickAtPeak =
                fluidUsagePerFactorioTick * FACTORIO_TICKS_PER_SECOND / MINECRAFT_TICKS_PER_SECOND;
        this.energyPerTickAtPeak =
                maxPowerOutputWatts / MINECRAFT_TICKS_PER_SECOND / JOULES_PER_FE;
        this.fluidBoxVolume = fluidBoxVolume;
        this.efficiency = efficiency;
    }

    public static SteamEngineSpec fromCorpus(SteamChainCorpus corpus, DoubleUnaryOperator efficiency) {
        return new SteamEngineSpec(corpus.steamEngineFluidUsagePerTick(),
                corpus.steamEngineMaxPowerOutput(), corpus.steamEngineFluidBoxVolume(), efficiency);
    }

    /** The fractions of a millibucket and of an FE owed from earlier ticks. */
    public record Carry(double steam, double energy) {
        public static final Carry NONE = new Carry(0.0, 0.0);
    }

    /** What one tick burns and makes, in whole units, and what it leaves owed. */
    public record Tick(int steam, long energy, Carry carry) {
    }

    /** A row's master steam tank: Factorio's steam box per engine, not Oritech's 8,000 mB. */
    public int tankCapacity(int rowLength) {
        return fluidBoxVolume * rowLength;
    }

    /** A row's FE buffer: one tick of peak output per engine, so it hides no outage. */
    public long bufferCapacity(int rowLength) {
        return Math.round(energyPerTickAtPeak) * rowLength;
    }

    /** How much steam a row asks for this tick, before anything is drawn from the tank. */
    public Tick request(double speed, int rowLength, Carry carry) {
        double exact = steamPerTickAtPeak * (speed / PEAK_SPEED) * rowLength + carry.steam();
        int whole = (int) Math.floor(exact + EPSILON);
        return new Tick(whole, 0L, new Carry(Math.max(0.0, exact - whole), carry.energy()));
    }

    /**
     * What {@code steam} millibuckets actually drawn are worth at {@code speed}.
     *
     * <p>Split from {@link #request} because the tank may hold less than was asked for, and the
     * energy is owed on what was burnt, not on what was wanted.
     */
    public Tick burn(int steam, double speed, Carry carry) {
        double perUnitAtPeak = energyPerTickAtPeak / steamPerTickAtPeak;
        double ratio = efficiency.applyAsDouble(speed) / efficiency.applyAsDouble(PEAK_SPEED);
        double exact = steam * perUnitAtPeak * ratio + carry.energy();
        long whole = (long) Math.floor(exact + EPSILON);
        return new Tick(steam, whole, new Carry(carry.steam(), Math.max(0.0, exact - whole)));
    }

    /** {@link #request} then {@link #burn}, when the tank covers the whole request. */
    public Tick tick(double speed, int rowLength, Carry carry) {
        Tick asked = request(speed, rowLength, carry);
        return burn(asked.steam(), speed, asked.carry());
    }
}
