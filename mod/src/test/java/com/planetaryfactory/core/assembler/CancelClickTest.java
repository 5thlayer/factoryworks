package com.planetaryfactory.core.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** A click on a queue icon cancels crafts of that row's final item, Factorio's way (#290). */
class CancelClickTest {

    @Test
    void leftClickCancelsOne() {
        assertEquals(1, CancelClick.crafts(0, false));
    }

    @Test
    void rightClickCancelsFive() {
        assertEquals(5, CancelClick.crafts(1, false));
    }

    @Test
    void shiftCancelsAllWhicheverButton() {
        assertEquals(AssemblerQueue.ALL, CancelClick.crafts(0, true));
        assertEquals(AssemblerQueue.ALL, CancelClick.crafts(1, true));
    }

    @Test
    void anyOtherButtonCancelsNothing() {
        assertEquals(0, CancelClick.crafts(2, false));
        assertEquals(0, CancelClick.crafts(2, true));
    }
}
