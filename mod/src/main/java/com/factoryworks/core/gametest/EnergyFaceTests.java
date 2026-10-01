package com.factoryworks.core.gametest;

import io.github._5thlayer.wireworks.WireworksRegistries;
import com.factoryworks.core.PFBlocks;
import io.github._5thlayer.wireworks.PoleTier;
import io.github._5thlayer.wireworks.SupplyAreaPoleBlockEntity;
import com.factoryworks.core.smelting.FurnaceBlockEntity;
import com.factoryworks.core.smelting.FurnaceSlots;
import com.factoryworks.core.smelting.FurnaceTier;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What the FE faces do in a world, and nothing that can be asked without one (#271, #266).
 *
 * <h2>Why these three and no others</h2>
 *
 * <p>The arithmetic on both sides is already checked on a plain JVM -- {@code NetworkBalanceTest},
 * {@code EnergyShareTest}, {@code SupplyAreaTest}, {@code FurnaceEnergyBufferTest},
 * {@code FurnaceCycleTest}. The pack's two static checks
 * ({@code tests/pack/test_capability_registration.py}, {@code tests/pack/test_energy_faces.py})
 * assert the faces are registered and that {@code insert} reaches a buffer through the journal.
 * Both of those read source text, so they prove the lines are present and cannot prove the lines
 * are wired to each other: a journal pointed at the wrong buffer passes every assertion in them.
 * The three tests here are exactly the seams that survive that.
 *
 * <h2>The layout</h2>
 *
 * <p>A pole, and an Electric Furnace one block east of it: {@code dx = 1}, inside the
 * small pole's 5x5 supply area with room to spare. The platform is stone at relative y 0, so
 * both stand at y 1. A pole holds no energy (ADR-0062), so "fed" means a creative pole, which is an
 * unlimited generator on its network, and "unfed" means a small pole with no generator in reach.
 */
final class EnergyFaceTests {

    /** The pole. Centre of the platform, so the whole 5x5 area is inside the structure. */
    private static final BlockPos POLE = new BlockPos(3, 1, 3);

    /** The furnace, one block east of the pole and well inside its area. */
    private static final BlockPos FURNACE = new BlockPos(4, 1, 3);

    /**
     * What an empty Electric Furnace asks for: its whole buffer, which is one steel plate's smelt
     * at 90 FE a tick.
     *
     * <p>Stated as a literal rather than read off {@link FurnaceTier}, because reading it off the
     * enum would make this test agree with the enum by construction -- {@code FurnaceTierTest}
     * owns the derivation, and what is being checked here is that the number survives the trip
     * through a capability lookup and an aborted transaction.
     */
    private static final long EMPTY_FURNACE_DEMAND = 14_400L;

    /** What a running Electric Furnace draws every tick (ADR-0060: 180 kW at 100 J per FE). */
    private static final long FE_PER_TICK = 90L;

    /**
     * How long a pole may take to notice a machine, in ticks.
     *
     * <p>Stated here rather than read off the pole, whose own constant is private. That is the
     * right way round: this is the promise the pole makes to a player who has just placed a
     * machine, and a test that read the field would agree with whatever the field became.
     */
    private static final int RESCAN_INTERVAL = 40;

    private EnergyFaceTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("pole_finds_electric_furnace", 100, EnergyFaceTests::poleFindsElectricFurnace);
        tests.test("demand_probe_leaves_nothing_behind", 100, EnergyFaceTests::probeLeavesNothing);
        tests.test("powered_furnace_smelts", 200, EnergyFaceTests::poweredFurnaceSmelts);
    }

    /**
     * The lookup finds it.
     *
     * <p>Three things have to agree for this to pass and no static check sees more than one of
     * them: the block entity type's capability registration, the scan's geometry, and the pole's
     * fallback from a null context to the six faces. A machine the pole cannot see is not broken,
     * it is unpowered, with nothing thrown and nothing logged.
     */
    private static void poleFindsElectricFurnace(GameTestHelper helper) {
        // The furnace is deliberately NOT placed with the pole. A pole starts with its rescan
        // counter already at the interval, so it scans on its very first tick whatever happens --
        // place both together and a pole whose periodic rescan never fired again would still pass.
        // Placing the machine into a pole that has already scanned makes the *rescan* the thing
        // under test, which is the half that decides whether a machine placed next to a running
        // factory is ever picked up.
        helper.setBlock(POLE, WireworksRegistries.pole(PoleTier.SMALL).get());
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    int found = pole(helper).machineCount();
                    if (found != 0) {
                        helper.fail("pole sees " + found + " machines before one was placed", POLE);
                    }
                    helper.setBlock(FURNACE, PFBlocks.furnace(FurnaceTier.ELECTRIC).get());
                })
                // One whole interval and no more. The pole's phase is its own, so the furnace may
                // be picked up on the next tick or on the fortieth; what the interval promises is
                // that it cannot take longer than this.
                .thenIdle(RESCAN_INTERVAL)
                .thenExecute(() -> {
                    int found = pole(helper).machineCount();
                    if (found != 1) {
                        helper.fail("pole sees " + found + " machines a full rescan interval after"
                                + " one was placed, expected 1", FURNACE);
                    }
                })
                .thenSucceed();
    }

    /**
     * The abort leaves nothing behind.
     *
     * <p>The pole measures a machine's room with an insert inside a transaction it then aborts,
     * because the transfer API has no "how much room is there" question. A face that takes energy
     * without journalling its buffer keeps a probe's worth every tick -- a furnace running on
     * power nobody spent, at a rate nothing displays.
     *
     * <p>So: a small pole with no generator on its network, ticked. It has nothing to give, and the
     * probe still runs. The furnace must still hold zero, and the demand it reported must be the whole buffer.
     */
    private static void probeLeavesNothing(GameTestHelper helper) {
        place(helper);
        helper.startSequence()
                .thenIdle(45)
                .thenExecute(() -> {
                    long stored = stored(helper);
                    if (stored != 0L) {
                        helper.fail("furnace kept " + stored + " FE from the demand probe", FURNACE);
                    }
                    long demanded = pole(helper).demandedFePerTick();
                    if (demanded != EMPTY_FURNACE_DEMAND) {
                        helper.fail("pole read a demand of " + demanded + " FE/t, expected "
                                + EMPTY_FURNACE_DEMAND, POLE);
                    }
                })
                .thenSucceed();
    }

    /**
     * Filling it makes it smelt, and starving it stops the smelt where it stood.
     *
     * <p>Fed by a creative pole, then cut off by swapping it for a small pole with nothing to give.
     */
    private static void poweredFurnaceSmelts(GameTestHelper helper) {
        helper.setBlock(POLE, WireworksRegistries.CREATIVE_POLE.get());
        helper.setBlock(FURNACE, PFBlocks.furnace(FurnaceTier.ELECTRIC).get());
        long[] window = new long[1];
        int[] progressMark = new int[1];
        helper.startSequence()
                .thenExecute(() -> {
                    furnace(helper).setItem(FurnaceSlots.INPUT, new ItemStack(Items.COBBLESTONE, 64));
                })
                // The grid half: a fed pole makes the furnace produce. 32 ticks is the stone brick
                // smelt at the Electric tier (64 cooking ticks over a crafting speed of 2), and
                // this is comfortably past the first completion.
                .thenIdle(45)
                .thenExecute(() -> {
                    ItemStack output = furnace(helper).getItem(FurnaceSlots.OUTPUT);
                    if (!output.is(Items.STONE_BRICKS)) {
                        helper.fail("furnace output holds " + output + ", expected stone bricks",
                                FURNACE);
                    }
                })
                // Now cut the grid off and hand the furnace a buffer it can count down on its own.
                // The per-tick draw is measured here rather than while the pole is feeding,
                // because both block entities tick in the same game tick and their order is a
                // property of the chunk's block entity list -- with the pole refilling, the
                // furnace's stored FE reads 90 higher or lower depending on which went first.
                // Ten ticks' worth, so the buffer cannot run out inside the window.
                .thenExecute(() -> {
                    helper.setBlock(POLE, WireworksRegistries.pole(PoleTier.SMALL).get());
                    setStored(helper, FE_PER_TICK * 10L);
                    window[0] = stored(helper);
                    progressMark[0] = progress(helper);
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    long drawn = window[0] - stored(helper);
                    if (drawn != FE_PER_TICK) {
                        helper.fail("furnace drew " + drawn + " FE in one tick, expected "
                                + FE_PER_TICK, FURNACE);
                    }
                    int advanced = progress(helper) - progressMark[0];
                    if (advanced != 1) {
                        helper.fail("furnace progress moved by " + advanced + " on a paid tick,"
                                + " expected 1", FURNACE);
                    }
                    progressMark[0] = progress(helper);
                })
                // And starve it outright: no grid, no buffer. A furnace that keeps going here is
                // running on the pole's own demand probe.
                .thenExecute(() -> {
                    setStored(helper, 0L);
                    progressMark[0] = progress(helper);
                    if (progressMark[0] <= 0) {
                        helper.fail("nothing to freeze: the furnace was between smelts when it was"
                                + " starved, so this assertion would pass vacuously", FURNACE);
                    }
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    int progress = progress(helper);
                    if (progress != progressMark[0]) {
                        helper.fail("starved furnace advanced from " + progressMark[0] + " to "
                                + progress, FURNACE);
                    }
                    long stored = stored(helper);
                    if (stored != 0L) {
                        helper.fail("starved furnace holds " + stored + " FE", FURNACE);
                    }
                })
                .thenSucceed();
    }

    private static void place(GameTestHelper helper) {
        helper.setBlock(POLE, WireworksRegistries.pole(PoleTier.SMALL).get());
        helper.setBlock(FURNACE, PFBlocks.furnace(FurnaceTier.ELECTRIC).get());
    }

    private static SupplyAreaPoleBlockEntity pole(GameTestHelper helper) {
        return helper.getBlockEntity(POLE, SupplyAreaPoleBlockEntity.class);
    }

    private static FurnaceBlockEntity furnace(GameTestHelper helper) {
        return helper.getBlockEntity(FURNACE, FurnaceBlockEntity.class);
    }

    /** The furnace's stored FE, read the way its own screen reads it. */
    private static long stored(GameTestHelper helper) {
        return furnace(helper).data().get(FurnaceBlockEntity.DATA_ENERGY);
    }

    private static void setStored(GameTestHelper helper, long fe) {
        furnace(helper).data().set(FurnaceBlockEntity.DATA_ENERGY, (int) fe);
    }

    /** How far into the current smelt the furnace is, in ticks. */
    private static int progress(GameTestHelper helper) {
        return furnace(helper).data().get(FurnaceBlockEntity.DATA_PROGRESS);
    }
}
