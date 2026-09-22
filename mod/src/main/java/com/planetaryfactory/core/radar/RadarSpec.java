package com.planetaryfactory.core.radar;

import com.planetaryfactory.core.energy.ForgeEnergy;

/**
 * The Radar in the pack's units: its draw per Minecraft tick, the cost of a long-range sector and of
 * a nearby pulse, the reach of the nearby area and of the long range in sectors (#368, ADR-0079).
 */
public record RadarSpec(long fePerTick, long fePerSector, long fePerNearbyScan, int nearReach, int reach) {

    private static final int TICKS_PER_SECOND = 20;

    /**
     * Not Factorio's 3: 9x9 sectors, 18 Minecraft chunks, so the pulse reveals past a normal render
     * distance rather than only what a player standing there already sees (ADR-0079).
     */
    private static final int NEARBY_REACH = 4;

    public static RadarSpec fromCorpus() {
        RadarCorpus corpus = RadarCorpus.get();
        return new RadarSpec(
                Math.round(corpus.energyUsageWatts() / TICKS_PER_SECOND / ForgeEnergy.JOULES_PER_FE),
                Math.round(corpus.energyPerSectorJoules() / ForgeEnergy.JOULES_PER_FE),
                Math.round(corpus.energyPerNearbyScanJoules() / ForgeEnergy.JOULES_PER_FE),
                NEARBY_REACH,
                corpus.sectorReach());
    }
}
