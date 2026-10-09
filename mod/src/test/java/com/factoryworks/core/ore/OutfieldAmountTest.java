package com.factoryworks.core.ore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** An outfield disc's per-block amount (ADR-0045 as amended by #317, #319). */
class OutfieldAmountTest {

    private static final OutfieldLaw IRON = OutfieldLaw.of(OreResource.IRON);

    @Test
    void theDiscsBlocksShareOneAmountThatAddsUpToTheDiscsTotal() {
        OutfieldDisc disc = new OutfieldDisc(OreResource.IRON, 1.5, 1200, 3000, 400);
        double total = IRON.quantity(1.5, disc.distance()) * IRON.richness(disc.distance());

        long dealt = (long) disc.amountPerBlock() * disc.blockCount();

        assertTrue(dealt <= total && dealt > total - disc.blockCount(),
                "the disc holds its total less the floor's remainder: " + dealt + " of " + total);
    }

    @Test
    void theAmountIsReadAtTheDiscCentresDistanceFromOrigin() {
        OutfieldDisc east = new OutfieldDisc(OreResource.IRON, 1.0, 1000, 5000, 0);
        OutfieldDisc diagonal = new OutfieldDisc(OreResource.IRON, 1.0, 1000, 3000, 4000);

        assertEquals(5000, diagonal.distance(), 1e-9);
        assertEquals(east.amountPerBlock(), diagonal.amountPerBlock());
        // 1e6 / 2.5 × 20 × (1000 + 5000) / 2600 over 1000 blocks
        assertEquals(18_461, east.amountPerBlock());
    }

    @Test
    void theSpotStopsGrowingAt1600AndRichnessKeepsRisingWithNoCap() {
        assertEquals(IRON.quantity(1.0, 1600), IRON.quantity(1.0, 5000), 1e-6);
        assertEquals(IRON.quantity(1.0, 1600), IRON.quantity(1.0, 1_000_000), 1e-6);
        assertTrue(IRON.quantity(1.0, 1599) < IRON.quantity(1.0, 1600));

        assertEquals(1.0, IRON.richness(1600), 1e-9);
        assertEquals((1000 + 1_000_000) / 2600.0, IRON.richness(1_000_000), 1e-9);

        OutfieldDisc near = new OutfieldDisc(OreResource.IRON, 1.0, 1000, 10_000, 0);
        OutfieldDisc far = new OutfieldDisc(OreResource.IRON, 1.0, 1000, 1_000_000, 0);
        assertTrue(far.amountPerBlock() > 90 * near.amountPerBlock(),
                "richness is linear in distance forever");
    }

    @Test
    void nothingIsDealtInsideThePlacementRadius() {
        assertEquals(0.0, IRON.quantity(2.0, 150), 1e-9);
        assertTrue(IRON.quantity(2.0, 151) > 0);
    }

    @Test
    void uraniumTakesItsOwnLaw() {
        OutfieldLaw uranium = OutfieldLaw.of(OreResource.URANIUM);

        // 1e6 / 1.25 × 0.9 × 2
        assertEquals(1_440_000, uranium.quantity(1.0, 1600), 1e-6);
        assertEquals(1_440, new OutfieldDisc(OreResource.URANIUM, 1.0, 1000, 1600, 0).amountPerBlock());
    }
}
