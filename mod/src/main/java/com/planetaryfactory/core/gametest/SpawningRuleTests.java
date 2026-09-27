package com.planetaryfactory.core.gametest;

import com.planetaryfactory.core.worldgen.VanillaSpawning;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * A new world starts with vanilla's spawners off (#480, ADR-0093). The GameTest server makes its
 * world fresh, so it passes through the same creation hook a player's new world does.
 */
final class SpawningRuleTests {

    private SpawningRuleTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("new_world_starts_with_vanilla_spawning_off", 20, SpawningRuleTests::newWorldStartsWithSpawningOff);
    }

    private static void newWorldStartsWithSpawningOff(GameTestHelper helper) {
        GameRules rules = helper.getLevel().getGameRules();
        // The GameTest server turns SPAWN_MOBS off itself, so only the other three can fail here.
        for (GameRule<Boolean> rule : VanillaSpawning.OFF) {
            if (rules.get(rule)) {
                helper.fail(rule + " is on in a new world");
                return;
            }
        }
        helper.succeed();
    }
}
