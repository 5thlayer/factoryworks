package com.planetaryfactory.core.energy;

/**
 * One tick of an Electric Network as a player reads it (#285): Factorio's hover summary.
 *
 * <p>Every flow is what the settle step <em>committed</em>, not what it planned, so an aborted tick
 * reads as nothing moved. Pure: no Minecraft types.
 *
 * @param produced     FE drawn from generators, creative poles included
 * @param delivered    FE granted to consumers
 * @param demanded     FE consumers asked for, whether or not it was there
 * @param charged      FE inserted into accumulators
 * @param discharged   FE extracted from accumulators
 * @param stored       FE held by the network's accumulators after the tick
 * @param capacity     FE the network's accumulators can hold
 * @param accumulators how many accumulators the network has
 * @param poles        how many poles are linked into the network
 */
public record NetworkReading(long produced, long delivered, long demanded, long charged,
                             long discharged, long stored, long capacity, int accumulators,
                             int poles) {

    public static final NetworkReading NONE = new NetworkReading(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0, 0);

    /**
     * Delivered over demanded, floored, so 99.99% reads 99 and a shortfall never shows as full. A
     * network nobody asks anything of is satisfied, and one that delivered anything at all reads
     * at least 1, so 0 always means nothing arrived.
     */
    public int satisfactionPercent() {
        if (demanded <= 0L) {
            return 100;
        }
        long percent = Math.min(100L, delivered * 100L / demanded);
        return (int) (delivered > 0L ? Math.max(1L, percent) : percent);
    }

    /** Positive while charging, negative while discharging. */
    public long accumulatorFlow() {
        return charged - discharged;
    }

    public boolean hasAccumulators() {
        return accumulators > 0;
    }
}
