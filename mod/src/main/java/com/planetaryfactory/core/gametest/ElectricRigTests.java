package com.planetaryfactory.core.gametest;

import java.util.List;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.PFItems;
import com.planetaryfactory.core.energy.PoleTier;
import com.planetaryfactory.core.energy.SupplyAreaPoleBlockEntity;
import com.planetaryfactory.core.mining.rig.RigBlock;
import com.planetaryfactory.core.mining.rig.RigBlockEntity;
import com.planetaryfactory.core.mining.rig.RigMiningArea;
import com.planetaryfactory.core.mining.rig.RigPartBlock;
import com.planetaryfactory.core.mining.rig.RigTier;
import com.planetaryfactory.core.ore.OreFields;
import com.planetaryfactory.core.ore.OreResource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Electric Mining Drill as a supply-area pole customer (#194).
 *
 * <p>The pole is placed beside the column of the footprint that does not hold the anchor, so it
 * reaches only part blocks: the drill is found through {@code RigPartBlockEntity}'s energy owner,
 * and counted once however many of its parts are in reach.
 */
final class ElectricRigTests {

    /** Clicked to place the rig, so the anchor stands on it. Mid-platform, with room either side. */
    private static final BlockPos FLOOR = new BlockPos(3, 0, 3);

    /**
     * What an empty drill asks a pole for: one 40-tick iron operation at 45 FE/t. Typed rather than
     * read off {@code RigRate}, so the test does not agree with it by construction.
     */
    private static final long EMPTY_DRILL_DEMAND = 1_800L;

    private static final long FE_PER_TICK = 45L;

    private ElectricRigTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("pole_counts_electric_rig_once_through_its_parts", 100,
                ElectricRigTests::poleCountsRigOnce);
        tests.test("powered_electric_rig_mines_and_starved_one_freezes", 200,
                ElectricRigTests::poweredRigMines);
    }

    /** A pole with no generator: it finds the drill once, reads its whole buffer, and leaks nothing. */
    private static void poleCountsRigOnce(GameTestHelper helper) {
        Layout layout = place(helper);
        setPole(helper, layout, PFBlocks.pole(PoleTier.SMALL).get());
        helper.startSequence()
                .thenIdle(45)
                .thenExecute(() -> {
                    int found = pole(helper, layout).machineCount();
                    if (found != 1) {
                        helper.fail("pole sees " + found + " machines, expected the one drill",
                                FLOOR);
                    }
                    long demanded = pole(helper, layout).demandedFePerTick();
                    if (demanded != EMPTY_DRILL_DEMAND) {
                        helper.fail("pole read a demand of " + demanded + " FE/t, expected "
                                + EMPTY_DRILL_DEMAND, FLOOR);
                    }
                    long stored = stored(helper, layout);
                    if (stored != 0L) {
                        helper.fail("drill kept " + stored + " FE from the demand probe",
                                FLOOR);
                    }
                })
                .thenSucceed();
    }

    /** Fed, it mines iron at 45 FE a tick; cut off with an empty buffer, it stops where it stood. */
    private static void poweredRigMines(GameTestHelper helper) {
        Layout layout = place(helper);
        seedIron(helper, layout);
        setPole(helper, layout, PFBlocks.CREATIVE_POLE.get());
        long[] window = new long[1];
        int[] mark = new int[2];
        helper.startSequence()
                // One iron operation is 40 ticks at mining speed 0.5.
                .thenIdle(60)
                .thenExecute(() -> {
                    if (rig(helper, layout).bufferedCount() < 1) {
                        helper.fail("a powered drill on iron mined nothing in 60 ticks",
                                FLOOR);
                    }
                })
                .thenExecute(() -> {
                    setPole(helper, layout, PFBlocks.pole(PoleTier.SMALL).get());
                    setStored(helper, layout, FE_PER_TICK * 10L);
                    setProgress(helper, layout, 10);
                    window[0] = stored(helper, layout);
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    long drawn = window[0] - stored(helper, layout);
                    if (drawn != FE_PER_TICK) {
                        helper.fail("drill drew " + drawn + " FE in one tick, expected "
                                + FE_PER_TICK, FLOOR);
                    }
                    setStored(helper, layout, 0L);
                    mark[0] = progress(helper, layout);
                    mark[1] = rig(helper, layout).bufferedCount();
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    int progress = progress(helper, layout);
                    if (progress != mark[0]) {
                        helper.fail("starved drill advanced from " + mark[0] + " to " + progress,
                                FLOOR);
                    }
                    int banked = rig(helper, layout).bufferedCount();
                    if (banked != mark[1]) {
                        helper.fail("starved drill banked " + (banked - mark[1]) + " more ore",
                                FLOOR);
                    }
                })
                .thenSucceed();
    }

    /** Where the drill and its pole ended up, in absolute positions. */
    private record Layout(BlockPos anchor, BlockPos pole) {
    }

    /** Places the drill with its own item, the way a player does, and a pole beside a part column. */
    private static Layout place(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PFItems.rig(RigTier.ELECTRIC).get()));
        BlockPos absolute = helper.absolutePos(FLOOR);
        helper.useBlock(FLOOR, player, new BlockHitResult(
                Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false));

        List<BlockPos> footprint = BlockPos.betweenClosedStream(new AABB(absolute).inflate(4))
                .filter(pos -> {
                    Block block = helper.getLevel().getBlockState(pos).getBlock();
                    return block instanceof RigBlock || block instanceof RigPartBlock;
                })
                .map(BlockPos::immutable)
                .toList();
        BlockPos anchor = footprint.stream()
                .filter(pos -> helper.getLevel().getBlockState(pos).getBlock() instanceof RigBlock)
                .findFirst()
                .orElse(null);
        if (anchor == null) {
            helper.fail("placing the electric drill put down no anchor among " + footprint, FLOOR);
            throw new IllegalStateException("unreachable");
        }
        int minX = footprint.stream().mapToInt(BlockPos::getX).min().orElseThrow();
        int maxX = footprint.stream().mapToInt(BlockPos::getX).max().orElseThrow();
        // A small pole reaches two blocks either side, so two past the edge reaches one column.
        int poleX = anchor.getX() == maxX ? minX - 2 : maxX + 2;
        return new Layout(anchor, new BlockPos(poleX, anchor.getY(), anchor.getZ()));
    }

    /** Iron under the drill, in a recorded field so each block holds an amount (ADR-0041). */
    private static void seedIron(GameTestHelper helper, Layout layout) {
        BlockPos anchor = layout.anchor();
        Direction facing = helper.getLevel().getBlockState(anchor).getValue(RigBlock.FACING);
        List<BlockPos> area = RigMiningArea.positions(anchor, RigTier.ELECTRIC, facing);
        for (BlockPos pos : area) {
            helper.getLevel().setBlockAndUpdate(pos,
                    PFBlocks.ore(OreResource.IRON).get().defaultBlockState());
        }
        helper.getLevel().getDataStorage().computeIfAbsent(OreFields.TYPE)
                .record(OreResource.IRON, BoundingBox.encapsulatingPositions(area).orElseThrow(),
                        area.size());
    }

    private static void setPole(GameTestHelper helper, Layout layout, Block pole) {
        helper.getLevel().setBlockAndUpdate(layout.pole(), pole.defaultBlockState());
    }

    private static SupplyAreaPoleBlockEntity pole(GameTestHelper helper, Layout layout) {
        return (SupplyAreaPoleBlockEntity) helper.getLevel().getBlockEntity(layout.pole());
    }

    private static RigBlockEntity rig(GameTestHelper helper, Layout layout) {
        return (RigBlockEntity) helper.getLevel().getBlockEntity(layout.anchor());
    }

    private static long stored(GameTestHelper helper, Layout layout) {
        return rig(helper, layout).data().get(RigBlockEntity.DATA_FUEL);
    }

    private static void setStored(GameTestHelper helper, Layout layout, long fe) {
        rig(helper, layout).data().set(RigBlockEntity.DATA_FUEL, (int) fe);
    }

    private static int progress(GameTestHelper helper, Layout layout) {
        return rig(helper, layout).data().get(RigBlockEntity.DATA_PROGRESS);
    }

    private static void setProgress(GameTestHelper helper, Layout layout, int ticks) {
        rig(helper, layout).data().set(RigBlockEntity.DATA_PROGRESS, ticks);
    }
}
