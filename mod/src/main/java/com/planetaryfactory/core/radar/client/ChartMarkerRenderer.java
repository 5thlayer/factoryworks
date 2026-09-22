package com.planetaryfactory.core.radar.client;

/** A map that draws the client's patch markers (ADR-0079). FTB Chunks is the only one. */
public interface ChartMarkerRenderer {

    /** The markers changed; the map reads them again from {@link RadarMapClient#markers}. */
    void markersChanged();
}
