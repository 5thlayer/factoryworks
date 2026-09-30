package com.factoryworks.core.mining.rig;

import com.factoryworks.core.mining.rig.RigGeometry.Offset;

/**
 * Which {@link RigPanel} each block of a footprint wears (#536). Only the front row's blocks in the
 * Drop Position's column differ: the port at the base, the machine's front on the top layer.
 */
public final class RigPanels {

    private RigPanels() {
    }

    /**
     * @param offset a position from {@link RigGeometry#footprint}, anchor-relative, in the world's
     *               frame for {@code facing}
     */
    public static RigPanel of(int width, int height, int blocksTall, double vectorX,
            RigFacing facing, Offset offset) {
        RigFacing right = facing.rightOf();
        int depth = offset.dx() * facing.dx() + offset.dz() * facing.dz();
        int lateral = offset.dx() * right.dx() + offset.dz() * right.dz();
        if (depth != height - 1 || lateral != RigOutputTile.lateral(width, vectorX)) {
            return RigPanel.CASING;
        }
        if (offset.dy() == 0) {
            return RigPanel.PORT;
        }
        return offset.dy() == blocksTall - 1 ? RigPanel.FRONT : RigPanel.CASING;
    }
}
