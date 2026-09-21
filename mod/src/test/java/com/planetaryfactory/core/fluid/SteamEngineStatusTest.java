package com.planetaryfactory.core.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The one state the Steam Engine's HUD line names (#352). Where several hold, the first link of the
 * chain from steam to pole that is broken wins, so fixing what it names is what lets it run.
 */
class SteamEngineStatusTest {

    @Test
    void aFedDrawnEngineWithRoomIsRunning() {
        assertEquals(SteamEngineStatus.RUNNING, SteamEngineStatus.of(true, false, true));
    }

    @Test
    void anEmptyTankIsNoSteamWhateverElseHolds() {
        assertEquals(SteamEngineStatus.NO_STEAM, SteamEngineStatus.of(false, false, true));
        assertEquals(SteamEngineStatus.NO_STEAM, SteamEngineStatus.of(false, true, true));
        assertEquals(SteamEngineStatus.NO_STEAM, SteamEngineStatus.of(false, true, false));
    }

    @Test
    void noPoleOutranksTheFullBufferItCauses() {
        assertEquals(SteamEngineStatus.NOT_IN_POLE_AREA, SteamEngineStatus.of(true, true, false));
        assertEquals(SteamEngineStatus.NOT_IN_POLE_AREA, SteamEngineStatus.of(true, false, false));
    }

    @Test
    void aFullBufferUnderAPoleIsChargeFull() {
        assertEquals(SteamEngineStatus.CHARGE_FULL, SteamEngineStatus.of(true, true, true));
    }

    @Test
    void anOrdinalOutOfRangeReadsAsNoSteam() {
        assertEquals(SteamEngineStatus.NO_STEAM, SteamEngineStatus.fromOrdinal(-1));
        assertEquals(SteamEngineStatus.NO_STEAM, SteamEngineStatus.fromOrdinal(SteamEngineStatus.values().length));
    }
}
