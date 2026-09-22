package com.planetaryfactory.core.gametest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import rearth.belts.BlockContent;
import rearth.belts.ComponentContent;
import rearth.belts.ItemContent;
import rearth.belts.items.BeltItem;
import rearth.belts.items.BeltPlan;
import rearth.belts.model.BeltPath.Bound;
import rearth.belts.model.BeltTier;
import rearth.belts.model.SupportSlots;

/**
 * The belt item's click executes the plan its preview draws (#372, ADR-0069): each test asks the
 * item for the plan of a click, clicks, then holds the world to the plan. An accepted plan puts every
 * block it names down in the state it names and charges what it names; a refused plan changes no
 * block and no slot and tells the player its reason. The world is read before the click as well as
 * after.
 */
final class BeltPlanTests {

    private static final BlockPos FROM_GROUND = new BlockPos(3, 0, 3);
    private static final BlockPos TO_GROUND = new BlockPos(7, 0, 3);
    private static final BlockPos FROM = FROM_GROUND.above();
    private static final BlockPos TO = TO_GROUND.above();
    private static final BlockPos BEHIND_FROM = FROM.west();
    private static final BlockPos BEYOND_TO = TO.east();
    private static final int BELTS = 16;

    private BeltPlanTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_used_loader_answers_the_click_and_nothing_is_planned", 20, BeltPlanTests::usedLoader);
        tests.test("a_plan_refusing_a_taken_support_changes_nothing", 20, BeltPlanTests::takenSupport);
        tests.test("a_plan_ending_where_it_starts_changes_nothing", 20, BeltPlanTests::endsWhereItStarts);
        tests.test("a_plan_ending_in_a_solid_block_changes_nothing", 20, BeltPlanTests::blocked);
        tests.test("a_plan_past_a_bound_changes_nothing", 20, BeltPlanTests::pastABound);
        tests.test("a_plan_short_of_belts_changes_nothing", 20, BeltPlanTests::shortOfBelts);
        tests.test("a_plan_short_of_loaders_changes_nothing", 20, BeltPlanTests::shortOfLoaders);
        tests.test("a_plan_repeating_a_support_changes_nothing", 20, BeltPlanTests::repeatedSupport);
        tests.test("a_loader_to_loader_plan_places_what_it_names", 20, BeltPlanTests::loaderToLoader);
        tests.test("a_plan_ending_on_a_free_support_turns_it", 20, BeltPlanTests::turnsFreeSupport);
        tests.test("a_plan_chained_from_a_support_places_what_it_names", 20, BeltPlanTests::chained);
        tests.test("a_plan_with_a_mid_belt_support_places_it", 20, BeltPlanTests::midBeltSupport);
        tests.test("a_start_plan_fixes_a_direction_only_where_the_click_does", 20, BeltPlanTests::startDirections);
    }

    private static void usedLoader(GameTestHelper helper) {
        chests(helper);
        var player = player(helper, BeltTier.BELT, 2);
        held(helper, player, FROM_GROUND);
        held(helper, player, TO_GROUND);
        if (planOf(helper, player, FROM) != null) {
            helper.fail("a used loader sets its filter on a click, yet the belt item planned one", FROM);
        }
        helper.succeed();
    }

    private static void takenSupport(GameTestHelper helper) {
        helper.setBlock(BEHIND_FROM, Blocks.CHEST);
        helper.setBlock(TO, support(Direction.EAST));
        var player = player(helper, BeltTier.BELT, 1);
        held(helper, player, FROM_GROUND);
        held(helper, player, TO);
        held(helper, player, new BlockPos(11, 0, 3));
        player.getMainHandItem().remove(ComponentContent.BELT_START.get());
        player.getMainHandItem().remove(ComponentContent.BELT_DIR.get());
        refused(helper, player, TO, SupportSlots.Refusal.JOINED.messageKey());
    }

    private static void endsWhereItStarts(GameTestHelper helper) {
        helper.setBlock(BEHIND_FROM, Blocks.CHEST);
        helper.setBlock(FROM, loader(Direction.EAST));
        var player = player(helper, BeltTier.BELT, 0);
        held(helper, player, FROM);
        refused(helper, player, FROM, "message.belts.chute_used");
    }

    private static void blocked(GameTestHelper helper) {
        chests(helper);
        helper.setBlock(TO, Blocks.STONE);
        var player = player(helper, BeltTier.BELT, 2);
        held(helper, player, FROM_GROUND);
        refused(helper, player, TO_GROUND, "message.belts.blocked");
    }

    // A curve three blocks long rising one: 35.4° at its steepest.
    private static void pastABound(GameTestHelper helper) {
        helper.setBlock(FROM, loader(Direction.EAST));
        helper.setBlock(new BlockPos(5, 2, 3), loader(Direction.WEST));
        var player = player(helper, BeltTier.BELT, 0);
        held(helper, player, FROM);
        refused(helper, player, new BlockPos(5, 2, 3), Bound.TOO_STEEP.messageKey());
    }

    private static void shortOfBelts(GameTestHelper helper) {
        chests(helper);
        var player = player(helper, BeltTier.BELT, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemContent.beltFor(BeltTier.BELT), 2));
        held(helper, player, FROM_GROUND);
        refused(helper, player, TO_GROUND, "message.belts.not_enough_belts");
    }

    private static void shortOfLoaders(GameTestHelper helper) {
        chests(helper);
        var player = player(helper, BeltTier.BELT, 1);
        held(helper, player, FROM_GROUND);
        refused(helper, player, TO_GROUND, "message.belts.not_enough_loaders");
    }

    private static void repeatedSupport(GameTestHelper helper) {
        chests(helper);
        var player = player(helper, BeltTier.BELT, 2);
        held(helper, player, FROM_GROUND);
        player.setShiftKeyDown(true);
        player.setYRot(-90);
        held(helper, player, new BlockPos(5, 0, 3));
        refused(helper, player, new BlockPos(5, 0, 3), "message.belts.midpoint_duplicate");
    }

    private static void loaderToLoader(GameTestHelper helper) {
        chests(helper);
        var player = player(helper, BeltTier.BELT, 2);
        held(helper, player, FROM_GROUND);
        var plan = held(helper, player, TO_GROUND);
        expectActions(helper, plan, BeltPlan.Action.PLACE_LOADER, BeltPlan.Action.PLACE_LOADER);
        helper.succeed();
    }

    private static void turnsFreeSupport(GameTestHelper helper) {
        helper.setBlock(BEHIND_FROM, Blocks.CHEST);
        helper.setBlock(TO, support(Direction.WEST));
        var player = player(helper, BeltTier.BELT, 1);
        held(helper, player, FROM_GROUND);
        var plan = held(helper, player, TO);
        expectActions(helper, plan, BeltPlan.Action.PLACE_LOADER, BeltPlan.Action.TURN_SUPPORT);
        if (helper.getBlockState(TO).getValue(HorizontalDirectionalBlock.FACING) != Direction.EAST) {
            helper.fail("the free support was not turned along the belt", TO);
        }
        helper.succeed();
    }

    private static void chained(GameTestHelper helper) {
        helper.setBlock(BEHIND_FROM, Blocks.CHEST);
        helper.setBlock(TO, support(Direction.EAST));
        var player = player(helper, BeltTier.BELT, 1);
        held(helper, player, FROM_GROUND);
        held(helper, player, TO);
        if (!helper.absolutePos(TO).equals(player.getMainHandItem().get(ComponentContent.BELT_START.get()))) {
            helper.fail("a belt ending on a support did not chain from it", TO);
        }
        var plan = held(helper, player, new BlockPos(11, 0, 3));
        expectActions(helper, plan, BeltPlan.Action.USE, BeltPlan.Action.PLACE_SUPPORT);
        helper.succeed();
    }

    private static void midBeltSupport(GameTestHelper helper) {
        chests(helper);
        var player = player(helper, BeltTier.BELT, 2);
        held(helper, player, FROM_GROUND);
        player.setShiftKeyDown(true);
        player.setYRot(-90);
        var planned = held(helper, player, new BlockPos(5, 0, 3));
        if (planned.supports().size() != 1) {
            helper.fail("a sneak-click planned " + planned.supports().size() + " supports, not one", FROM);
        }
        player.setShiftKeyDown(false);
        var plan = held(helper, player, TO_GROUND);
        if (!plan.supports().equals(planned.supports())) {
            helper.fail("the belt's plan lost the planned support", FROM);
        }
        helper.succeed();
    }

    private static void startDirections(GameTestHelper helper) {
        helper.setBlock(BEHIND_FROM, Blocks.CHEST);
        helper.setBlock(TO, support(Direction.WEST));
        var player = player(helper, BeltTier.BELT, 0);
        var item = (BeltItem) player.getMainHandItem().getItem();

        var loader = item.plan(helper.getLevel(), player.getMainHandItem(), player, hit(helper, FROM_GROUND)).ends().getFirst();
        if (loader.action() != BeltPlan.Action.PLACE_LOADER || loader.arrow() != Direction.EAST) {
            helper.fail("a start beside a chest plans " + loader.action() + " with arrow " + loader.arrow()
                    + ", not a loader pointing away from it", FROM);
        }
        var ground = item.plan(helper.getLevel(), player.getMainHandItem(), player, hit(helper, new BlockPos(5, 0, 6))).ends().getFirst();
        if (ground.action() != BeltPlan.Action.PLACE_SUPPORT || ground.arrow() != null) {
            helper.fail("a start on open ground plans " + ground.action() + " with arrow " + ground.arrow()
                    + ", not a support with none", new BlockPos(5, 1, 6));
        }
        var free = item.plan(helper.getLevel(), player.getMainHandItem(), player, hit(helper, TO)).ends().getFirst();
        if (free.action() != BeltPlan.Action.USE || free.arrow() != null) {
            helper.fail("a free support as a start plans arrow " + free.arrow() + ", but the belt's end decides it", TO);
        }
        helper.succeed();
    }

    /** Asks the plan, clicks, and holds an accepted plan to what it named. */
    private static BeltPlan held(GameTestHelper helper, ListeningPlayer player, BlockPos target) {
        var plan = planOf(helper, player, target);
        if (plan == null) {
            helper.fail("the belt item planned nothing for a click it should take", target);
            return null;
        }
        if (plan.refused()) {
            helper.fail("a click expected to be taken was refused: " + plan.refusal().key(), target);
        }
        Map<BlockPos, BlockState> before = world(helper);
        Map<Item, Integer> carried = inventory(player);
        click(helper, player, target);
        Map<BlockPos, BlockState> after = world(helper);

        Map<BlockPos, BlockState> named = new HashMap<>();
        Set<BlockPos> touched = new HashSet<>();
        // Only a belt places blocks; a start or a mid-belt support is stored on the item.
        if (plan.click() == BeltPlan.Click.BELT) {
            for (var end : plan.ends()) {
                touched.add(end.pos());
                if (end.state() != null) named.put(end.pos(), end.state());
            }
            for (var support : plan.supports()) {
                touched.add(support.pos());
                named.put(support.pos(), BlockContent.CONVEYOR_SUPPORT_BLOCK.get().defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, support.facing()));
            }
        }
        named.forEach((pos, state) -> {
            if (!state.equals(after.get(pos))) {
                helper.fail("the plan named " + state + " at " + pos + " but the click left " + after.get(pos), target);
            }
        });
        for (BlockPos pos : before.keySet()) {
            if (!touched.contains(pos) && before.get(pos) != after.get(pos)) {
                helper.fail("the click changed " + pos + ", which its plan did not name", target);
            }
        }

        Map<Item, Integer> charged = new HashMap<>();
        charged.merge(ItemContent.beltFor(plan.tier()), plan.beltCost(), Integer::sum);
        for (var end : plan.ends()) {
            if (plan.click() == BeltPlan.Click.BELT && end.action() == BeltPlan.Action.PLACE_LOADER) {
                charged.merge(BlockContent.loaderFor(end.loader()).asItem(), 1, Integer::sum);
            }
        }
        Map<Item, Integer> now = inventory(player);
        for (Item item : union(carried.keySet(), now.keySet())) {
            int spent = carried.getOrDefault(item, 0) - now.getOrDefault(item, 0);
            if (spent != charged.getOrDefault(item, 0)) {
                helper.fail("the click spent " + spent + " " + item + ", its plan named " + charged.getOrDefault(item, 0), target);
            }
        }
        return plan;
    }

    private static void refused(GameTestHelper helper, ListeningPlayer player, BlockPos target, String key) {
        var plan = planOf(helper, player, target);
        if (plan == null) {
            helper.fail("the belt item planned nothing for a click it should refuse", target);
            return;
        }
        if (!plan.refused() || !plan.refusal().key().equals(key)) {
            helper.fail("the plan's refusal is " + (plan.refused() ? plan.refusal().key() : "none") + ", not " + key, target);
        }
        Map<BlockPos, BlockState> before = world(helper);
        Map<Item, Integer> carried = inventory(player);
        var stored = player.getMainHandItem().getComponents();
        player.heard.clear();
        click(helper, player, target);

        Map<BlockPos, BlockState> after = world(helper);
        List<BlockPos> changed = before.keySet().stream().filter(pos -> before.get(pos) != after.get(pos)).toList();
        if (!changed.isEmpty()) {
            helper.fail("a refused plan changed " + changed.size() + " blocks, first at " + changed.getFirst(), target);
        }
        if (!carried.equals(inventory(player)) || !Objects.equals(stored, player.getMainHandItem().getComponents())) {
            helper.fail("a refused plan changed the player's inventory or the belt item", target);
        }
        if (!player.heard.equals(List.of(key))) {
            helper.fail("the player was told " + player.heard + ", not " + key, target);
        }
        helper.succeed();
    }

    private static void expectActions(GameTestHelper helper, BeltPlan plan, BeltPlan.Action start, BeltPlan.Action end) {
        var actions = plan.ends().stream().map(BeltPlan.End::action).toList();
        if (!actions.equals(List.of(start, end))) {
            helper.fail("the plan's ends are " + actions + ", not " + List.of(start, end), FROM);
        }
    }

    private static @Nullable BeltPlan planOf(GameTestHelper helper, ListeningPlayer player, BlockPos target) {
        var stack = player.getMainHandItem();
        return ((BeltItem) stack.getItem()).plan(helper.getLevel(), stack, player, hit(helper, target));
    }

    /** A survival player facing west, so a start loader faces east along the belt. */
    private static ListeningPlayer player(GameTestHelper helper, BeltTier tier, int loaders) {
        var player = new ListeningPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        player.setYRot(90);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemContent.beltFor(tier), BELTS));
        if (loaders > 0) player.getInventory().add(new ItemStack(BlockContent.loaderFor(tier).asItem(), loaders));
        return player;
    }

    private static void chests(GameTestHelper helper) {
        helper.setBlock(BEHIND_FROM, Blocks.CHEST);
        helper.setBlock(BEYOND_TO, Blocks.CHEST);
    }

    private static BlockState loader(Direction facing) {
        return BlockContent.loaderFor(BeltTier.BELT).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    private static BlockState support(Direction facing) {
        return BlockContent.CONVEYOR_SUPPORT_BLOCK.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos target) {
        BlockPos absolute = helper.absolutePos(target);
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
    }

    private static void click(GameTestHelper helper, ListeningPlayer player, BlockPos target) {
        helper.useBlock(target, player, hit(helper, target));
    }

    private static Map<BlockPos, BlockState> world(GameTestHelper helper) {
        return BlockPos.betweenClosedStream(helper.getBounds())
                .map(BlockPos::immutable)
                .collect(Collectors.toMap(Function.identity(), pos -> helper.getLevel().getBlockState(pos)));
    }

    private static Map<Item, Integer> inventory(ListeningPlayer player) {
        Map<Item, Integer> counts = new HashMap<>();
        for (ItemStack stack : player.getInventory()) {
            if (!stack.isEmpty()) counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        return counts;
    }

    private static Set<Item> union(Set<Item> a, Set<Item> b) {
        Set<Item> all = new HashSet<>(a);
        all.addAll(b);
        return all;
    }
}
