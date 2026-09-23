package com.planetaryfactory.core.reach;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

/** The player's Reach (#413): 16 blocks for placing, using and breaking a Building. */
public final class Reach {

    public static final double BLOCKS = 16.0;

    private Reach() {
    }

    /**
     * The base value rather than a modifier added on login, so a respawned or freshly constructed
     * player has it with nothing to re-apply, and creative's own bonus still stacks on top.
     */
    public static void onEntityAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, Attributes.BLOCK_INTERACTION_RANGE, BLOCKS);
    }
}
