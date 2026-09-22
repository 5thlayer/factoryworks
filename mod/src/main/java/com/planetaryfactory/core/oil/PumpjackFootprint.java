package com.planetaryfactory.core.oil;

import com.planetaryfactory.core.machine.footprint.Footprint;

/** Where the Pumpjack stands: Factorio's tile square, as tall as it is wide, its anchor over the well. */
public final class PumpjackFootprint {

    public static final Footprint FOOTPRINT =
            Footprint.standing(PumpjackCorpus.get().tileWidth(), PumpjackCorpus.get().tileHeight());

    private PumpjackFootprint() {
    }
}
