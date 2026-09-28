package com.planetaryfactory.core.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.planetaryfactory.core.smelting.FuelBuffer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Each fuel lasts as long in the Boiler as in Factorio's 1.8 MW boiler (#510), run through
 * {@link BoilerCycle} with the draw the block entity derives from the corpus. The corpus's
 * {@code effectivity} is asserted to be 1, since the block entity scales each lit item by it.
 */
class BoilerFuelBurnTest {

    private static final SteamChainCorpus CORPUS = SteamChainCorpus.get();

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "wood,        2000000,   22,   20000",
            "coal,        4000000,   44,   40000",
            "solid-fuel,  12000000,  133,  30000",
            "rocket-fuel, 100000000, 1111, 10000",
    })
    void oneItemBoilsForFactoriosDuration(String fuel, long joules, int wholeTicks, long banked) {
        assertEquals(1.0, CORPUS.boilerEffectivity());
        long perTick = BoilerSpec.joulesPerTick(CORPUS.boilerEnergyConsumption());
        FuelBuffer buffer = new FuelBuffer();
        int[] items = {1};
        int ticks = 0;
        while (BoilerCycle.tick(1000, 1000, 1, perTick, buffer,
                () -> items[0]-- > 0 ? joules : 0L) > 0) {
            ticks++;
        }
        assertEquals(wholeTicks, ticks);
        assertEquals(banked, buffer.storedJoules());
    }
}
