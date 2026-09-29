package com.factoryworks.core.smelting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Each fuel lasts as long in a burner furnace as in Factorio's 90 kW furnace (#510). The whole
 * ticks and the banked remainder are typed rather than derived from {@link FurnaceTier}.
 */
class FurnaceFuelBurnTest {

    @ParameterizedTest(name = "{0} on {1}")
    @CsvSource({
            "wood,        STONE, 2000000,   444,   2000",
            "coal,        STONE, 4000000,   888,   4000",
            "solid-fuel,  STONE, 12000000,  2666,  3000",
            "rocket-fuel, STONE, 100000000, 22222, 1000",
            "coal,        STEEL, 4000000,   888,   4000",
    })
    void oneItemBurnsForFactoriosDuration(String fuel, FurnaceTier tier, long joules,
            int wholeTicks, long banked) {
        FuelBuffer buffer = new FuelBuffer();
        buffer.light(joules);
        int ticks = 0;
        while (buffer.drawTick(tier.joulesPerTick())) {
            ticks++;
        }
        assertEquals(wholeTicks, ticks);
        assertEquals(banked, buffer.storedJoules());
    }
}
