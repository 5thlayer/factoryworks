package com.planetaryfactory.core.radar;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * The Radar's prototype figures as Factorio states them, read from the resource
 * {@code scripts/build-radar-assets.py} copies out of the corpus (#368). {@link RadarSpec} converts.
 */
public final class RadarCorpus {

    private static final String PATH = "/planetaryfactory_core/radar/radars.json";
    private static final String RADAR = "radar";

    private static final RadarCorpus INSTANCE = load(PATH);

    private final double energyUsageWatts;
    private final double energyPerSectorJoules;
    private final int sectorReach;
    private final int tileWidth;
    private final int tileHeight;

    private RadarCorpus(double energyUsageWatts, double energyPerSectorJoules, int sectorReach,
                        int tileWidth, int tileHeight) {
        this.energyUsageWatts = energyUsageWatts;
        this.energyPerSectorJoules = energyPerSectorJoules;
        this.sectorReach = sectorReach;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
    }

    public static RadarCorpus get() {
        return INSTANCE;
    }

    static RadarCorpus load(String path) {
        try (InputStream stream = RadarCorpus.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "no " + path + " on the classpath -- run scripts/build-radar-assets.py");
            }
            JsonObject root = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            JsonObject row = root.getAsJsonObject(RADAR);
            if (row == null) {
                throw new IllegalStateException(
                        path + " carries no " + RADAR + " row -- re-run scripts/build-radar-assets.py");
            }
            return new RadarCorpus(
                    row.get("energy_usage").getAsDouble(),
                    row.get("energy_per_sector").getAsDouble(),
                    row.get("max_distance_of_sector_revealed").getAsInt(),
                    row.get("tile_width").getAsInt(),
                    row.get("tile_height").getAsInt());
        } catch (IOException e) {
            throw new IllegalStateException("could not read " + path, e);
        }
    }

    public double energyUsageWatts() {
        return energyUsageWatts;
    }

    public double energyPerSectorJoules() {
        return energyPerSectorJoules;
    }

    /** In sectors, counted from the Radar's own. */
    public int sectorReach() {
        return sectorReach;
    }

    public int tileWidth() {
        return tileWidth;
    }

    public int tileHeight() {
        return tileHeight;
    }
}
