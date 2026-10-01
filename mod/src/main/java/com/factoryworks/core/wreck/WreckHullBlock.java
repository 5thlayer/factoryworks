package com.factoryworks.core.wreck;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** The wreck's wall (ADR-0107): opaque, solid and unbreakable. */
public class WreckHullBlock extends Block {

    public WreckHullBlock(BlockBehaviour.Properties props) {
        super(WreckBlocks.indestructible(props).noLootTable());
    }
}
