package com.factoryworks.core.machine.footprint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class MachineTooltipTest {

    @Test
    void aCraftingMachineShowsItsSpeedAndDraw() {
        assertEquals(List.of(
                        new MachineTooltip.Line("tooltip.factoryworks.speed", List.of("0.5")),
                        new MachineTooltip.Line("tooltip.factoryworks.draws", List.of("37.5"))),
                MachineTooltip.crafting(0.5, 75_000));
    }

    @Test
    void wholeFiguresCarryNoDecimal() {
        assertEquals(List.of(
                        new MachineTooltip.Line("tooltip.factoryworks.speed", List.of("1")),
                        new MachineTooltip.Line("tooltip.factoryworks.draws", List.of("21"))),
                MachineTooltip.crafting(1.0, 42_000));
    }

    @Test
    void theSteamEngineShowsWhatItBurnsAndMakes() {
        assertEquals(List.of(
                        new MachineTooltip.Line("tooltip.factoryworks.burns_steam", List.of("30")),
                        new MachineTooltip.Line("tooltip.factoryworks.makes_up_to", List.of("450"))),
                MachineTooltip.steamEngine(30.0, 450.0));
    }

    @Test
    void theAccumulatorShowsWhatItHoldsAndPasses() {
        assertEquals(List.of(
                        new MachineTooltip.Line("tooltip.factoryworks.holds", List.of("50,000")),
                        new MachineTooltip.Line("tooltip.factoryworks.in_and_out", List.of("150"))),
                MachineTooltip.accumulator(50_000, 150));
    }

    @Test
    void theRadarShowsItsDrawAndReach() {
        assertEquals(List.of(
                        new MachineTooltip.Line("tooltip.factoryworks.draws", List.of("150")),
                        new MachineTooltip.Line("tooltip.factoryworks.charts", List.of("4", "14"))),
                MachineTooltip.radar(150, 4, 14));
    }

    @Test
    void theSolarPanelShowsItsPeakAndItsSky() {
        assertEquals(List.of(
                        new MachineTooltip.Line("tooltip.factoryworks.makes_up_to", List.of("30")),
                        new MachineTooltip.Line("tooltip.factoryworks.needs_open_sky", List.of())),
                MachineTooltip.solarPanel(30));
    }

    private static MachineTooltip.Line line(String key, String... args) {
        return new MachineTooltip.Line("tooltip.factoryworks." + key, List.of(args));
    }

    @Test
    void aBurnerFurnaceShowsItsSpeedAndBurn() {
        assertEquals(List.of(line("speed", "1"), line("burns", "4,500")), MachineTooltip.furnace(1.0, 4_500, 0));
    }

    @Test
    void anElectricFurnaceShowsItsSpeedAndDraw() {
        assertEquals(List.of(line("speed", "2"), line("draws", "90")), MachineTooltip.furnace(2.0, 0, 90));
    }

    @Test
    void aBurnerDrillShowsItsRateAndBurn() {
        assertEquals(List.of(line("mines", "0.25"), line("burns", "7,500")), MachineTooltip.drill(0.25, 7_500, 0));
    }

    @Test
    void anElectricDrillShowsItsRateAndDraw() {
        assertEquals(List.of(line("mines", "0.5"), line("draws", "45")), MachineTooltip.drill(0.5, 0, 45));
    }

    @Test
    void theBoilerShowsItsSteamAndBurn() {
        assertEquals(List.of(line("makes_steam", "60"), line("burns", "90,000")), MachineTooltip.boiler(60, 90_000));
    }

    @Test
    void theOffshorePumpShowsItsWaterAndItsSiting() {
        assertEquals(List.of(line("pumps_water", "1,200"), line("place_beside_water")),
                MachineTooltip.offshorePump(1_200));
    }

    @Test
    void thePumpjackShowsNoYieldAndItsDraw() {
        assertEquals(List.of(line("pumps_crude"), line("draws", "45")), MachineTooltip.pumpjack(45));
    }
}
