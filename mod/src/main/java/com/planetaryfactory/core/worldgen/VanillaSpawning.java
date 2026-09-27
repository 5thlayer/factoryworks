package com.planetaryfactory.core.worldgen;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * A new world starts with vanilla's own spawners off, as a default rather than forced (ADR-0093).
 * {@code SPAWN_MOBS} is the one that stops village cats and sieges, which a cluster of beds would
 * otherwise summon.
 */
public final class VanillaSpawning {

    public static final List<GameRule<Boolean>> OFF = List.of(
            GameRules.SPAWN_MOBS,
            GameRules.SPAWN_PHANTOMS,
            GameRules.SPAWN_PATROLS,
            GameRules.SPAWN_WANDERING_TRADERS);

    private VanillaSpawning() {
    }

    public static void onCreateSpawnPosition(LevelEvent.CreateSpawnPosition event) {
        if (event.getLevel() instanceof ServerLevel level) {
            GameRules rules = level.getGameRules();
            for (GameRule<Boolean> rule : OFF) {
                rules.set(rule, false, level.getServer());
            }
        }
    }
}
