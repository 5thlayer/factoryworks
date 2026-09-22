package com.planetaryfactory.core.radar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.planetaryfactory.core.ore.PatchId;

/**
 * Which patch markers each online player's map has been sent, and with what amount (#370, #371,
 * ADR-0079). The map shows a patch's amount as it was when last charted or walked past, as
 * Factorio's does, so a marker is sent again only when one of those finds its amount changed, and
 * once with nothing left, which removes it.
 *
 * <p>The client holds its markers in memory only, so what a player has been sent is forgotten at
 * logout and not saved: a login lacks every marker of the team's chart and the player's walking.
 */
public final class MarkerDelivery {

    /** What is left in a patch now. */
    @FunctionalInterface
    public interface Amounts {
        long of(String dimension, PatchMarker marker);
    }

    /** A marker as the client is sent it; an amount of zero removes it. */
    public record Update(PatchMarker marker, long amount) {
    }

    private final Map<UUID, Map<String, Map<PatchMarker, Long>>> sent = new HashMap<>();
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
     * team or dimension, queues every marker there, of the team's chart and of what the player has
     * walked.
     */
    public void observe(UUID player, UUID team, String dimension, RadarCharts charts, SectorPatches patches,
            WalkedPatches walked) {
        Session session = sessions.get(player);
        if (session != null && session.team.equals(team) && session.dimension.equals(dimension)) {
            return;
        }
        session = new Session(team, dimension);
        sessions.put(player, session);
        session.queue.addAll(patches.in(dimension, charts.sectors(team, dimension)));
        session.queue.addAll(walked.of(player, dimension));
    }

    /** Markers the team's Radars charted or re-scanned, queued for every member online there. */
    public void charted(UUID team, String dimension, Iterable<PatchMarker> markers) {
        sessions.values().forEach(session -> {
            if (session.team.equals(team) && session.dimension.equals(dimension)) {
                markers.forEach(session.queue::add);
            }
        });
    }

    /** Markers in a chunk just sent to the player, queued while they are in that dimension. */
    public void found(UUID player, String dimension, Iterable<PatchMarker> markers) {
        Session session = sessions.get(player);
        if (session != null && session.dimension.equals(dimension)) {
            markers.forEach(session.queue::add);
        }
    }

    /** A patch ran out: every map in its dimension holding it is sent its removal. */
    public void ranOut(PatchId patch) {
        sessions.forEach((player, session) -> {
            if (!session.dimension.equals(patch.dimension())) {
                return;
            }
            sentTo(player, session.dimension).keySet().stream()
                    .filter(marker -> marker.id(patch.dimension()).equals(patch))
                    .forEach(session.queue::add);
        });
    }

    /**
     * Every queued marker whose amount the player's map does not already show. One that has run
     * out is sent only to a map holding it.
     */
    public List<Update> take(UUID player, Amounts amounts) {
        Session session = sessions.get(player);
        if (session == null || session.queue.isEmpty()) {
            return List.of();
        }
        Map<PatchMarker, Long> shown = sent.computeIfAbsent(player, p -> new HashMap<>())
                .computeIfAbsent(session.dimension, d -> new HashMap<>());
        List<Update> taken = new ArrayList<>();
        for (PatchMarker marker : session.queue) {
            long amount = amounts.of(session.dimension, marker);
            Long before = shown.get(marker);
            if (before == null ? amount > 0 : before != amount) {
                shown.put(marker, amount);
                taken.add(new Update(marker, amount));
            }
        }
        session.queue.clear();
        return taken;
    }

    public void logout(UUID player) {
        sessions.remove(player);
        sent.remove(player);
    }

    private Map<PatchMarker, Long> sentTo(UUID player, String dimension) {
        return sent.getOrDefault(player, Map.of()).getOrDefault(dimension, Map.of());
    }
}
