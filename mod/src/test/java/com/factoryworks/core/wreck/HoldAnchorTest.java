package com.factoryworks.core.wreck;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The anchor walk over a grid of cells, whichever way the hold lies (ADR-0107, #548). */
class HoldAnchorTest {

    private record Cell(int x, int y, int z) {
    }

    private static List<Cell> around(Cell c) {
        List<Cell> out = new ArrayList<>();
        for (int[] d : new int[][] {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
            out.add(new Cell(c.x + d[0], c.y + d[1], c.z + d[2]));
        }
        return out;
    }

    /** Five along an axis by two up, the anchor the bottom middle. */
    private static Set<Cell> hold(boolean alongX) {
        Set<Cell> cells = new java.util.HashSet<>();
        for (int i = 0; i < 5; i++) {
            for (int y = 0; y < 2; y++) {
                cells.add(alongX ? new Cell(i, y, 0) : new Cell(0, y, i));
            }
        }
        return cells;
    }

    private static Cell anchorOf(boolean alongX) {
        return alongX ? new Cell(2, 0, 0) : new Cell(0, 0, 2);
    }

    private static Optional<Cell> resolve(Set<Cell> cells, Set<Cell> anchors, Cell from) {
        return HoldAnchor.resolve(from, HoldAnchorTest::around, cells::contains, anchors::contains);
    }

    @Test
    void everyPartResolvesToTheAnchorWhicheverWayTheHoldLies() {
        for (boolean alongX : new boolean[] {true, false}) {
            Set<Cell> cells = hold(alongX);
            Set<Cell> anchors = Set.of(anchorOf(alongX));
            for (Cell part : cells) {
                assertEquals(Optional.of(anchorOf(alongX)), resolve(cells, anchors, part));
            }
        }
    }

    @Test
    void aHoldWithNoAnchorResolvesToNothing() {
        Set<Cell> cells = hold(true);
        assertTrue(resolve(cells, Set.of(), new Cell(0, 0, 0)).isEmpty());
    }

    @Test
    void aHoldWithTwoAnchorsResolvesToNothingFromEveryPart() {
        Set<Cell> cells = hold(true);
        Set<Cell> anchors = Set.of(new Cell(2, 0, 0), new Cell(4, 1, 0));
        for (Cell part : cells) {
            assertTrue(resolve(cells, anchors, part).isEmpty());
        }
    }

    @Test
    void aBlockThatIsNotHoldResolvesToNothing() {
        Set<Cell> cells = hold(true);
        assertTrue(resolve(cells, Set.of(anchorOf(true)), new Cell(9, 9, 9)).isEmpty());
    }

    @Test
    void anotherHoldNotTouchingThisOneIsNotCounted() {
        Set<Cell> cells = hold(true);
        Cell far = new Cell(2, 0, 5);
        cells.add(far);
        assertEquals(Optional.of(anchorOf(true)),
                resolve(cells, Set.of(anchorOf(true), far), new Cell(0, 0, 0)));
    }

    @Test
    void aMassLargerThanTheLimitIsNotWalked() {
        Set<Cell> cells = new java.util.HashSet<>();
        for (int x = 0; x <= HoldAnchor.LIMIT; x++) {
            cells.add(new Cell(x, 0, 0));
        }
        Cell anchor = new Cell(HoldAnchor.LIMIT, 0, 0);
        assertTrue(resolve(cells, Set.of(anchor), new Cell(0, 0, 0)).isEmpty());
        cells.remove(anchor);
        Cell near = new Cell(HoldAnchor.LIMIT - 1, 0, 0);
        assertEquals(Optional.of(near), resolve(cells, Set.of(near), new Cell(0, 0, 0)));
    }
}
