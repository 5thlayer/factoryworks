package com.planetaryfactory.core.radar.client;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.planetaryfactory.core.radar.ChartDeliveries;
import com.planetaryfactory.core.radar.PatchMarker;
import com.planetaryfactory.core.radar.ftb.FtbMapMarkers;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The patch markers this client has been sent, per dimension (#370, ADR-0079). Held in memory only:
 * the server sends them again at each login.
 */
public final class RadarMapClient {

    private static final Map<ResourceKey<Level>, Set<PatchMarker>> MARKERS = new HashMap<>();
    private static final ChartMarkerRenderer RENDERER = ChartDeliveries.FTB_CHUNKS ? FtbMapMarkers.create() : () -> {
    };

    private RadarMapClient() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(RadarMapClient::onLoggingOut);
    }

    public static void receive(ResourceKey<Level> dimension, List<PatchMarker> markers) {
        if (MARKERS.computeIfAbsent(dimension, d -> new LinkedHashSet<>()).addAll(markers)) {
            RENDERER.markersChanged();
        }
    }

    public static Set<PatchMarker> markers(ResourceKey<Level> dimension) {
        return MARKERS.getOrDefault(dimension, Set.of());
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        MARKERS.clear();
    }
}
