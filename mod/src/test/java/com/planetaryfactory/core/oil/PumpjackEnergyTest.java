package com.planetaryfactory.core.oil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** A Pumpjack's buffer: a cycle per 900 FE worked, a drain paid whether or not it works (ADR-0081). */
class PumpjackEnergyTest {

    private static final PumpjackSpec SPEC = new PumpjackSpec(45, 150, 900, 1_000, "oritech:still_oil");

    @Test
    void theBufferHoldsOneTicksWorkAndDrain() {
        PumpjackEnergy energy = new PumpjackEnergy(SPEC);
        assertEquals(47, energy.capacity());
        assertEquals(47, energy.insert(100));
        assertEquals(0, energy.insert(1));
    }

    @Test
    void aFedPumpjackCyclesEveryTwentyTicksAndPaysThreeFeDrainEveryTwo() {
        PumpjackEnergy energy = new PumpjackEnergy(SPEC);
        int cycles = 0;
        long spent = 0;
        for (int tick = 0; tick < 200; tick++) {
            energy.insert(energy.capacity());
            long before = energy.buffered();
            if (energy.tick(true)) {
                cycles++;
            }
            spent += before - energy.buffered();
        }
        assertEquals(10, cycles);
        assertEquals(200 * 45 + 300, spent);
    }

    @Test
    void anIdlePumpjackPaysOnlyItsDrain() {
        PumpjackEnergy energy = new PumpjackEnergy(SPEC);
        long spent = 0;
        for (int tick = 0; tick < 20; tick++) {
            energy.insert(energy.capacity());
            long before = energy.buffered();
            assertFalse(energy.tick(false));
            spent += before - energy.buffered();
        }
        assertEquals(30, spent);
        assertEquals(0, energy.progress());
    }

    @Test
    void aStarvedPumpjackNeverCycles() {
        PumpjackEnergy energy = new PumpjackEnergy(SPEC);
        for (int tick = 0; tick < 1_000; tick++) {
            assertFalse(energy.tick(true));
        }
        assertEquals(0, energy.progress());
    }

    @Test
    void anUnderfedPumpjackCyclesProportionallySlower() {
        PumpjackEnergy energy = new PumpjackEnergy(SPEC);
        int cycles = 0;
        for (int tick = 0; tick < 400; tick++) {
            energy.insert(24);
            if (energy.tick(true)) {
                cycles++;
            }
        }
        // 24 FE a tick less a 1.5 FE drain works 22.5 a tick: half the rate.
        assertEquals(10, cycles);
    }

    @Test
    void unpaidDrainIsNotOwed() {
        PumpjackEnergy energy = new PumpjackEnergy(SPEC);
        for (int tick = 0; tick < 100; tick++) {
            energy.tick(true);
        }
        energy.insert(energy.capacity());
        long before = energy.buffered();
        energy.tick(true);
        assertTrue(before - energy.buffered() <= 45 + 2);
    }
}
