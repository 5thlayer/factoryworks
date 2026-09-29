package com.factoryworks.core.dismantle;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Whether two touching members of one family are joined, where touching is not enough (ADR-0086). */
@FunctionalInterface
public interface JoinRule {

    JoinRule TOUCHING = (level, a, b) -> true;

    boolean joined(Level level, BlockPos a, BlockPos b);
}
