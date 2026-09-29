package com.factoryworks.core.machine;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every crafting machine's {@link MachineSpec}, read from
 * {@code factoryworks_core/machine/specs.json}, which {@code scripts/build-machine-specs.py}
 * writes from the corpus (#489, ADR-0096). Keyed by Factorio name.
 */
public final class MachineSpecs {

    private static final String PATH = "/factoryworks_core/machine/specs.json";

    private static final MachineSpecs INSTANCE = load(PATH);

    private final Map<String, MachineSpec> byName;

    private MachineSpecs(Map<String, MachineSpec> byName) {
        this.byName = byName;
    }

    public static MachineSpecs get() {
        return INSTANCE;
    }

    public MachineSpec spec(String name) {
        MachineSpec spec = byName.get(name);
        if (spec == null) {
            throw new IllegalStateException(PATH + " carries no " + name + " -- re-run scripts/build-machine-specs.py");
        }
        return spec;
    }

    /**
     * The most input tanks any machine has. One block entity serves every tier of a machine and a
     * Fast Replace changes the tier under it (ADR-0082), so its tanks are laid out for the widest.
     */
    public int maxFluidInputs() {
        return byName.values().stream().mapToInt(spec -> spec.fluidInputs().size()).max().orElse(0);
    }

    /** The most output tanks any machine has; see {@link #maxFluidInputs}. */
    public int maxFluidOutputs() {
        return byName.values().stream().mapToInt(spec -> spec.fluidOutputs().size()).max().orElse(0);
    }

    static MachineSpecs load(String path) {
        try (InputStream stream = MachineSpecs.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("no " + path + " on the classpath -- run scripts/build-machine-specs.py");
            }
            JsonObject root = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            Map<String, MachineSpec> specs = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                JsonObject row = entry.getValue().getAsJsonObject();
                specs.put(entry.getKey(), new MachineSpec(
                        entry.getKey(),
                        row.get("recipe_type").getAsString(),
                        strings(row.getAsJsonArray("categories")),
                        row.get("crafting_speed").getAsDouble(),
                        Math.round(row.get("energy_usage").getAsDouble()),
                        Math.round(row.get("drain").getAsDouble()),
                        row.get("fast_replaceable_group").getAsString(),
                        row.get("item_inputs").getAsInt(),
                        row.get("item_outputs").getAsInt(),
                        ints(row.getAsJsonArray("fluid_inputs")),
                        ints(row.getAsJsonArray("fluid_outputs")),
                        ints(row.getAsJsonArray("fluid_output_boxes"))));
            }
            return new MachineSpecs(Map.copyOf(specs));
        } catch (IOException e) {
            throw new IllegalStateException("could not read " + path, e);
        }
    }

    private static Set<String> strings(JsonArray array) {
        Set<String> out = new HashSet<>();
        array.forEach(element -> out.add(element.getAsString()));
        return out;
    }

    private static List<Integer> ints(JsonArray array) {
        return array.asList().stream().map(JsonElement::getAsInt).toList();
    }
}
