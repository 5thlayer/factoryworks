package com.planetaryfactory.core.gametest;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import com.planetaryfactory.core.PFBlocks;
import rearth.belts.BlockContent;
import rearth.belts.ComponentContent;
import rearth.belts.ItemContent;
import rearth.belts.blocks.ChuteBlockEntity;
import rearth.belts.model.BeltTier;
import rearth.belts.model.SupportSlots;
import rearth.belts.model.SupportSlots.Click;
import rearth.belts.model.SupportSlots.Refusal;

/**
 * A support is a belt end (#366): two belts join at it at the slower belt's rate, a belt ending on
 * it with none leaving backs up, a click on it takes a free slot or is refused with nothing
 * changed, and breaking it breaks every belt it holds, refunded to the breaker.
 *
 * <p>The belts run east. A support facing east is an end both belts run through the same way. The
 * rates and capacity are typed rather than read off the fork. Which click each support refuses is
 * the fork's {@code SupportSlotsTest}.
 */
final class BeltSupportTests {

    private static final BlockPos SOURCE = new BlockPos(2, 1, 3);
    private static final BlockPos FROM = new BlockPos(3, 1, 3);
    private static final BlockPos SUPPORT = new BlockPos(8, 1, 3);
    private static final BlockPos TO = new BlockPos(13, 1, 3);
    private static final BlockPos TARGET = new BlockPos(14, 1, 3);
    private static final BlockPos POLE = new BlockPos(5, 1, 4);
    // Ground a second belt's start is stored on, clear of the first belt.
    private static final BlockPos ELSEWHERE_GROUND = new BlockPos(3, 0, 5);

    private static final int TIER_1_ITEMS_PER_SECOND = 15;
    private static final int RATE_WINDOW_TICKS = 200;
    private static final int RATE_WARMUP_TICKS = 160;
    private static final int RATE_SUPPLY = 27 * 64;

    // From the loader's west face at x = 3 to the support's centre at x = 8.5, eight items a block.
    private static final int DEAD_END_HOLDS = 44;
    private static final int DEAD_END_SUPPLY = 64;
    private static final int DEAD_END_SETTLED_TICKS = 200;

    // Laid through the belt item: a loader at x = 3, a mid-belt support at x = 5 and an end support
    // at x = 8, which is 5.5 blocks of belt.
    private static final BlockPos FROM_GROUND = FROM.below();
    private static final BlockPos MID = new BlockPos(5, 1, 3);
    private static final BlockPos END = SUPPORT;
    private static final int LAID_COST = 6;
    private static final int HELD_BELTS = 16;
    private static final int FILL_TICKS = 60;

