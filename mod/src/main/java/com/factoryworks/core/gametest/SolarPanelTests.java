package com.factoryworks.core.gametest;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.energy.DayFraction;
import com.factoryworks.core.energy.NetworkReading;
import com.factoryworks.core.energy.PoleTier;
import com.factoryworks.core.energy.SupplyAreaPoleBlockEntity;
import com.factoryworks.core.energy.SupplyAreaScan;
import com.factoryworks.core.smelting.FurnaceBlockEntity;
import com.factoryworks.core.smelting.FurnaceTier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * The Solar Panel on the pole network (#529, ADR-0062): 30 FE/t drawn by a pole, through a part as
 * well as the anchor, and one panel however many of its blocks the pole reaches. The 30 is typed,
 * not read off the spec, so the test cannot agree with the spec by construction.
 *
 * <p>The panel's 3x3 base spans x 4-6 around its anchor at x 5. A small pole's 5x5 area at x 7 holds
 * the anchor and a column of parts; at x 8 it holds only the parts at x 6. An Electric Furnace in
 * reach, emptied every tick, always asks for more than the panel makes.
 */
final class SolarPanelTests {

    private static final long FE_PER_TICK = 30L;

    /** Fractions of the clock's day: Minecraft's noon is a quarter in, and its midnight three quarters. */
    private static final double NOON = 0.25;
    private static final double MIDNIGHT = 0.75;
    private static final BlockPos ANCHOR = new BlockPos(5, 1, 5);

    /** Past one rescan interval (40) plus the tick the network is rebuilt on. */
    private static final int SETTLE = 45;
    private static final int MEASURED = 20;

    /**
     * The tests share the world's one clock and weather, and a batch runs at once, so each holds them
     * in a slot of its own: from its start, past settling and measuring.
     */
    private static final int SLOT = SETTLE + MEASURED + 10;
    private static final int TIMEOUT = 5 * SLOT + SETTLE + MEASURED + 20;

    private SolarPanelTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("pole_draws_a_solar_panel_once_through_its_anchor_and_parts", TIMEOUT,
                helper -> drawsFrom(helper, new BlockPos(7, 1, 5), 0, NOON, false, false, FE_PER_TICK));
        tests.test("pole_draws_a_solar_panel_through_a_part_alone", TIMEOUT,
                helper -> drawsFrom(helper, new BlockPos(8, 1, 5), 1, NOON, false, false, FE_PER_TICK));
        tests.test("solar_panel_makes_nothing_at_midnight", TIMEOUT,
                helper -> drawsFrom(helper, new BlockPos(7, 1, 5), 2, MIDNIGHT, false, false, 0L));
        tests.test("solar_panel_makes_nothing_at_noon_under_a_roof", TIMEOUT,
                helper -> drawsFrom(helper, new BlockPos(7, 1, 5), 3, NOON, true, false, 0L));
        tests.test("solar_panel_makes_full_output_at_noon_in_rain", TIMEOUT,
                helper -> drawsFrom(helper, new BlockPos(7, 1, 5), 4, NOON, false, true, FE_PER_TICK));
    }

    private static void drawsFrom(GameTestHelper helper, BlockPos pole, int slot, double clockFraction,
            boolean roof, boolean rain, long fePerTick) {
        helper.startSequence()
                .thenIdle(slot * SLOT)
                .thenExecute(() -> drawsFromNow(helper, pole, clockFraction, roof, rain, fePerTick));
    }

    private static void drawsFromNow(GameTestHelper helper, BlockPos pole, double clockFraction, boolean roof,
            boolean rain, long fePerTick) {
        setDay(helper, clockFraction, rain);
        if (roof) {
            helper.setBlock(ANCHOR.offset(0, 4, 0), Blocks.STONE);
        }
        PFBlocks.SOLAR_PANEL_FOOTPRINT.placeAll(helper.getLevel(), helper.absolutePos(ANCHOR), Direction.NORTH);
        BlockPos furnace = pole.offset(0, 0, 2);
        helper.setBlock(pole, PFBlocks.pole(PoleTier.SMALL).get());
        helper.setBlock(furnace, PFBlocks.furnace(FurnaceTier.ELECTRIC).get());

        var generators = SupplyAreaScan.of(helper.getLevel(), helper.absolutePos(pole), PoleTier.SMALL).generators();
        if (fePerTick > 0 && !generators.equals(java.util.List.of(helper.absolutePos(ANCHOR)))) {
            helper.fail("the pole finds generators " + generators + " where the panel's anchor is the one", pole);
        }

        long[] produced = {0L};
        helper.onEachTick(() -> helper.getBlockEntity(furnace, FurnaceBlockEntity.class)
                .data().set(FurnaceBlockEntity.DATA_ENERGY, 0));
        helper.startSequence()
                .thenIdle(SETTLE)
                .thenExecuteFor(MEASURED, () -> {
                    SupplyAreaPoleBlockEntity at = helper.getBlockEntity(pole, SupplyAreaPoleBlockEntity.class);
                    NetworkReading reading = at.networkReading();
                    if (at.machineCount() != 1) {
                        helper.fail("the pole files " + at.machineCount() + " consumers; only the furnace"
                                + " is one, so a panel block is being fed", pole);
                    }
                    if (reading.produced() != fePerTick) {
                        helper.fail("the pole drew " + reading.produced() + " FE from a panel making "
                                + fePerTick + " a tick", pole);
                    }
                    produced[0] += reading.produced();
                })
                .thenExecute(() -> {
                    if (produced[0] != fePerTick * MEASURED) {
                        helper.fail("drew " + produced[0] + " FE over " + MEASURED + " ticks", pole);
                    }
                })
                .thenExecute(() -> endDay(helper))
                .thenSucceed();
    }

    /** Holds the clock at a fraction of the dimension's own day, whatever its length. */
    private static void setDay(GameTestHelper helper, double clockFraction, boolean rain) {
        ServerLevel level = helper.getLevel();
        var clock = level.dimensionType().defaultClock().orElseThrow();
        int period = DayFraction.periodTicks(level);
        level.clockManager().setPaused(clock, true);
        level.clockManager().setTotalTicks(clock, Math.round(clockFraction * period));
        level.getWeatherData().setRaining(rain);
    }

    private static void endDay(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.clockManager().setPaused(level.dimensionType().defaultClock().orElseThrow(), false);
        level.getWeatherData().setRaining(false);
    }
}
