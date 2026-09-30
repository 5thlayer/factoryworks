package com.factoryworks.core.mining.rig;

import java.util.function.IntPredicate;

/**
 * Which ore block a rig works now (#537): {@value #OPERATIONS_PER_TURN} operations on a block,
 * then the next block in {@link RigArea}'s order that still has ore, wrapping. Every block gets the
 * same share whatever its type or amount.
 *
 * <p>The state is the block's index and the operations spent on it; both are saved so a reload
 * resumes the turn.
 */
public final class RigRotation {

    /** Factorio's per-block turn before a drill moves on. */
    public static final int OPERATIONS_PER_TURN = 10;

    private int index;
    private int spent;

    /**
     * The index of the block to mine, or {@code -1} when none of {@code size} has ore. Repeated
     * calls with no {@link #completed()} between them answer the same block.
     */
    public int current(int size, IntPredicate hasOre) {
        if (size <= 0) {
            return -1;
        }
        if (index >= size || index < 0) {
            index = 0;
            spent = 0;
        }
        if (spent < OPERATIONS_PER_TURN && hasOre.test(index)) {
            return index;
        }
        // The block's own index is tried last, so a lone block with ore starts a fresh turn.
        for (int step = 1; step <= size; step++) {
            int candidate = (index + step) % size;
            if (hasOre.test(candidate)) {
                index = candidate;
                spent = 0;
                return candidate;
            }
        }
        return -1;
    }

    /** One operation was performed on the block {@link #current} named. */
    public void completed() {
        spent++;
    }

    public int index() {
        return index;
    }

    public int spent() {
        return spent;
    }

    public void load(int index, int spent) {
        this.index = Math.max(0, index);
        this.spent = Math.max(0, spent);
    }
}
