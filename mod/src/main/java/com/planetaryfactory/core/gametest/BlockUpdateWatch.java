package com.planetaryfactory.core.gametest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;

/** Counts the block updates a level sends for the positions a GameTest watches (#351). */
public final class BlockUpdateWatch {

    private static final Map<BlockPos, Integer> COUNTS = new ConcurrentHashMap<>();

    private BlockUpdateWatch() {
    }

    static void watch(BlockPos pos) {
        COUNTS.put(pos.immutable(), 0);
    }

    /** Stops watching the position, returning the updates sent for it since {@link #watch}. */
    static int stop(BlockPos pos) {
        Integer count = COUNTS.remove(pos);
        return count == null ? 0 : count;
    }

    public static void saw(BlockPos pos) {
        if (!COUNTS.isEmpty()) COUNTS.computeIfPresent(pos, (watched, count) -> count + 1);
    }
}
