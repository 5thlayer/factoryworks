package com.factoryworks.core.energy;

/**
 * The accumulator's buffer and flow limits in FE (#283, ADR-0062). The joule and watt figures are
 * Factorio's, held to {@code data/factorio/machine.json}'s accumulator row by
 * {@code tests/factorio/test_machine_extract.py}.
 */
public final class AccumulatorSpec {

    static final long BUFFER_JOULES = 5_000_000L;
    static final long INPUT_FLOW_WATTS = 300_000L;
    static final long OUTPUT_FLOW_WATTS = 300_000L;

    private static final long TICKS_PER_SECOND = 20L;

    private AccumulatorSpec() {
    }

    public static long capacityFe() {
        return BUFFER_JOULES / ForgeEnergy.JOULES_PER_FE;
    }

    public static long inputFePerTick() {
        return INPUT_FLOW_WATTS / TICKS_PER_SECOND / ForgeEnergy.JOULES_PER_FE;
    }

    public static long outputFePerTick() {
        return OUTPUT_FLOW_WATTS / TICKS_PER_SECOND / ForgeEnergy.JOULES_PER_FE;
    }
}
