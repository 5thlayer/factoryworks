package com.factoryworks.core.fluid;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.factoryworks.core.fluid.OffshorePumpSiting.Neighbour.DRY;
import static com.factoryworks.core.fluid.OffshorePumpSiting.Neighbour.FLOWING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ADR-0050's predicate, asserted away from any world.
 *
 * <p>The rule the whole design rests on is one sentence -- one adjacent block whose fluid state is
 * a source of a pumpable fluid -- and the reason it is safe is not in this class at all: it is that
 * the pack never creates a source, so every source in the world is one worldgen or a structure
 * placed. What these tests hold is that the predicate stays that sentence. A size test or a biome
 * test creeping in here is the rejected design of ADR-0050 arriving by the back door, and both were
 * rejected for making water something the player hunts for.
 *
 * <p>#256 added the fluid: the pump emits what it was sited on, and only a whitelisted fluid is
 * pumpable, so a body's new fluid is a row added rather than a refusal removed.
 */
class OffshorePumpSitingTest {

    private static final OffshorePumpSiting.Neighbour WATER = OffshorePumpSiting.Neighbour.source("minecraft:water");
    private static final OffshorePumpSiting.Neighbour LAVA = OffshorePumpSiting.Neighbour.source("minecraft:lava");
    private static final OffshorePumpSiting.Neighbour OIL = OffshorePumpSiting.Neighbour.source("factoryworks:crude_oil");

    @Test
    @DisplayName("one adjacent water source is enough, whatever else surrounds the pump")
    void oneSourceIsEnough() {
        assertEquals(Optional.of("minecraft:water"), OffshorePumpSiting.site(List.of(WATER)));
        assertEquals(Optional.of("minecraft:water"), OffshorePumpSiting.site(List.of(DRY, DRY, WATER, DRY)));
        assertTrue(OffshorePumpSiting.accepts(List.of(FLOWING, WATER)));
    }

    @Test
    @DisplayName("lava is pumpable and pumps lava -- Vulcanus's offshore pump")
    void lavaIsPumpable() {
        assertEquals(Optional.of("minecraft:lava"), OffshorePumpSiting.site(List.of(DRY, LAVA, FLOWING)));
    }

    @Test
    @DisplayName("a source not on the whitelist is refused -- oil must never be made infinite")
    void unlistedSourceIsRefused() {
        assertEquals(Optional.empty(), OffshorePumpSiting.site(List.of(OIL)));
        assertFalse(OffshorePumpSiting.accepts(List.of(OIL, DRY, FLOWING)));
    }

    @Test
    @DisplayName("an unlisted source beside a pumpable one does not spoil the site")
    void unlistedBesidePumpableIsIgnored() {
        assertEquals(Optional.of("minecraft:water"), OffshorePumpSiting.site(List.of(OIL, WATER)));
    }

    @Test
    @DisplayName("the whitelist is water and lava, and nothing else yet")
    void whitelistIsWaterAndLava() {
        assertEquals(List.of("minecraft:water", "minecraft:lava"), OffshorePumpSiting.PUMPABLE);
    }

    @Test
    @DisplayName("flowing fluid is refused -- it is what a placed outlet makes")
    void flowingIsRefused() {
        assertFalse(OffshorePumpSiting.accepts(List.of(FLOWING)),
                "flowing water is the one state ADR-0050's deferred outlet block may create, and "
                        + "admitting it here would make that block a source of water rather than a "
                        + "way to move it");
        assertFalse(OffshorePumpSiting.accepts(List.of(FLOWING, FLOWING, FLOWING, FLOWING)));
    }

    @Test
    @DisplayName("dry ground is refused, and so is nothing at all")
    void dryIsRefused() {
        assertFalse(OffshorePumpSiting.accepts(List.of(DRY)));
        assertFalse(OffshorePumpSiting.accepts(List.of()));
    }

    @Test
    @DisplayName("no minimum body size: a single source block is a valid site")
    void noMinimumBodySize() {
        assertTrue(OffshorePumpSiting.accepts(List.of(WATER)),
                "ADR-0050 rejected every minimum-size rule; one block is a site");
    }
}
