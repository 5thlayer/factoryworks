package com.planetaryfactory.core.gametest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import rearth.belts.model.BeltTier;

/**
 * The pack's SimpleBelts fork loaded and moving items: chest, loader, belt, loader, chest (#342),
 * at each tier's Factorio rate (#344, #345) and capacity (#344), and at the slowest piece's rate
 * when the loaders and the belt are of different tiers (#347).
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

    private static final int TIER_1_ITEMS_PER_SECOND = 15;
    private static final int TIER_2_ITEMS_PER_SECOND = 30;
    private static final int TIER_3_ITEMS_PER_SECOND = 45;
    private static final int TIER_4_ITEMS_PER_SECOND = 60;
    // A multiple of four ticks: tiers 1 and 3 deliver a whole number of items only every four.
    private static final int RATE_WINDOW_TICKS = 200;
    private static final int RATE_WARMUP_TICKS = 100;
    // A full chest, so the source outlasts the window at tier 4.
    private static final int RATE_SUPPLY = 27 * 64;

    private static final String EXPRESS_BELT_RECIPE = "planetaryfactory:assembling/express_transport_belt";

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
        rateTest(tests, BeltTier.BELT, TIER_1_ITEMS_PER_SECOND);
        rateTest(tests, BeltTier.IMPROVED, TIER_2_ITEMS_PER_SECOND);
        rateTest(tests, BeltTier.EXPRESS, TIER_3_ITEMS_PER_SECOND);
        rateTest(tests, BeltTier.TURBO, TIER_4_ITEMS_PER_SECOND);
        tests.test("belt_tier_3_between_tier_1_loaders_delivers_" + TIER_1_ITEMS_PER_SECOND,
                RATE_WARMUP_TICKS + RATE_WINDOW_TICKS + 20,
                helper -> deliversAtRate(helper, BeltTier.BELT, BeltTier.EXPRESS, TIER_1_ITEMS_PER_SECOND));
        tests.test("belt_tier_1_between_tier_4_loaders_delivers_" + TIER_1_ITEMS_PER_SECOND,
                RATE_WARMUP_TICKS + RATE_WINDOW_TICKS + 20,
                helper -> deliversAtRate(helper, BeltTier.TURBO, BeltTier.BELT, TIER_1_ITEMS_PER_SECOND));
        tests.test("belts_own_recipes_are_swept", 20, BeltHandoffTests::ownRecipesAreSwept);
        tests.test("backed_up_64_block_belt_holds_512", LONG_BELT_SETTLED_TICKS + 20,
                PFGameTests.LONG_PLATFORM, BeltHandoffTests::backedUpBeltHolds512);
    }

    private static void handsOff(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.CHEST);
        placeBelt(helper, SOURCE, FROM, TO, ITEMS, BeltTier.BELT, BeltTier.BELT);

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

    private static void rateTest(PFGameTests.Registrar tests, BeltTier tier, int itemsPerSecond) {
        tests.test("belt_tier_" + tier.number() + "_delivers_" + itemsPerSecond + "_items_per_second",
                RATE_WARMUP_TICKS + RATE_WINDOW_TICKS + 20,
                helper -> deliversAtRate(helper, tier, tier, itemsPerSecond));
    }

    private static void deliversAtRate(GameTestHelper helper, BeltTier loaders, BeltTier tier,
            int itemsPerSecond) {
        helper.setBlock(TARGET, Blocks.CHEST);
        placeBelt(helper, SOURCE, FROM, TO, RATE_SUPPLY, loaders, tier);

        int expected = itemsPerSecond * RATE_WINDOW_TICKS / 20;
        int[] before = new int[1];
        helper.startSequence()
                .thenIdle(RATE_WARMUP_TICKS)
                .thenExecute(() -> before[0] = count(chest(helper, TARGET)))
                .thenIdle(RATE_WINDOW_TICKS)
                .thenExecute(() -> {
                    int delivered = count(chest(helper, TARGET)) - before[0];
                    if (delivered != expected) {
                        helper.fail("a tier-" + tier.number() + " belt between tier-" + loaders.number()
                                + " loaders delivered " + delivered + " items in "
                                + RATE_WINDOW_TICKS + " ticks, expected " + expected, TARGET);
                    }
                })
                .thenSucceed();
    }

    // The pack's express belt recipe is the control: without it an empty manager would pass.
    private static void ownRecipesAreSwept(GameTestHelper helper) {
        Set<String> loaded = helper.getLevel().getServer().getRecipeManager().recipeMap().values()
                .stream()
                .map(holder -> holder.id().identifier().toString())
                .collect(Collectors.toSet());
        if (!loaded.contains(EXPRESS_BELT_RECIPE)) {
            helper.fail(EXPRESS_BELT_RECIPE + " is not loaded, so this proves nothing");
            return;
        }
        List<String> survivors = loaded.stream().filter(id -> id.startsWith("belts:")).sorted().toList();
        if (!survivors.isEmpty()) {
            helper.fail("SimpleBelts' own recipes survived the sweep: " + survivors);
            return;
        }
        helper.succeed();
    }

    // Nothing behind the far loader, so the end refuses every item and the belt backs up.
    private static void backedUpBeltHolds512(GameTestHelper helper) {
        ChuteBlockEntity belt = placeBelt(helper, LONG_SOURCE, LONG_FROM, LONG_TO, LONG_SUPPLY,
                BeltTier.BELT, BeltTier.BELT);

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
            BlockPos to, int items, BeltTier loaders, BeltTier tier) {
        helper.setBlock(source, Blocks.CHEST);
        helper.setBlock(from, loader(loaders, Direction.EAST));
        helper.setBlock(to, loader(loaders, Direction.WEST));
        for (int slot = 0; items > 0; slot++, items -= 64) {
            chest(helper, source).setItem(slot, new ItemStack(Items.COBBLESTONE, Math.min(items, 64)));
        }
        ChuteBlockEntity belt = helper.getBlockEntity(from, ChuteBlockEntity.class);
        belt.assignFromBeltItem(helper.absolutePos(to), List.of(), tier, 0);
        return belt;
    }

    private static BlockState loader(BeltTier tier, Direction facing) {
        return BlockContent.loaderFor(tier).defaultBlockState()
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
