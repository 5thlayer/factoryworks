package com.planetaryfactory.core.energy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * One tick of an Electric Network's books (ADR-0062): generators before accumulators, only
 * generator surplus charges, and generators share their load in proportion to what each can give.
 */
class NetworkBalanceTest {

    private static final long[] NONE = {};

    @Test
    void generatorsMeetDemandAndAccumulatorsAreUntouched() {
        NetworkBalance.Settlement s = NetworkBalance.settle(
                new long[]{450}, new long[]{150}, new long[]{0}, new long[]{90});
        assertArrayEquals(new long[]{90}, s.consumerGrants());
        assertArrayEquals(new long[]{90}, s.generatorDraws());
        assertArrayEquals(new long[]{0}, s.accumulatorDischarges());
    }

    @Test
    void accumulatorsCoverOnlyTheShortfall() {
        NetworkBalance.Settlement s = NetworkBalance.settle(
                new long[]{50}, new long[]{150}, new long[]{0}, new long[]{90});
        assertArrayEquals(new long[]{90}, s.consumerGrants());
        assertArrayEquals(new long[]{50}, s.generatorDraws());
        assertArrayEquals(new long[]{40}, s.accumulatorDischarges());
    }

    @Test
    void onlyGeneratorSurplusCharges() {
        NetworkBalance.Settlement s = NetworkBalance.settle(
                new long[]{450}, new long[]{0}, new long[]{150}, new long[]{90});
        assertArrayEquals(new long[]{150}, s.accumulatorCharges());
        assertArrayEquals(new long[]{240}, s.generatorDraws());
    }

    @Test
    void anAccumulatorNeverChargesFromAnother() {
        NetworkBalance.Settlement s = NetworkBalance.settle(
                NONE, new long[]{150, 0}, new long[]{0, 150}, NONE);
        assertArrayEquals(new long[]{0, 0}, s.accumulatorDischarges());
        assertArrayEquals(new long[]{0, 0}, s.accumulatorCharges());
    }

    @Test
    void generatorsShareLoadInProportionToWhatTheyCanGive() {
        NetworkBalance.Settlement s = NetworkBalance.settle(
                new long[]{300, 100}, NONE, NONE, new long[]{200});
        assertArrayEquals(new long[]{150, 50}, s.generatorDraws());
    }

    @Test
    void proportionalRemaindersAreNotLost() {
        NetworkBalance.Settlement s = NetworkBalance.settle(
                new long[]{100, 100, 100}, NONE, NONE, new long[]{100});
        assertEquals(100, java.util.Arrays.stream(s.generatorDraws()).sum());
    }

    @Test
    void aShortfallIsSharedAcrossConsumers() {
        NetworkBalance.Settlement s = NetworkBalance.settle(
                new long[]{100}, NONE, NONE, new long[]{90, 90});
        assertArrayEquals(new long[]{50, 50}, s.consumerGrants());
    }

    @Test
    void whatIsDrawnEqualsWhatIsDelivered() {
        NetworkBalance.Settlement s = NetworkBalance.settle(
                new long[]{70, 30}, new long[]{40, 40}, new long[]{0, 0}, new long[]{90, 45, 20});
        long in = java.util.Arrays.stream(s.generatorDraws()).sum()
                + java.util.Arrays.stream(s.accumulatorDischarges()).sum();
        long out = java.util.Arrays.stream(s.consumerGrants()).sum()
                + java.util.Arrays.stream(s.accumulatorCharges()).sum();
        assertEquals(in, out);
        assertEquals(155, out);
    }
}
