package com.factoryworks.core.radar;

import java.util.UUID;

import com.factoryworks.core.radar.ftb.FtbTeamLookup;

import net.neoforged.fml.ModList;

/**
 * Whose chart a Radar's owner charts into (#368): their FTB team, or the player alone when FTB Teams
 * is absent. Asked at each sector, so a player who changes team charts for the new one.
 */
public final class ChartOwners {

    private static final boolean FTB_TEAMS = ModList.get().isLoaded("ftbteams");

    private ChartOwners() {
    }

    public static UUID teamOf(UUID player) {
        return FTB_TEAMS ? FtbTeamLookup.teamOf(player).orElse(player) : player;
    }
}
