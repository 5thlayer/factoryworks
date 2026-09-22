package com.planetaryfactory.core.radar;

import com.planetaryfactory.core.energy.ForgeEnergy;

/**
 * The Radar in the pack's units: its draw per Minecraft tick, the cost of a long-range sector and of
 * a nearby pulse, the nearby area's width and the long range's reach in sectors (#368, ADR-0079).
 */
public record RadarSpec(long fePerTick, long fePerSector, long fePerNearbyScan, int nearbySpan, int reach) {

    private static final int TICKS_PER_SECOND = 20;

    /**
     * Not Factorio's 7: 16 Minecraft chunks, so the pulse reveals past a short render distance rather
     * than only what a player standing there already sees (ADR-0079).
     */
    private static final int NEARBY_SPAN = 8;

    public static RadarSpec fromCorpus() {
        RadarCorpus corpus = RadarCorpus.get();
        return new RadarSpec(
                Math.round(corpus.energyUsageWatts() / TICKS_PER_SECOND / ForgeEnergy.JOULES_PER_FE),
                Math.round(corpus.energyPerSectorJoules() / ForgeEnergy.JOULES_PER_FE),
                Math.round(corpus.energyPerNearbyScanJoules() / ForgeEnergy.JOULES_PER_FE),
                NEARBY_SPAN,
                corpus.sectorReach());
    }
}
