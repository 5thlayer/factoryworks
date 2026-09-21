package com.planetaryfactory.core.fluid;

import java.util.Locale;

/**
 * The one state the Steam Engine's HUD line names (#352), derived on each ask as
 * {@code AssemblingStatus} is.
 *
 * <p>Where several hold, the first broken link from steam to pole wins. No pole outranks a full
 * buffer because it is the usual cause of one: a buffer is one tick of output (ADR-0062), so an
 * engine nothing draws is full a tick after it starts.
 */
public enum SteamEngineStatus {
    RUNNING,
    NO_STEAM,
    NOT_IN_POLE_AREA,
    CHARGE_FULL;

    public static SteamEngineStatus of(boolean hasSteam, boolean bufferFull, boolean inPoleArea) {
        if (!hasSteam) {
            return NO_STEAM;
        }
        if (!inPoleArea) {
            return NOT_IN_POLE_AREA;
        }
        return bufferFull ? CHARGE_FULL : RUNNING;
    }

    /** A synced ordinal back to its status; one out of range, from a mismatched peer, reads as no steam. */
    public static SteamEngineStatus fromOrdinal(int ordinal) {
        SteamEngineStatus[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NO_STEAM;
    }

    public String langKey() {
        return "gui.planetaryfactory.steam_engine.status." + name().toLowerCase(Locale.ROOT);
    }

    public boolean problem() {
        return this != RUNNING;
    }
}
