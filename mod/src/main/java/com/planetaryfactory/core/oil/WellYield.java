package com.planetaryfactory.core.oil;

/**
 * What a Pumpjack gets from an oil well each cycle, and what the cycle takes off it (ADR-0081).
 *
 * <p>Crude is whole millibuckets, one per Factorio fluid unit, so a cycle's fraction is carried to
 * the next rather than floored away.
 */
public record WellYield(long normal, long minimum, long depletion, int amountPerCycle) {

    /** ADR-0081's cap on one cycle's crude. */
    public static final int MAX_PER_CYCLE = 1_000;

    /** The engine's second floor, stated only on the wiki's Crude oil page (ADR-0081). */
    private static final int FLOOR_PERCENT_OF_INITIAL = 20;

    /**
     * @param crude millibuckets this cycle
     * @param amount the well's amount after it
     * @param carry the fraction owed to the next cycle, in units of {@code 1/normal} mB
     */
    public record Cycle(int crude, long amount, long carry) {
    }

    public static WellYield fromCorpus() {
        OilCorpus corpus = OilCorpus.get();
        return new WellYield(corpus.normal(), corpus.minimum(), corpus.depletion(), corpus.amountPerCycle());
    }

    public double yield(long amount) {
        return (double) amount / normal;
    }

    public long floor(long initial) {
        return Math.max(minimum, initial * FLOOR_PERCENT_OF_INITIAL / 100);
    }

    public Cycle cycle(long amount, long initial, long carry) {
        long owed = amountPerCycle * amount + carry;
        long crude = owed / normal;
        long left = owed % normal;
        if (crude >= MAX_PER_CYCLE) {
            crude = MAX_PER_CYCLE;
            left = 0;
        }
        long floor = floor(initial);
        long after = amount <= floor ? amount : Math.max(floor, amount - depletion);
        return new Cycle((int) crude, after, left);
    }
}
