package com.planetaryfactory.core.ore;

/**
 * One generated outfield patch, as its structure piece records it: the size factor it drew, the
 * blocks it placed, and its centre column.
 *
 * <p>Every block of the disc holds the same amount, read at the centre's distance from origin
 * rather than the block's (ADR-0045).
 */
public record OutfieldDisc(OreResource resource, double sizeFactor, int blockCount, int centreX,
        int centreZ) {

    /** Implemented by the structure piece that places a disc (#320). */
    public interface Source {
        OutfieldDisc disc();

        /** Whether the disc placed ore in this column; its box is a square around a ragged disc. */
        boolean covers(int x, int z);
    }

    public double distance() {
        return Math.sqrt((double) centreX * centreX + (double) centreZ * centreZ);
    }

    /** What the placed blocks hold between them, which the floored quotient leaves under the law's total. */
    public long total() {
        return (long) blockCount * amountPerBlock();
    }

    public int amountPerBlock() {
        long total = OutfieldLaw.of(resource).total(sizeFactor, distance());
        return new OreField(resource.key(), total, blockCount).amountPerBlock();
    }
}
