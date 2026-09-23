package com.planetaryfactory.core.gametest;

import java.util.HashSet;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import com.planetaryfactory.core.PFBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import rearth.belts.blocks.BeltTileBlock;
import rearth.belts.blocks.BeltTileBlockEntity;
import rearth.belts.blocks.ChuteBlockEntity;
import rearth.belts.blocks.SplitterBlock;
import rearth.belts.items.SplitterItem;
import rearth.belts.model.BeltTier;

/**
 * A splitter between belt tiles (#394): its split, fallback, cap and break, and a splitter placed
 * across a tile line taking the place of the tile under it.
 *
 * <p>Items flow east. A chest and a loader feed a line of tiles into the splitter's left half, and a
 * line of tiles leaves each half for a loader with a chest behind it or not. The rates are typed.
 */
final class SplitterTileTests {

    private static final BlockPos SOURCE = new BlockPos(1, 1, 2);
    private static final BlockPos FROM = new BlockPos(2, 1, 2);
    private static final BlockPos LEFT = new BlockPos(6, 1, 2);
    private static final BlockPos RIGHT = LEFT.relative(Direction.EAST.getClockWise());
    private static final BlockPos LEFT_END = new BlockPos(10, 1, 2);
    private static final BlockPos RIGHT_END = new BlockPos(10, 1, 3);
    private static final BlockPos SOURCE_RIGHT = new BlockPos(1, 1, 3);
    private static final BlockPos FROM_RIGHT = new BlockPos(2, 1, 3);
    private static final BlockPos POLE = new BlockPos(4, 1, 5);
    private static final BlockPos STANDING = new BlockPos(6, 1, 0);

    private static final int TIER_1_ITEMS_PER_SECOND = 15;
    // A multiple of four ticks: tier 1 delivers a whole number of items only every four.
    private static final int WINDOW_TICKS = 200;
    private static final int WARMUP_TICKS = 200;
    private static final int SUPPLY = 27 * 64;
    // Two blocks of belt, one per half.
    private static final int BACKED_UP = 16;

    // Fewer than the seven-tile line holds, so none waits in the chest, and enough that the tile the
    // splitter replaces and the tiles before it carry some.
    private static final int CROSSED_SUPPLY = 48;
    private static final int CROSSED_FILL_TICKS = 200;
    private static final int CROSSED_DRAIN_TICKS = 300;