    private BeltSupportTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_tier_1_belt_joined_to_a_tier_3_belt_delivers_" + TIER_1_ITEMS_PER_SECOND,
                RATE_WARMUP_TICKS + RATE_WINDOW_TICKS + 20, helper -> joinRate(helper, BeltTier.BELT, BeltTier.EXPRESS));
        tests.test("a_tier_3_belt_joined_to_a_tier_1_belt_delivers_" + TIER_1_ITEMS_PER_SECOND,
                RATE_WARMUP_TICKS + RATE_WINDOW_TICKS + 20, helper -> joinRate(helper, BeltTier.EXPRESS, BeltTier.BELT));
        tests.test("a_belt_ending_on_a_support_backs_up", DEAD_END_SETTLED_TICKS + 20, BeltSupportTests::deadEndBacksUp);

        tests.test("a_dead_end_refuses_a_second_belt_ending_there", 20,
                helper -> refused(helper, BeltSupportTests::deadEnd, Click.END, Refusal.IN_TAKEN));
        tests.test("a_loose_start_refuses_a_second_belt_starting_there", 20,
                helper -> refused(helper, BeltSupportTests::looseStart, Click.START, Refusal.OUT_TAKEN));
        tests.test("a_join_refuses_a_belt_starting_there", 20,
                helper -> refused(helper, BeltSupportTests::join, Click.START, Refusal.JOINED));
        tests.test("a_join_refuses_a_belt_ending_there", 20,
                helper -> refused(helper, BeltSupportTests::join, Click.END, Refusal.JOINED));
        tests.test("a_mid_belt_support_refuses_a_belt_starting_there", 20,
                helper -> refused(helper, BeltSupportTests::midBelt, Click.START, Refusal.MID_BELT));
        tests.test("a_mid_belt_support_refuses_a_belt_ending_there", 20,
                helper -> refused(helper, BeltSupportTests::midBelt, Click.END, Refusal.MID_BELT));
        tests.test("a_sneak_click_on_a_placed_support_is_refused", 20,
                helper -> refused(helper, BeltSupportTests::deadEnd, Click.MIDPOINT, Refusal.NOT_A_MIDPOINT));

        tests.test("a_belt_ends_on_a_free_support_and_carries_on_from_it", 20, BeltSupportTests::chains);
        tests.test("breaking_a_belts_end_support_refunds_it_to_the_breaker", FILL_TICKS + 20,
                helper -> breakingRefunds(helper, END, MID));
        tests.test("breaking_a_mid_belt_support_refunds_its_belt_to_the_breaker", FILL_TICKS + 20,
                helper -> breakingRefunds(helper, MID, END));
    }

    private static void joinRate(GameTestHelper helper, BeltTier in, BeltTier out) {
        // Tier-3 loaders, so neither loader is the slowest piece; they pay FE (#348).
        helper.setBlock(POLE, PFBlocks.CREATIVE_POLE.get());
        helper.setBlock(TARGET, Blocks.CHEST);
        join(helper, BeltTier.EXPRESS, in, out);
        fill(helper, RATE_SUPPLY);

        int expected = TIER_1_ITEMS_PER_SECOND * RATE_WINDOW_TICKS / 20;
        int[] before = new int[1];
        helper.startSequence()
                .thenIdle(RATE_WARMUP_TICKS)
                .thenExecute(() -> before[0] = BeltHandoffTests.count(BeltHandoffTests.chest(helper, TARGET)))
                .thenIdle(RATE_WINDOW_TICKS)
                .thenExecute(() -> {
                    int delivered = BeltHandoffTests.count(BeltHandoffTests.chest(helper, TARGET)) - before[0];
                    if (delivered != expected) {
                        helper.fail("a tier-" + in.number() + " belt joined to a tier-" + out.number() + " belt delivered "
                                + delivered + " items in " + RATE_WINDOW_TICKS + " ticks, expected " + expected, SUPPORT);
                    }
                    int accounted = BeltHandoffTests.count(BeltHandoffTests.chest(helper, SOURCE))
                            + BeltHandoffTests.count(BeltHandoffTests.chest(helper, TARGET))
                            + belt(helper, FROM).getBeltEntries().size() + belt(helper, SUPPORT).getBeltEntries().size();
                    if (accounted != RATE_SUPPLY) {
                        helper.fail("the join lost " + (RATE_SUPPLY - accounted) + " items", SUPPORT);
                    }
                })
                .thenSucceed();
    }

    private static void deadEndBacksUp(GameTestHelper helper) {
        deadEnd(helper);
        fill(helper, DEAD_END_SUPPLY);

        helper.startSequence().thenIdle(DEAD_END_SETTLED_TICKS).thenExecute(() -> {
            int held = belt(helper, FROM).getBeltEntries().size();
            if (held != DEAD_END_HOLDS) {
                helper.fail("a belt ending on a support holds " + held + ", expected " + DEAD_END_HOLDS, SUPPORT);
            }
            int left = BeltHandoffTests.count(BeltHandoffTests.chest(helper, SOURCE));
            if (left != DEAD_END_SUPPLY - DEAD_END_HOLDS) {
                helper.fail("the source chest holds " + left + ", expected " + (DEAD_END_SUPPLY - DEAD_END_HOLDS), SOURCE);
            }
            if (lying(helper) != 0) {
                helper.fail("a dead end dropped " + lying(helper) + " items", SUPPORT);
            }
        }).thenSucceed();
    }

    private static void refused(GameTestHelper helper, Consumer<GameTestHelper> fixture, Click click, Refusal refusal) {
        fixture.accept(helper);
        boolean asEnd = click != Click.START;
        var player = new ListeningPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        player.setYRot(90);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemContent.beltFor(BeltTier.BELT), HELD_BELTS));
        player.getInventory().add(new ItemStack(BlockContent.loaderFor(BeltTier.BELT).asItem(), 2));

        Map<BlockPos, BlockState> before = world(helper);
        SupportSlots.Use slots = belt(helper, SUPPORT).supportUse();
        if (asEnd) click(helper, player, ELSEWHERE_GROUND);
        player.setShiftKeyDown(click == Click.MIDPOINT);
        click(helper, player, SUPPORT);

        Map<BlockPos, BlockState> after = world(helper);
        List<BlockPos> changed = before.keySet().stream().filter(pos -> before.get(pos) != after.get(pos)).toList();
        if (!changed.isEmpty()) {
            helper.fail("a refused click changed " + changed.size() + " blocks, first at " + changed.getFirst(), SUPPORT);
        }
        if (!belt(helper, SUPPORT).supportUse().equals(slots)) {
            helper.fail("a refused click changed the support's slots from " + slots + " to "
                    + belt(helper, SUPPORT).supportUse(), SUPPORT);
        }
        if (!asEnd && player.getMainHandItem().has(ComponentContent.BELT_START.get())) {
            helper.fail("a refused first click stored the support as the belt's start", SUPPORT);
        }
        if (count(player, ItemContent.beltFor(BeltTier.BELT)) != HELD_BELTS
                || count(player, BlockContent.loaderFor(BeltTier.BELT).asItem()) != 2) {
            helper.fail("a refused click charged the player", SUPPORT);
        }
        if (!player.heard.contains(refusal.messageKey())) {
            helper.fail("the player was told " + player.heard + ", not " + refusal.messageKey(), SUPPORT);
        }
        helper.succeed();
    }

    private static void chains(GameTestHelper helper) {
        Player player = lay(helper, helper.makeMockPlayer(GameType.SURVIVAL));

        if (!helper.getBlockState(FROM).is(BlockContent.CHUTE_BLOCK.get())) {
            helper.fail("the start against a chest is " + helper.getBlockState(FROM).getBlock() + ", not a loader", FROM);
        }
        for (BlockPos support : List.of(MID, END)) {
            if (!helper.getBlockState(support).is(BlockContent.CONVEYOR_SUPPORT_BLOCK.get())) {
                helper.fail("expected a support, found " + helper.getBlockState(support).getBlock(), support);
            }
        }
        if (!helper.absolutePos(END).equals(belt(helper, FROM).getTarget())) {
            helper.fail("the belt does not end on the support", END);
        }
        if (!belt(helper, MID).supportUse().midBelt()) {
            helper.fail("the planned support does not read as mid-belt", MID);
        }
        int left = count(player, ItemContent.beltFor(BeltTier.BELT));
        if (left != HELD_BELTS - LAID_COST) {
            helper.fail("a " + LAID_COST + "-block belt through two supports charged " + (HELD_BELTS - left)
                    + " belt items; supports are free", END);
        }
        ItemStack held = player.getMainHandItem();
        if (!helper.absolutePos(END).equals(held.get(ComponentContent.BELT_START.get()))
                || held.get(ComponentContent.BELT_DIR.get()) != Direction.EAST) {
            helper.fail("the belt item does not carry on from the end support: it holds "
                    + held.get(ComponentContent.BELT_START.get()) + " facing " + held.get(ComponentContent.BELT_DIR.get()), END);
        }
        helper.succeed();
    }

    private static void breakingRefunds(GameTestHelper helper, BlockPos broken, BlockPos other) {
        lay(helper, helper.makeMockPlayer(GameType.SURVIVAL));
        ChuteBlockEntity start = belt(helper, FROM);

        // A player of its own: the shared fake player's inventory is every test's at once.
        ServerPlayer breaker = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "pf_support_breaker"));
        breaker.setGameMode(GameType.SURVIVAL);

        helper.startSequence().thenIdle(FILL_TICKS).thenExecute(() -> {
            int onBelt = start.getBeltEntries().size();
            if (onBelt == 0) {
                helper.fail("the belt carries nothing, so this proves nothing about its items", FROM);
            }
            breaker.gameMode.destroyBlock(helper.absolutePos(broken));

            int refunded = count(breaker, ItemContent.beltFor(BeltTier.BELT));
            if (refunded != LAID_COST) {
                helper.fail("breaking the support refunded " + refunded + " belt items, expected " + LAID_COST, broken);
            }
            int carried = count(breaker, Items.COBBLESTONE);
            if (carried != onBelt) {
                helper.fail("breaking the support put " + carried + " of the belt's " + onBelt
                        + " items into the inventory", broken);
            }
            if (lying(helper) != 0) {
                helper.fail(lying(helper) + " of the belt's items or refund lie on the ground", broken);
            }
            if (!helper.getBlockState(broken).isAir()) {
                helper.fail("the broken support is still standing", broken);
            }
            if (start.isUsed()) {
                helper.fail("the loader still reads as part of a belt", FROM);
            }
            if (belt(helper, other).isUsed()) {
                helper.fail("the other support still reads as part of a belt", other);
            }
        }).thenSucceed();
    }

    /**
     * A chest of cobblestone, then through the belt item: the ground in front of it, a sneak-click
     * two blocks on for a mid-belt support, and a click on open ground for the end.
     */
    private static Player lay(GameTestHelper helper, Player player) {
        helper.setBlock(SOURCE, Blocks.CHEST);
        BeltHandoffTests.chest(helper, SOURCE).setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        player.setYRot(90);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemContent.beltFor(BeltTier.BELT), HELD_BELTS));
        player.getInventory().add(new ItemStack(BlockContent.loaderFor(BeltTier.BELT).asItem(), 1));
        click(helper, player, FROM_GROUND);
        player.setShiftKeyDown(true);
        click(helper, player, MID.below());
        player.setShiftKeyDown(false);
        click(helper, player, END.below());
        return player;
    }

    private static void deadEnd(GameTestHelper helper) {
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.setBlock(FROM, loader(BeltTier.BELT, Direction.EAST));
        helper.setBlock(SUPPORT, support());
        BeltHandoffTests.link(helper, FROM, SUPPORT, List.of(), BeltTier.BELT);
    }

    private static void looseStart(GameTestHelper helper) {
        helper.setBlock(SUPPORT, support());
        helper.setBlock(TO, loader(BeltTier.BELT, Direction.WEST));
        BeltHandoffTests.link(helper, SUPPORT, TO, List.of(), BeltTier.BELT);
    }

    private static void join(GameTestHelper helper) {
        join(helper, BeltTier.BELT, BeltTier.BELT, BeltTier.BELT);
    }

    private static void join(GameTestHelper helper, BeltTier loaders, BeltTier in, BeltTier out) {
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.setBlock(FROM, loader(loaders, Direction.EAST));
        helper.setBlock(SUPPORT, support());
        helper.setBlock(TO, loader(loaders, Direction.WEST));
        BeltHandoffTests.link(helper, FROM, SUPPORT, List.of(), in);
        BeltHandoffTests.link(helper, SUPPORT, TO, List.of(), out);
    }

    private static void midBelt(GameTestHelper helper) {
        helper.setBlock(FROM, loader(BeltTier.BELT, Direction.EAST));
        helper.setBlock(SUPPORT, support());
        helper.setBlock(TO, loader(BeltTier.BELT, Direction.WEST));
        BeltHandoffTests.link(helper, FROM, TO, List.of(SUPPORT), BeltTier.BELT);
    }

    private static void fill(GameTestHelper helper, int items) {
        for (int slot = 0; items > 0; slot++, items -= 64) {
            BeltHandoffTests.chest(helper, SOURCE).setItem(slot, new ItemStack(Items.COBBLESTONE, Math.min(items, 64)));
        }
    }

    private static BlockState loader(BeltTier tier, Direction facing) {
        return BlockContent.loaderFor(tier).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    private static BlockState support() {
        return BlockContent.CONVEYOR_SUPPORT_BLOCK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST);
    }

    private static ChuteBlockEntity belt(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, ChuteBlockEntity.class);
    }

    private static int lying(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(2.0))
                .stream()
                .map(ItemEntity::getItem)
                .filter(stack -> stack.is(Items.COBBLESTONE) || stack.is(ItemContent.beltFor(BeltTier.BELT)))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private static Map<BlockPos, BlockState> world(GameTestHelper helper) {
        return BlockPos.betweenClosedStream(helper.getBounds())
                .map(BlockPos::immutable)
                .collect(Collectors.toMap(Function.identity(), pos -> helper.getLevel().getBlockState(pos)));
    }

    private static void click(GameTestHelper helper, Player player, BlockPos target) {
        BlockPos absolute = helper.absolutePos(target);
        helper.useBlock(target, player, new BlockHitResult(
                Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false));
    }

    private static int count(Player player, Item item) {
        return ContainerHelper.clearOrCountMatchingItems(player.getInventory(), stack -> stack.is(item), 0, true);
    }
}
