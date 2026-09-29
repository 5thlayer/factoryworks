package com.planetaryfactory.core.fluid;

import com.planetaryfactory.core.energy.ForgeEnergy;
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

    private static final int FACTORIO_TICKS_PER_SECOND = 60;
    private static final int MINECRAFT_TICKS_PER_SECOND = 20;

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
                maxPowerOutputWatts / MINECRAFT_TICKS_PER_SECOND / ForgeEnergy.JOULES_PER_FE;
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

    /** What a row draws from its tank this tick, in whole millibuckets, and what it leaves owed. */
    public record Request(int steam, Carry carry) {
    }

    /** What one tick burnt and made, in whole units, and what it leaves owed. */
    public record Tick(int steam, long energy, Carry carry) {
    }

    /** One engine's steam at peak speed, in mB a second. */
    public double steamPerSecondAtPeak() {
        return steamPerTickAtPeak * MINECRAFT_TICKS_PER_SECOND;
    }

    /** One engine's output at peak speed, in FE a tick. */
    public double energyPerTickAtPeak() {
        return energyPerTickAtPeak;
    }

    /** A row's master steam tank: Factorio's steam box per engine, not Oritech's 8,000 mB. */
    public int tankCapacity(int rowLength) {
        return fluidBoxVolume * rowLength;
    }

    /** A row's FE buffer: one tick of peak output per engine, so it hides no outage. */
    public long bufferCapacity(int rowLength) {
        return Math.round(energyPerTickAtPeak) * rowLength;
    }

    /**
     * How much steam a row asks for this tick, before anything is drawn from the tank.
     *
     * <p>{@code energyRoom} is what the FE buffer can still take. The buffer is one tick of output,
     * so a pole that drew less than that last tick leaves less room than a full burn makes, and the
     * request is cut: steam burnt into a full buffer is steam spent for nothing. The cut keeps every
     * millibucket whose energy <em>starts</em> inside the room, and {@link #burn} carries the part of
     * the last one that overshoots. Cutting to what fits whole instead held a row drained dry by a
     * pole below its rate every tick: a millibucket is 300 FE and the carried half millibucket's
     * tick never fits (#292). A cut request owes no steam forward, or an engine held back for a
     * minute would owe a burst.
     */
    public Request request(double speed, int rowLength, Carry carry, long energyRoom) {
        double exact = steamPerTickAtPeak * (speed / PEAK_SPEED) * rowLength + carry.steam();
        int whole = (int) Math.floor(exact + EPSILON);
        double perUnit = energyPerUnit(speed);
        int fits = perUnit <= 0.0
                ? whole
                : (int) Math.ceil((energyRoom - carry.energy()) / perUnit - EPSILON);
        if (fits < whole) {
            return new Request(Math.max(0, fits), new Carry(0.0, carry.energy()));
        }
        return new Request(whole, new Carry(Math.max(0.0, exact - whole), carry.energy()));
    }

    /** What one millibucket is worth at {@code speed}, in FE. */
    private double energyPerUnit(double speed) {
        double ratio = efficiency.applyAsDouble(speed) / efficiency.applyAsDouble(PEAK_SPEED);
        return energyPerTickAtPeak / steamPerTickAtPeak * ratio;
    }

    /**
     * What {@code steam} millibuckets actually drawn are worth at {@code speed}, of which no more
     * than {@code energyRoom} is paid out this tick; the rest is owed.
     *
     * <p>Split from {@link #request} because the tank may hold less than was asked for, and the
     * energy is owed on what was burnt, not on what was wanted.
     */
    public Tick burn(int steam, double speed, Carry carry, long energyRoom) {
        double exact = steam * energyPerUnit(speed) + carry.energy();
        long whole = Math.max(0L, Math.min((long) Math.floor(exact + EPSILON), energyRoom));
        return new Tick(steam, whole, new Carry(carry.steam(), Math.max(0.0, exact - whole)));
    }

    /** {@link #request} then {@link #burn}, when the tank covers the whole request. */
    public Tick tick(double speed, int rowLength, Carry carry, long energyRoom) {
        Request asked = request(speed, rowLength, carry, energyRoom);
        return burn(asked.steam(), speed, asked.carry(), energyRoom);
    }
}
