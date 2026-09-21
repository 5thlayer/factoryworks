package com.planetaryfactory.core.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import rearth.belts.BlockContent;
import rearth.belts.blocks.ChuteBlockEntity;

/**
 * The pack's SimpleBelts fork loaded and moving items: chest, loader, belt, loader, chest (#342),
 * at Factorio's rate and capacity (#344).
 *
 * <p>A loader pulls from the inventory behind its facing and pushes into the one behind the far
 * loader's, so the two face each other along the row.
 *
 * <p>The rate and the capacity are typed rather than read off the fork, so the test cannot agree
 * with the fork by construction; {@code tests/factorio/test_logistics_extract.py} derives the same
 * figures from Factorio's belt prototypes.
 */
final class BeltHandoffTests {

    private static final BlockPos SOURCE = new BlockPos(2, 1, 3);
    private static final BlockPos FROM = new BlockPos(3, 1, 3);
    private static final BlockPos TO = new BlockPos(7, 1, 3);
    private static final BlockPos TARGET = new BlockPos(8, 1, 3);

    private static final int ITEMS = 16;

    private static final int BELT_TIER = 1;

    private static final int TIER_1_ITEMS_PER_SECOND = 15;
    // A multiple of four ticks: tier 1 delivers three items every four.
    private static final int RATE_WINDOW_TICKS = 200;
    private static final int RATE_WARMUP_TICKS = 100;

    private static final int LONG_BELT_BLOCKS = 64;
    private static final int LONG_BELT_HOLDS = 512;
    private static final BlockPos LONG_SOURCE = new BlockPos(0, 1, 1);
    private static final BlockPos LONG_FROM = new BlockPos(1, 1, 1);
    private static final BlockPos LONG_TO = LONG_FROM.east(LONG_BELT_BLOCKS - 1);
    private static final int LONG_SUPPLY = 640;
    // The belt is full by tick 683.
    private static final int LONG_BELT_SETTLED_TICKS = 1000;

    private BeltHandoffTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("belt_hands_items_from_chest_to_chest", 400, BeltHandoffTests::handsOff);
        tests.test("belt_tier_1_delivers_15_items_per_second",
                RATE_WARMUP_TICKS + RATE_WINDOW_TICKS + 20, BeltHandoffTests::deliversAtTierOneRate);
        tests.test("backed_up_64_block_belt_holds_512", LONG_BELT_SETTLED_TICKS + 20,
                PFGameTests.LONG_PLATFORM, BeltHandoffTests::backedUpBeltHolds512);
    }

    private static void handsOff(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.CHEST);
        placeBelt(helper, SOURCE, FROM, TO, ITEMS);

        helper.succeedWhen(() -> {
            int arrived = count(chest(helper, TARGET));
            if (arrived != ITEMS) {
                helper.fail("target chest holds " + arrived + " of " + ITEMS + " cobblestone",
                        TARGET);
            }
            int left = count(chest(helper, SOURCE));
            if (left != 0) {
                helper.fail("source chest still holds " + left + " cobblestone", SOURCE);
            }
        });
    }

    private static void deliversAtTierOneRate(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.CHEST);
        placeBelt(helper, SOURCE, FROM, TO, 512);

        int expected = TIER_1_ITEMS_PER_SECOND * RATE_WINDOW_TICKS / 20;
        int[] before = new int[1];
        helper.startSequence()
                .thenIdle(RATE_WARMUP_TICKS)
                .thenExecute(() -> before[0] = count(chest(helper, TARGET)))
                .thenIdle(RATE_WINDOW_TICKS)
                .thenExecute(() -> {
                    int delivered = count(chest(helper, TARGET)) - before[0];
                    if (delivered != expected) {
                        helper.fail("a tier-1 belt delivered " + delivered + " items in "
                                + RATE_WINDOW_TICKS + " ticks, expected " + expected, TARGET);
                    }
                })
                .thenSucceed();
    }

    // Nothing behind the far loader, so the end refuses every item and the belt backs up.
    private static void backedUpBeltHolds512(GameTestHelper helper) {
        ChuteBlockEntity belt = placeBelt(helper, LONG_SOURCE, LONG_FROM, LONG_TO, LONG_SUPPLY);

        // Read once rather than polled: succeedWhen would pass while the belt was still filling.
        helper.startSequence().thenIdle(LONG_BELT_SETTLED_TICKS).thenExecute(() -> {
            int held = belt.getBeltEntries().size();
            if (held != LONG_BELT_HOLDS) {
                helper.fail("a backed-up " + LONG_BELT_BLOCKS + "-block belt holds " + held
                        + ", expected " + LONG_BELT_HOLDS, LONG_FROM);
            }
            int left = count(chest(helper, LONG_SOURCE));
            if (left != LONG_SUPPLY - LONG_BELT_HOLDS) {
                helper.fail("the source chest holds " + left + " after filling the belt, expected "
                        + (LONG_SUPPLY - LONG_BELT_HOLDS), LONG_SOURCE);
            }
        }).thenSucceed();
    }

    /** A chest of cobblestone behind an east-facing loader, belted to a west-facing one. */
    private static ChuteBlockEntity placeBelt(GameTestHelper helper, BlockPos source, BlockPos from,
            BlockPos to, int items) {
        helper.setBlock(source, Blocks.CHEST);
        helper.setBlock(from, loader(Direction.EAST));
        helper.setBlock(to, loader(Direction.WEST));
        for (int slot = 0; items > 0; slot++, items -= 64) {
            chest(helper, source).setItem(slot, new ItemStack(Items.COBBLESTONE, Math.min(items, 64)));
        }
        ChuteBlockEntity belt = helper.getBlockEntity(from, ChuteBlockEntity.class);
        belt.assignFromBeltItem(helper.absolutePos(to), List.of(), BELT_TIER);
        return belt;
    }

    private static BlockState loader(Direction facing) {
        return BlockContent.CHUTE_BLOCK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    private static ChestBlockEntity chest(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, ChestBlockEntity.class);
    }

    private static int count(Container container) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            total += container.getItem(slot).getCount();
        }
        return total;
    }
}
