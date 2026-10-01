package com.factoryworks.core.wreck;

import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** The wreck's glass (ADR-0107): translucent, solid and unbreakable. */
public class WreckWindowBlock extends TransparentBlock {

    public WreckWindowBlock(BlockBehaviour.Properties props) {
        super(WreckBlocks.indestructible(props)
                .noOcclusion()
                .isValidSpawn((state, level, pos, type) -> false)
                .isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false)
                .noLootTable());
    }
}
