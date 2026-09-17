package com.planetaryfactory.core.fluid;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.planetaryfactory.core.fluid.OffshorePumpSiting.Neighbour.DRY;
import static com.planetaryfactory.core.fluid.OffshorePumpSiting.Neighbour.FLOWING;
import static com.planetaryfactory.core.fluid.OffshorePumpSiting.Neighbour.LAVA_SOURCE;
import static com.planetaryfactory.core.fluid.OffshorePumpSiting.Neighbour.OTHER_SOURCE;
import static com.planetaryfactory.core.fluid.OffshorePumpSiting.Neighbour.WATER_SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ADR-0050's predicate, asserted away from any world.
 *
 * <p>The rule the whole design rests on is one sentence -- one adjacent block whose fluid state is
 * a source of an admitted fluid -- and the reason it is safe is not in this class at all: it is
 * that the pack never creates a source, so every source in the world is one worldgen or a structure
 * placed. What these tests hold is that the predicate stays that sentence. A size test or a biome
 * test creeping in here is the rejected design of ADR-0050 arriving by the back door, and both were
 * rejected for making water something the player hunts for.
 *
 * <p>#256 added the fluid: the pump emits what it was sited on, so the verdict names it, and a site
 * that is not exactly one admitted fluid names why it is refused.
 */
class OffshorePumpSitingTest {

    @Test
    @DisplayName("one adjacent water source is enough, whatever else surrounds the pump")
    void oneSourceIsEnough() {
        assertEquals(OffshorePumpSiting.Verdict.WATER, OffshorePumpSiting.site(List.of(WATER_SOURCE)));
        assertEquals(OffshorePumpSiting.Verdict.WATER,
                OffshorePumpSiting.site(List.of(DRY, DRY, WATER_SOURCE, DRY)));
        assertTrue(OffshorePumpSiting.accepts(List.of(FLOWING, WATER_SOURCE)));
    }

    @Test
    @DisplayName("lava is admitted and pumps lava -- Vulcanus's offshore pump")
    void lavaIsAdmitted() {
        assertEquals(OffshorePumpSiting.Verdict.LAVA, OffshorePumpSiting.site(List.of(LAVA_SOURCE)));
        assertEquals(OffshorePumpSiting.Verdict.LAVA,
                OffshorePumpSiting.site(List.of(DRY, LAVA_SOURCE, LAVA_SOURCE, FLOWING)));
    }

    @Test
    @DisplayName("a source of any other fluid is refused -- oil must never be made infinite")
    void otherSourceIsRefused() {
        assertEquals(OffshorePumpSiting.Verdict.NO_SOURCE, OffshorePumpSiting.site(List.of(OTHER_SOURCE)));
        assertFalse(OffshorePumpSiting.accepts(List.of(OTHER_SOURCE, DRY, FLOWING)));
    }

    @Test
    @DisplayName("a non-admitted source beside an admitted one does not spoil the site")
    void otherSourceBesideAdmittedIsIgnored() {
        assertEquals(OffshorePumpSiting.Verdict.WATER,
                OffshorePumpSiting.site(List.of(OTHER_SOURCE, WATER_SOURCE)));
    }

    @Test
    @DisplayName("water and lava together are refused rather than picked between")
    void mixedIsRefused() {
        assertEquals(OffshorePumpSiting.Verdict.MIXED,
                OffshorePumpSiting.site(List.of(WATER_SOURCE, LAVA_SOURCE)));
        assertFalse(OffshorePumpSiting.accepts(List.of(DRY, LAVA_SOURCE, WATER_SOURCE, WATER_SOURCE)));
    }

    @Test
    @DisplayName("flowing fluid is refused -- it is what a placed outlet makes")
    void flowingIsRefused() {
        assertEquals(OffshorePumpSiting.Verdict.NO_SOURCE, OffshorePumpSiting.site(List.of(FLOWING)),
                "flowing water is the one state ADR-0050's deferred outlet block may create, and "
                        + "admitting it here would make that block a source of water rather than a "
                        + "way to move it");
        assertFalse(OffshorePumpSiting.accepts(List.of(FLOWING, FLOWING, FLOWING, FLOWING)));
    }

    @Test
    @DisplayName("dry ground is refused, and so is nothing at all")
    void dryIsRefused() {
        assertFalse(OffshorePumpSiting.accepts(List.of(DRY)));
        assertEquals(OffshorePumpSiting.Verdict.NO_SOURCE, OffshorePumpSiting.site(List.of()));
    }

    @Test
    @DisplayName("no minimum body size: a single source block is a valid site")
    void noMinimumBodySize() {
        assertTrue(OffshorePumpSiting.accepts(List.of(WATER_SOURCE)),
                "ADR-0050 rejected every minimum-size rule; one block is a site");
    }
}
