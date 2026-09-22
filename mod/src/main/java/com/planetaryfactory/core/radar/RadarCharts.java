package com.planetaryfactory.core.radar;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Every team's chart: the sectors its Radars have charted, per dimension (#368, ADR-0079).
 *
 * <p>Teams are keyed by UUID and dimensions by their id string, so the rule and its codec stay
 * Minecraft-free.
 */
public final class RadarCharts {

    private static final Codec<UUID> TEAM = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    private static final Codec<Map<String, List<Long>>> CHART =
            Codec.unboundedMap(Codec.STRING, Codec.LONG.listOf());

    public static final Codec<RadarCharts> CODEC = Codec.unboundedMap(TEAM, CHART)
            .xmap(RadarCharts::decode, RadarCharts::encode);

    private final Map<UUID, Map<String, Set<Long>>> charts = new HashMap<>();

    /** Records the sector; true if the team had not charted it before. */
    public boolean chart(UUID team, String dimension, Sector sector) {
        return charts.computeIfAbsent(team, t -> new HashMap<>())
                .computeIfAbsent(dimension, d -> new HashSet<>())
                .add(sector.pack());
    }

    public boolean isCharted(UUID team, String dimension, Sector sector) {
        return charts.getOrDefault(team, Map.of()).getOrDefault(dimension, Set.of()).contains(sector.pack());
    }

    public Set<Sector> sectors(UUID team, String dimension) {
        return charts.getOrDefault(team, Map.of()).getOrDefault(dimension, Set.of()).stream()
                .map(Sector::unpack)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static RadarCharts decode(Map<UUID, Map<String, List<Long>>> encoded) {
        RadarCharts charts = new RadarCharts();
        encoded.forEach((team, dimensions) -> dimensions.forEach((dimension, sectors) ->
                charts.charts.computeIfAbsent(team, t -> new HashMap<>())
                        .put(dimension, new HashSet<>(sectors))));
        return charts;
    }

    private Map<UUID, Map<String, List<Long>>> encode() {
        Map<UUID, Map<String, List<Long>>> encoded = new HashMap<>();
        charts.forEach((team, dimensions) -> {
            Map<String, List<Long>> byDimension = new HashMap<>();
            dimensions.forEach((dimension, sectors) -> byDimension.put(dimension, List.copyOf(sectors)));
            encoded.put(team, byDimension);
        });
        return encoded;
    }
}
