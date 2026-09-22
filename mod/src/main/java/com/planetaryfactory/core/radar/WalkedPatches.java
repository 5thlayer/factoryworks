package com.planetaryfactory.core.radar;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The outfield patches whose centre chunk the server has sent each player, per dimension (#370,
 * ADR-0079). Walked terrain is on the walker's map only, so these are the player's, not the team's.
 */
public final class WalkedPatches {

    private static final Codec<UUID> PLAYER = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<WalkedPatches> CODEC =
            Codec.unboundedMap(PLAYER, Codec.unboundedMap(Codec.STRING, PatchMarker.CODEC.listOf()))
                    .xmap(WalkedPatches::decode, WalkedPatches::encode);

    private final Map<UUID, Map<String, Set<PatchMarker>>> walked = new HashMap<>();

    /** Records the markers; answers those the player had not walked before. */
    public List<PatchMarker> add(UUID player, String dimension, Collection<PatchMarker> markers) {
        Set<PatchMarker> have = walked.computeIfAbsent(player, p -> new HashMap<>())
                .computeIfAbsent(dimension, d -> new HashSet<>());
        List<PatchMarker> added = new ArrayList<>();
        for (PatchMarker marker : markers) {
            if (have.add(marker)) {
                added.add(marker);
            }
        }
        return added;
    }

    public Set<PatchMarker> of(UUID player, String dimension) {
        return walked.getOrDefault(player, Map.of()).getOrDefault(dimension, Set.of());
    }

    private static WalkedPatches decode(Map<UUID, Map<String, List<PatchMarker>>> encoded) {
        WalkedPatches decoded = new WalkedPatches();
        encoded.forEach((player, dimensions) -> dimensions.forEach((dimension, markers) ->
                decoded.walked.computeIfAbsent(player, p -> new HashMap<>())
                        .put(dimension, new HashSet<>(markers))));
        return decoded;
    }

    private Map<UUID, Map<String, List<PatchMarker>>> encode() {
        Map<UUID, Map<String, List<PatchMarker>>> encoded = new HashMap<>();
        walked.forEach((player, dimensions) -> {
            Map<String, List<PatchMarker>> byDimension = new HashMap<>();
            dimensions.forEach((dimension, markers) -> byDimension.put(dimension, List.copyOf(markers)));
            encoded.put(player, byDimension);
        });
        return encoded;
    }
}
