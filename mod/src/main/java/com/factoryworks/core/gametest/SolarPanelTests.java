package com.factoryworks.core.gametest;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.energy.NetworkReading;
import com.factoryworks.core.energy.PoleTier;
import com.factoryworks.core.energy.SupplyAreaPoleBlockEntity;
import com.factoryworks.core.energy.SupplyAreaScan;
import com.factoryworks.core.smelting.FurnaceBlockEntity;
import com.factoryworks.core.smelting.FurnaceTier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;

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
    private static final BlockPos ANCHOR = new BlockPos(5, 1, 5);

    /** Past one rescan interval (40) plus the tick the network is rebuilt on. */
    private static final int SETTLE = 45;
    private static final int MEASURED = 20;

    private SolarPanelTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("pole_draws_a_solar_panel_once_through_its_anchor_and_parts", 200,
                helper -> drawsFrom(helper, new BlockPos(7, 1, 5)));
        tests.test("pole_draws_a_solar_panel_through_a_part_alone", 200,
                helper -> drawsFrom(helper, new BlockPos(8, 1, 5)));
    }

    private static void drawsFrom(GameTestHelper helper, BlockPos pole) {
        PFBlocks.SOLAR_PANEL_FOOTPRINT.placeAll(helper.getLevel(), helper.absolutePos(ANCHOR), Direction.NORTH);
        BlockPos furnace = pole.offset(0, 0, 2);
        helper.setBlock(pole, PFBlocks.pole(PoleTier.SMALL).get());
        helper.setBlock(furnace, PFBlocks.furnace(FurnaceTier.ELECTRIC).get());

        var generators = SupplyAreaScan.of(helper.getLevel(), helper.absolutePos(pole), PoleTier.SMALL).generators();
        if (!generators.equals(java.util.List.of(helper.absolutePos(ANCHOR)))) {
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
                    if (reading.produced() != FE_PER_TICK) {
                        helper.fail("the pole drew " + reading.produced() + " FE from a panel making "
                                + FE_PER_TICK + " a tick", pole);
                    }
                    produced[0] += reading.produced();
                })
                .thenExecute(() -> {
                    if (produced[0] != FE_PER_TICK * MEASURED) {
                        helper.fail("drew " + produced[0] + " FE over " + MEASURED + " ticks", pole);
                    }
                })
                .thenSucceed();
    }
}
