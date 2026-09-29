package com.planetaryfactory.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Every figure is one {@code scripts/factorio-overload-fluid-probe/} read in Factorio 2.1.20 (#520). */
class OutputTankVolumeTest {

    private static final List<Integer> CHEMICAL_PLANT = List.of(100, 100);
    private static final List<Integer> REFINERY = List.of(100, 100, 100);

    @Test
    void aLoneProductTakesTheUnusedBoxes() {
        assertEquals(List.of(200), OutputTankVolume.of(CHEMICAL_PLANT, List.of(10), false, 3));
        assertEquals(List.of(300), OutputTankVolume.of(REFINERY, List.of(20), false, 3));
    }

    @Test
    void threeCraftsWinOverTheBoxes() {
        assertEquals(List.of(300), OutputTankVolume.of(CHEMICAL_PLANT, List.of(100), false, 3));
        assertEquals(List.of(200), OutputTankVolume.of(CHEMICAL_PLANT, List.of(60), false, 3));
        assertEquals(List.of(270, 100, 100), OutputTankVolume.of(REFINERY, List.of(90, 20, 10), false, 3));
    }

    @Test
    void onlyTheFirstProductTakesTheUnusedBoxes() {
        assertEquals(List.of(200, 100), OutputTankVolume.of(REFINERY, List.of(20, 20), false, 3));
        assertEquals(List.of(100, 100), OutputTankVolume.of(CHEMICAL_PLANT, List.of(10, 10), false, 3));
    }

    @Test
    void aPinnedProductTakesNoUnusedBox() {
        assertEquals(List.of(135), OutputTankVolume.of(REFINERY, List.of(45), true, 3));
    }

    @Test
    void advancedOilProcessing() {
        assertEquals(List.of(100, 135, 165), OutputTankVolume.of(REFINERY, List.of(25, 45, 55), false, 3));
    }
}
