package com.planetaryfactory.core.dismantle;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** What a sneak-click with a stored start would take up, start first, or why it takes up nothing. */
public record DismantlePlan(List<BlockPos> blocks, DismantleSpan.@Nullable Refusal refusal) {

    public DismantlePlan {
        blocks = List.copyOf(blocks);
    }

    public boolean refused() {
        return refusal != null;
    }

    public Component message() {
        return Component.translatable(messageKey());
    }

    String messageKey() {
        return switch (refusal) {
            case OUTSIDE_FAMILY -> "message.planetaryfactory.dismantle.outside_family";
            case NOT_JOINED -> "message.planetaryfactory.dismantle.not_joined";
            case TIED -> "message.planetaryfactory.dismantle.tied";
            case null -> throw new IllegalStateException("an accepted plan has no refusal message");
        };
    }
}
