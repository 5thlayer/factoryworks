package com.planetaryfactory.core.gametest;

import java.util.List;

import com.planetaryfactory.core.PFBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import rearth.belts.BlockContent;
import rearth.belts.ItemContent;
import rearth.belts.blocks.ChuteBlockEntity;
import rearth.belts.blocks.SplitterBlock;
import rearth.belts.model.BeltTier;

/**
 * A splitter in the pack's SimpleBelts fork (#349, ADR-0076): one belt split evenly across two, a
 * backed-up side sending everything to the other, and a tier-1 splitter capping a tier-3 line.
 * It places and breaks as both halves.
 *
 * <p>Items flow east. A chest and a loader feed the splitter's left half, and each half's output
 * belt ends at a loader with a chest behind it. The rates are typed rather than read off the fork,
 * and {@code tests/factorio/test_logistics_extract.py} derives them from Factorio's prototypes.
 */
final class SplitterTests {

    private static final BlockPos SOURCE = new BlockPos(1, 1, 2);
    private static final BlockPos FROM = new BlockPos(2, 1, 2);
    private static final BlockPos LEFT = new BlockPos(6, 1, 2);
    private static final BlockPos RIGHT = LEFT.relative(Direction.EAST.getClockWise());
    private static final BlockPos LEFT_END = new BlockPos(10, 1, 2);
    private static final BlockPos RIGHT_END = new BlockPos(10, 1, 3);
    private static final BlockPos POLE = new BlockPos(4, 1, 5);

    private static final int TIER_1_ITEMS_PER_SECOND = 15;
    private static final int TIER_1_SPLITTER_ITEMS_PER_SECOND = 15;
    // A multiple of four ticks: tier 1 delivers a whole number of items only every four.
    private static final int WINDOW_TICKS = 200;
    // Long enough for a four-block side to back up at half the line's rate.
    private static final int WARMUP_TICKS = 200;
    private static final int SUPPLY = 27 * 64;

    private static final BlockPos FLOOR = new BlockPos(3, 0, 3);

