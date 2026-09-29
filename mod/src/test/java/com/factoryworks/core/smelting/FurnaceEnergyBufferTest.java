package com.factoryworks.core.smelting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The Electric tier's buffer, which is what ADR-0036's supply-area pole water-fills against
 * (#147, #155, #266). It holds FE since ADR-0060, and the pole's FE face is the only thing that
 * reads {@code getEnergyCanBeInserted()} and pays with {@code addEnergy}.
 */
class FurnaceEnergyBufferTest {

    @Test
    void anEmptyBufferAsksForOneWholeCraft() {
        FurnaceEnergyBuffer buffer = new FurnaceEnergyBuffer(14_400L);
        assertEquals(14_400L, buffer.getEnergyCanBeInserted());
        assertEquals(0L, buffer.getEnergyStored());
    }

    @Test
    void itTakesOnlyWhatFits() {
        FurnaceEnergyBuffer buffer = new FurnaceEnergyBuffer(14_400L);
        assertEquals(14_000L, buffer.addEnergy(14_000L));
        assertEquals(400L, buffer.addEnergy(500L));
        assertEquals(14_400L, buffer.getEnergyStored());
        assertEquals(0L, buffer.getEnergyCanBeInserted());
    }

    @Test
    void aTickIsDrawnOnlyIfItIsWhollyThere() {
        FurnaceEnergyBuffer buffer = new FurnaceEnergyBuffer(14_400L);
        buffer.addEnergy(100L);
        assertTrue(buffer.drawTick(90L));
        assertEquals(10L, buffer.getEnergyStored());
        assertFalse(buffer.drawTick(90L), "a partial tick is not a tick; the smelt waits");
        assertEquals(10L, buffer.getEnergyStored());
    }

    /**
     * An aborted insert has to be undoable, which on this side is a stored value put back. The
     * pole measures a machine's room by inserting inside a transaction it then aborts, so a
     * buffer that could not be restored would collect a probe's worth of free FE every tick.
     */
    @Test
    void aStoredValueCanBePutBack() {
        FurnaceEnergyBuffer buffer = new FurnaceEnergyBuffer(14_400L);
        long before = buffer.getEnergyStored();
        buffer.addEnergy(5_000L);
        buffer.setStoredFe(before);
        assertEquals(0L, buffer.getEnergyStored());
        assertEquals(14_400L, buffer.getEnergyCanBeInserted());
    }
}
