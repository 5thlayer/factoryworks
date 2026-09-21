package com.planetaryfactory.core.placement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.planetaryfactory.core.mining.rig.RigCorpus;
import com.planetaryfactory.core.mining.rig.RigFacing;
import com.planetaryfactory.core.mining.rig.RigGeometry;
import com.planetaryfactory.core.mining.rig.RigTier;
import com.planetaryfactory.core.placement.PlanHull.Cell;
import com.planetaryfactory.core.placement.PlanHull.Face;
import com.planetaryfactory.core.placement.PlanHull.Side;

import org.junit.jupiter.api.Test;

class PlanHullTest {

    @Test
    void aSingleBlockShowsAllSixFaces() {
        assertEquals(6, PlanHull.boundary(List.of(new Cell(0, 0, 0))).size());
    }

    @Test
    void twoTouchingBlocksHideTheFaceBetweenThem() {
        Set<Face> faces = PlanHull.boundary(List.of(new Cell(0, 0, 0), new Cell(1, 0, 0)));
        assertEquals(10, faces.size());
        assertFalse(faces.contains(new Face(new Cell(0, 0, 0), Side.EAST)));
        assertFalse(faces.contains(new Face(new Cell(1, 0, 0), Side.WEST)));
    }

    /** A solid box shows its surface area and nothing inside, for every rig at every facing. */
    @Test
    void everyRigFootprintShowsOnlyItsOuterShell() {
        for (RigTier tier : RigTier.values()) {
            RigCorpus.Row row = RigCorpus.get().rowOf(tier);
            int w = row.width();
            int depth = row.height();
            int h = tier.blocksTall();
            for (RigFacing facing : RigFacing.values()) {
                List<Cell> cells = new ArrayList<>();
                for (RigGeometry.Offset o : RigGeometry.footprint(w, depth, h, facing)) {
                    cells.add(new Cell(o.dx(), o.dy(), o.dz()));
                }
                Set<Face> faces = PlanHull.boundary(cells);
                assertEquals(2 * (w * depth + w * h + depth * h), faces.size(), tier + " " + facing);
                for (Face face : faces) {
                    assertFalse(cells.contains(face.cell().step(face.side())),
                            tier + " " + facing + " draws an inner face " + face);
                }
            }
        }
    }

    /**
     * A hole follows the footprint rather than the bounding box (#311): a 3x3 ring keeps the four
     * faces that look into its empty middle.
     */
    @Test
    void aHoleKeepsItsInwardFaces() {
        List<Cell> ring = new ArrayList<>();
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                if (x != 1 || z != 1) {
                    ring.add(new Cell(x, 0, z));
                }
            }
        }
        Set<Face> faces = PlanHull.boundary(ring);
        assertEquals(8 * 6 - 2 * 8, faces.size());
        assertTrue(faces.contains(new Face(new Cell(1, 0, 0), Side.SOUTH)));
        assertTrue(faces.contains(new Face(new Cell(1, 0, 2), Side.NORTH)));
        assertTrue(faces.contains(new Face(new Cell(0, 0, 1), Side.EAST)));
        assertTrue(faces.contains(new Face(new Cell(2, 0, 1), Side.WEST)));
    }

    /** The client maps a side to Minecraft's {@code Direction} by name. */
    @Test
    void sidesAreNamedAsMinecraftsDirections() {
        assertEquals(Set.of("DOWN", "UP", "NORTH", "SOUTH", "WEST", "EAST"),
                Set.copyOf(List.of(Side.values()).stream().map(Side::name).toList()));
    }
}
