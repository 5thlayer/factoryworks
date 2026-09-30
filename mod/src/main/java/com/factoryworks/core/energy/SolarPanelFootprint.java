package com.factoryworks.core.energy;

import com.factoryworks.core.machine.footprint.Footprint;
import com.factoryworks.core.machine.footprint.Footprint.Local;
import java.util.ArrayList;
import java.util.List;

/**
 * Oritech's Big Solar Panel controller and its fifteen cores, as one footprint (#529, ADR-0077): a 3x3
 * base with the controller at its centre under a 3x3 top. {@code BigSolarPanelEntity.getCorePositions}
 * on the installed 2.0.0-exp6 jar names every other position of that box.
 */
public final class SolarPanelFootprint {

    public static final Footprint FOOTPRINT = box();

    private SolarPanelFootprint() {
    }

    private static Footprint box() {
        List<Local> parts = new ArrayList<>();
        for (int y = 0; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        parts.add(new Local(x, y, z));
                    }
                }
            }
        }
        return Footprint.of(parts.toArray(Local[]::new));
    }
}
