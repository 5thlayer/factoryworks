package com.factoryworks.core.wreck;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * The cargo hold's slot count, read from {@code factoryworks_core/wreck/containers.json}, which
 * {@code scripts/build-wreck-assets.py} copies out of Factorio's {@code crash-site-spaceship} row
 * (ADR-0107). Free of Minecraft.
 */
public final class CargoHoldCorpus {

    private static final String PATH = "/factoryworks_core/wreck/containers.json";
    private static final String CRASH_SITE_SPACESHIP = "crash-site-spaceship";

    private static final CargoHoldCorpus INSTANCE = load(PATH);

    private final int slots;

    private CargoHoldCorpus(int slots) {
        this.slots = slots;
    }

    public static CargoHoldCorpus get() {
        return INSTANCE;
    }

    static CargoHoldCorpus load(String path) {
        try (InputStream stream = CargoHoldCorpus.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "no " + path + " on the classpath -- run scripts/build-wreck-assets.py");
            }
            JsonObject root = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            JsonObject row = root.getAsJsonObject(CRASH_SITE_SPACESHIP);
            if (row == null) {
                throw new IllegalStateException(path + " carries no " + CRASH_SITE_SPACESHIP
                        + " row -- re-run scripts/build-wreck-assets.py");
            }
            return new CargoHoldCorpus(row.get("inventory_size").getAsInt());
        } catch (IOException e) {
            throw new IllegalStateException("could not read " + path, e);
        }
    }

    public int slots() {
        return slots;
    }
}
