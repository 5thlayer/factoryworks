package com.planetaryfactory.core.radar;

import com.planetaryfactory.core.energy.ForgeEnergy;

/** The Radar in the pack's units: its draw per Minecraft tick, a sector's cost and its reach (#368). */
public record RadarSpec(long fePerTick, long fePerSector, int reach) {

    private static final int TICKS_PER_SECOND = 20;

    public static RadarSpec fromCorpus() {
        RadarCorpus corpus = RadarCorpus.get();
        return new RadarSpec(
                Math.round(corpus.energyUsageWatts() / TICKS_PER_SECOND / ForgeEnergy.JOULES_PER_FE),
                Math.round(corpus.energyPerSectorJoules() / ForgeEnergy.JOULES_PER_FE),
                corpus.sectorReach());
    }
}
