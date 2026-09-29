package com.factoryworks.core.fluid;

import java.util.Locale;
import java.util.Optional;

/**
 * The problem the Steam Engine's HUD line names (#352), derived on each ask as
 * {@code AssemblingStatus} is. A full buffer is left to Jade's
 * energy row (#469).
 */
public enum SteamEngineStatus {
    NO_STEAM,
    NOT_IN_POLE_AREA;

    public static Optional<SteamEngineStatus> of(boolean hasSteam, boolean inPoleArea) {
        if (!hasSteam) {
            return Optional.of(NO_STEAM);
        }
        return inPoleArea ? Optional.empty() : Optional.of(NOT_IN_POLE_AREA);
    }

    /** A synced ordinal back to its status; -1, or one out of range from a mismatched peer, names nothing. */
    public static Optional<SteamEngineStatus> fromOrdinal(int ordinal) {
        SteamEngineStatus[] all = values();
        return ordinal >= 0 && ordinal < all.length ? Optional.of(all[ordinal]) : Optional.empty();
    }

    public String langKey() {
        return "gui.factoryworks.steam_engine.status." + name().toLowerCase(Locale.ROOT);
    }
}
