package com.planetaryfactory.core.placement;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * The faces on the outside of a set of blocks: a face whose neighbour is not in the set (#311).
 * The preview draws only these, so a multiblock reads as the formed structure. A hole in the set
 * keeps its inward faces, since it follows the footprint rather than the bounding box (ADR-0069).
 */
public final class PlanHull {

    private PlanHull() {
    }

    /** Named as Minecraft's {@code Direction}, which the client maps it to by name. */
    public enum Side {
        DOWN(0, -1, 0),
        UP(0, 1, 0),
        NORTH(0, 0, -1),
        SOUTH(0, 0, 1),
        WEST(-1, 0, 0),
        EAST(1, 0, 0);

        private final int dx;
        private final int dy;
        private final int dz;

        Side(int dx, int dy, int dz) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }
    }

    public record Cell(int x, int y, int z) {

        public Cell step(Side side) {
            return new Cell(x + side.dx, y + side.dy, z + side.dz);
        }
    }

    public record Face(Cell cell, Side side) {
    }

    public static Set<Face> boundary(Collection<Cell> cells) {
        Set<Cell> occupied = Set.copyOf(cells);
        Set<Face> faces = new HashSet<>();
        for (Cell cell : occupied) {
            for (Side side : Side.values()) {
                if (!occupied.contains(cell.step(side))) {
                    faces.add(new Face(cell, side));
                }
            }
        }
        return faces;
    }
}
