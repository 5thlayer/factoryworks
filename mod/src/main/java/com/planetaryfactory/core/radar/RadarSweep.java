package com.planetaryfactory.core.radar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The order a Radar charts its reach in (#368): every sector within {@code reach} of its own,
 * nearest first. A pass is one walk of that order, so a Radar's whole state is a cursor into it.
 */
public final class RadarSweep {

    private final List<Sector> offsets;

    private RadarSweep(List<Sector> offsets) {
        this.offsets = List.copyOf(offsets);
    }

    public static RadarSweep of(int reach) {
        List<Sector> offsets = new ArrayList<>((2 * reach + 1) * (2 * reach + 1));
        for (int dz = -reach; dz <= reach; dz++) {
            for (int dx = -reach; dx <= reach; dx++) {
                offsets.add(new Sector(dx, dz));
            }
        }
        offsets.sort(Comparator.<Sector>comparingInt(o -> o.x() * o.x() + o.z() * o.z())
                .thenComparingInt(Sector::z)
                .thenComparingInt(Sector::x));
        return new RadarSweep(offsets);
    }

    public int size() {
        return offsets.size();
    }

    public Sector sectorAt(Sector origin, int cursor) {
        Sector offset = offsets.get(cursor);
        return origin.offset(offset.x(), offset.z());
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
