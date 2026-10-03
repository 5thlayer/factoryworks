package com.factoryworks.core.fluid;

import com.factoryworks.core.machine.footprint.Footprint;
import com.factoryworks.core.machine.footprint.Footprint.Local;

/** The Steam Engine: the anchor and three parts, 2x1x2 until its model lands (ADR-0113, #586). */
public final class SteamEngineFootprint {

    public static final Footprint FOOTPRINT =
            Footprint.of(new Local(0, 1, 0), new Local(0, 0, -1), new Local(0, 1, -1));

    private SteamEngineFootprint() {
    }
}
