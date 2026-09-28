package com.planetaryfactory.core.machine;

import java.util.List;

import com.planetaryfactory.core.machine.footprint.Footprint;
import com.planetaryfactory.core.machine.footprint.Footprint.Local;

/**
 * Where the Chemical Plant stands (ADR-0096): Oritech's Centrifuge's controller and its one core
 * above, and the Centrifuge's two addon slots either side, read off the 2.0.0-exp6 jar.
 */
public final class ChemicalPlantFootprint {

    public static final Footprint FOOTPRINT = Footprint.of(new Local(0, 1, 0));

    private ChemicalPlantFootprint() {
    }

    public static List<Local> addonSlots() {
        return List.of(new Local(0, 0, -1), new Local(0, 0, 1));
    }
}
