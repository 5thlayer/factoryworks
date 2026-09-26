package com.planetaryfactory.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * The state the accumulator's HUD line names (#468): only what Jade's energy row cannot say, so a
 * tick with no flow names nothing.
 */
class AccumulatorStatusTest {

    @Test
    void noPoleOutranksAnyFlow() {
        assertEquals(Optional.of(AccumulatorStatus.NOT_IN_POLE_AREA), AccumulatorStatus.of(false, 0L));
        assertEquals(Optional.of(AccumulatorStatus.NOT_IN_POLE_AREA), AccumulatorStatus.of(false, 150L));
    }

    @Test
    void aPositiveFlowIsCharging() {
        assertEquals(Optional.of(AccumulatorStatus.CHARGING), AccumulatorStatus.of(true, 1L));
    }

    @Test
    void aNegativeFlowIsDischarging() {
        assertEquals(Optional.of(AccumulatorStatus.DISCHARGING), AccumulatorStatus.of(true, -150L));
    }

    @Test
    void noFlowUnderAPoleNamesNothing() {
        assertEquals(Optional.empty(), AccumulatorStatus.of(true, 0L));
    }

    @Test
    void anOrdinalOutOfRangeNamesNothing() {
        assertEquals(Optional.empty(), AccumulatorStatus.fromOrdinal(-1));
        assertEquals(Optional.empty(), AccumulatorStatus.fromOrdinal(AccumulatorStatus.values().length));
        assertEquals(Optional.of(AccumulatorStatus.CHARGING),
                AccumulatorStatus.fromOrdinal(AccumulatorStatus.CHARGING.ordinal()));
    }
}