    private SplitterTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("splitter_splits_one_belt_evenly", WARMUP_TICKS + WINDOW_TICKS + 20,
                SplitterTests::splitsEvenly);
        tests.test("splitter_sends_everything_to_the_free_side", WARMUP_TICKS + WINDOW_TICKS + 20,
                SplitterTests::switchesToTheFreeSide);
        tests.test("tier_1_splitter_caps_a_tier_3_line_at_" + TIER_1_SPLITTER_ITEMS_PER_SECOND,
                WARMUP_TICKS + WINDOW_TICKS + 20, SplitterTests::capsTheLine);
        tests.test("belt_item_links_a_splitter_at_both_ends", 20, SplitterTests::beltItemLinks);
        tests.test("splitter_places_both_halves", 20, helper -> places(helper, false));
        tests.test("splitter_blocked_at_one_half_places_neither", 20, helper -> places(helper, true));
        tests.test("splitter_broken_at_its_left_half_leaves_nothing", 20, helper -> breaks(helper, LEFT));
        tests.test("splitter_broken_at_its_right_half_leaves_nothing", 20, helper -> breaks(helper, RIGHT));
    }

    private static void splitsEvenly(GameTestHelper helper) {
        line(helper, BeltTier.BELT, BeltTier.BELT, true, true);
        int expected = TIER_1_ITEMS_PER_SECOND * WINDOW_TICKS / 20;
        measure(helper, (left, right) -> {
            if (left + right != expected || Math.abs(left - right) > 1) {
                helper.fail("a splitter on a tier-1 line delivered " + left + " left and " + right
                        + " right in " + WINDOW_TICKS + " ticks, expected " + expected / 2 + " each", LEFT);
            }
        });
    }

    // Nothing behind the right end's loader, so that side backs up.
    private static void switchesToTheFreeSide(GameTestHelper helper) {
        line(helper, BeltTier.BELT, BeltTier.BELT, true, false);
        int expected = TIER_1_ITEMS_PER_SECOND * WINDOW_TICKS / 20;
        measure(helper, (left, right) -> {
            if (left != expected) {
                helper.fail("with its right side backed up a splitter delivered " + left
                        + " left in " + WINDOW_TICKS + " ticks, expected " + expected, LEFT);
            }
        });
    }

    private static void capsTheLine(GameTestHelper helper) {
        line(helper, BeltTier.EXPRESS, BeltTier.BELT, true, false);
        int expected = TIER_1_SPLITTER_ITEMS_PER_SECOND * WINDOW_TICKS / 20;
        measure(helper, (left, right) -> {
            if (left != expected) {
                helper.fail("a tier-1 splitter on a tier-3 line delivered " + left + " in "
                        + WINDOW_TICKS + " ticks, expected " + expected, LEFT);
            }
        });
    }

    /** A belt into the splitter's left half and one out of it, laid by clicking with the belt item. */
    private static void beltItemLinks(GameTestHelper helper) {
        placeSplitter(helper, BeltTier.BELT);
        helper.setBlock(FROM, loader(BeltTier.BELT, Direction.EAST));
        helper.setBlock(LEFT_END, loader(BeltTier.BELT, Direction.WEST));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemContent.beltFor(BeltTier.BELT), 64));

        click(helper, player, FROM);
        click(helper, player, LEFT);
        click(helper, player, LEFT);
        click(helper, player, LEFT_END);

        if (!helper.absolutePos(LEFT).equals(helper.getBlockEntity(FROM, ChuteBlockEntity.class).getTarget())) {
            helper.fail("clicking a loader then the splitter laid no belt into the splitter", FROM);
        }
        if (!helper.absolutePos(LEFT_END).equals(helper.getBlockEntity(LEFT, ChuteBlockEntity.class).getTarget())) {
            helper.fail("clicking the splitter then a loader laid no belt out of the splitter", LEFT);
        }
        for (BlockPos pos : List.of(LEFT.above(), LEFT.west(), LEFT.east())) {
            if (!helper.getBlockState(pos).isAir()) {
                helper.fail("clicking the splitter placed " + helper.getBlockState(pos) + " beside it", pos);
            }
        }
        helper.succeed();
    }

    private static void click(GameTestHelper helper, Player player, BlockPos target) {
        BlockPos absolute = helper.absolutePos(target);
        helper.useBlock(target, player, new BlockHitResult(
                Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false));
    }

    private static void places(GameTestHelper helper, boolean blocked) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(-90);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemContent.SPLITTER.get()));
        BlockPos left = FLOOR.above();
        BlockPos right = left.relative(Direction.EAST.getClockWise());
        if (blocked) helper.setBlock(right, Blocks.STONE);

        BlockPos absolute = helper.absolutePos(FLOOR);
        helper.useBlock(FLOOR, player, new BlockHitResult(
                Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false));

        BlockState leftState = helper.getBlockState(left);
        BlockState rightState = helper.getBlockState(right);
        if (blocked) {
            if (!leftState.isAir() || !rightState.is(Blocks.STONE)) {
                helper.fail("a splitter blocked at its right half left " + leftState + " and " + rightState, left);
            }
            if (player.getMainHandItem().getCount() != 1) {
                helper.fail("a refused splitter was consumed", left);
            }
        } else if (!leftState.equals(half(BeltTier.BELT, SplitterBlock.Side.LEFT))
                       || !rightState.equals(half(BeltTier.BELT, SplitterBlock.Side.RIGHT))) {
            helper.fail("placing a splitter facing east left " + leftState + " and " + rightState, left);
        }
        helper.succeed();
    }

    private static void breaks(GameTestHelper helper, BlockPos broken) {
        placeSplitter(helper, BeltTier.BELT);
        // Not makeMockServerPlayerInLevel: joining the level fires KubeJS's login sync, which
        // refuses the mock connection.
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.gameMode.destroyBlock(helper.absolutePos(broken));

        for (BlockPos pos : List.of(LEFT, RIGHT)) {
            if (helper.getBlockState(pos).getBlock() instanceof SplitterBlock) {
                helper.fail("breaking one half left the other standing", pos);
            }
        }
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16);
        int dropped = helper.getLevel().getEntities(EntityType.ITEM, area, e -> true).stream()
                .map(ItemEntity::getItem)
                .filter(stack -> stack.is(ItemContent.SPLITTER.get()))
                .mapToInt(ItemStack::getCount)
                .sum();
        if (dropped != 1) {
            helper.fail("breaking a splitter dropped " + dropped + " splitters where exactly one is paid", broken);
        }
        helper.succeed();
    }

    /** A chest and loader feeding the left half, each half belted to a loader with a chest behind it or not. */
    private static void line(GameTestHelper helper, BeltTier belts, BeltTier splitter,
            boolean leftDrains, boolean rightDrains) {
        if (belts != BeltTier.BELT) helper.setBlock(POLE, PFBlocks.CREATIVE_POLE.get());
        placeSplitter(helper, splitter);
        helper.setBlock(SOURCE, Blocks.CHEST);
        for (int slot = 0, left = SUPPLY; left > 0; slot++, left -= 64) {
            BeltHandoffTests.chest(helper, SOURCE).setItem(slot, new ItemStack(Items.COBBLESTONE, Math.min(left, 64)));
        }
        helper.setBlock(FROM, loader(belts, Direction.EAST));
        helper.setBlock(LEFT_END, loader(belts, Direction.WEST));
        helper.setBlock(RIGHT_END, loader(belts, Direction.WEST));
        if (leftDrains) helper.setBlock(LEFT_END.east(), Blocks.CHEST);
        if (rightDrains) helper.setBlock(RIGHT_END.east(), Blocks.CHEST);

        belt(helper, FROM, LEFT, belts);
        belt(helper, LEFT, LEFT_END, belts);
        belt(helper, RIGHT, RIGHT_END, belts);
    }

    private static void measure(GameTestHelper helper, Check check) {
        int[] before = new int[2];
        helper.startSequence()
                .thenIdle(WARMUP_TICKS)
                .thenExecute(() -> {
                    before[0] = delivered(helper, LEFT_END);
                    before[1] = delivered(helper, RIGHT_END);
                })
                .thenIdle(WINDOW_TICKS)
                .thenExecute(() -> check.accept(delivered(helper, LEFT_END) - before[0],
                        delivered(helper, RIGHT_END) - before[1]))
                .thenSucceed();
    }

    private static int delivered(GameTestHelper helper, BlockPos end) {
        return helper.getBlockState(end.east()).is(Blocks.CHEST)
                ? BeltHandoffTests.count(BeltHandoffTests.chest(helper, end.east())) : 0;
    }

    private static void placeSplitter(GameTestHelper helper, BeltTier tier) {
        helper.setBlock(LEFT, half(tier, SplitterBlock.Side.LEFT));
        helper.setBlock(RIGHT, half(tier, SplitterBlock.Side.RIGHT));
    }

    private static void belt(GameTestHelper helper, BlockPos from, BlockPos to, BeltTier tier) {
        helper.getBlockEntity(from, ChuteBlockEntity.class)
                .assignFromBeltItem(helper.absolutePos(to), List.of(), tier, 0);
    }

    private static BlockState half(BeltTier tier, SplitterBlock.Side side) {
        return BlockContent.splitterFor(tier).defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)
                .setValue(SplitterBlock.SIDE, side);
    }

    private static BlockState loader(BeltTier tier, Direction facing) {
        return BlockContent.loaderFor(tier).defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    @FunctionalInterface
    private interface Check {
        void accept(int left, int right);
    }
}
