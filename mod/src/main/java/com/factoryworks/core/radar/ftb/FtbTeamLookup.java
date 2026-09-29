package com.factoryworks.core.radar.ftb;

import java.util.Optional;
import java.util.UUID;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;

/** FTB Teams' answer to which team a player is in. Loaded only when {@code ftbteams} is (ADR-0079). */
public final class FtbTeamLookup {

    private FtbTeamLookup() {
    }

    public static Optional<UUID> teamOf(UUID player) {
        if (!FTBTeamsAPI.api().isManagerLoaded()) {
            return Optional.empty();
        }
        return FTBTeamsAPI.api().getManager().getTeamForPlayerID(player).map(Team::getId);
    }
}
