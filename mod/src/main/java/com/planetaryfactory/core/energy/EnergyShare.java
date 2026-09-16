package com.planetaryfactory.core.energy;

/**
 * How a pole splits a shortfall across the machines in its area.
 *
 * <p>First-come-first-served would be wrong in a way the player cannot see or plan around: the
 * machines the scan happens to reach first would run and the rest would starve, permanently, on an
 * iteration order that is an implementation detail of {@link SupplyArea#forEachOffset}. Factorio
 * shares a shortfall out instead, and so does this.
 *
 * <p>Water-filling: everyone gets an equal cut, and a machine whose demand is below that cut takes
 * only what it asked for and its spare goes back into the pot for the machines that can still use
 * it. Repeat until the pot is empty or every demand is met.
 *
 * <p>Pure: no Minecraft types.
 */
public final class EnergyShare {

    private EnergyShare() {
    }

    /**
     * Splits {@code available} across {@code demands}, granting no machine more than it asked and
     * handing out the whole pot whenever the demands can absorb it.
     *
     * @return a grant per demand, index-aligned with the input; the input is not modified
     */
    public static long[] waterFill(long available, long[] demands) {
        long[] grants = new long[demands.length];
        if (demands.length == 0 || available <= 0L) {
            return grants;
        }

        long pot = available;
        boolean[] satisfied = new boolean[demands.length];
        int outstanding = 0;
        for (int i = 0; i < demands.length; i++) {
            if (demands[i] > 0L) {
                outstanding++;
            } else {
                satisfied[i] = true;
            }
        }

        while (pot > 0L && outstanding > 0) {
            long cut = pot / outstanding;
            if (cut == 0L) {
                // Fewer FE left than there are machines wanting them. An equal cut is impossible,
                // so the remainder goes one FE at a time rather than evaporating.
                for (int i = 0; i < demands.length && pot > 0L; i++) {
                    if (!satisfied[i]) {
                        grants[i]++;
                        pot--;
                        if (grants[i] >= demands[i]) {
                            satisfied[i] = true;
                            outstanding--;
                        }
                    }
                }
                break;
            }

            for (int i = 0; i < demands.length; i++) {
                if (satisfied[i]) {
                    continue;
                }
                long want = demands[i] - grants[i];
                long give = Math.min(cut, want);
                grants[i] += give;
                pot -= give;
                if (grants[i] >= demands[i]) {
                    satisfied[i] = true;
                    outstanding--;
                }
            }
        }

        return grants;
    }

    /**
     * Splits {@code total} across {@code weights} in proportion to each, never granting more than a
     * weight, with the integer remainder handed out by largest fractional part so no FE is lost.
     * {@code total} must not exceed the sum of the weights.
     *
     * @return a share per weight, index-aligned with the input
     */
    public static long[] proportional(long total, long[] weights) {
        long[] shares = new long[weights.length];
        long sum = 0L;
        for (long w : weights) {
            sum += Math.max(0L, w);
        }
        if (total <= 0L || sum <= 0L) {
            return shares;
        }
        total = Math.min(total, sum);
        long given = 0L;
        double[] remainders = new double[weights.length];
        for (int i = 0; i < weights.length; i++) {
            long w = Math.max(0L, weights[i]);
            double exact = (double) total * w / sum;
            shares[i] = Math.min(w, (long) Math.floor(exact));
            remainders[i] = exact - shares[i];
            given += shares[i];
        }
        while (given < total) {
            int best = -1;
            for (int i = 0; i < weights.length; i++) {
                if (shares[i] < Math.max(0L, weights[i]) && (best < 0 || remainders[i] > remainders[best])) {
                    best = i;
                }
            }
            shares[best]++;
            remainders[best] = -1.0;
            given++;
        }
        return shares;
    }
}
