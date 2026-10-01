package com.factoryworks.core.placement;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Which blocks may Fast Replace which, read from {@code factoryworks_core/placement/replace_groups.json},
 * which {@code scripts/build-replace-groups.py} copies out of Factorio's {@code fast_replaceable_group}
 * (ADR-0082). Blocks are named by registry id, so this stays free of Minecraft.
 */
public final class ReplaceGroups {

    private static final String PATH = "/factoryworks_core/placement/replace_groups.json";

    private static final ReplaceGroups INSTANCE = load(PATH);

    private final Map<String, String> groupByBlock;

    private ReplaceGroups(Map<String, String> groupByBlock) {
        this.groupByBlock = groupByBlock;
    }

    public static ReplaceGroups get() {
        return INSTANCE;
    }

    /** A block never replaces itself: the same tier in hand extends a pole or opens a screen (ADR-0082). */
    public boolean isIn(String group, String block) {
        return group.equals(groupByBlock.get(block));
    }

    public boolean canReplace(String held, String placed) {
        String group = groupByBlock.get(held);
        return group != null && !held.equals(placed) && group.equals(groupByBlock.get(placed));
    }

    static ReplaceGroups load(String path) {
        try (InputStream stream = ReplaceGroups.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "no " + path + " on the classpath -- run scripts/build-replace-groups.py");
            }
            JsonObject root = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            Map<String, String> groups = new HashMap<>();
            root.entrySet().forEach(entry -> groups.put(entry.getKey(), entry.getValue().getAsString()));
            return new ReplaceGroups(Map.copyOf(groups));
        } catch (IOException e) {
            throw new IllegalStateException("could not read " + path, e);
        }
    }
}
