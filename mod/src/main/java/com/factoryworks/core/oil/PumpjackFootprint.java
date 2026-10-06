package com.factoryworks.core.oil;

import com.factoryworks.core.machine.footprint.Footprint;

/** Where the Pumpjack stands: Factorio's tile square, two blocks tall, its anchor over the well (ADR-0111). */
public final class PumpjackFootprint {

    public static final int LAYERS = 2;

    public static final Footprint FOOTPRINT = Footprint.standing(
            PumpjackCorpus.get().tileWidth(), PumpjackCorpus.get().tileHeight(), LAYERS);

    private PumpjackFootprint() {
    }
}
