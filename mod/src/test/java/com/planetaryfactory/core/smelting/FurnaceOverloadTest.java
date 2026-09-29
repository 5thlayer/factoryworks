package com.planetaryfactory.core.smelting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Typed from Factorio: a furnace's input stops at two smelts' worth for every pack smelt (#518). */
class FurnaceOverloadTest {

    private static final int IRON_PLATE_TICKS = 64;
    private static final int STEEL_PLATE_TICKS = 320;

    @Test
    void ironOreStopsAtTwoOnEveryTier() {
        for (FurnaceTier tier : FurnaceTier.values()) {
            assertEquals(2, tier.overloadRoom(1, IRON_PLATE_TICKS, 0), tier.name());
        }
    }

    @Test
    void theSteelSmeltHoldsTenPlates() {
        for (FurnaceTier tier : FurnaceTier.values()) {
            assertEquals(10, tier.overloadRoom(5, STEEL_PLATE_TICKS, 0), tier.name());
        }
    }

    @Test
    void roomShrinksByWhatTheSlotHolds() {
        assertEquals(3, FurnaceTier.STONE.overloadRoom(5, STEEL_PLATE_TICKS, 7));
        assertEquals(0, FurnaceTier.STONE.overloadRoom(1, IRON_PLATE_TICKS, 64));
    }

}
