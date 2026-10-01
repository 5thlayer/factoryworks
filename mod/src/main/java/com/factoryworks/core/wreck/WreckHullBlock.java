package com.factoryworks.core.wreck;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** The wreck's wall (ADR-0107): opaque, solid and unbreakable, scorched where the crash burned it (#550). */
public class WreckHullBlock extends Block {

    public static final BooleanProperty SCORCHED = BooleanProperty.create("scorched");

    public WreckHullBlock(BlockBehaviour.Properties props) {
        super(WreckBlocks.indestructible(props).noLootTable());
        registerDefaultState(stateDefinition.any().setValue(SCORCHED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SCORCHED);
    }
}
