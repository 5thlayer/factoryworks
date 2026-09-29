package com.planetaryfactory.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The figures are what {@code scripts/factorio-overload-probe/} read in Factorio 2.1.20 (#517), and
 * the fluid figures what {@code scripts/factorio-overload-fluid-probe/} read (#519).
 */
class OverloadLimitTest {

    private static final OverloadLimit LIMIT = OverloadLimit.get();

    @Test
    void readsFactoriosConstants() {
        assertEquals(new OverloadLimit(1.166, 2, 100, 4, 3,
                Set.of("planetaryfactory:oil_processing/basic_oil_processing")), LIMIT);
    }

    @Test
    void gearOnTierOneHoldsThreeCrafts() {
        assertEquals(3, LIMIT.crafts(0.5, 10));
    }

    @Test
    void gearOnTierThreeHoldsFourCrafts() {
        assertEquals(4, LIMIT.crafts(1.25, 10));
    }

    @Test
    void theOneIsAddedBeforeTheClamp() {
        assertEquals(2, LIMIT.crafts(0.75, 200));
    }

    @Test
    void neverAboveTheMaximum() {
        assertEquals(100, new OverloadLimit(10.0, 2, 100, 4, 3, Set.of()).crafts(1.25, 1));
    }

    @Test
    void anExactQuotientIsNotRoundedUpPastItself() {
        assertEquals(3, new OverloadLimit(1.0, 2, 100, 4, 3, Set.of()).crafts(1.0, 10));
    }

    @Test
    void roomIsWhatTheSlotLacksOfTheLimit() {
        assertEquals(4, OverloadLimit.room(2, 3, 2));
        assertEquals(0, OverloadLimit.room(2, 3, 6));
        assertEquals(0, OverloadLimit.room(2, 3, 64));
    }

    @Test
    void aFluidInputHoldsFourCraftsWhateverTheSpeed() {
        assertEquals(400, LIMIT.fluidRoom(100, 0));
        assertEquals(40, LIMIT.fluidRoom(10, 0));
    }

    @Test
    void fluidRoomIsWhatTheTankLacksOfTheLimit() {
        assertEquals(250, LIMIT.fluidRoom(100, 150));
        assertEquals(0, LIMIT.fluidRoom(100, 400));
        assertEquals(0, LIMIT.fluidRoom(100, 1000));
    }
}
