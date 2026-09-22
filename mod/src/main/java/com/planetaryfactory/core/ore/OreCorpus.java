package com.planetaryfactory.core.ore;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Factorio's resource amounts, as the mod sees them.
 *
 * <p>ADR-0022's rule is that Factorio's numbers are extracted and never transcribed, and ADR-0041
 * puts patch totals under it. So this class holds no number: it reads
 * {@code planetaryfactory_core/ore/amounts.json}, which
 * {@code scripts/factorio-resource-extract.py} writes out of the same dump the rest of the corpus
 * comes from.
 *
 * <p><b>A classpath resource, not a datapack file.</b> The stage count sizes a blockstate property,
 * which is fixed at registration -- before any world, any datapack and any reload exists. Loading
 * it here means the ladder a block renders and the ladder Factorio ships are the same list, with no
 * step where someone keeps them in step by hand.
 *
 * <p>Free of Minecraft, so the parsing is checkable in an ordinary unit test.
 */
public final class OreCorpus {

    private static final String PATH = "/planetaryfactory_core/ore/amounts.json";

    private static final OreCorpus INSTANCE = load(PATH);

    private final Map<String, Resource> resources;
    private final DistanceLaw law;
    private final DensityLaw density;
    private final Edge edge;

    private OreCorpus(Map<String, Resource> resources, DistanceLaw law, DensityLaw density, Edge edge) {
        this.resources = resources;
        this.law = law;
        this.density = density;
        this.edge = edge;
    }

    public static OreCorpus get() {
        return INSTANCE;
    }

