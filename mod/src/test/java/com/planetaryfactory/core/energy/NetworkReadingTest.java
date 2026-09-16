package com.planetaryfactory.core.energy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What Jade prints for a network (#285): Factorio's hover summary, from one tick's committed flows. */
class NetworkReadingTest {

    private static NetworkReading reading(long produced, long delivered, long demanded) {
        return new NetworkReading(produced, delivered, demanded, 0L, 0L, 0L, 0L, 0, 1);
    }

    @Test
    void a_network_asked_for_nothing_is_satisfied() {
        assertEquals(100, reading(0L, 0L, 0L).satisfactionPercent());
    }

    @Test
    void satisfaction_floors_so_a_shortfall_never_reads_as_full() {
        assertEquals(99, reading(14_399L, 14_399L, 14_400L).satisfactionPercent());
        assertEquals(100, reading(90L, 90L, 90L).satisfactionPercent());
    }

    @Test
    void satisfaction_is_zero_only_when_nothing_arrived() {
        assertEquals(0, reading(0L, 0L, 14_400L).satisfactionPercent());
        assertEquals(1, reading(1L, 1L, 1_000L).satisfactionPercent());
    }

    @Test
    void accumulator_flow_is_charge_minus_discharge() {
        NetworkReading charging = new NetworkReading(200L, 50L, 50L, 150L, 0L, 1_000L, 50_000L, 1, 1);
        NetworkReading draining = new NetworkReading(0L, 150L, 150L, 0L, 150L, 1_000L, 50_000L, 1, 1);
        assertEquals(150L, charging.accumulatorFlow());
        assertEquals(-150L, draining.accumulatorFlow());
    }

    @Test
    void accumulators_are_shown_only_when_the_network_has_one() {
        assertFalse(reading(90L, 90L, 90L).hasAccumulators());
        assertTrue(new NetworkReading(0L, 0L, 0L, 0L, 0L, 0L, 50_000L, 1, 1).hasAccumulators());
    }
}
