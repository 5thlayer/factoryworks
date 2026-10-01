package com.factoryworks.core.wreck;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** A piece of the wreck's Debris (#550): breakable in Factorio's mining time, and yields nothing. */
public class WreckDebrisBlock extends Block {

    public WreckDebrisBlock(DebrisSize size, BlockBehaviour.Properties props) {
        super(props.strength(DebrisCorpus.get().hardness(size)).sound(SoundType.METAL).noLootTable());
    }
}
