package com.planetaryfactory.core.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

/**
 * A press of Rotate or Reverse Rotate on the server. As in Factorio, it turns the held stack if that
 * is placeable, and otherwise the block under the crosshair (ADR-0083, ADR-0087).
 */
public final class RotatePress {

    private RotatePress() {
    }

    public static void press(ServerPlayer player, @Nullable BlockPos aimed, boolean reverse) {
        if (HeldTurn.press(player.getMainHandItem(), reverse)) {
            return;
        }
        if (aimed == null || !player.mayBuild() || !player.level().isLoaded(aimed)
                || !player.isWithinBlockInteractionRange(aimed, 1.0) || !player.level().mayInteract(player, aimed)) {
            return;
        }
        // A claim is guarded through the place event, which no custom payload fires on its own (ADR-0087).
        if (EventHooks.onBlockPlace(player, BlockSnapshot.create(player.level().dimension(), player.level(), aimed),
                Direction.UP)) {
            return;
        }
        if (PlacedTurns.turn(player.level(), aimed, reverse) instanceof PlacedTurn.Refused<?>(String reason)) {
            player.sendSystemMessage(Component.translatable(reason), true);
        }
    }
}
