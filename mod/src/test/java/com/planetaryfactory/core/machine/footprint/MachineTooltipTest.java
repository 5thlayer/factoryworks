package com.planetaryfactory.core.machine.footprint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class MachineTooltipTest {

    @Test
    void aCraftingMachineShowsItsSpeedAndDraw() {
        assertEquals(List.of(
                        new MachineTooltip.Line("tooltip.planetaryfactory.speed", List.of("0.5")),
                        new MachineTooltip.Line("tooltip.planetaryfactory.draws", List.of("37.5"))),
                MachineTooltip.crafting(0.5, 75_000));
    }

    @Test
    void wholeFiguresCarryNoDecimal() {
        assertEquals(List.of(
                        new MachineTooltip.Line("tooltip.planetaryfactory.speed", List.of("1")),
                        new MachineTooltip.Line("tooltip.planetaryfactory.draws", List.of("21"))),
                MachineTooltip.crafting(1.0, 42_000));
    }

    @Test
    void theSteamEngineShowsWhatItBurnsAndMakes() {
        assertEquals(List.of(
                        new MachineTooltip.Line("tooltip.planetaryfactory.burns_steam", List.of("30")),
                        new MachineTooltip.Line("tooltip.planetaryfactory.makes_up_to", List.of("450"))),
                MachineTooltip.steamEngine(30.0, 450.0));
    }

    @Test
    void theAccumulatorShowsWhatItHoldsAndPasses() {
        assertEquals(List.of(
                        new MachineTooltip.Line("tooltip.planetaryfactory.holds", List.of("50,000")),
                        new MachineTooltip.Line("tooltip.planetaryfactory.in_and_out", List.of("150"))),
                MachineTooltip.accumulator(50_000, 150));
    }

    @Test
    void theRadarShowsItsDrawAndReach() {
        assertEquals(List.of(
                        new MachineTooltip.Line("tooltip.planetaryfactory.draws", List.of("150")),
                        new MachineTooltip.Line("tooltip.planetaryfactory.charts", List.of("4", "14"))),
                MachineTooltip.radar(150, 4, 14));
    }
}
