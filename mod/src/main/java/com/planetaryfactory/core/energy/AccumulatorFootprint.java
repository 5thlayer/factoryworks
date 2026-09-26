package com.planetaryfactory.core.energy;

import com.planetaryfactory.core.machine.footprint.Footprint;
import com.planetaryfactory.core.machine.footprint.Footprint.Local;

/** Oritech's Large Energy Storage controller and its three cores, as one footprint (#283, ADR-0077). */
public final class AccumulatorFootprint {

    public static final Footprint FOOTPRINT =
            Footprint.of(new Local(0, 1, 0), new Local(1, 0, 0), new Local(1, 1, 0));

    private AccumulatorFootprint() {
    }
}
