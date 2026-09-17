package com.planetaryfactory.core.energy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A wire, not reach, joins two poles (ADR-0068): networks are built from the stored wires alone.
 */
class PoleNetworksFromWiresTest {

    private static PoleLinks.Pole small(int x, int y, int z) {
        return new PoleLinks.Pole(x, y, z, PoleTier.SMALL);
    }

    @Test
    void polesInReachButUnwiredAreTwoNetworks() {
        int[] network = PoleLinks.networks(List.of(small(0, 0, 0), small(7, 0, 0)), Set.of());
        assertEquals(2, java.util.Arrays.stream(network).distinct().count());
    }

    private static PoleLinks.Wire wire(int ax, int bx) {
        return new PoleLinks.Wire(new PoleLinks.Pos(ax, 0, 0), new PoleLinks.Pos(bx, 0, 0));
    }

    @Test
    void aWiredChainIsOneNetworkEvenWhereItsEndsAreOutOfReach() {
        int[] network = PoleLinks.networks(List.of(small(0, 0, 0), small(7, 0, 0), small(14, 0, 0)),
                Set.of(wire(0, 7), wire(7, 14)));
        assertEquals(network[0], network[2]);
    }

    @Test
    void cuttingTheOnlyWireBetweenTwoHalvesSplitsTheNetwork() {
        int[] network = PoleLinks.networks(List.of(small(0, 0, 0), small(7, 0, 0), small(14, 0, 0)),
                Set.of(wire(7, 14)));
        assertEquals(network[1], network[2]);
        assertEquals(2, java.util.Arrays.stream(network).distinct().count());
    }

    @Test
    void aWireIsTheSamePairWhicheverEndIsNamedFirst() {
        int[] network = PoleLinks.networks(List.of(small(0, 0, 0), small(7, 0, 0)), Set.of(wire(7, 0)));
        assertEquals(network[0], network[1]);
    }

    @Test
    void aWireToAPoleNoLongerStandingJoinsNothing() {
        int[] network = PoleLinks.networks(List.of(small(0, 0, 0)), Set.of(wire(0, 7)));
        assertEquals(1, network.length);
        assertEquals(0, network[0]);
    }
}
