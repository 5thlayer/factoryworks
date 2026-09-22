package com.planetaryfactory.core.radar;

import com.planetaryfactory.core.machine.footprint.Footprint;

/** Where the Radar stands (#368): Factorio's tile square, as tall as it is wide. */
public final class RadarFootprint {

    public static final Footprint FOOTPRINT =
            Footprint.standing(RadarCorpus.get().tileWidth(), RadarCorpus.get().tileHeight());

    private RadarFootprint() {
    }
}
