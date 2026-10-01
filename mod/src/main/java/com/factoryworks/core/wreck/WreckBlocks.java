package com.factoryworks.core.wreck;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

/** What the three wreck blocks share: nothing breaks, burns, blasts or pushes them (ADR-0107). */
final class WreckBlocks {

    private WreckBlocks() {
    }

    /** Bedrock's hardness and blast resistance. */
    static BlockBehaviour.Properties indestructible(BlockBehaviour.Properties props) {
        return props
                .strength(-1.0F, 3_600_000.0F)
                .pushReaction(PushReaction.BLOCK)
                .sound(SoundType.METAL);
    }
}
