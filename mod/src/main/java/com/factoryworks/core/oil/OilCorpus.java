package com.factoryworks.core.oil;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.factoryworks.core.ore.OreCorpus;
import com.factoryworks.core.ore.OutfieldLaw;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Crude oil as {@code amounts.json} holds it, beside the outfield law it shares with the ores (ADR-0081). */
public final class OilCorpus {

    private static final String PATH = "/factoryworks_core/ore/amounts.json";

    private static final OilCorpus INSTANCE = load(PATH);

    private final String fluid;
    private final int amountPerCycle;
    private final double miningTime;
    private final long normal;
    private final long minimum;
    private final long depletion;
    private final double randomProbability;
    private final double richnessDivisor;
    private final double additionalRichness;
    private final double collisionWidth;
    private final OutfieldLaw law;

    private OilCorpus(JsonObject crude, OutfieldLaw law) {
        this.fluid = crude.get("fluid").getAsString();
        this.amountPerCycle = crude.get("amount_per_cycle").getAsInt();
        this.miningTime = crude.get("mining_time").getAsDouble();
        this.normal = crude.get("normal").getAsLong();
        this.minimum = crude.get("minimum").getAsLong();
        this.depletion = crude.get("infinite_depletion_amount").getAsLong();
        this.randomProbability = crude.get("random_probability").getAsDouble();
        this.richnessDivisor = crude.get("richness_divisor").getAsDouble();
        this.additionalRichness = crude.get("additional_richness").getAsDouble();
        this.collisionWidth = crude.get("collision_width").getAsDouble();
        this.law = law;
    }

    public static OilCorpus get() {
        return INSTANCE;
    }

    static OilCorpus load(String path) {
        try (InputStream stream = OilCorpus.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "no " + path + " on the classpath -- it is hand-owned data");
            }
            JsonObject root = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            JsonObject crude = root.getAsJsonObject("crude_oil");
            if (crude == null) {
                throw new IllegalStateException(path + " carries no crude_oil -- it is hand-owned data");
            }
            JsonObject spot = crude.getAsJsonObject("outfield");
            OreCorpus ores = OreCorpus.get();
            OutfieldLaw law = new OutfieldLaw(new OreCorpus.Outfield(
                    spot.get("base_density").getAsDouble(),
                    spot.get("base_spots_per_km2").getAsDouble(),
                    spot.get("random_spot_size_minimum").getAsDouble(),
                    spot.get("random_spot_size_maximum").getAsDouble(),
                    spot.get("regular_rq_factor").getAsDouble(),
                    spot.get("regular_blob_amplitude_multiplier").getAsDouble(),
                    spot.get("regular_blob_amplitude_maximum_distance").getAsDouble()),
                    ores.densityLaw(), ores.distanceLaw(), ores.edge());
            return new OilCorpus(crude, law);
        } catch (IOException broken) {
            throw new IllegalStateException("could not read " + path, broken);
        }
    }

    /** Factorio's name for the fluid a cycle yields. */
    public String fluid() {
        return fluid;
    }

    public int amountPerCycle() {
        return amountPerCycle;
    }

    public double miningTime() {
        return miningTime;
    }

    public long normal() {
        return normal;
    }

    public long minimum() {
        return minimum;
    }

    public long depletion() {
        return depletion;
    }

    /** The fraction of a field's tiles {@code random_penalty} leaves a positive value on: 1/48. */
    public double randomProbability() {
        return randomProbability;
    }

    public double richnessDivisor() {
        return richnessDivisor;
    }

    public double additionalRichness() {
        return additionalRichness;
    }

    public double collisionWidth() {
        return collisionWidth;
    }

    public OutfieldLaw law() {
        return law;
    }
}
