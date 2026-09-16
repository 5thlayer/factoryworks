package com.planetaryfactory.core.energy;

/**
 * The creative pole's books: always full, and a drain takes nothing out (#272).
 *
 * <p>This is the whole of what the creative pole changes. It is a subclass rather than a flag on
 * {@link EnergyLedger} so that the shipped ledger's every method stays the one that ships, with no
 * branch in it that only a dev tool takes.
 *
 * <h2>Why the capacity is not {@code Long.MAX_VALUE}</h2>
 *
 * <p>The pole hands {@link #availableFe()} to {@link EnergyShare#waterFill} as the pot, and the
 * tick sums demands beside it. A saturated pot would overflow the first time anything added to it,
 * and an overflowed pot is negative, which reads to {@code waterFill} as <em>no energy at all</em>
 * -- a creative pole that powers nothing, for a reason no line of it mentions. The figure here is
 * chosen to be past any demand a supply area can state and still a long way from the edge: a
 * substation covers 18x18x5 = 1620 positions, a receiver's demand is an {@code int} insert, so
 * 1620 x {@link Integer#MAX_VALUE} is the most an area can ask for in a tick.
 *
 * <p>Pure: no Minecraft types, like the ledger it extends.
 */
public final class InfiniteEnergyLedger extends EnergyLedger {

    /**
     * A petaFE: past 1620 x {@link Integer#MAX_VALUE} (about 3.5e12) by three orders of magnitude,
     * and under a thousandth of {@link Long#MAX_VALUE}.
     */
    public static final long CREATIVE_FE = 1_000_000_000_000_000L;

    public InfiniteEnergyLedger() {
        super(CREATIVE_FE);
        // Reads as dead code and is not: every method that can see the base class's private
        // `storedFe` is overridden below, so nothing reads this today. It is the hedge for the
        // next method added to EnergyLedger, which would otherwise answer "empty" here.
        super.setStoredFe(CREATIVE_FE);
    }

    /** Full, always: nothing the pole does may empty it. */
    @Override
    public long storedFe() {
        return capacityFe();
    }

    @Override
    public long availableFe() {
        return capacityFe();
    }

    /**
     * Hands out everything asked for and debits nothing.
     *
     * <p>Reporting the full amount rather than zero matters: the pole's tick drains exactly what
     * the receivers took, and the Jade line reads what was delivered. A drain that reported less
     * than it gave would show a fed machine beside a pole claiming it delivered nothing.
     */
    @Override
    public long drainFe(long maxFe) {
        return Math.max(0L, maxFe);
    }

    /**
     * Accepts anything offered and stores none of it: the energy is <em>destroyed</em>.
     *
     * <p>Reporting the full amount rather than zero keeps a connector from being told its energy
     * was refused, which is the shape a grid mod backs up or logs on. The cost is that a real
     * generator wired into a creative pole is voided rather than buffered, which is the right
     * trade for a block that is already an infinite source.
     */
    @Override
    public long simulateReceiveFe(long fe) {
        return Math.max(0L, fe);
    }

    @Override
    public long receiveFe(long fe) {
        return simulateReceiveFe(fe);
    }

    /** The save has no say. A creative pole loaded out of a world saved empty is still full. */
    @Override
    public void setStoredFe(long fe) {
    }
}
