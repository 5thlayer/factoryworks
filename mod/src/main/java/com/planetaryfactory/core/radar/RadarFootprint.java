package com.planetaryfactory.core.radar;

import java.util.ArrayList;
import java.util.List;

import com.planetaryfactory.core.machine.footprint.Footprint;
import com.planetaryfactory.core.machine.footprint.Footprint.Local;

/**
 * Where the Radar stands (#368): Factorio's tile square, as tall as it is wide, with the anchor at
 * the bottom centre so the machine sits centred on the block the player clicks.
 */
public final class RadarFootprint {

    public static final Footprint FOOTPRINT = build(RadarCorpus.get().tileWidth(), RadarCorpus.get().tileHeight());

    private RadarFootprint() {
    }

    /** Factorio's tile width runs along the footprint's lateral {@code z}, its height along {@code x}. */
    private static Footprint build(int tileWidth, int tileHeight) {
        if (tileWidth % 2 == 0 || tileHeight % 2 == 0) {
            throw new IllegalStateException("a " + tileWidth + "x" + tileHeight + " radar has no centre block");
        }
        int halfX = tileHeight / 2;
        int halfZ = tileWidth / 2;
        int tall = tileWidth;
        List<Local> parts = new ArrayList<>();
        for (int y = 0; y < tall; y++) {
            for (int x = -halfX; x <= halfX; x++) {
                for (int z = -halfZ; z <= halfZ; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        parts.add(new Local(x, y, z));
                    }
                }
            }
        }
        return Footprint.of(parts.toArray(Local[]::new));
    }
}
