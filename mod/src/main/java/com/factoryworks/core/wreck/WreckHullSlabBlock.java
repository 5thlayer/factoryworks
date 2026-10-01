package com.factoryworks.core.wreck;

import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** The roof's edge on the wreck's hull (ADR-0107): unbreakable. */
public class WreckHullSlabBlock extends SlabBlock {

    public WreckHullSlabBlock(BlockBehaviour.Properties props) {
        super(WreckBlocks.indestructible(props).noLootTable());
    }
}
