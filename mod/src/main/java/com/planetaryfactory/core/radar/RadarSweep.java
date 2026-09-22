package com.planetaryfactory.core.radar;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Factorio's Radar order (ADR-0079): the nearby area, charted as one pulse, and beyond it the long
 * range in square rings outward, each from its top-left sector clockwise. A long-range scan takes
 * the first sector in that order the chart lacks, and re-scans in turn once none is left.
 */
public final class RadarSweep {

    private final List<Sector> nearby;
    private final List<Sector> offsets;

    private RadarSweep(List<Sector> nearby, List<Sector> offsets) {
        this.nearby = List.copyOf(nearby);
        this.offsets = List.copyOf(offsets);
    }

    public static RadarSweep of(int nearReach, int reach) {
        return new RadarSweep(rings(0, nearReach), rings(nearReach + 1, reach));
    }

    /** Rings {@code from} to {@code to}, each clockwise from its top-left, north being -z. */
    private static List<Sector> rings(int from, int to) {
        List<Sector> offsets = new ArrayList<>();
        for (int r = from; r <= to; r++) {
            if (r == 0) {
                offsets.add(new Sector(0, 0));
                continue;
            }
            for (int dx = -r; dx < r; dx++) {
                offsets.add(new Sector(dx, -r));
            }
            for (int dz = -r; dz < r; dz++) {
                offsets.add(new Sector(r, dz));
            }
            for (int dx = r; dx > -r; dx--) {
                offsets.add(new Sector(dx, r));
            }
            for (int dz = r; dz > -r; dz--) {
                offsets.add(new Sector(-r, dz));
            }
        }
        return offsets;
    }

    public List<Sector> nearby(Sector origin) {
        return nearby.stream().map(offset -> origin.offset(offset.x(), offset.z())).toList();
    }

    public int size() {
        return offsets.size();
    }

    public Sector sectorAt(Sector origin, int cursor) {
        Sector offset = offsets.get(cursor);
        return origin.offset(offset.x(), offset.z());
    }

    /** The long-range scan due: the first sector the chart lacks, or else the re-scan at {@code cursor}. */
    public int pick(Sector origin, Predicate<Sector> charted, int cursor) {
        for (int i = 0; i < offsets.size(); i++) {
            if (!charted.test(sectorAt(origin, i))) {
                return i;
            }
        }
        return resume(cursor);
    }

    /** The cursor after {@code cursor}, starting the next pass after the last. */
    public int next(int cursor) {
        return resume(cursor + 1);
    }

    /** A saved cursor, or the start of a pass if it no longer points into this reach. */
    public int resume(int cursor) {
        return cursor >= 0 && cursor < offsets.size() ? cursor : 0;
    }
}
