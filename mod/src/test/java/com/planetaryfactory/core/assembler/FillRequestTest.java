package com.planetaryfactory.core.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * EMI's {@code + Fill Recipe} on the Assembler's screen queues without the Crafting Plan (#288,
 * ADR-0065): which click asks for what, and when the plan opens instead.
 */
class FillRequestTest {

    private static final int NO_BUTTON = FillRequest.NO_BUTTON;

    @Test
    void leftClickAsksForOne() {
        assertEquals(FillRequest.ONE, FillRequest.of(0, false));
    }

    @Test
    void rightClickAsksForFive() {
        assertEquals(FillRequest.FIVE, FillRequest.of(1, false));
    }

    @Test
    void middleClickAsksForThePlan() {
        assertEquals(FillRequest.PLAN, FillRequest.of(2, false));
    }

    @Test
    void shiftAsksForAllWhicheverButton() {
        assertEquals(FillRequest.ALL, FillRequest.of(0, true));
        assertEquals(FillRequest.ALL, FillRequest.of(1, true));
        assertEquals(FillRequest.ALL, FillRequest.of(2, true));
    }

    @Test
    void anExtraMouseButtonChangesNothing() {
        assertEquals(FillRequest.PLAN, FillRequest.of(4, false));
    }

    @Test
    void emisHotkeysCarryNoButtonAndReadAsOneOrAll() {
        assertEquals(FillRequest.ONE, FillRequest.of(NO_BUTTON, false));
        assertEquals(FillRequest.ALL, FillRequest.of(NO_BUTTON, true));
    }

    @Test
    void anAffordableRequestQueuesItsCount() {
        assertEquals(1, FillRequest.ONE.queueCount(1));
        assertEquals(5, FillRequest.FIVE.queueCount(5));
        assertEquals(12, FillRequest.ALL.queueCount(12));
    }

    @Test
    void fiveNeverBecomesThree() {
        assertEquals(0, FillRequest.FIVE.queueCount(3));
    }

    @Test
    void nothingAffordableQueuesNothing() {
        assertEquals(0, FillRequest.ONE.queueCount(0));
        assertEquals(0, FillRequest.ALL.queueCount(0));
        assertEquals(0, FillRequest.ALL.queueCount(-1));
    }

    @Test
    void thePlanQueuesNothingHoweverMuchIsAffordable() {
        assertEquals(0, FillRequest.PLAN.queueCount(50));
    }
}
