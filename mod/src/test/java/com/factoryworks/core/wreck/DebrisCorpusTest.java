package com.factoryworks.core.wreck;

import com.factoryworks.core.mining.MiningSpeed;
import com.factoryworks.core.mining.PickTier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * That each debris class breaks in Factorio's hand-mining time, {@code mining_time / mining_speed},
 * on both Picks (#550). The mining times are typed from the corpus rather than read back, so a
 * changed resource fails here too.
 */
class DebrisCorpusTest {

    @ParameterizedTest(name = "{0} debris: {1} s of Factorio mining time")
    @CsvSource({"BIG, 1.25", "MEDIUM, 1.0", "SMALL, 0.75"})
    void breaksInFactoriosTime(DebrisSize size, float miningTime) {
        DebrisCorpus corpus = DebrisCorpus.get();
        assertEquals(miningTime, corpus.miningTime(size), 1e-6);
        for (PickTier tier : PickTier.values()) {
            assertEquals(miningTime / tier.miningSpeed(),
                    MiningSpeed.secondsAt(corpus.hardness(size), tier.vanillaSpeed()), 1e-5,
                    tier.name());
        }
    }
}
