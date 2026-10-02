package com.factoryworks.core.oil;

import com.factoryworks.core.energy.ForgeEnergy;

/**
 * The Pumpjack in the pack's units (ADR-0081): its working draw per tick, its drain in joules per
 * tick, since 3 kW is 1.5 FE a tick, the energy one cycle costs, and the capacity its port adds to the segment.
 */
public record PumpjackSpec(long fePerTick, long drainJoulesPerTick, long fePerCycle, int portCapacityMillibuckets,
        String fluid) {

    private static final int TICKS_PER_SECOND = 20;

    public static PumpjackSpec fromCorpus() {
        PumpjackCorpus corpus = PumpjackCorpus.get();
        double secondsPerCycle = OilCorpus.get().miningTime() / corpus.miningSpeed();
        return new PumpjackSpec(
                Math.round(corpus.energyUsageWatts() / TICKS_PER_SECOND / ForgeEnergy.JOULES_PER_FE),
                Math.round(corpus.drainWatts() / TICKS_PER_SECOND),
                Math.round(corpus.energyUsageWatts() * secondsPerCycle / ForgeEnergy.JOULES_PER_FE),
                corpus.outputVolume(),
                corpus.fluid());
    }
}
