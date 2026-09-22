package com.planetaryfactory.core.oil;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * The Pumpjack's prototype figures as Factorio states them, read from the resource
 * {@code scripts/build-pumpjack-assets.py} copies out of the corpus (ADR-0081). {@link PumpjackSpec}
 * converts.
 */
public record PumpjackCorpus(double miningSpeed, double energyUsageWatts, double drainWatts, int tileWidth,
        int tileHeight, int outputVolume, String fluid) {

    private static final String PATH = "/planetaryfactory_core/oil/pumpjack.json";
    private static final String PUMPJACK = "pumpjack";

    private static final PumpjackCorpus INSTANCE = load(PATH);

    public static PumpjackCorpus get() {
        return INSTANCE;
    }

    static PumpjackCorpus load(String path) {
        try (InputStream stream = PumpjackCorpus.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "no " + path + " on the classpath -- run scripts/build-pumpjack-assets.py");
            }
            JsonObject row = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class).getAsJsonObject(PUMPJACK);
            if (row == null) {
                throw new IllegalStateException(
                        path + " carries no " + PUMPJACK + " row -- re-run scripts/build-pumpjack-assets.py");
            }
            return new PumpjackCorpus(
                    row.get("mining_speed").getAsDouble(),
                    row.get("energy_usage").getAsDouble(),
                    row.get("drain").getAsDouble(),
                    row.get("tile_width").getAsInt(),
                    row.get("tile_height").getAsInt(),
                    row.get("output_volume").getAsInt(),
                    row.get("fluid").getAsString());
        } catch (IOException e) {
            throw new IllegalStateException("could not read " + path, e);
        }
    }
}
