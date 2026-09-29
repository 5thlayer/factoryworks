package com.factoryworks.core.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One seed's sampled columns, held to a body's {@link WaterFixture} row (#356).
 *
 * <p>A surface is the generator's base height, the first free block above the ground, so a column
 * is water when it is below sea level.
 */
public final class WaterCensus {

    private final WaterFixture row;
    private final int seaLevel;

    private int columns;
    private int water;
    private int sea;
    private int seaWater;
    private int shore;
    private int shelf;
    private int midDepth;
    private int startColumns;
    private int startWater;

    public WaterCensus(WaterFixture row, int seaLevel) {
        this.row = row;
        this.seaLevel = seaLevel;
    }

    public void column(int surface, boolean isSea, boolean isShore) {
        columns++;
        boolean wet = surface < seaLevel;
        if (isSea) {
            sea++;
        }
        if (isShore) {
            shore++;
        }
        if (!wet) {
            return;
        }
        water++;
        if (isSea) {
            seaWater++;
        }
        if (surface >= seaLevel - row.shelfDepth()) {
            shelf++;
        } else if (surface > row.deepFloor()) {
            midDepth++;
        }
    }

    public void startColumn(int surface) {
        startColumns++;
        if (surface < seaLevel) {
            startWater++;
        }
    }

    public List<String> failures() {
        List<String> failures = new ArrayList<>();
        if (columns == 0 || water == 0 || sea == 0 || startColumns == 0) {
            failures.add("nothing to judge: " + summary());
            return failures;
        }
        double waterShare = share(water, columns);
        if (!row.water().contains(waterShare)) {
            failures.add(String.format(Locale.ROOT, "water covers %.1f%% of columns, outside %.0f-%.0f%%",
                    100 * waterShare, 100 * row.water().min(), 100 * row.water().max()));
        }
        if (share(seaWater, sea) < row.seaIsWater()) {
            failures.add(String.format(Locale.ROOT, "only %.1f%% of sea columns are water",
                    100 * share(seaWater, sea)));
        }
        if (share(seaWater, water) < row.waterIsSea()) {
            failures.add(String.format(Locale.ROOT, "only %.1f%% of water columns are sea",
                    100 * share(seaWater, water)));
        }
        if (share(shore, columns) > row.maxShore()) {
            failures.add(String.format(Locale.ROOT, "shore covers %.1f%% of columns, above %.0f%%",
                    100 * share(shore, columns), 100 * row.maxShore()));
        }
        if (!row.shelf().contains(share(shelf, water))) {
            failures.add(String.format(Locale.ROOT, "the shelf is %.1f%% of water, outside %.0f-%.0f%%",
                    100 * share(shelf, water), 100 * row.shelf().min(), 100 * row.shelf().max()));
        }
        if (midDepth > 0) {
            failures.add(midDepth + " water columns are neither shelf nor bedrock band (surface above y "
                    + row.deepFloor() + ")");
        }
        if (startWater > 0) {
            failures.add(startWater + " of " + startColumns + " columns within " + row.startReach()
                    + " blocks of spawn are water");
        }
        return failures;
    }

    public String summary() {
        return String.format(Locale.ROOT,
                "%d columns: water %.1f%%, sea %.1f%% (%.1f%% wet), water in sea %.1f%%, shore %.1f%%,"
                        + " shelf %.1f%% of water, mid-depth %d, start %d/%d wet",
                columns, 100 * share(water, columns), 100 * share(sea, columns), 100 * share(seaWater, sea),
                100 * share(seaWater, water), 100 * share(shore, columns), 100 * share(shelf, water),
                midDepth, startWater, startColumns);
    }

    private static double share(int part, int whole) {
        return whole == 0 ? 0 : (double) part / whole;
    }
}
