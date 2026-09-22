package com.planetaryfactory.core.radar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Which patch markers each online player's map has been sent (#370, ADR-0079). The client holds its
 * markers in memory only, so what a player has been sent is forgotten at logout and not saved: a
 * login lacks every marker of the team's chart.
 */
public final class MarkerDelivery {

    private final Map<UUID, Map<String, Set<PatchMarker>>> sent = new HashMap<>();
    private final Map<UUID, Session> sessions = new HashMap<>();

    private static final class Session {
        private final UUID team;
        private final String dimension;
        private final LinkedHashSet<PatchMarker> queue = new LinkedHashSet<>();

        private Session(UUID team, String dimension) {
            this.team = team;
            this.dimension = dimension;
        }
    }

    /**
     * The player's team and dimension as of now. The first call since login, or one naming a new
     * team or dimension, queues every marker of that team's chart there the player lacks.
     */
    public void observe(UUID player, UUID team, String dimension, RadarCharts charts, SectorPatches patches) {
        Session session = sessions.get(player);
        if (session != null && session.team.equals(team) && session.dimension.equals(dimension)) {
            return;
        }
        session = new Session(team, dimension);
        sessions.put(player, session);
        queue(player, session, patches.in(dimension, charts.sectors(team, dimension)));
    }

    /** Markers newly in the team's chart, queued for every member online in their dimension. */
    public void charted(UUID team, String dimension, Iterable<PatchMarker> markers) {
        sessions.forEach((player, session) -> {
            if (session.team.equals(team) && session.dimension.equals(dimension)) {
                queue(player, session, markers);
            }
        });
    }

    /** Every queued marker, recorded as sent. */
    public List<PatchMarker> take(UUID player) {
        Session session = sessions.get(player);
        if (session == null || session.queue.isEmpty()) {
            return List.of();
        }
        List<PatchMarker> taken = new ArrayList<>(session.queue);
        session.queue.clear();
        sent.computeIfAbsent(player, p -> new HashMap<>())
                .computeIfAbsent(session.dimension, d -> new HashSet<>())
                .addAll(taken);
        return taken;
    }

    public void logout(UUID player) {
        sessions.remove(player);
        sent.remove(player);
    }

    private void queue(UUID player, Session session, Iterable<PatchMarker> markers) {
        Set<PatchMarker> have = sent.getOrDefault(player, Map.of()).getOrDefault(session.dimension, Set.of());
        for (PatchMarker marker : markers) {
            if (!have.contains(marker)) {
                session.queue.add(marker);
            }
        }
    }
}
