package com.factoryworks.core.fluid;

import com.factoryworks.core.machine.footprint.Footprint;
import com.factoryworks.core.machine.footprint.Footprint.Local;

/** Oritech's Steam Engine controller and its three cores, as one footprint (ADR-0077). */
public final class SteamEngineFootprint {

    public static final Footprint FOOTPRINT =
            Footprint.of(new Local(0, 1, 0), new Local(0, 0, -1), new Local(0, 1, -1));

    private SteamEngineFootprint() {
    }
}
