package com.factoryworks.core.gametest;

import io.github._5thlayer.wireworks.WireworksRegistries;
import java.util.Optional;

import com.factoryworks.core.energy.AccumulatorBlockEntity;
import com.factoryworks.core.energy.AccumulatorStatus;
import com.factoryworks.core.PFBlocks;
import io.github._5thlayer.wireworks.PoleTier;
import com.factoryworks.core.smelting.FurnaceBlockEntity;
import com.factoryworks.core.smelting.FurnaceTier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.minecraft.world.level.block.Blocks;

/**
 * The accumulator (#283, ADR-0062): the mixin's figures reach the placed block through the pack's
 * subclass, a pole charges it at them, and with the generator gone a pole feeds a machine from it. The figures are typed rather than read off
 * {@code AccumulatorSpec}, so the test cannot agree with the spec by construction.
 */
final class AccumulatorTests {

    private static final BlockPos CREATIVE = new BlockPos(1, 1, 3);
    private static final BlockPos ACCUMULATOR = new BlockPos(5, 1, 5);
    /** A small pole's 5x5 area here covers the accumulator's anchor and the furnace, nothing else. */
    private static final BlockPos SMALL_POLE = new BlockPos(5, 1, 7);
    private static final BlockPos FURNACE = new BlockPos(6, 1, 8);

    /** Past one rescan interval (40) plus the tick the network is rebuilt on. */
    private static final int SETTLE = 45;
    private static final int WINDOW = 10;

    private AccumulatorTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("accumulator_holds_5_mj", 20, AccumulatorTests::holdsFiveMegajoules);
        tests.test("accumulator_charges_at_300_kw", 100, AccumulatorTests::chargesAtThreeHundredKw);
        tests.test("accumulator_feeds_a_machine_with_no_generator", 200,
                AccumulatorTests::feedsAMachineWithNoGenerator);
    }

    private static void holdsFiveMegajoules(GameTestHelper helper) {
        place(helper);
        helper.startSequence()
                .thenExecute(() -> {
                    long capacity = face(helper).getCapacityAsLong();
                    if (capacity != 50_000L) {
                        helper.fail("the accumulator holds " + capacity + " FE, not 50,000",
                                ACCUMULATOR);
                    }
                    expectStatus(helper, AccumulatorStatus.NOT_IN_POLE_AREA);
                })
                .thenSucceed();
    }

    /** A creative pole offers without limit, so only the accumulator's own rate holds the charge. */
    private static void chargesAtThreeHundredKw(GameTestHelper helper) {
        helper.setBlock(CREATIVE, WireworksRegistries.CREATIVE_POLE.get());
        place(helper);
        long[] before = new long[1];
        helper.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> before[0] = face(helper).getAmountAsLong())
                .thenIdle(WINDOW)
                .thenExecute(() -> {
                    long charged = face(helper).getAmountAsLong() - before[0];
                    if (charged != 150L * WINDOW) {
                        helper.fail("the accumulator charged " + charged + " FE in " + WINDOW
                                + " ticks, not 150 FE/t", ACCUMULATOR);
                    }
                    expectStatus(helper, AccumulatorStatus.CHARGING);
                })
                .thenSucceed();
    }

    /**
     * The creative pole is broken before the small pole and the furnace are placed: its 18x18 area
     * covers the furnace too, and would feed it without the accumulator.
     */
    private static void feedsAMachineWithNoGenerator(GameTestHelper helper) {
        helper.setBlock(CREATIVE, WireworksRegistries.CREATIVE_POLE.get());
        place(helper);
        long[] charged = new long[1];
        helper.startSequence()
                .thenIdle(SETTLE + WINDOW)
                .thenExecute(() -> {
                    helper.setBlock(CREATIVE, Blocks.AIR);
                    charged[0] = face(helper).getAmountAsLong();
                    if (charged[0] <= 0L) {
                        helper.fail("the creative pole never charged the accumulator", ACCUMULATOR);
                    }
                    helper.setBlock(SMALL_POLE, WireworksRegistries.pole(PoleTier.SMALL).get());
                    helper.setBlock(FURNACE, PFBlocks.furnace(FurnaceTier.ELECTRIC).get());
                })
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    long fed = helper.getBlockEntity(FURNACE, FurnaceBlockEntity.class).data()
                            .get(FurnaceBlockEntity.DATA_ENERGY);
                    if (fed <= 0L) {
                        helper.fail("a furnace on a pole with only a charged accumulator holds no FE",
                                FURNACE);
                    }
                    long discharged = charged[0] - face(helper).getAmountAsLong();
                    if (discharged < fed) {
                        helper.fail("the furnace holds " + fed + " FE but the accumulator gave only "
                                + discharged, ACCUMULATOR);
                    }
                    if (discharged > 150L * SETTLE) {
                        helper.fail("the accumulator gave " + discharged + " FE in " + SETTLE
                                + " ticks, past 150 FE/t", ACCUMULATOR);
                    }
                    expectStatus(helper, AccumulatorStatus.DISCHARGING);
                })
                .thenSucceed();
    }

    private static void place(GameTestHelper helper) {
        PFBlocks.ACCUMULATOR_FOOTPRINT.placeAll(helper.getLevel(), helper.absolutePos(ACCUMULATOR),
                Direction.NORTH);
    }

    private static void expectStatus(GameTestHelper helper, AccumulatorStatus expected) {
        Optional<AccumulatorStatus> status =
                helper.getBlockEntity(ACCUMULATOR, AccumulatorBlockEntity.class).status();
        if (!status.equals(Optional.of(expected))) {
            helper.fail("the accumulator's HUD names " + status + ", not " + expected, ACCUMULATOR);
        }
    }

    private static EnergyHandler face(GameTestHelper helper) {
        EnergyHandler handler = helper.getLevel().getCapability(Capabilities.Energy.BLOCK,
                helper.absolutePos(ACCUMULATOR), null);
        if (handler == null) {
            helper.fail("the accumulator has no energy face", ACCUMULATOR);
        }
        return handler;
    }
}
