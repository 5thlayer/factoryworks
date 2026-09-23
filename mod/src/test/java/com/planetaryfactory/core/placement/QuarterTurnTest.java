package com.planetaryfactory.core.placement;

import com.planetaryfactory.core.placement.QuarterTurn.Heading;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class QuarterTurnTest {

    @Test
    @DisplayName("absent reads as no turn")
    void absentIsZero() {
        assertEquals(0, QuarterTurn.orNone(null).quarters());
        assertEquals(QuarterTurn.NONE, QuarterTurn.of(0));
    }

    @Test
    @DisplayName("Rotate wraps from 3 to 0")
    void rotateWraps() {
        QuarterTurn turn = QuarterTurn.NONE;
        int[] seen = new int[5];
        for (int press = 0; press < 5; press++) {
            seen[press] = turn.quarters();
            turn = turn.rotate();
        }
        assertArrayEquals(new int[] {0, 1, 2, 3, 0}, seen);
    }

    @Test
    @DisplayName("Reverse Rotate wraps from 0 to 3")
    void reverseWraps() {
        assertEquals(3, QuarterTurn.NONE.reverseRotate().quarters());
        assertEquals(2, QuarterTurn.NONE.reverseRotate().reverseRotate().quarters());
        assertEquals(QuarterTurn.NONE, QuarterTurn.of(1).reverseRotate());
    }

    @Test
    @DisplayName("of wraps any integer into 0 to 3")
    void ofWraps() {
        assertEquals(1, QuarterTurn.of(5).quarters());
        assertEquals(3, QuarterTurn.of(-1).quarters());
        assertEquals(0, QuarterTurn.of(-8).quarters());
    }

    @Test
    @DisplayName("each offset turns each heading clockwise that many quarters")
    void turnsEveryHeading() {
        Heading[] clockwise = {Heading.NORTH, Heading.EAST, Heading.SOUTH, Heading.WEST};
        for (int start = 0; start < 4; start++) {
            for (int quarters = 0; quarters < 4; quarters++) {
                assertEquals(clockwise[(start + quarters) % 4], QuarterTurn.of(quarters).turn(clockwise[start]),
                        clockwise[start] + " turned " + quarters);
            }
        }
    }

    @Test
    @DisplayName("Minecraft's 2D data value order is the headings' order")
    void dataValueOrder() {
        assertEquals(Heading.SOUTH, Heading.fromDataValue(0));
        assertEquals(Heading.WEST, Heading.fromDataValue(1));
        assertEquals(Heading.NORTH, Heading.fromDataValue(2));
        assertEquals(Heading.EAST, Heading.fromDataValue(3));
    }

    @Test
    @DisplayName("a yaw turns by 90 degrees a quarter")
    void yawTurns() {
        assertEquals(30f, QuarterTurn.NONE.turnYaw(30f));
        assertEquals(120f, QuarterTurn.of(1).turnYaw(30f));
        assertEquals(300f, QuarterTurn.of(3).turnYaw(30f));
    }
}
