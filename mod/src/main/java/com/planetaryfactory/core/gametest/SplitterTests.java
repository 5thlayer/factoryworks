package com.planetaryfactory.core.gametest;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import com.planetaryfactory.core.PFBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import rearth.belts.BlockContent;
import rearth.belts.ItemContent;
import rearth.belts.blocks.ChuteBlockEntity;
import rearth.belts.blocks.SplitterBlock;
import rearth.belts.model.BeltContents;
import rearth.belts.model.BeltTier;

/**
 * A splitter in the pack's SimpleBelts fork (#349, ADR-0076): one belt split evenly across two, a
 * backed-up side sending everything to the other, and a tier-1 splitter capping a tier-3 line.
 * It places and breaks as both halves. Each half is a block of belt (#373): a backed-up splitter
 * holds 16, and its items are saved, handed to the player who breaks it and taken by a held hand.
 *
 * <p>A splitter placed across a running belt cuts it (#361): the belt ends at the half's back face
 * and a belt of the rest starts at its front, items staying where they were and one belt refunded.
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
    private static final BlockPos SOURCE_RIGHT = new BlockPos(1, 1, 3);
    private static final BlockPos FROM_RIGHT = new BlockPos(2, 1, 3);
    // Beside the left half, well within reach of it.
    private static final BlockPos STANDING = new BlockPos(6, 1, 0);

    private static final int TIER_1_ITEMS_PER_SECOND = 15;
    private static final int TIER_1_SPLITTER_ITEMS_PER_SECOND = 15;
    // A multiple of four ticks: tier 1 delivers a whole number of items only every four.
    private static final int WINDOW_TICKS = 200;
    // Long enough for a four-block side to back up at half the line's rate.
    private static final int WARMUP_TICKS = 200;
    private static final int SUPPLY = 27 * 64;
    // Two blocks of belt, one per half.
    private static final int BACKED_UP = 16;
    // Before the midline, so every item entering the left half reaches the hand.
    private static final double HELD_AT = 0.25;
    // A multiple of four ticks: tier 1 moves a whole number of items only every four.
    private static final int HOLD_TICKS = 100;
    private static final int HELD_ITEMS = TIER_1_ITEMS_PER_SECOND * HOLD_TICKS / 20;

    private static final BlockPos FLOOR = new BlockPos(3, 0, 3);

    // A nine-block belt cut four blocks along: 32 behind the half, 8 on it and 32 past it.
    private static final int NINE_BLOCKS_FULL = 72;
    private static final int NINE_BLOCKS_COST = 9;
    private static final int UPSTREAM_ENTRIES = 32;
    private static final int HALF_ENTRIES = 8;
    private static final int DOWNSTREAM_ENTRIES = 32;
    private static final int FLOW_TICKS = 200;
    // A five-block belt cut two blocks along keeps two blocks either side and refunds the third.
    private static final BlockPos SHORT_END = new BlockPos(6, 1, 2);
    private static final BlockPos SHORT_CUT = new BlockPos(4, 1, 2);
    private static final int SHORT_COST = 5;
    private static final int SHORT_HALF_COST = 2;

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
        tests.test("backed_up_splitter_holds_" + BACKED_UP, WARMUP_TICKS + 20, SplitterTests::backsUp);
        tests.test("splitter_broken_at_its_left_half_hands_its_items_to_the_breaker", WARMUP_TICKS + 20,
                helper -> breakingHandsItsItemsBack(helper, LEFT));
        tests.test("splitter_broken_at_its_right_half_hands_its_items_to_the_breaker", WARMUP_TICKS + 20,
                helper -> breakingHandsItsItemsBack(helper, RIGHT));
        tests.test("saved_splitter_restores_every_entry", WARMUP_TICKS + 20, SplitterTests::savedSplitterRestores);
        tests.test("held_splitter_half_fills_the_inventory", WARMUP_TICKS + HOLD_TICKS + 20,
                SplitterTests::heldHalfFillsTheInventory);
        tests.test("splitter_placed_across_a_belt_links_one_in_and_one_out", FLOW_TICKS + 20,
                helper -> cutsLayout(helper, List.of(new Column(LEFT, Side.LONG, Side.LONG))));
        tests.test("splitter_placed_across_two_belts_links_two_in_and_one_out", FLOW_TICKS + 20,
                helper -> cutsLayout(helper, List.of(new Column(LEFT, Side.LONG, Side.LONG), new Column(RIGHT, Side.LONG, Side.STUB))));
        tests.test("splitter_placed_across_two_belts_links_one_in_and_two_out", FLOW_TICKS + 20,
                helper -> cutsLayout(helper, List.of(new Column(LEFT, Side.LONG, Side.LONG), new Column(RIGHT, Side.STUB, Side.LONG))));
        tests.test("splitter_placed_across_two_belts_links_two_in_and_two_out", FLOW_TICKS + 20,
                helper -> cutsLayout(helper, List.of(new Column(LEFT, Side.LONG, Side.LONG), new Column(RIGHT, Side.LONG, Side.LONG))));
        tests.test("splitter_placed_on_a_loaded_belt_keeps_every_entry", WARMUP_TICKS + 20,
                SplitterTests::cutsALoadedBelt);
        tests.test("splitter_placed_on_a_five_block_belt_refunds_one_belt", 20, SplitterTests::cutRefunds);
    }

    /**
     * A long side is a belt to or from a loader four blocks off the splitter. A stub is the one
     * block of belt inside a loader touching the splitter, which is a half with a belt on one side
     * only: the cut links the loader straight to the half.
     */
    private enum Side { LONG, STUB }

    private record Column(BlockPos half, Side in, Side out) {

        BlockPos from() {
            return new BlockPos(in == Side.LONG ? 2 : 5, 1, half.getZ());
        }

        BlockPos to() {
            return new BlockPos(out == Side.LONG ? 10 : 7, 1, half.getZ());
        }
    }

    private static void cutsLayout(GameTestHelper helper, List<Column> columns) {
        for (Column column : columns) {
            helper.setBlock(column.to(), loader(BeltTier.BELT, Direction.WEST));
            helper.setBlock(column.to().east(), Blocks.CHEST);
            feed(helper, column.from().west(), column.from(), column.to(), BeltTier.BELT);
        }
        ServerPlayer player = placer(helper, "pf_splitter_layout");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    placeByHand(helper, player, LEFT);
                    for (Column column : columns) {
                        ChuteBlockEntity half = chute(helper, column.half());
                        if (!helper.absolutePos(column.half()).equals(chute(helper, column.from()).getTarget())) {
                            helper.fail("the belt from " + column.from() + " was not cut at the splitter", column.half());
                        }
                        if (!helper.absolutePos(column.to()).equals(half.getTarget())) {
                            helper.fail("the half at " + column.half() + " starts no belt to " + column.to(), column.half());
                        }
                        if (chute(helper, column.to()).incomingBelt() != half) {
                            helper.fail("the loader at " + column.to() + " is not the end of the half's belt", column.to());
                        }
                    }
                })
                .thenIdle(FLOW_TICKS)
                .thenExecute(() -> {
                    for (Column column : columns) {
                        if (delivered(helper, column.to()) == 0) {
                            helper.fail("nothing reached " + column.to() + " through the cut belt", column.to());
                        }
                    }
                })
                .thenSucceed();
    }

    private static void cutsALoadedBelt(GameTestHelper helper) {
        helper.setBlock(LEFT_END, loader(BeltTier.BELT, Direction.WEST));
        feed(helper, SOURCE, FROM, LEFT_END, BeltTier.BELT);
        chute(helper, FROM).assignFromBeltItem(helper.absolutePos(LEFT_END), List.of(), BeltTier.BELT, NINE_BLOCKS_COST);
        ServerPlayer player = placer(helper, "pf_splitter_loaded");
        helper.startSequence()
                .thenIdle(WARMUP_TICKS)
                .thenExecute(() -> {
                    ChuteBlockEntity from = chute(helper, FROM);
                    if (from.getBeltEntries().size() != NINE_BLOCKS_FULL) {
                        helper.fail("the belt holds " + from.getBeltEntries().size() + ", so this proves little", FROM);
                    }
                    List<Double> before = from.getBeltEntries().stream().map(BeltContents.Entry::position).toList();
                    placeByHand(helper, player, LEFT);

                    ChuteBlockEntity half = chute(helper, LEFT);
                    int upstream = from.getBeltEntries().size();
                    int onHalf = half.getHalf().size();
                    int downstream = half.getBeltEntries().size();
                    int handed = cobblestone(player);
                    if (upstream != UPSTREAM_ENTRIES || onHalf != HALF_ENTRIES || downstream != DOWNSTREAM_ENTRIES || handed != 0) {
                        helper.fail("the cut left " + upstream + " behind the half, " + onHalf + " on it, " + downstream
                                + " past it and handed " + handed + ", expected " + UPSTREAM_ENTRIES + ", " + HALF_ENTRIES
                                + ", " + DOWNSTREAM_ENTRIES + " and 0", LEFT);
                    }
                    List<Double> kept = from.getBeltEntries().stream().map(BeltContents.Entry::position).toList();
                    if (!kept.equals(before.subList(0, kept.size()))) {
                        helper.fail("the entries behind the half moved: " + kept, FROM);
                    }
                    double first = half.getBeltEntries().getFirst().position();
                    if (Math.abs(first) > 1e-6) helper.fail("the first entry past the half is at " + first + ", not its head", LEFT);
                    if (belts(player) != 1) helper.fail("the cut refunded " + belts(player) + " belts, not 1", LEFT);
                })
                .thenSucceed();
    }

    private static void cutRefunds(GameTestHelper helper) {
        helper.setBlock(FROM, loader(BeltTier.BELT, Direction.EAST));
        helper.setBlock(SHORT_END, loader(BeltTier.BELT, Direction.WEST));
        chute(helper, FROM).assignFromBeltItem(helper.absolutePos(SHORT_END), List.of(), BeltTier.BELT, SHORT_COST)
                .ifPresent(refusal -> helper.fail("the fixture's belt is refused: " + refusal, FROM));
        ServerPlayer player = placer(helper, "pf_splitter_refund");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    placeByHand(helper, player, SHORT_CUT);
                    if (belts(player) != 1) helper.fail("placing the splitter refunded " + belts(player) + " belts, not 1", SHORT_CUT);
                    int upstream = chute(helper, FROM).getCost();
                    int downstream = chute(helper, SHORT_CUT).getCost();
                    if (upstream != SHORT_HALF_COST || downstream != SHORT_HALF_COST) {
                        helper.fail("the halves store " + upstream + " and " + downstream + ", expected "
                                + SHORT_HALF_COST + " each", SHORT_CUT);
                    }
                    for (BlockPos end : List.of(FROM, SHORT_END)) {
                        ServerPlayer breaker = player(helper, "pf_splitter_refund_breaker");
                        breaker.gameMode.destroyBlock(helper.absolutePos(end));
                        if (belts(breaker) != SHORT_HALF_COST) {
                            helper.fail("breaking " + end + " refunded " + belts(breaker) + " belts, not " + SHORT_HALF_COST, end);
                        }
                    }
                })
                .thenSucceed();
    }

    /** A survival player facing east holding a splitter. */
    private static ServerPlayer placer(GameTestHelper helper, String name) {
        ServerPlayer player = player(helper, name);
        player.setYRot(-90);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemContent.SPLITTER.get()));
        return player;
    }

    /** Clicks the top of the block under {@code left}, as a player placing a splitter there does. */
    private static void placeByHand(GameTestHelper helper, Player player, BlockPos left) {
        BlockPos floor = left.below();
        BlockPos absolute = helper.absolutePos(floor);
        helper.useBlock(floor, player, new BlockHitResult(
                Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false));
        if (!(helper.getBlockState(left).getBlock() instanceof SplitterBlock)) {
            helper.fail("no splitter was placed at " + left, left);
        }
    }

    private static int belts(Player player) {
        return ContainerHelper.clearOrCountMatchingItems(player.getInventory(),
                stack -> stack.is(ItemContent.beltFor(BeltTier.BELT)), 0, true);
    }

    private static void backsUp(GameTestHelper helper) {
        backedUpLine(helper);
        helper.startSequence()
                .thenIdle(WARMUP_TICKS)
                .thenExecute(() -> {
                    int left = chute(helper, LEFT).getHalf().size();
                    int right = chute(helper, RIGHT).getHalf().size();
                    if (left + right != BACKED_UP || left != right) {
                        helper.fail("a backed-up splitter holds " + left + " on its left half and " + right
                                + " on its right, expected " + BACKED_UP / 2 + " each", LEFT);
                    }
                })
                .thenSucceed();
    }

    private static void breakingHandsItsItemsBack(GameTestHelper helper, BlockPos broken) {
        backedUpLine(helper);
        ServerPlayer player = player(helper, "pf_splitter_breaker");
        helper.startSequence()
                .thenIdle(WARMUP_TICKS)
                .thenExecute(() -> {
                    int onSplitter = chute(helper, LEFT).getHalf().size() + chute(helper, RIGHT).getHalf().size();
                    int onBelts = chute(helper, FROM).getBeltEntries().size() + chute(helper, FROM_RIGHT).getBeltEntries().size();
                    int refund = chute(helper, FROM).getCost() + chute(helper, FROM_RIGHT).getCost();
                    if (onSplitter != BACKED_UP) {
                        helper.fail("the splitter holds " + onSplitter + ", so this proves little", LEFT);
                    }
                    player.gameMode.destroyBlock(helper.absolutePos(broken));

                    int handed = cobblestone(player);
                    if (handed != onSplitter + onBelts) {
                        helper.fail("breaking a splitter holding " + onSplitter + " with " + onBelts
                                + " on the belts into it handed the breaker " + handed, broken);
                    }
                    int belts = ContainerHelper.clearOrCountMatchingItems(player.getInventory(),
                            stack -> stack.is(ItemContent.beltFor(BeltTier.BELT)), 0, true);
                    if (belts != refund) {
                        helper.fail("breaking a splitter refunded " + belts + " belt items of the " + refund
                                + " its two belts cost", broken);
                    }
                    int lying = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(2.0))
                            .stream().map(ItemEntity::getItem).filter(stack -> stack.is(Items.COBBLESTONE))
                            .mapToInt(ItemStack::getCount).sum();
                    if (lying != 0) helper.fail(lying + " cobblestone lie on the ground", broken);
                })
                .thenSucceed();
    }

    // Through the block entity's own save and load, which is what a world save and reload runs.
    private static void savedSplitterRestores(GameTestHelper helper) {
        backedUpLine(helper);
        helper.startSequence()
                .thenIdle(WARMUP_TICKS)
                .thenExecute(() -> {
                    for (BlockPos pos : List.of(LEFT, RIGHT)) {
                        ChuteBlockEntity half = chute(helper, pos);
                        var registries = helper.getLevel().registryAccess();
                        var saved = half.saveWithFullMetadata(registries);
                        var loaded = (ChuteBlockEntity) BlockEntity.loadStatic(half.getBlockPos(), half.getBlockState(), saved, registries);
                        if (half.getHalf().size() != BACKED_UP / 2) {
                            helper.fail("the half holds " + half.getHalf().size() + ", so this proves little", pos);
                        }
                        sameEntries(helper, pos, half.getHalf().entering(), loaded.getHalf().entering());
                        sameEntries(helper, pos, half.getHalf().leaving(), loaded.getHalf().leaving());
                    }
                })
                .thenSucceed();
    }

    private static void sameEntries(GameTestHelper helper, BlockPos pos, BeltContents<ItemStack> saved,
            BeltContents<ItemStack> loaded) {
        var before = saved.entries();
        var after = loaded.entries();
        if (after.size() != before.size()) {
            helper.fail("a half saved with " + before.size() + " entries loaded with " + after.size(), pos);
        }
        for (int i = 0; i < before.size(); i++) {
            var was = before.get(i);
            var is = after.get(i);
            if (was.id() != is.id() || was.position() != is.position() || !ItemStack.matches(was.payload(), is.payload())) {
                helper.fail("entry " + i + " saved as #" + was.id() + " " + was.payload() + " at " + was.position()
                        + " loaded as #" + is.id() + " " + is.payload() + " at " + is.position(), pos);
            }
        }
    }

    private static void heldHalfFillsTheInventory(GameTestHelper helper) {
        line(helper, BeltTier.BELT, BeltTier.BELT, true, true);
        ServerPlayer player = player(helper, "pf_splitter_hand");
        ChuteBlockEntity left = chute(helper, LEFT);
        helper.startSequence()
                .thenIdle(WARMUP_TICKS)
                .thenExecuteFor(HOLD_TICKS, () -> left.holdHand(player, true, HELD_AT))
                .thenExecute(() -> {
                    int taken = cobblestone(player);
                    if (Math.abs(taken - HELD_ITEMS) > 1) {
                        helper.fail("holding a tier-1 splitter's half for " + HOLD_TICKS + " ticks took " + taken
                                + " items, expected " + HELD_ITEMS, LEFT);
                    }
                })
                .thenSucceed();
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

    /** A chest and loader feeding each half, and no belt leaving either, so both back up. */
    private static void backedUpLine(GameTestHelper helper) {
        placeSplitter(helper, BeltTier.BELT);
        feed(helper, SOURCE, FROM, LEFT, BeltTier.BELT);
        feed(helper, SOURCE_RIGHT, FROM_RIGHT, RIGHT, BeltTier.BELT);
    }

    private static void feed(GameTestHelper helper, BlockPos source, BlockPos from, BlockPos to, BeltTier tier) {
        helper.setBlock(source, Blocks.CHEST);
        for (int slot = 0, left = SUPPLY; left > 0; slot++, left -= 64) {
            BeltHandoffTests.chest(helper, source).setItem(slot, new ItemStack(Items.COBBLESTONE, Math.min(left, 64)));
        }
        helper.setBlock(from, loader(tier, Direction.EAST));
        belt(helper, from, to, tier);
    }

    private static ChuteBlockEntity chute(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, ChuteBlockEntity.class);
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

    /** A chest and loader feeding the left half, each half belted to a loader with a chest behind it or not. */
    private static void line(GameTestHelper helper, BeltTier belts, BeltTier splitter,
            boolean leftDrains, boolean rightDrains) {
        if (belts != BeltTier.BELT) helper.setBlock(POLE, PFBlocks.CREATIVE_POLE.get());
        placeSplitter(helper, splitter);
        feed(helper, SOURCE, FROM, LEFT, belts);
        helper.setBlock(LEFT_END, loader(belts, Direction.WEST));
        helper.setBlock(RIGHT_END, loader(belts, Direction.WEST));
        if (leftDrains) helper.setBlock(LEFT_END.east(), Blocks.CHEST);
        if (rightDrains) helper.setBlock(RIGHT_END.east(), Blocks.CHEST);

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
        BeltHandoffTests.link(helper, from, to, List.of(), tier);
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