    private SplitterTileTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("splitter_between_tiles_splits_one_line_evenly", WARMUP_TICKS + WINDOW_TICKS + 20,
                SplitterTileTests::splitsEvenly);
        tests.test("splitter_between_tiles_sends_everything_to_the_free_side", WARMUP_TICKS + WINDOW_TICKS + 20,
                SplitterTileTests::switchesToTheFreeSide);
        tests.test("tier_1_splitter_caps_a_tier_3_tile_line_at_" + TIER_1_ITEMS_PER_SECOND,
                WARMUP_TICKS + WINDOW_TICKS + 20, SplitterTileTests::capsTheLine);
        tests.test("splitter_placed_across_a_tile_line_replaces_the_tile_and_loses_nothing",
                CROSSED_FILL_TICKS + CROSSED_DRAIN_TICKS + 20, SplitterTileTests::placedAcrossALine);
        tests.test("splitter_aimed_across_a_tile_line_facing_north_places_nothing", 20,
                helper -> refusedAcrossALine(helper, Direction.NORTH));
        tests.test("splitter_aimed_across_a_tile_line_facing_south_places_nothing", 20,
                helper -> refusedAcrossALine(helper, Direction.SOUTH));
        tests.test("splitter_between_tiles_broken_at_its_left_half_hands_its_items_to_the_breaker",
                WARMUP_TICKS + 20, helper -> breaks(helper, LEFT));
        tests.test("splitter_between_tiles_broken_at_its_right_half_hands_its_items_to_the_breaker",
                WARMUP_TICKS + 20, helper -> breaks(helper, RIGHT));
    }

    private static void splitsEvenly(GameTestHelper helper) {
        line(helper, BeltTier.BELT, BeltTier.BELT, true, true);
        int expected = TIER_1_ITEMS_PER_SECOND * WINDOW_TICKS / 20;
        measure(helper, (left, right) -> {
            if (left + right != expected || Math.abs(left - right) > 1) {
                helper.fail("a splitter between tier-1 tiles delivered " + left + " left and " + right
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
                helper.fail("with its right side backed up a splitter between tiles delivered " + left
                        + " left in " + WINDOW_TICKS + " ticks, expected " + expected, LEFT);
            }
        });
    }

    private static void capsTheLine(GameTestHelper helper) {
        line(helper, BeltTier.EXPRESS, BeltTier.BELT, true, false);
        int expected = TIER_1_ITEMS_PER_SECOND * WINDOW_TICKS / 20;
        measure(helper, (left, right) -> {
            if (left != expected) {
                helper.fail("a tier-1 splitter between tier-3 tiles delivered " + left + " in "
                        + WINDOW_TICKS + " ticks, expected " + expected, LEFT);
            }
        });
    }

    /**
     * A backed-up line of seven tiles, a splitter placed by hand on its middle tile: the tile is
     * refunded, every item stays on the line or the half, and once the end has a chest every
     * item reaches it or the right half, which no line leaves.
     */
    private static void placedAcrossALine(GameTestHelper helper) {
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.setBlock(FROM, BeltTileTests.loader(BeltTier.BELT, Direction.EAST));
        for (BlockPos at = FROM.east(); at.getX() < LEFT_END.getX(); at = at.east()) {
            helper.setBlock(at, BeltTileTests.tile(BeltTier.BELT, Direction.EAST));
        }
        helper.setBlock(LEFT_END, BeltTileTests.loader(BeltTier.BELT, Direction.WEST));
        BeltTileTests.fill(helper, SOURCE, CROSSED_SUPPLY);
        ServerPlayer player = player(helper, "pf_splitter_tile_placer");
        player.setYRot(-90);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemContent.SPLITTER.get()));

        helper.startSequence()
                .thenIdle(CROSSED_FILL_TICKS)
                .thenExecute(() -> {
                    if (onLines(helper) != CROSSED_SUPPLY) {
                        helper.fail("the line holds " + onLines(helper) + " of " + CROSSED_SUPPLY
                                + " before the splitter, so this proves little", LEFT);
                    }
                    BlockPos floor = helper.absolutePos(LEFT.below());
                    helper.useBlock(LEFT.below(), player, new BlockHitResult(
                            Vec3.atCenterOf(floor).relative(Direction.UP, 0.5), Direction.UP, floor, false));
                    if (!(helper.getBlockState(LEFT).getBlock() instanceof SplitterBlock)
                            || !(helper.getBlockState(RIGHT).getBlock() instanceof SplitterBlock)) {
                        helper.fail("no splitter was placed across the tile line", LEFT);
                    }
                    int tiles = ContainerHelper.clearOrCountMatchingItems(player.getInventory(),
                            stack -> stack.is(BlockContent.tileFor(BeltTier.BELT).asItem()), 0, true);
                    if (tiles != 1) {
                        helper.fail("placing the splitter refunded " + tiles + " tiles, expected the one it replaced", LEFT);
                    }
                    if (!player.getMainHandItem().isEmpty()) {
                        helper.fail("the splitter was not spent", LEFT);
                    }
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    int held = onLines(helper) + half(helper, LEFT) + half(helper, RIGHT);
                    if (held != CROSSED_SUPPLY || cobblestone(player) != 0 || onGround(helper) != 0) {
                        helper.fail("after the splitter the line and halves hold " + held + " of " + CROSSED_SUPPLY
                                + ", the placer " + cobblestone(player) + " and the ground " + onGround(helper), LEFT);
                    }
                    helper.setBlock(LEFT_END.east(), Blocks.CHEST);
                })
                .thenIdle(CROSSED_DRAIN_TICKS)
                .thenExecute(() -> {
                    // The right half has no line leaving it, so what it is split backs up there.
                    int delivered = delivered(helper, LEFT_END);
                    int right = half(helper, RIGHT);
                    if (delivered + right != CROSSED_SUPPLY || delivered == 0) {
                        helper.fail("the chest past the splitter holds " + delivered + " and the right half " + right
                                + " of " + CROSSED_SUPPLY, LEFT_END);
                    }
                })
                .thenSucceed();
    }

    /**
     * A splitter facing across a line of east-running tiles, clicked on a tile's top: whichever
     * side its second half falls on, it is planned refused on the tile, and goes neither on it nor above it.
     */
    private static void refusedAcrossALine(GameTestHelper helper, Direction facing) {
        for (BlockPos at = FROM.east(); at.getX() < LEFT_END.getX(); at = at.east()) {
            helper.setBlock(at, BeltTileTests.tile(BeltTier.BELT, Direction.EAST));
        }
        ServerPlayer player = player(helper, "pf_splitter_tile_across_" + facing.getSerializedName());
        player.setYRot(facing.toYRot());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemContent.SPLITTER.get()));
        BlockPos tile = helper.absolutePos(LEFT);
        BlockHitResult hit = new BlockHitResult(Vec3.atBottomCenterOf(tile).add(0, 6 / 16d, 0), Direction.UP, tile, false);
        var plan = ((SplitterItem) player.getMainHandItem().getItem())
                .plan(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, player.getMainHandItem(), hit));
        if (plan == null || !plan.blocked() || !plan.halves().getFirst().pos().equals(tile)) {
            helper.fail("a splitter facing " + facing + " is not planned refused on the tile it was aimed at", LEFT);
        }
        helper.useBlock(LEFT, player, hit);

        for (BlockPos at = FROM.east(); at.getX() < LEFT_END.getX(); at = at.east()) {
            if (!(helper.getBlockState(at).getBlock() instanceof BeltTileBlock)) {
                helper.fail("a splitter facing " + facing + " replaced the tile at " + at, at);
            }
            if (!helper.getBlockState(at.above()).isAir()) {
                helper.fail("a splitter facing " + facing + " went on top of the line at " + at, at.above());
            }
        }
        if (player.getMainHandItem().getCount() != 1) {
            helper.fail("a refused splitter was spent", LEFT);
        }
        helper.succeed();
    }

    /** Both halves fed by tile lines with no line leaving either, then broken at one half. */
    private static void breaks(GameTestHelper helper, BlockPos broken) {
        placeSplitter(helper, BeltTier.BELT);
        feed(helper, SOURCE, FROM, BeltTier.BELT);
        feed(helper, SOURCE_RIGHT, FROM_RIGHT, BeltTier.BELT);
        ServerPlayer player = player(helper, "pf_splitter_tile_breaker");
        helper.startSequence()
                .thenIdle(WARMUP_TICKS)
                .thenExecute(() -> {
                    int onSplitter = half(helper, LEFT) + half(helper, RIGHT);
                    if (onSplitter != BACKED_UP) {
                        helper.fail("the splitter holds " + onSplitter + ", expected " + BACKED_UP, LEFT);
                    }
                    player.gameMode.destroyBlock(helper.absolutePos(broken));
                    for (BlockPos pos : new BlockPos[] {LEFT, RIGHT}) {
                        if (helper.getBlockState(pos).getBlock() instanceof SplitterBlock) {
                            helper.fail("breaking one half left the other standing", pos);
                        }
                    }
                    int handed = cobblestone(player);
                    if (handed != onSplitter || onGround(helper) != 0) {
                        helper.fail("breaking the splitter handed the breaker " + handed + " of its " + onSplitter
                                + " items and left " + onGround(helper) + " on the ground", broken);
                    }
                    int splitters = helper.getLevel().getEntities(EntityType.ITEM, area(helper), e -> true).stream()
                            .map(ItemEntity::getItem)
                            .filter(stack -> stack.is(ItemContent.SPLITTER.get()))
                            .mapToInt(ItemStack::getCount)
                            .sum();
                    if (splitters != 1) {
                        helper.fail("breaking the splitter dropped " + splitters + " splitters where exactly one is paid", broken);
                    }
                })
                .thenSucceed();
    }

    /** A tile line into the left half, a tile line out of each half to a loader with a chest behind it or not. */
    private static void line(GameTestHelper helper, BeltTier belts, BeltTier splitter, boolean leftDrains, boolean rightDrains) {
        if (belts != BeltTier.BELT) helper.setBlock(POLE, PFBlocks.CREATIVE_POLE.get());
        placeSplitter(helper, splitter);
        feed(helper, SOURCE, FROM, belts);
        for (BlockPos end : new BlockPos[] {LEFT_END, RIGHT_END}) {
            for (BlockPos at = new BlockPos(LEFT.getX() + 1, 1, end.getZ()); at.getX() < end.getX(); at = at.east()) {
                helper.setBlock(at, BeltTileTests.tile(belts, Direction.EAST));
            }
            helper.setBlock(end, BeltTileTests.loader(belts, Direction.WEST));
        }
        if (leftDrains) helper.setBlock(LEFT_END.east(), Blocks.CHEST);
        if (rightDrains) helper.setBlock(RIGHT_END.east(), Blocks.CHEST);
    }

    /** A chest and an east-facing loader, and tiles from the loader to the splitter half on its row. */
    private static void feed(GameTestHelper helper, BlockPos source, BlockPos from, BeltTier tier) {
        helper.setBlock(source, Blocks.CHEST);
        BeltTileTests.fill(helper, source, SUPPLY);
        helper.setBlock(from, BeltTileTests.loader(tier, Direction.EAST));
        for (BlockPos at = from.east(); at.getX() < LEFT.getX(); at = at.east()) {
            helper.setBlock(at, BeltTileTests.tile(tier, Direction.EAST));
        }
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
                ? BeltTileTests.count(BeltTileTests.chest(helper, end.east())) : 0;
    }

    /** Every item on a tile of the splitter's row, each line counted once. */
    private static int onLines(GameTestHelper helper) {
        var counted = new HashSet<Object>();
        int items = 0;
        for (BlockPos at = FROM.east(); at.getX() < LEFT_END.getX(); at = at.east()) {
            if (!(helper.getLevel().getBlockEntity(helper.absolutePos(at)) instanceof BeltTileBlockEntity tile)) continue;
            var line = tile.line();
            if (line != null && counted.add(line)) items += line.size();
        }
        return items;
    }

    private static int half(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, ChuteBlockEntity.class).getHalf().size();
    }

    private static int onGround(GameTestHelper helper) {
        return helper.getLevel().getEntities(EntityType.ITEM, area(helper), e -> true).stream()
                .map(ItemEntity::getItem)
                .filter(stack -> stack.is(Items.COBBLESTONE))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private static AABB area(GameTestHelper helper) {
        return new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16);
    }

    private static void placeSplitter(GameTestHelper helper, BeltTier tier) {
        helper.setBlock(LEFT, half(tier, SplitterBlock.Side.LEFT));
        helper.setBlock(RIGHT, half(tier, SplitterBlock.Side.RIGHT));
    }

    private static BlockState half(BeltTier tier, SplitterBlock.Side side) {
        return BlockContent.splitterFor(tier).defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)
                .setValue(SplitterBlock.SIDE, side);
    }

    // A player of its own: the shared fake player's inventory is every test's at once.
    private static ServerPlayer player(GameTestHelper helper, String name) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        var at = helper.absoluteVec(STANDING.getBottomCenter());
        player.setPos(at.x, at.y, at.z);
        return player;
    }

    private static int cobblestone(ServerPlayer player) {
        return ContainerHelper.clearOrCountMatchingItems(player.getInventory(),
                stack -> stack.is(Items.COBBLESTONE), 0, true);
    }

    @FunctionalInterface
    private interface Check {
        void accept(int left, int right);
    }
}
