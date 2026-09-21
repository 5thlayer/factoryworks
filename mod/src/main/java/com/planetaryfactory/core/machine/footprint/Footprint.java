package com.planetaryfactory.core.machine.footprint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Where a placed-whole machine stands, relative to its anchor (ADR-0072, ADR-0077): the anchor
 * first, then each part, numbered from 1 in the order given.
 *
 * <p>The offsets are in Oritech's controller-local frame -- {@code x} forward, {@code y} up,
 * {@code z} lateral -- because Oritech's renderer rotates the model by the block's facing in that
 * frame, and the blocks land under the model only while the footprint is rotated the same way.
 */
public record Footprint(List<Local> offsets) {

    private static final Local ANCHOR = new Local(0, 0, 0);

    public Footprint {
        if (offsets.isEmpty() || !offsets.getFirst().equals(ANCHOR)) {
            throw new IllegalArgumentException("a footprint starts at its anchor, got " + offsets);
        }
        if (new HashSet<>(offsets).size() != offsets.size()) {
            throw new IllegalArgumentException("a footprint names a position twice: " + offsets);
        }
        offsets = List.copyOf(offsets);
    }

    public static Footprint of(Local... parts) {
        List<Local> offsets = new ArrayList<>(parts.length + 1);
        offsets.add(ANCHOR);
        offsets.addAll(List.of(parts));
        return new Footprint(offsets);
    }

    public int partCount() {
        return offsets.size() - 1;
    }

    public Local offsetOfPart(int part) {
        if (part < 1 || part > partCount()) {
            throw new IllegalArgumentException("a part is numbered 1 to " + partCount() + ", got " + part);
        }
        return offsets.get(part);
    }

    /** One position relative to the anchor, in Oritech's controller-local frame. */
    public record Local(int x, int y, int z) {
    }
}
