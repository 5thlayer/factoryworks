package com.planetaryfactory.core.radar;

import com.planetaryfactory.core.energy.ForgeEnergy;

/**
 * The Radar in the pack's units: its draw per Minecraft tick, the cost of a long-range sector and of
 * a nearby pulse, and the two reaches in sectors (#368, ADR-0079).
 */
public record RadarSpec(long fePerTick, long fePerSector, long fePerNearbyScan, int nearReach, int reach) {

    private static final int TICKS_PER_SECOND = 20;

    public static RadarSpec fromCorpus() {
        RadarCorpus corpus = RadarCorpus.get();
        return new RadarSpec(
                Math.round(corpus.energyUsageWatts() / TICKS_PER_SECOND / ForgeEnergy.JOULES_PER_FE),
                Math.round(corpus.energyPerSectorJoules() / ForgeEnergy.JOULES_PER_FE),
                Math.round(corpus.energyPerNearbyScanJoules() / ForgeEnergy.JOULES_PER_FE),
                corpus.nearbyReach(),
                corpus.sectorReach());
    }
}
