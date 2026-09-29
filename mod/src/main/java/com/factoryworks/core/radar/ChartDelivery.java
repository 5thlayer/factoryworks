package com.factoryworks.core.radar;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Which of a team's charted sectors each player's map has been sent (#369, ADR-0079). FTB Chunks
 * keeps the map on the client, so the server remembers what each player's map already holds and
 * sends only the rest: at login, on joining a team, on entering a dimension, and as sectors are
 * charted. What a player has been sent is saved; who is online and what is queued is not.
 */
public final class ChartDelivery {

    private static final Codec<UUID> PLAYER = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<ChartDelivery> CODEC =
            Codec.unboundedMap(PLAYER, Codec.unboundedMap(Codec.STRING, Codec.LONG.listOf()))
                    .xmap(ChartDelivery::decode, ChartDelivery::encode);

    private final Map<UUID, Map<String, Set<Long>>> delivered = new HashMap<>();
    private final Map<UUID, Session> sessions = new HashMap<>();

    private static final class Session {
        private final UUID team;
        private final String dimension;
        private final LinkedHashSet<Long> queue = new LinkedHashSet<>();

        private Session(UUID team, String dimension) {
            this.team = team;
            this.dimension = dimension;
        }
    }

    /**
     * The player's team and dimension as of now. The first call since login, or one naming a new
     * team or dimension, queues every sector of that team's chart there the player lacks.
     */
    public void observe(UUID player, UUID team, String dimension, RadarCharts charts) {
        Session session = sessions.get(player);
        if (session != null && session.team.equals(team) && session.dimension.equals(dimension)) {
            return;
        }
        session = new Session(team, dimension);
        sessions.put(player, session);
        Set<Long> have = deliveredTo(player, dimension);
        for (Sector sector : charts.sectors(team, dimension)) {
            if (!have.contains(sector.pack())) {
                session.queue.add(sector.pack());
            }
        }
    }

    /** A sector newly in the team's chart, queued for every member online in its dimension. */
    public void charted(UUID team, String dimension, Sector sector) {
        sessions.forEach((player, session) -> {
            if (session.team.equals(team) && session.dimension.equals(dimension)
                    && !deliveredTo(player, dimension).contains(sector.pack())) {
                session.queue.add(sector.pack());
            }
        });
    }

    /** Up to {@code max} queued sectors, recorded as sent. */
    public List<Sector> take(UUID player, int max) {
        Session session = sessions.get(player);
        if (session == null) {
            return List.of();
        }
        List<Sector> taken = new ArrayList<>();
        Set<Long> have = delivered.computeIfAbsent(player, p -> new HashMap<>())
                .computeIfAbsent(session.dimension, d -> new HashSet<>());
        Iterator<Long> it = session.queue.iterator();
        while (taken.size() < max && it.hasNext()) {
            long packed = it.next();
            it.remove();
            if (have.add(packed)) {
                taken.add(Sector.unpack(packed));
            }
        }
        return taken;
    }

    public void logout(UUID player) {
        sessions.remove(player);
    }

    private Set<Long> deliveredTo(UUID player, String dimension) {
        return delivered.getOrDefault(player, Map.of()).getOrDefault(dimension, Set.of());
    }

    private static ChartDelivery decode(Map<UUID, Map<String, List<Long>>> encoded) {
        ChartDelivery delivery = new ChartDelivery();
        encoded.forEach((player, dimensions) -> dimensions.forEach((dimension, sectors) ->
                delivery.delivered.computeIfAbsent(player, p -> new HashMap<>())
                        .put(dimension, new HashSet<>(sectors))));
        return delivery;
    }

    private Map<UUID, Map<String, List<Long>>> encode() {
        Map<UUID, Map<String, List<Long>>> encoded = new HashMap<>();
        delivered.forEach((player, dimensions) -> {
            Map<String, List<Long>> byDimension = new HashMap<>();
            dimensions.forEach((dimension, sectors) -> byDimension.put(dimension, List.copyOf(sectors)));
            encoded.put(player, byDimension);
        });
        return encoded;
    }
}
