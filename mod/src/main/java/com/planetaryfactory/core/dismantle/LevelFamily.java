package com.planetaryfactory.core.dismantle;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** A family as the blocks of a level carrying its tag, joined by its rule. */
record LevelFamily(Level level, TagKey<Block> tag, JoinRule rule) implements DismantleFamily<BlockPos> {

    @Override
    public boolean member(BlockPos pos) {
        // A span stops at a chunk's edge rather than loading the next one.
        return level.isLoaded(pos) && level.getBlockState(pos).is(tag);
    }

    @Override
    public List<BlockPos> neighbours(BlockPos pos) {
        List<BlockPos> around = new ArrayList<>(6);
        for (Direction side : Direction.values()) {
            around.add(pos.relative(side));
        }
        return around;
    }

    @Override
    public boolean joined(BlockPos a, BlockPos b) {
        return rule.joined(level, a, b);
    }
}
