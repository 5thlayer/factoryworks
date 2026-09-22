package com.planetaryfactory.core.radar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * One Radar's order (ADR-0079): its nearby area, charted as one pulse, and beyond it the long range
 * in square rings outward from its own sector, each from its top-left sector clockwise. A long-range
 * scan takes the first sector in that order the chart lacks, and re-scans in turn once none is left.
 */
public final class RadarSweep {

    private final List<Sector> nearby;
    private final List<Sector> longRange;

    private RadarSweep(List<Sector> nearby, List<Sector> longRange) {
        this.nearby = List.copyOf(nearby);
        this.longRange = List.copyOf(longRange);
    }

    /**
     * The nearby area is the {@code span}-wide square of sectors whose centre is nearest the Radar's
     * block. An even span has no middle sector, so which way it reaches follows the half of its own
     * sector the Radar stands in.
     */
    public static RadarSweep around(int blockX, int blockZ, int span, int reach) {
        Sector own = Sector.ofBlock(blockX, blockZ);
        int half = span * Sector.SIZE / 2;
        int minX = Math.floorDiv(blockX - half + Sector.SIZE / 2, Sector.SIZE);
        int minZ = Math.floorDiv(blockZ - half + Sector.SIZE / 2, Sector.SIZE);
        Set<Sector> square = new HashSet<>();
        for (int x = minX; x < minX + span; x++) {
            for (int z = minZ; z < minZ + span; z++) {
                square.add(new Sector(x, z));
            }
        }
        List<Sector> nearby = new ArrayList<>();
        List<Sector> longRange = new ArrayList<>();
        for (Sector sector : rings(own, Math.max(reach, span))) {
            if (square.contains(sector)) {
                nearby.add(sector);
            } else if (Math.max(Math.abs(sector.x() - own.x()), Math.abs(sector.z() - own.z())) <= reach) {
                longRange.add(sector);
            }
        }
        return new RadarSweep(nearby, longRange);
    }

    /** Rings 0 to {@code to} around {@code own}, each clockwise from its top-left, north being -z. */
    private static List<Sector> rings(Sector own, int to) {
        List<Sector> sectors = new ArrayList<>();
        sectors.add(own);
        for (int r = 1; r <= to; r++) {
            for (int dx = -r; dx < r; dx++) {
                sectors.add(own.offset(dx, -r));
            }
            for (int dz = -r; dz < r; dz++) {
                sectors.add(own.offset(r, dz));
            }
            for (int dx = r; dx > -r; dx--) {
                sectors.add(own.offset(dx, r));
            }
            for (int dz = r; dz > -r; dz--) {
                sectors.add(own.offset(-r, dz));
            }
        }
        return sectors;
    }

    public List<Sector> nearby() {
        return nearby;
    }

    public int size() {
        return longRange.size();
    }

    public Sector sectorAt(int cursor) {
        return longRange.get(cursor);
    }

    /** The long-range scan due: the first sector the chart lacks, or else the re-scan at {@code cursor}. */
    public int pick(Predicate<Sector> charted, int cursor) {
        for (int i = 0; i < longRange.size(); i++) {
            if (!charted.test(longRange.get(i))) {
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
        return cursor >= 0 && cursor < longRange.size() ? cursor : 0;
    }
}
