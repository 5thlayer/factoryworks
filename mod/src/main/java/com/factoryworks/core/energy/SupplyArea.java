package com.factoryworks.core.energy;

/**
 * The shape of a pole's supply area, as pure integer geometry.
 *
 * <p>A square column: a Factorio supply square horizontally, and a shallow band vertically, which
 * is the dimension Factorio does not have. Deliberately free of every Minecraft type -- the mod's
 * test source set has no Minecraft on its classpath, and this is the half of the pole that can be
 * checked without standing a server up.
 */
public final class SupplyArea {

    /** Receives an offset from the pole, in blocks. */
    @FunctionalInterface
    public interface OffsetSink {
        void accept(int dx, int dy, int dz);
    }

    /**
     * The area's extent as offsets from the pole's base, inclusive on both ends.
     *
     * <p>This is what the <b>Supply Area Box</b> is drawn from (#158, ADR-0070). The overlay shows
     * the area's whole volume rather than a surface, so the extent is the only thing a renderer
     * needs -- and it is asked for here rather than recomputed client-side, because the substation's
     * even-sided offset is exactly the arithmetic that looks right while being half a block wrong.
     * One source for the box and the scan is what keeps them describing the same region.
     */
    public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    }

    private SupplyArea() {
    }

    /**
     * The box this tier's area occupies, as offsets from the base.
     *
     * <p>Exactly the region {@link #forEachOffset} walks and {@link #covers} admits: the area is a
     * box, so its bounds lose nothing and enclose nothing extra.
     */
    public static Bounds bounds(PoleTier tier) {
        int v = tier.verticalRadius();
        return new Bounds(tier.minOffset(), -v, tier.minOffset(),
                tier.maxOffset(), v, tier.maxOffset());
    }

    /** Whether a block at this offset from the pole is inside its supply area. */
    public static boolean covers(PoleTier tier, int dx, int dy, int dz) {
        return dx >= tier.minOffset() && dx <= tier.maxOffset()
                && dz >= tier.minOffset() && dz <= tier.maxOffset()
                && Math.abs(dy) <= tier.verticalRadius();
    }

    /**
     * Every offset in the area, each exactly once, the pole's own position included.
     *
     * <p>The pole's own block is in the enumeration rather than skipped: the caller is scanning for
     * an energy capability, and the pole does not expose one to itself, so excluding it here would
     * be a special case that buys nothing.
     */
    public static void forEachOffset(PoleTier tier, OffsetSink sink) {
        int v = tier.verticalRadius();
        for (int dx = tier.minOffset(); dx <= tier.maxOffset(); dx++) {
            for (int dz = tier.minOffset(); dz <= tier.maxOffset(); dz++) {
                for (int dy = -v; dy <= v; dy++) {
                    sink.accept(dx, dy, dz);
                }
            }
        }
    }

    /** How many blocks a tier's scan visits. Useful for sizing collections and for the tests. */
    public static int volume(PoleTier tier) {
        int side = tier.supplySize();
        return side * side * (2 * tier.verticalRadius() + 1);
    }
}
