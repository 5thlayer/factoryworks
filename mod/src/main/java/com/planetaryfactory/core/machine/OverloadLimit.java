package com.planetaryfactory.core.machine;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * The Overload Limit: how many crafts' worth of an ingredient automated insertion leaves in a
 * machine, Factorio's {@code clamp(ceil(factor * speed / energy_required) + 1, minimum, maximum)}.
 * The constants are read from {@code planetaryfactory_core/machine/overload.json}, which
 * {@code scripts/build-machine-specs.py} copies out of the corpus (#517).
 *
 * <p>Pure: no Minecraft types.
 */
public record OverloadLimit(double factor, int minimum, int maximum) {

    private static final String PATH = "/planetaryfactory_core/machine/overload.json";
    private static final double TICKS_PER_SECOND = 20.0;

    private static final OverloadLimit INSTANCE = load();

    public static OverloadLimit get() {
        return INSTANCE;
    }

    /** Crafts' worth at {@code craftingSpeed} of a recipe of {@code recipeTicks}, never below one tick. */
    public int crafts(double craftingSpeed, int recipeTicks) {
        double perSwing = factor * craftingSpeed * TICKS_PER_SECOND / Math.max(1, recipeTicks);
        // The epsilon keeps an exact quotient from rounding up past itself.
        int swing = (int) Math.ceil(perSwing - 1e-9);
        return Math.min(Math.max(swing + 1, minimum), maximum);
    }

    /** How many more a slot holding {@code held} takes of an ingredient needing {@code perCraft}. */
    public static int room(int perCraft, int crafts, int held) {
        return Math.max(0, perCraft * crafts - held);
    }

    private static OverloadLimit load() {
        try (InputStream stream = OverloadLimit.class.getResourceAsStream(PATH)) {
            if (stream == null) {
                throw new IllegalStateException("no " + PATH + " on the classpath -- run scripts/build-machine-specs.py");
            }
            JsonObject root = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            return new OverloadLimit(
                    root.get("dynamic_recipe_overload_factor").getAsDouble(),
                    root.get("minimum_recipe_overload_multiplier").getAsInt(),
                    root.get("maximum_recipe_overload_multiplier").getAsInt());
        } catch (IOException e) {
            throw new IllegalStateException("could not read " + PATH, e);
        }
    }
}
