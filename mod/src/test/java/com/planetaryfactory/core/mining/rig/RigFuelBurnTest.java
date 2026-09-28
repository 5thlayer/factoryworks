package com.planetaryfactory.core.mining.rig;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.planetaryfactory.core.smelting.FuelBuffer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Each fuel lasts as long in the Burner Mining Drill as in Factorio's 150 kW drill (#510), at the
 * draw the block entity reads off the corpus row.
 */
class RigFuelBurnTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "wood,        2000000,   266,   5000",
            "coal,        4000000,   533,   2500",
            "solid-fuel,  12000000,  1600,  0",
            "rocket-fuel, 100000000, 13333, 2500",
    })
    void oneItemMinesForFactoriosDuration(String fuel, long joules, int wholeTicks, long banked) {
        long perTick = RigRate.joulesPerTick(RigCorpus.get().rowOf(RigTier.BURNER).energyUsage());
        FuelBuffer buffer = new FuelBuffer();
        buffer.light(joules);
        int ticks = 0;
        while (buffer.drawTick(perTick)) {
            ticks++;
        }
        assertEquals(wholeTicks, ticks);
        assertEquals(banked, buffer.storedJoules());
    }
}
