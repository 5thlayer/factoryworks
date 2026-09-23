package com.planetaryfactory.core.gametest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.planetaryfactory.core.placement.PlacementPlan;
import com.planetaryfactory.core.placement.Placements;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import rearth.belts.BlockContent;
import rearth.belts.ComponentContent;
import rearth.belts.ItemContent;
import rearth.belts.blocks.BeltTileBlock;
import rearth.belts.blocks.BeltTileBlockEntity;
import rearth.belts.model.BeltTier;
import rearth.belts.model.TransportLine;

/**
 * A stretch of belt tiles (#393, ADR-0069): each test sneak-clicks a start, perhaps corners, asks
 * {@link Placements} for the plan of the next click, clicks, then holds the world, the inventory and
 * the stored start to the plan. An accepted plan puts every tile it names down in the state it names
 * and charges one held-tier tile for each one placed or replaced; a refused plan changes no block, no
 * slot and no stored start, and names its reason on the action bar.
 */
final class StretchTests {

    private static final BlockPos START = new BlockPos(2, 1, 3);
    private static final int TILES = 32;

    private StretchTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_stretch_straight_ahead_places_what_it_names", 20, StretchTests::straight);
        tests.test("a_stretch_off_the_look_turns_once", 20, StretchTests::lShape);
        tests.test("a_stretch_beside_its_start_runs_sideways_from_it", 20, StretchTests::sideways);
        tests.test("a_stretch_ending_on_its_start_places_one_tile_facing_the_look", 20, StretchTests::zeroLength);
        tests.test("a_stretch_turns_a_tile_of_its_tier_for_nothing", 20, StretchTests::turns);
        tests.test("a_stretch_replaces_a_tile_of_another_tier_and_hands_it_back", 30, StretchTests::replaces);
        tests.test("a_stretch_behind_the_look_changes_nothing", 20, helper -> refused(helper,
                player -> {}, START.west(2), PlacementPlan.Refusal.BEHIND_LOOK, "message.belts.stretch_behind"));
        tests.test("a_stretch_through_a_block_changes_nothing", 20, helper -> refused(helper,
                player -> helper.setBlock(START.east(3), Blocks.STONE), START.east(5),
                PlacementPlan.Refusal.FOOTPRINT_BLOCKED, "message.belts.stretch_blocked"));
        tests.test("a_stretch_over_no_ground_changes_nothing", 20, helper -> refused(helper,
                player -> helper.setBlock(START.east(3).below(), Blocks.AIR), START.east(5),
                PlacementPlan.Refusal.NO_GROUND, "message.belts.stretch_no_ground"));
        tests.test("a_stretch_short_of_tiles_changes_nothing", 20, helper -> refused(helper,
                player -> player.getMainHandItem().setCount(3), START.east(5),
                PlacementPlan.Refusal.NOT_ENOUGH_ITEMS, "message.belts.stretch_not_enough"));
        tests.test("a_stretch_with_no_room_to_hand_back_changes_nothing", 20, helper -> refused(helper,
                player -> {
                    helper.setBlock(START.east(2), tile(BeltTier.IMPROVED, Direction.EAST));
                    for (int slot = 0; slot < player.getInventory().getNonEquipmentItems().size(); slot++) {
                        if (player.getInventory().getItem(slot).isEmpty()) {
                            player.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
                        }
                    }
                }, START.east(5), PlacementPlan.Refusal.NO_ROOM_TO_RETURN, "message.belts.stretch_no_room"));
        tests.test("a_stretch_through_a_loader_changes_nothing", 20, helper -> refused(helper,
                player -> helper.setBlock(START.east(3), BlockContent.loaderFor(BeltTier.BELT).defaultBlockState()),
                START.east(5), PlacementPlan.Refusal.FOOTPRINT_BLOCKED, "message.belts.stretch_blocked"));
        tests.test("a_stretch_both_blocked_and_short_names_the_block", 20, helper -> refused(helper,
                player -> {
                    helper.setBlock(START.east(3), Blocks.STONE);
                    player.getMainHandItem().setCount(3);
                }, START.east(5), PlacementPlan.Refusal.FOOTPRINT_BLOCKED, "message.belts.stretch_blocked"));
        tests.test("a_creative_stretch_charges_nothing", 20, StretchTests::creative);
        tests.test("a_sneak_click_adds_a_corner_the_stretch_runs_on_from", 20, StretchTests::corner);
        tests.test("a_sneak_click_behind_the_look_adds_no_corner", 20, StretchTests::cornerBehind);
        tests.test("a_sneak_use_in_the_air_clears_the_start_and_its_corners", 20, StretchTests::clears);
    }

    private static void straight(GameTestHelper helper) {
        var player = started(helper, Direction.EAST);
        var plan = accepted(helper, player, START.east(4), 5);
        if (plan == null) return;
        for (int i = 0; i <= 4; i++) {
            if (facing(helper, START.east(i)) != Direction.EAST) {
                helper.fail("tile " + i + " of a stretch east faces " + facing(helper, START.east(i)), START.east(i));
            }
        }
        helper.succeed();
    }

    // Looking east, an end two ahead and two south turns right where the first leg meets it.
    private static void lShape(GameTestHelper helper) {
        var player = started(helper, Direction.EAST);
        var corner = START.east(2);
        var plan = accepted(helper, player, corner.south(2), 5);
        if (plan == null) return;
        if (facing(helper, START.east()) != Direction.EAST || facing(helper, corner) != Direction.SOUTH
                || facing(helper, corner.south(2)) != Direction.SOUTH) {
            helper.fail("an L stretch faces east then south, not " + facing(helper, START.east()) + " then "
                    + facing(helper, corner) + " then " + facing(helper, corner.south(2)), corner);
        }
        if (helper.getBlockState(corner).getValue(BeltTileBlock.CORNER) == BeltTileBlock.Shape.STRAIGHT) {
            helper.fail("the stretch's corner tile is straight", corner);
        }
        helper.succeed();
    }

    private static void sideways(GameTestHelper helper) {
        var player = started(helper, Direction.EAST);
        var plan = accepted(helper, player, START.north(2), 3);
        if (plan == null) return;
        if (facing(helper, START) != Direction.NORTH) {
            helper.fail("a stretch beside its start faces its start " + facing(helper, START) + ", not north", START);
        }
        helper.succeed();
    }

    private static void zeroLength(GameTestHelper helper) {
        var player = started(helper, Direction.SOUTH);
        var plan = accepted(helper, player, START, 1);
        if (plan == null) return;
        if (facing(helper, START) != Direction.SOUTH) {
            helper.fail("a stretch ending on its start faces " + facing(helper, START) + ", not the stored south", START);
        }
        helper.succeed();
    }

    private static void turns(GameTestHelper helper) {
        var turned = START.east(2);
        helper.setBlock(turned, tile(BeltTier.BELT, Direction.NORTH));
        helper.getBlockEntity(turned, BeltTileBlockEntity.class)
                .carry(List.of(new TransportLine.Share<>(0.5, new ItemStack(Items.IRON_INGOT))));
        var player = started(helper, Direction.EAST);
        var plan = accepted(helper, player, START.east(4), 4);
        if (plan == null) return;
        if (!plan.replaces().equals(List.of(helper.absolutePos(turned)))) {
            helper.fail("the plan names " + plan.replaces() + " as turned, not the one tile facing north", turned);
        }
        if (facing(helper, turned) != Direction.EAST) {
            helper.fail("the turned tile faces " + facing(helper, turned), turned);
        }
        keepsItsItem(helper, START, 5, turned);
    }

    private static void replaces(GameTestHelper helper) {
        var replaced = START.east(2);
        helper.setBlock(replaced, tile(BeltTier.IMPROVED, Direction.EAST));
        helper.getBlockEntity(replaced, BeltTileBlockEntity.class)
                .carry(List.of(new TransportLine.Share<>(0.5, new ItemStack(Items.IRON_INGOT))));
        var player = started(helper, Direction.EAST);
        var improved = ItemContent.tileFor(BeltTier.IMPROVED);
        var plan = accepted(helper, player, START.east(4), 5);
        if (plan == null) return;
        if (!plan.replaces().equals(List.of(helper.absolutePos(replaced)))) {
            helper.fail("the plan names " + plan.replaces() + " as replaced, not the tier-2 tile", replaced);
        }
        if (!helper.getBlockState(replaced).is(tile(BeltTier.BELT, Direction.EAST).getBlock())) {
            helper.fail("the replaced tile is " + helper.getBlockState(replaced), replaced);
        }
        if (count(player, improved) != 1) {
            helper.fail("the player was handed back " + count(player, improved) + " tier-2 tiles, not 1", replaced);
        }
        keepsItsItem(helper, START, 5, replaced);
    }

    /** One tick on, the stretch's tiles hold the one item the turned or replaced tile carried, and none is on the ground. */
    private static void keepsItsItem(GameTestHelper helper, BlockPos first, int tiles, BlockPos at) {
        helper.runAfterDelay(2, () -> {
            int held = 0;
            for (int i = 0; i < tiles; i++) {
                for (ItemStack stack : helper.getBlockEntity(first.east(i), BeltTileBlockEntity.class).heldHere()) {
                    held += stack.getCount();
                }
            }
            int dropped = helper.getEntities(EntityType.ITEM).size();
            if (held != 1 || dropped != 0) {
                helper.fail("the stretch holds " + held + " items and " + dropped + " lie on the ground, not 1 and 0", at);
            }
            helper.succeed();
        });
    }

    private static void creative(GameTestHelper helper) {
        var player = started(helper, Direction.EAST);
        player.setGameMode(GameType.CREATIVE);
        helper.setBlock(START.east(2), tile(BeltTier.IMPROVED, Direction.EAST));
        if (accepted(helper, player, START.east(4), 0) == null) return;
        if (count(player, ItemContent.tileFor(BeltTier.IMPROVED)) != 0) {
            helper.fail("a creative stretch handed back the tile it replaced", START.east(2));
        }
        helper.succeed();
    }

    // Looking east, a corner three ahead, then an end two north of it: the second leg heads east
    // from the corner and turns north at once.
    private static void corner(GameTestHelper helper) {
        var player = started(helper, Direction.EAST);
        var corner = START.east(3);
        player.setShiftKeyDown(true);
        var plan = planOf(helper, player, corner);
        if (plan == null || plan.isRefused() || plan.blocks().size() != 4) {
            helper.fail("a sneak-click's stretch to a corner was planned as " + (plan == null ? "nothing" : plan.refusal()), corner);
            return;
        }
        player.heard.clear();
        click(helper, player, corner);
        var stack = player.getMainHandItem();
        if (!List.of(helper.absolutePos(corner)).equals(stack.get(ComponentContent.STRETCH_CORNERS.get()))
                || !helper.absolutePos(START).equals(stack.get(ComponentContent.BELT_START.get()))) {
            helper.fail("a sneak-click stored corners " + stack.get(ComponentContent.STRETCH_CORNERS.get())
                    + " and start " + stack.get(ComponentContent.BELT_START.get()), corner);
            return;
        }
        if (!helper.getBlockState(corner).isAir() || count(player, ItemContent.tileFor(BeltTier.BELT)) != TILES) {
            helper.fail("a sneak-click adding a corner placed or spent a tile", corner);
            return;
        }
        if (!player.heard.equals(List.of("message.belts.stretch_corner"))) {
            helper.fail("adding a corner told the player " + player.heard, corner);
            return;
        }
        player.setShiftKeyDown(false);
        if (accepted(helper, player, corner.north(2), 6) == null) return;
        if (facing(helper, START.east(2)) != Direction.EAST || facing(helper, corner) != Direction.NORTH
                || facing(helper, corner.north(2)) != Direction.NORTH) {
            helper.fail("a stretch through a corner faces " + facing(helper, START.east(2)) + " then "
                    + facing(helper, corner) + " then " + facing(helper, corner.north(2)), corner);
        }
        if (player.getMainHandItem().has(ComponentContent.STRETCH_CORNERS.get())) {
            helper.fail("a laid stretch left its corners stored", corner);
        }
        helper.succeed();
    }

    private static void cornerBehind(GameTestHelper helper) {
        var player = started(helper, Direction.EAST);
        player.setShiftKeyDown(true);
        var stored = player.getMainHandItem().getComponents();
        player.heard.clear();
        click(helper, player, START.west(2));
        if (!Objects.equals(stored, player.getMainHandItem().getComponents())) {
            helper.fail("a sneak-click behind the look changed the stored start or corners", START.west(2));
        }
        if (!player.heard.equals(List.of("message.belts.stretch_behind"))) {
            helper.fail("a sneak-click behind the look told the player " + player.heard, START.west(2));
        }
        helper.succeed();
    }

    private static void clears(GameTestHelper helper) {
        var player = started(helper, Direction.EAST);
        var stack = player.getMainHandItem();
        player.setShiftKeyDown(true);
        click(helper, player, START.east(3));
        stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        if (stack.has(ComponentContent.BELT_START.get()) || stack.has(ComponentContent.BELT_DIR.get())
                || stack.has(ComponentContent.STRETCH_CORNERS.get())) {
            helper.fail("a sneak-use in the air left a start or corner stored", START.east(3));
        }
        if (count(player, ItemContent.tileFor(BeltTier.BELT)) != TILES) {
            helper.fail("storing a start and a corner and clearing them spent tiles", START);
        }
        helper.succeed();
    }

    /** A survival player holding tier-1 tiles who has sneak-clicked a start at {@link #START} looking {@code look}. */
    private static ListeningPlayer started(GameTestHelper helper, Direction look) {
        var player = new ListeningPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemContent.tileFor(BeltTier.BELT), TILES));
        player.setYRot(look.toYRot());
        player.setShiftKeyDown(true);
        helper.useBlock(START.below(), player, hit(helper, START.below()));
        player.setShiftKeyDown(false);
        return player;
    }

    /**
     * Asks the plan of a click ending at {@code end}, clicks, and holds the world to it: every tile
     * named placed in its state, {@code charged} tier-1 tiles spent, and the start cleared.
     */
    private static @Nullable PlacementPlan accepted(GameTestHelper helper, ListeningPlayer player, BlockPos end, int charged) {
        var plan = planOf(helper, player, end);
        if (plan == null || plan.isRefused()) {
            helper.fail("the stretch to " + end + " was planned as " + (plan == null ? "nothing" : plan.refusal()), end);
            return null;
        }
        var tile = ItemContent.tileFor(BeltTier.BELT);
        int before = count(player, tile);
        click(helper, player, end);
        for (PlacementPlan.Placed placed : plan.blocks()) {
            BlockState now = helper.getLevel().getBlockState(placed.pos());
            if (!now.equals(placed.state())) {
                helper.fail("the plan named " + placed.state() + " and the click left " + now, helper.relativePos(placed.pos()));
                return null;
            }
        }
        if (before - count(player, tile) != charged) {
            helper.fail("the stretch spent " + (before - count(player, tile)) + " tiles, not " + charged, end);
            return null;
        }
        if (player.getMainHandItem().has(ComponentContent.BELT_START.get())) {
            helper.fail("a laid stretch left its start stored", end);
            return null;
        }
        return plan;
    }

    private static void refused(GameTestHelper helper, Consumer<ListeningPlayer> setUp, BlockPos end,
                                PlacementPlan.Refusal refusal, String key) {
        var player = started(helper, Direction.EAST);
        setUp.accept(player);
        var plan = planOf(helper, player, end);
        if (plan == null || plan.refusal() != refusal) {
            helper.fail("the stretch was planned as " + (plan == null ? "nothing" : plan.refusal()) + ", not " + refusal, end);
            return;
        }
        Map<BlockPos, BlockState> before = world(helper);
        Map<Item, Integer> carried = inventory(player);
        var stored = player.getMainHandItem().getComponents();
        player.heard.clear();
        click(helper, player, end);

        Map<BlockPos, BlockState> after = world(helper);
        List<BlockPos> changed = before.keySet().stream().filter(pos -> before.get(pos) != after.get(pos)).toList();
        if (!changed.isEmpty()) {
            helper.fail("a refused stretch changed " + changed.size() + " blocks, first at " + changed.getFirst(), end);
        }
        if (!carried.equals(inventory(player)) || !Objects.equals(stored, player.getMainHandItem().getComponents())) {
            helper.fail("a refused stretch changed the player's inventory or the stored start", end);
        }
        if (!player.heard.equals(List.of(key))) {
            helper.fail("the player was told " + player.heard + ", not " + key, end);
        }
        helper.succeed();
    }

    private static @Nullable PlacementPlan planOf(GameTestHelper helper, ListeningPlayer player, BlockPos end) {
        return Placements.planFor(helper.getLevel(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(),
                hit(helper, end.below()));
    }

    private static void click(GameTestHelper helper, ListeningPlayer player, BlockPos end) {
        helper.useBlock(end.below(), player, hit(helper, end.below()));
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos ground) {
        BlockPos absolute = helper.absolutePos(ground);
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
    }

    private static Direction facing(GameTestHelper helper, BlockPos pos) {
        BlockState state = helper.getBlockState(pos);
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : null;
    }

    private static BlockState tile(BeltTier tier, Direction facing) {
        return ((BlockItem) ItemContent.tileFor(tier)).getBlock().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
    }

    private static int count(ListeningPlayer player, Item item) {
        return inventory(player).getOrDefault(item, 0);
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
}