    static OreCorpus load(String path) {
        try (InputStream stream = OreCorpus.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "no " + path + " on the classpath -- run scripts/factorio-resource-extract.py");
            }
            JsonObject root = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            Map<String, Resource> resources = new LinkedHashMap<>();
            JsonObject entries = root.getAsJsonObject("resources");
            for (String name : entries.keySet()) {
                JsonObject entry = entries.getAsJsonObject(name);
                List<Double> ratios = new ArrayList<>();
                entry.getAsJsonArray("stage_ratios").forEach(value -> ratios.add(value.getAsDouble()));
                JsonObject outfield = entry.getAsJsonObject("outfield");
                resources.put(name, new Resource(
                        name,
                        entry.get("factorio_name").getAsString(),
                        entry.get("starting_amount").isJsonNull()
                                ? 0L
                                : (long) entry.get("starting_amount").getAsDouble(),
                        entry.get("mining_time").getAsDouble(),
                        rgb(entry.getAsJsonArray("map_color")),
                        List.copyOf(ratios),
                        new Outfield(
                                outfield.get("base_density").getAsDouble(),
                                outfield.get("base_spots_per_km2").getAsDouble(),
                                outfield.get("random_spot_size_minimum").getAsDouble(),
                                outfield.get("random_spot_size_maximum").getAsDouble(),
                                outfield.get("regular_rq_factor").getAsDouble(),
                                outfield.get("regular_blob_amplitude_multiplier").getAsDouble(),
                                outfield.get("regular_blob_amplitude_maximum_distance").getAsDouble())));
            }
            JsonObject distance = root.getAsJsonObject("distance_law");
            JsonObject density = root.getAsJsonObject("density_law");
            JsonObject edge = root.getAsJsonObject("outfield_edge");
            List<Octave> octaves = new ArrayList<>();
            edge.getAsJsonArray("octaves").forEach(value -> octaves.add(new Octave(
                    value.getAsJsonObject().get("input_scale").getAsDouble(),
                    value.getAsJsonObject().get("output_scale").getAsDouble())));
            return new OreCorpus(
                    Map.copyOf(resources),
                    new DistanceLaw(distance.get("offset").getAsInt(), distance.get("divisor").getAsInt()),
                    new DensityLaw(
                            density.get("starting_resource_placement_radius").getAsDouble(),
                            density.get("regular_patch_fade_in_distance").getAsDouble(),
                            density.get("double_density_distance").getAsDouble()),
                    new Edge(edge.get("radius_cap").getAsDouble(), List.copyOf(octaves),
                            edge.get("offset").getAsDouble()));
        } catch (IOException broken) {
            throw new IllegalStateException("could not read " + path, broken);
        }
    }

    /** Factorio states a colour in 0..1 unless a component exceeds 1, when it is 0..255 (#370). */
    static int rgb(JsonArray color) {
        double scale = 255;
        for (int i = 0; i < 3; i++) {
            if (color.get(i).getAsDouble() > 1) {
                scale = 1;
            }
        }
        int rgb = 0;
        for (int i = 0; i < 3; i++) {
            rgb = rgb << 8 | (int) Math.round(color.get(i).getAsDouble() * scale);
        }
        return rgb;
    }

    public Resource resource(String name) {
        Resource found = resources.get(name);
        if (found == null) {
            throw new IllegalArgumentException(name + " is not one of Terra's ores: " + resources.keySet());
        }
        return found;
    }

    public Map<String, Resource> resources() {
        return resources;
    }

    /**
     * How many sprite stages every ore renders through -- Factorio's eight.
     *
     * <p>The maximum across the alphabet, because the blockstate property is one size for every
     * ore block and a resource with a shorter ladder simply never reaches the top of it.
     */
    public int stageCount() {
        return resources.values().stream()
                .mapToInt(resource -> OreStage.count(resource.stageRatios()))
                .max()
                .orElse(1);
    }

    public DistanceLaw distanceLaw() {
        return law;
    }

    public DensityLaw densityLaw() {
        return density;
    }

    public Edge edge() {
        return edge;
    }

    /**
     * One resource: its patch total, what an operation on it costs, and the ladder its stages are
     * rendered against.
     *
     * @param name the pack's block name -- {@code iron}, {@code stone}
     * @param factorioName the corpus key, which is Factorio's own (ADR-0028)
     * @param startingAmount Factorio's starting patch total, or {@code 0} where it deals none
     * @param miningTime seconds one unit costs a drill of speed 1 -- Factorio's own
     *         {@code minable.mining_time}, and the reason a rig's rate cannot live on the rig
     *         (#193): uranium's 2 costs the same drill twice what iron's 1 does
     * @param mapColor the RGB its patches are marked in on the map (#370)
     * @param stageRatios fractions of a block's own initial amount, richest first
     * @param outfield what the outfield amount law takes of this resource
     */
    public record Resource(String name, String factorioName, long startingAmount, double miningTime,
            int mapColor, List<Double> stageRatios, Outfield outfield) {
    }

    /** Factorio's per-resource autoplace arguments, and the range a spot's size factor is drawn from. */
    public record Outfield(double baseDensity, double baseSpotsPerKm2, double spotSizeMinimum,
            double spotSizeMaximum, double rqFactor, double blobAmplitudeMultiplier,
            double blobAmplitudeMaximumDistance) {

        public double meanSpotSize() {
            return (spotSizeMinimum + spotSizeMaximum) / 2;
        }
    }

    /** A spot's radius cap and its ragged edge: {@code (octaves - offset) * blob amplitude}. */
    public record Edge(double radiusCap, List<Octave> octaves, double offset) {
    }

    public record Octave(double inputScale, double outputScale) {
    }

    /**
     * Factorio's {@code regular_density_at}, taking only the branch for
     * {@code has_starting_area_placement != -1}, which every Nauvis resource is (ADR-0045).
     */
    public record DensityLaw(double placementRadius, double fadeIn, double doubleDensityDistance) {

        public double at(double baseDensity, double distance) {
            double fade = clamp((distance - placementRadius) / fadeIn);
            double doubling = 1 + clamp((distance - fadeIn) / doubleDensityDistance);
            return baseDensity * fade * doubling;
        }

        private static double clamp(double value) {
            return Math.max(0, Math.min(1, value));
        }
    }

    /**
     * Factorio's richness-by-distance term, {@code max((offset + distance) / divisor, 1)}.
     *
     * <p>Flat inside {@code divisor - offset} blocks of origin -- 1600 of them -- and uncapped
     * beyond (ADR-0045). A Factorio tile and a Minecraft block are both a metre.
     */
    public record DistanceLaw(int offset, int divisor) {

        public double richnessAt(double distance) {
            return Math.max((offset + distance) / divisor, 1.0);
        }

        public int flatWithin() {
            return divisor - offset;
        }
    }
}
