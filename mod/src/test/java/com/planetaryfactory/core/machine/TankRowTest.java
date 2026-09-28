package com.planetaryfactory.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class TankRowTest {

    @Test
    void noTanksLayOutNothing() {
        assertEquals(List.of(), TankRow.split(8, 70, 0));
    }

    @Test
    void oneTankTakesTheWholeRegion() {
        assertEquals(List.of(new TankRow.Bar(8, 70)), TankRow.split(8, 70, 1));
    }

    @Test
    void theRefinerysThreeOutputsShareTheRegionWithAGapBetween() {
        assertEquals(List.of(new TankRow.Bar(84, 26), new TankRow.Bar(113, 26), new TankRow.Bar(142, 26)),
                TankRow.split(84, 84, 3));
    }

    @Test
    void theRemainderGoesToTheLastBarSoTheRowEndsFlush() {
        List<TankRow.Bar> bars = TankRow.split(8, 70, 3);
        TankRow.Bar last = bars.getLast();
        assertEquals(8 + 70, last.x() + last.width());
    }
}
