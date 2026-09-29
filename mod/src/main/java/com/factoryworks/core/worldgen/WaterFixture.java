package com.factoryworks.core.worldgen;

import java.util.List;

/**
 * One body's water facts, as a fixture row: the next body adds a row, not harness code (#356).
 *
 * <p>Terra's water share is Nauvis's (ADR-0019's <em>Water</em> paragraph); the other bands are
 * #356's tolerances.
 *
 * @param stem       the datapack's dimension file, decoded rather than read off the level, because
 *                   the GameTest world is flat
 * @param shelfDepth the deepest a shelf column's water may be
 * @param deepFloor  the highest surface a column past the shelf may have: the bedrock band
 * @param startReach how far from the spawn search's point no column may be water
 */
public record WaterFixture(
        String body,
        String stem,
        String seaBiome,
        String shoreBiome,
        List<Long> seeds,
        int radius,
        int step,
        Band water,
        double seaIsWater,
        double waterIsSea,
        double maxShore,
        Band shelf,
        int shelfDepth,
        int deepFloor,
        int startReach,
        int startStep) {

    public record Band(double min, double max) {
        boolean contains(double value) {
            return value >= min && value <= max;
        }
    }

    public static final WaterFixture TERRA = new WaterFixture(
            "terra",
            "dimension/overworld.json",
            "factoryworks:terra_sea",
            "factoryworks:terra_shore",
            List.of(0L, 1L, 20260921L),
            4096,
            128,
            new Band(0.20, 0.32),
            0.90,
            0.90,
            0.05,
            new Band(0.08, 0.25),
            4,
            8,
            TerraStartingArea.MAX_DISTANCE_FROM_CENTER,
            8);

    public static final List<WaterFixture> ROWS = List.of(TERRA);
}
