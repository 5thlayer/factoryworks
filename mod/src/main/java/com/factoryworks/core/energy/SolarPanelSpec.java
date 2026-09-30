package com.factoryworks.core.energy;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * The Solar Panel in FE (#529), from the resource {@code scripts/build-solar-assets.py} copies out of
 * the corpus. The buffer is one tick of output, so it hides no outage (ADR-0062).
 *
 * <p>Pure: no Minecraft types.
 */
public final class SolarPanelSpec {

    private static final String PATH = "/factoryworks_core/energy/solar_panels.json";
    private static final String PANEL = "solar-panel";
    private static final long TICKS_PER_SECOND = 20L;

    private static final SolarPanelSpec INSTANCE = load(PATH);

    private final long peakFePerTick;

    private SolarPanelSpec(double productionWatts) {
        this.peakFePerTick = Math.round(productionWatts / TICKS_PER_SECOND / ForgeEnergy.JOULES_PER_FE);
    }

    public static SolarPanelSpec get() {
        return INSTANCE;
    }

    static SolarPanelSpec load(String path) {
        try (InputStream stream = SolarPanelSpec.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "no " + path + " on the classpath -- run scripts/build-solar-assets.py");
            }
            JsonObject root = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            JsonObject row = root.getAsJsonObject(PANEL);
            if (row == null) {
                throw new IllegalStateException(
                        path + " carries no " + PANEL + " row -- re-run scripts/build-solar-assets.py");
            }
            return new SolarPanelSpec(row.get("production").getAsDouble());
        } catch (IOException e) {
            throw new IllegalStateException("could not read " + path, e);
        }
    }

    public long peakFePerTick() {
        return peakFePerTick;
    }

    public long bufferFe() {
        return peakFePerTick;
    }
}
