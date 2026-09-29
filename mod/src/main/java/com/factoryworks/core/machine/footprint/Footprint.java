package com.factoryworks.core.machine.footprint;

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

    /**
     * Factorio's tile square, as tall as it is wide, with the anchor at the bottom centre so the
     * machine sits centred on the block the player clicks. The tile width runs along the lateral
     * {@code z}, the height along {@code x}.
     */
    public static Footprint standing(int tileWidth, int tileHeight) {
        if (tileWidth % 2 == 0 || tileHeight % 2 == 0) {
            throw new IllegalStateException("a " + tileWidth + "x" + tileHeight + " machine has no centre block");
        }
        int halfX = tileHeight / 2;
        int halfZ = tileWidth / 2;
        List<Local> parts = new ArrayList<>();
        for (int y = 0; y < tileWidth; y++) {
            for (int x = -halfX; x <= halfX; x++) {
                for (int z = -halfZ; z <= halfZ; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        parts.add(new Local(x, y, z));
                    }
                }
            }
        }
        return of(parts.toArray(Local[]::new));
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
