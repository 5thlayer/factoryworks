package com.factoryworks.core.energy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Which poles are one Electric Network (ADR-0062): linked when within wire reach, and linked
 * transitively. The graph is rebuilt from the poles standing, so a split is the same question as a
 * merge asked of a smaller set.
 */
class PoleLinksTest {

    private static PoleLinks.Pole small(int x, int y, int z) {
        return new PoleLinks.Pole(x, y, z, PoleTier.SMALL);
    }

    @Test
    void polesWithinReachAreOneNetwork() {
        int[] network = PoleLinks.networks(List.of(small(0, 0, 0), small(7, 0, 0)));
        assertEquals(network[0], network[1]);
    }

    @Test
    void polesBeyondReachAreTwoNetworks() {
        int[] network = PoleLinks.networks(List.of(small(0, 0, 0), small(8, 0, 0)));
        assertEquals(2, java.util.Arrays.stream(network).distinct().count());
    }

    @Test
    void theShorterReachOfTheTwoDecides() {
        // A substation reaches 18, a small pole 7.5: at 12 apart neither end may be the long one.
        int[] network = PoleLinks.networks(List.of(
                new PoleLinks.Pole(0, 0, 0, PoleTier.SUBSTATION), small(12, 0, 0)));
        assertEquals(2, java.util.Arrays.stream(network).distinct().count());
    }

    @Test
    void reachIsMeasuredInThreeDimensions() {
        // 6 across and 5 up is 7.8 blocks: out of a small pole's 7.5.
        int[] network = PoleLinks.networks(List.of(small(0, 0, 0), small(6, 5, 0)));
        assertEquals(2, java.util.Arrays.stream(network).distinct().count());
    }

    @Test
    void aChainIsOneNetworkEvenWhereItsEndsAreOutOfReach() {
        int[] network = PoleLinks.networks(List.of(small(0, 0, 0), small(7, 0, 0), small(14, 0, 0)));
        assertEquals(network[0], network[2]);
    }

    @Test
    void removingTheMiddleOfAChainSplitsIt() {
        int[] network = PoleLinks.networks(List.of(small(0, 0, 0), small(14, 0, 0)));
        assertEquals(2, java.util.Arrays.stream(network).distinct().count());
    }

    @Test
    void networkIdsAreDenseFromZero() {
        int[] network = PoleLinks.networks(List.of(small(0, 0, 0), small(100, 0, 0), small(7, 0, 0)));
        assertEquals(0, network[0]);
        assertEquals(1, network[1]);
        assertEquals(0, network[2]);
    }
}
