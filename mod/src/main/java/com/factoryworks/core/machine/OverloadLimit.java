package com.factoryworks.core.machine;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Overload Limit: how many crafts' worth of an ingredient automated insertion leaves in a
 * machine, Factorio's {@code clamp(ceil(factor * speed / energy_required) + 1, minimum, maximum)}.
 * A fluid input holds {@code fluidCrafts} crafts' worth whatever the speed (#519); the output tank
 * sizes are {@link OutputTankVolume}'s, with {@code fluidOutputCrafts} and the recipes that pin their
 * products (#520).
 * The constants are read from {@code factoryworks_core/machine/overload.json}, which
 * {@code scripts/build-machine-specs.py} copies out of the corpus (#517).
 *
 * <p>Pure: no Minecraft types.
 */
public record OverloadLimit(double factor, int minimum, int maximum, int fluidCrafts, int fluidOutputCrafts,
        Set<String> pinnedFluidOutputs) {

    public OverloadLimit {
        pinnedFluidOutputs = Set.copyOf(pinnedFluidOutputs);
    }

    private static final String PATH = "/factoryworks_core/machine/overload.json";
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

    /** How many more mB a tank holding {@code held} takes of a fluid ingredient needing {@code perCraft}. */
    public int fluidRoom(int perCraft, int held) {
        return room(perCraft, fluidCrafts, held);
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
                    root.get("maximum_recipe_overload_multiplier").getAsInt(),
                    root.get("fluid_input_multiplier").getAsInt(),
                    root.get("fluid_output_multiplier").getAsInt(),
                    root.getAsJsonArray("pinned_fluid_outputs").asList().stream()
                            .map(element -> element.getAsString()).collect(Collectors.toSet()));
        } catch (IOException e) {
            throw new IllegalStateException("could not read " + PATH, e);
        }
    }
}
