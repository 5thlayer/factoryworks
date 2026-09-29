package com.factoryworks.core.machine;

import java.util.ArrayList;
import java.util.List;

import com.factoryworks.core.machine.footprint.Footprint;
import com.factoryworks.core.machine.footprint.Footprint.Local;

/**
 * Where the Oil Refinery stands (ADR-0096): Oritech's Refinery's controller and cores, with its
 * chamber module's 3x2 layer stacked twice above, so it always has three outputs. The base's model
 * leaves (2, 0..1, 0) open, and so does the footprint.
 */
public final class OilRefineryFootprint {

    public static final int CHAMBERS = 2;

    public static final int BASE_HEIGHT = 2;

    public static final Footprint FOOTPRINT = build();

    private OilRefineryFootprint() {
    }

    private static Footprint build() {
        List<Local> parts = new ArrayList<>(List.of(
                new Local(0, 1, 0),
                new Local(0, 0, -1), new Local(0, 1, -1),
                new Local(1, 0, -1), new Local(1, 1, -1),
                new Local(1, 0, 0), new Local(1, 1, 0),
                new Local(2, 0, -1), new Local(2, 1, -1)));
        for (int chamber = 0; chamber < CHAMBERS; chamber++) {
            for (int x = 0; x <= 2; x++) {
                for (int z = -1; z <= 0; z++) {
                    parts.add(new Local(x, BASE_HEIGHT + chamber, z));
                }
            }
        }
        return Footprint.of(parts.toArray(Local[]::new));
    }
}
