package com.planetaryfactory.core;

import java.util.Arrays;
import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The pack's server config, {@code planetaryfactory_core-server.toml}. */
public final class PFServerConfig {

    /** What can lock a recipe in a machine, named as the TOML writes them (#260). */
    public enum LockSource {
        /** Locked while Researchd blocks the recipe for the team that placed the machine. */
        researchd
    }

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.ConfigValue<List<? extends String>> LOCK_SOURCES;

    static {
        var builder = new ModConfigSpec.Builder();
        LOCK_SOURCES = builder
                .comment("What locks a recipe in an Assembling Machine. Empty (the default) locks nothing.",
                        "  researchd: Locked while Researchd blocks the recipe for the team that placed the machine.")
                .defineListAllowEmpty("lockSources", List.of(), () -> LockSource.researchd.name(),
                        value -> value instanceof String name
                                && Arrays.stream(LockSource.values()).anyMatch(source -> source.name().equals(name)));
        SPEC = builder.build();
    }

    private PFServerConfig() {
    }

    /** Whether {@code source} is listed; nothing is before a world's config loads. */
    public static boolean locksBy(LockSource source) {
        return SPEC.isLoaded() && LOCK_SOURCES.get().contains(source.name());
    }
}
