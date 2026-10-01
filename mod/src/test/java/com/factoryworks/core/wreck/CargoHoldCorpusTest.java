package com.factoryworks.core.wreck;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * That the mod reads the resource the generator writes (ADR-0107). The generated copy is held to
 * the corpus in {@code tests/pack/test_wreck_assets.py}; this holds the parser to the key.
 */
class CargoHoldCorpusTest {

    @Test
    @DisplayName("the resource parses, and carries the crash site's inventory size")
    void resourceParses() {
        assertEquals(5, CargoHoldCorpus.get().slots());
    }
}
