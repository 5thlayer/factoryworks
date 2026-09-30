package com.factoryworks.core.mining.rig;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.factoryworks.core.mining.rig.RigGeometry.Offset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RigPanelsTest {

    private static final double BURNER_X = -0.35;
    private static final double BURNER_Y = -1.3;
    private static final double ELECTRIC_X = 0.0;
    private static final double ELECTRIC_Y = -1.85;

    private static List<Offset> wearing(RigPanel panel, int size, int tall, double vectorX,
            RigFacing facing) {
        List<Offset> found = new ArrayList<>();
        for (Offset offset : RigGeometry.footprint(size, size, tall, facing)) {
            if (RigPanels.of(size, size, tall, vectorX, facing, offset) == panel) {
                found.add(offset);
            }
        }
        return found;
    }

    /** The block standing just behind the Drop Position, at the rig's base. */
    private static Offset behind(Offset drop, RigFacing facing) {
        return new Offset(drop.dx() - facing.dx(), 0, drop.dz() - facing.dz());
    }

    @Test
    void everyFacingHasExactlyOnePortAndItTouchesTheDropPosition() {
        for (RigFacing facing : RigFacing.values()) {
            Offset burnerDrop = RigOutputTile.of(2, 2, BURNER_X, BURNER_Y, facing);
            assertEquals(List.of(behind(burnerDrop, facing)),
                    wearing(RigPanel.PORT, 2, 2, BURNER_X, facing), "burner " + facing);

            Offset electricDrop = RigOutputTile.of(3, 3, ELECTRIC_X, ELECTRIC_Y, facing);
            assertEquals(List.of(behind(electricDrop, facing)),
                    wearing(RigPanel.PORT, 3, 3, ELECTRIC_X, facing), "electric " + facing);
        }
    }

    @Test
    void theBurnerPortIsInItsLeftColumnAndTheElectricPortInItsMiddle() {
        // Facing north, lateral runs east from the anchor at dx 0, and the front row is the
        // furthest north.
        assertEquals(List.of(new Offset(0, 0, -1)),
                wearing(RigPanel.PORT, 2, 2, BURNER_X, RigFacing.NORTH));
        assertEquals(List.of(new Offset(1, 0, -2)),
                wearing(RigPanel.PORT, 3, 3, ELECTRIC_X, RigFacing.NORTH));
    }

    @Test
    void theFrontSitsOnTheTopLayerAboveThePort() {
        for (RigFacing facing : RigFacing.values()) {
            Offset port = wearing(RigPanel.PORT, 2, 2, BURNER_X, facing).getFirst();
            assertEquals(List.of(new Offset(port.dx(), 1, port.dz())),
                    wearing(RigPanel.FRONT, 2, 2, BURNER_X, facing), "burner " + facing);

            Offset electricPort = wearing(RigPanel.PORT, 3, 3, ELECTRIC_X, facing).getFirst();
            assertEquals(List.of(new Offset(electricPort.dx(), 2, electricPort.dz())),
                    wearing(RigPanel.FRONT, 3, 3, ELECTRIC_X, facing), "electric " + facing);
        }
    }

    @Test
    void everyOtherBlockIsCasingTheAnchorIncluded() {
        for (RigFacing facing : RigFacing.values()) {
            assertEquals(6, wearing(RigPanel.CASING, 2, 2, BURNER_X, facing).size());
            assertEquals(25, wearing(RigPanel.CASING, 3, 3, ELECTRIC_X, facing).size());
            assertEquals(RigPanel.CASING,
                    RigPanels.of(3, 3, 3, ELECTRIC_X, facing, new Offset(0, 0, 0)));
        }
    }
}
