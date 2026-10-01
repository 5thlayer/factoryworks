package com.factoryworks.core.wreck;

import com.factoryworks.core.mining.MiningSpeed;
import com.factoryworks.core.mining.PickTier;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * Each debris class's mining time, read from {@code factoryworks_core/wreck/debris.json}, which
 * {@code scripts/build-wreck-assets.py} copies out of Factorio's crash-site wrecks (#550). Free of
 * Minecraft.
 */
public final class DebrisCorpus {

    private static final String PATH = "/factoryworks_core/wreck/debris.json";

    private static final DebrisCorpus INSTANCE = load(PATH);

    private final Map<DebrisSize, Float> miningTimes;

    private DebrisCorpus(Map<DebrisSize, Float> miningTimes) {
        this.miningTimes = miningTimes;
    }

    public static DebrisCorpus get() {
        return INSTANCE;
    }

    static DebrisCorpus load(String path) {
        try (InputStream stream = DebrisCorpus.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "no " + path + " on the classpath -- run scripts/build-wreck-assets.py");
            }
            JsonObject root = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            Map<DebrisSize, Float> times = new EnumMap<>(DebrisSize.class);
            for (DebrisSize size : DebrisSize.values()) {
                JsonObject row = root.getAsJsonObject(size.id());
                if (row == null) {
                    throw new IllegalStateException(path + " carries no " + size.id()
                            + " row -- re-run scripts/build-wreck-assets.py");
                }
                times.put(size, row.get("mining_time").getAsFloat());
            }
            return new DebrisCorpus(times);
        } catch (IOException e) {
            throw new IllegalStateException("could not read " + path, e);
        }
    }

    public float miningTime(DebrisSize size) {
        return miningTimes.get(size);
    }

    /**
     * The hardness at which the Pick takes Factorio's own hand-mining time,
     * {@code mining_time / mining_speed}. The tier's speed cancels, so one hardness holds for both.
     */
    public float hardness(DebrisSize size) {
        PickTier tier = PickTier.IRON;
        return MiningSpeed.hardnessFor(miningTime(size) / tier.miningSpeed(), tier.vanillaSpeed());
    }
}
