package com.factoryworks.core.fluid;

import com.factoryworks.core.machine.footprint.Footprint;
import com.factoryworks.core.machine.footprint.Footprint.Local;

/**
 * Factorio's 3x2 boiler, one block tall (ADR-0114). The anchor is the front-middle block; local
 * {@code x} runs backward from the front, so the second row is {@code x = 1}.
 */
public final class BoilerFootprint {

    public static final Footprint FOOTPRINT = Footprint.of(
            new Local(0, 0, -1), new Local(0, 0, 1),
            new Local(1, 0, -1), new Local(1, 0, 0), new Local(1, 0, 1));

    private BoilerFootprint() {
    }
}
