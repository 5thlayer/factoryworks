package com.factoryworks.core.fluid;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

/**
 * The tints that draw Core's oil and chemistry fluids in Factorio's colours (ADR-0109).
 *
 * <p>This class holds no colour. It reads {@code factoryworks_core/fluid/tints.json}, which
 * {@code scripts/build-fluid-tints.py} derives from Factorio's {@code base_color} and each fluid's
 * sprite -- the {@link PumpCorpus} idiom: a classpath resource, loaded once, because fluid models
 * bake before any world exists.
 *
 * <p>Free of Minecraft, so the parsing is checkable in an ordinary unit test.
 */
public final class FluidTintCorpus {

    private static final String PATH = "/factoryworks_core/fluid/tints.json";

    private static final FluidTintCorpus INSTANCE = load(PATH);

    private final Map<String, Integer> tints;

    private FluidTintCorpus(Map<String, Integer> tints) {
        this.tints = tints;
    }

    public static FluidTintCorpus get() {
        return INSTANCE;
    }

    static FluidTintCorpus load(String path) {
        try (InputStream stream = FluidTintCorpus.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "no " + path + " on the classpath -- run scripts/build-fluid-tints.py");
            }
            JsonObject root = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            Map<String, Integer> tints = new HashMap<>();
            for (String fluid : root.keySet()) {
                String hex = root.getAsJsonObject(fluid).get("color").getAsString();
                tints.put(fluid, 0xFF000000 | Integer.parseInt(hex.substring(1), 16));
            }
            return new FluidTintCorpus(Map.copyOf(tints));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("could not read " + path, e);
        }
    }

    /** The opaque ARGB tint for a fluid id, or empty where the resource has no row. */
    public OptionalInt tint(String fluidId) {
        Integer argb = tints.get(fluidId);
        return argb == null ? OptionalInt.empty() : OptionalInt.of(argb);
    }
}
