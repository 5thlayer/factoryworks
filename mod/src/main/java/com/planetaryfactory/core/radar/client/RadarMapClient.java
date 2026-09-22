package com.planetaryfactory.core.radar.client;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.planetaryfactory.core.radar.ChartDeliveries;
import com.planetaryfactory.core.radar.MarkerDelivery;
import com.planetaryfactory.core.radar.PatchMarker;
import com.planetaryfactory.core.radar.ftb.FtbMapMarkers;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The patch markers this client has been sent, and what each patch held when last seen, per
 * dimension (#370, ADR-0079). Held in memory only: the server sends them again at each login.
 */
public final class RadarMapClient {

    private static final Map<ResourceKey<Level>, Map<PatchMarker, Long>> MARKERS = new HashMap<>();
    private static final ChartMarkerRenderer RENDERER = ChartDeliveries.FTB_CHUNKS ? FtbMapMarkers.create() : () -> {
    };

    private RadarMapClient() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(RadarMapClient::onLoggingOut);
    }

    public static void receive(ResourceKey<Level> dimension, List<MarkerDelivery.Update> updates) {
        Map<PatchMarker, Long> markers = MARKERS.computeIfAbsent(dimension, d -> new LinkedHashMap<>());
        for (MarkerDelivery.Update update : updates) {
            if (update.amount() > 0) {
                markers.put(update.marker(), update.amount());
            } else {
                markers.remove(update.marker());
            }
        }
        RENDERER.markersChanged();
    }

    public static Map<PatchMarker, Long> markers(ResourceKey<Level> dimension) {
        return MARKERS.getOrDefault(dimension, Map.of());
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        MARKERS.clear();
    }
}
