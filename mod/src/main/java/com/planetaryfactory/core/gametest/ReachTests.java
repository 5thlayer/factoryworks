package com.planetaryfactory.core.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;

/** The player's Reach (#413). The figures are typed, not read off {@code Reach}. */
final class ReachTests {

    private ReachTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_fresh_player_reaches_blocks_at_16_and_entities_at_3", 20,
                ReachTests::freshPlayer);
    }

    private static void freshPlayer(GameTestHelper helper) {
        ListeningPlayer player = new ListeningPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        helper.assertValueEqual(player.blockInteractionRange(), 16.0, "block interaction range");
        helper.assertValueEqual(player.entityInteractionRange(), 3.0, "entity interaction range");
        helper.succeed();
    }
}
