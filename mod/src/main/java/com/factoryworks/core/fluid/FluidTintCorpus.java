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
 * The tints that render borrowed Oritech fluids in Factorio's colours (#277, ADR-0067).
 *
 * <p>This class holds no colour. It reads {@code factoryworks_core/fluid/tints.json}, which
 * {@code scripts/build-fluid-tints.py} derives from Factorio's {@code base_color} and Oritech's own
 * sprites -- the {@link PumpCorpus} idiom: a classpath resource, loaded once, because fluid models
 * bake before any world exists. A fluid with no row keeps the tint Oritech registers.
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

    /** The opaque ARGB tint for a fluid id, or empty where Oritech's own tint stands. */
    public OptionalInt tint(String fluidId) {
        Integer argb = tints.get(fluidId);
        return argb == null ? OptionalInt.empty() : OptionalInt.of(argb);
    }
}
