package com.factoryworks.core.energy;

/**
 * One tick of an Electric Network's books (ADR-0062), in Factorio's order.
 *
 * <ol>
 *   <li>Consumers are fed from generators first.</li>
 *   <li>What generators cannot cover, accumulators discharge to cover.</li>
 *   <li>Generator output nobody consumed charges accumulators. An accumulator never charges from
 *       another accumulator: that would be energy moved in a circle at no one's request.</li>
 * </ol>
 *
 * <p>A shortfall is water-filled across consumers ({@link EnergyShare#waterFill}). Generators and
 * accumulators instead share their part in proportion to what each offered, which is Factorio's
 * rule for generators and keeps one engine from running flat out while its neighbour idles.
 *
 * <p>Energy is conserved exactly: what is drawn from generators and accumulators equals what is
 * granted to consumers and accumulators, to the FE.
 *
 * <p>Pure: no Minecraft types.
 */
public final class NetworkBalance {

    /** Every flow, index-aligned with the arrays handed to {@link #settle}. */
    public record Settlement(long[] generatorDraws, long[] accumulatorDischarges,
                             long[] accumulatorCharges, long[] consumerGrants) {
    }

    private NetworkBalance() {
    }

    /**
     * @param generatorOffers    FE each generator could give this tick
     * @param accumulatorOffers  FE each accumulator could give this tick
     * @param accumulatorRooms   FE each accumulator could take this tick
     * @param consumerDemands    FE each consumer asked for this tick
     */
    public static Settlement settle(long[] generatorOffers, long[] accumulatorOffers,
                                    long[] accumulatorRooms, long[] consumerDemands) {
        long demand = sum(consumerDemands);
        long generation = sum(generatorOffers);

        long fromGenerators = Math.min(demand, generation);
        long fromAccumulators = Math.min(demand - fromGenerators, sum(accumulatorOffers));
        long charge = Math.min(generation - fromGenerators, sum(accumulatorRooms));

        return new Settlement(
                EnergyShare.proportional(fromGenerators + charge, generatorOffers),
                EnergyShare.proportional(fromAccumulators, accumulatorOffers),
                EnergyShare.proportional(charge, accumulatorRooms),
                EnergyShare.waterFill(fromGenerators + fromAccumulators, consumerDemands));
    }

    private static long sum(long[] values) {
        long total = 0L;
        for (long v : values) {
            total += Math.max(0L, v);
        }
        return total;
    }
}
