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
import net.minecraft.world.entity.item.ItemEntity;
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
import rearth.belts.blocks.BeltWedgeBlock;
import rearth.belts.blocks.BeltTileBlockEntity;
import rearth.belts.model.BeltTier;
import rearth.belts.model.TransportLine;

/**
 * A stretch of belt tiles over the ground and over the lines across it (#393, #421, #422, ADR-0069): each test sneak-clicks a start, perhaps corners, asks
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
        tests.test("a_stretch_through_a_block_changes_nothing", 20, helper -> refused(helper, START,
                player -> helper.setBlock(START.east(3), Blocks.OAK_FENCE), START.east(5), START.east(3),
                PlacementPlan.Refusal.FOOTPRINT_BLOCKED, "message.belts.stretch_blocked"));
        tests.test("a_stretch_follows_a_step_up_and_a_step_down", 20, StretchTests::stepUpAndDown);
        tests.test("a_stretch_climbs_a_staircase", 20, StretchTests::staircase);
        tests.test("a_stretch_under_an_overhang_stays_level", 20, StretchTests::overhang);
        tests.test("a_stretch_climbs_a_two_block_step_through_the_air", 20, StretchTests::twoBlockStep);
        tests.test("a_stretch_descends_a_two_block_drop_through_the_air", 20, StretchTests::twoBlockDrop);
        tests.test("a_stretch_crosses_a_one_block_bump_by_a_two_tile_top", 20, StretchTests::bump);
        tests.test("a_stretch_into_a_wall_with_no_run_up_changes_nothing", 20, helper -> refused(helper, START,
                player -> {
                    for (int i = 1; i <= 3; i++) {
                        helper.setBlock(START.east(i), Blocks.STONE);
                        helper.setBlock(START.east(i).above(), Blocks.STONE);
                    }
                }, START.east(3).above(2), START.east(1).above(2), PlacementPlan.Refusal.UNEVEN_GROUND, "message.belts.stretch_uneven"));
        tests.test("a_stretch_ending_part_way_down_a_drop_changes_nothing", 20, helper -> {
            pillars(helper, 2);
            refused(helper, START.above(2), player -> {}, START.east(3), START.east(3).above(),
                    PlacementPlan.Refusal.UNEVEN_GROUND, "message.belts.stretch_uneven");
        });
        tests.test("a_corner_on_a_step_is_not_stored", 20, helper -> refused(helper,
                player -> {
                    helper.setBlock(START.east(3), Blocks.STONE);
                    player.setShiftKeyDown(true);
                }, START.east(3).above(), PlacementPlan.Refusal.SLOPE_TURNS, "message.belts.slope_turns"));
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
                    helper.setBlock(START.east(3), Blocks.OAK_FENCE);
                    player.getMainHandItem().setCount(3);
                }, START.east(5), PlacementPlan.Refusal.FOOTPRINT_BLOCKED, "message.belts.stretch_blocked"));
        tests.test("a_stretch_climbs_over_a_loaded_line_and_both_deliver_every_item",
                LOAD_TICKS + DELIVERY_TICKS + 20, StretchTests::crossesALoadedLine);
        tests.test("a_stretch_crossing_a_line_beside_its_start_changes_nothing", 20, helper -> refused(helper, START,
                player -> lineSouthAcross(helper, START.east(1)), START.east(5), START.east(1).above(),
                PlacementPlan.Refusal.NO_ROOM_TO_CROSS, "message.belts.stretch_no_room_to_cross"));
        tests.test("a_stretch_aimed_at_a_line_feeds_its_side", 20, StretchTests::joins);
        tests.test("a_creative_stretch_charges_nothing", 20, StretchTests::creative);
        tests.test("a_sneak_click_adds_a_corner_the_stretch_runs_on_from", 20, StretchTests::corner);
        tests.test("a_sneak_click_behind_the_look_adds_no_corner", 20, StretchTests::cornerBehind);
        tests.test("a_sneak_click_behind_the_look_keeps_the_stretch_to_its_last_corner_in_the_plan", 20, StretchTests::behindKeepsCorners);
        tests.test("a_sneak_with_no_start_plans_the_start_tile", 20, StretchTests::startPreview);
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

    // A block two wide, two ahead: a foot, two tops and a foot, each on the ground.
    private static void stepUpAndDown(GameTestHelper helper) {
        helper.setBlock(START.east(2), Blocks.STONE);
        helper.setBlock(START.east(3), Blocks.STONE);
        var player = started(helper, Direction.EAST);
        if (accepted(helper, player, START.east(5), 6) == null) return;
        pitched(helper, START, BeltTileBlock.PitchState.LEVEL);
        pitched(helper, START.east(1), BeltTileBlock.PitchState.FOOT_UP);
        pitched(helper, START.east(2).above(), BeltTileBlock.PitchState.TOP_UP);
        pitched(helper, START.east(3).above(), BeltTileBlock.PitchState.TOP_DOWN);
        pitched(helper, START.east(4), BeltTileBlock.PitchState.FOOT_DOWN);
        pitched(helper, START.east(5), BeltTileBlock.PitchState.LEVEL);
        helper.succeed();
    }

    // Three steps of one block up to a ledge, the end clicked on the ledge.
    private static void staircase(GameTestHelper helper) {
        for (int i = 2; i <= 6; i++) {
            for (int y = 0; y < Math.min(i - 1, 3); y++) helper.setBlock(START.east(i).above(y), Blocks.STONE);
        }
        var player = started(helper, Direction.EAST);
        if (accepted(helper, player, START.east(6).above(3), 7) == null) return;
        pitched(helper, START.east(1), BeltTileBlock.PitchState.FOOT_UP);
        pitched(helper, START.east(2).above(1), BeltTileBlock.PitchState.MIDDLE_UP);
        pitched(helper, START.east(3).above(2), BeltTileBlock.PitchState.MIDDLE_UP);
        pitched(helper, START.east(4).above(3), BeltTileBlock.PitchState.TOP_UP);
        pitched(helper, START.east(6).above(3), BeltTileBlock.PitchState.LEVEL);
        helper.succeed();
    }

    // A block hanging a block over the floor, over the path.
    private static void overhang(GameTestHelper helper) {
        helper.setBlock(START.east(2).above(), Blocks.STONE);
        var player = started(helper, Direction.EAST);
        if (accepted(helper, player, START.east(4), 5) == null) return;
        for (int i = 0; i <= 4; i++) pitched(helper, START.east(i), BeltTileBlock.PitchState.LEVEL);
        helper.succeed();
    }

    // A stone two high two ahead: a middle over the air, a two-tile top, a middle down over the air.
    private static void twoBlockStep(GameTestHelper helper) {
        helper.setBlock(START.east(3), Blocks.STONE);
        helper.setBlock(START.east(3).above(), Blocks.STONE);
        var player = started(helper, Direction.EAST);
        if (accepted(helper, player, START.east(5), 6) == null) return;
        pitched(helper, START, BeltTileBlock.PitchState.FOOT_UP);
        pitched(helper, START.east(1).above(), BeltTileBlock.PitchState.MIDDLE_UP);
        pitched(helper, START.east(2).above(2), BeltTileBlock.PitchState.TOP_UP);
        pitched(helper, START.east(3).above(2), BeltTileBlock.PitchState.TOP_DOWN);
        pitched(helper, START.east(4).above(), BeltTileBlock.PitchState.MIDDLE_DOWN);
        pitched(helper, START.east(5), BeltTileBlock.PitchState.FOOT_DOWN);
        wedged(helper, START.east(1), START.east(2).above(), START.east(4));
        helper.succeed();
    }

    // Off a ledge two high: a top on the ledge, a middle over the air, a foot on the ground.
    private static void twoBlockDrop(GameTestHelper helper) {
        pillars(helper, 2);
        var player = started(helper, Direction.EAST, START.above(2));
        if (accepted(helper, player, START.east(5), 6) == null) return;
        pitched(helper, START.east(2).above(2), BeltTileBlock.PitchState.TOP_DOWN);
        pitched(helper, START.east(3).above(), BeltTileBlock.PitchState.MIDDLE_DOWN);
        pitched(helper, START.east(4), BeltTileBlock.PitchState.FOOT_DOWN);
        wedged(helper, START.east(3));
        helper.succeed();
    }

    private static void bump(GameTestHelper helper) {
        helper.setBlock(START.east(3), Blocks.STONE);
        var player = started(helper, Direction.EAST);
        if (accepted(helper, player, START.east(5), 6) == null) return;
        pitched(helper, START.east(1), BeltTileBlock.PitchState.FOOT_UP);
        pitched(helper, START.east(2).above(), BeltTileBlock.PitchState.TOP_UP);
        pitched(helper, START.east(3).above(), BeltTileBlock.PitchState.TOP_DOWN);
        pitched(helper, START.east(4), BeltTileBlock.PitchState.FOOT_DOWN);
        wedged(helper, START.east(2));
        helper.succeed();
    }

    // Stone two high under the start and the two columns after it.
    private static void pillars(GameTestHelper helper, int last) {
        for (int i = 0; i <= last; i++) {
            helper.setBlock(START.east(i), Blocks.STONE);
            helper.setBlock(START.east(i).above(), Blocks.STONE);
        }
    }

    private static void wedged(GameTestHelper helper, BlockPos... wedges) {
        for (BlockPos pos : wedges) {
            if (!(helper.getBlockState(pos).getBlock() instanceof BeltWedgeBlock)) {
                helper.fail("no wedge under the slope over the air, but " + helper.getBlockState(pos), pos);
            }
        }
    }

    private static void pitched(GameTestHelper helper, BlockPos pos, BeltTileBlock.PitchState pitch) {
        BlockState state = helper.getBlockState(pos);
        if (!(state.getBlock() instanceof BeltTileBlock) || state.getValue(BeltTileBlock.PITCH) != pitch) {
            helper.fail("the stretch left " + state + " here, not a " + pitch + " tile", pos);
        }
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
        helper.setBlock(turned, tile(BeltTier.BELT, Direction.WEST));
        helper.getBlockEntity(turned, BeltTileBlockEntity.class)
                .carry(List.of(new TransportLine.Share<>(0.5, new ItemStack(Items.IRON_INGOT))));
        var player = started(helper, Direction.EAST);
        var plan = accepted(helper, player, START.east(4), 4);
        if (plan == null) return;
        if (!plan.replaces().equals(List.of(helper.absolutePos(turned)))) {
            helper.fail("the plan names " + plan.replaces() + " as turned, not the one tile facing back", turned);
        }
        if (facing(helper, turned) != Direction.EAST) {
            helper.fail("the turned tile faces " + facing(helper, turned), turned);
        }
        keepsItsItem(helper, START, 5, turned);
    }

    private static void lineSouthAcross(GameTestHelper helper, BlockPos centre) {
        for (int dz = 1; dz >= -1; dz--) helper.setBlock(centre.south(dz), tile(BeltTier.BELT, Direction.SOUTH));
    }

    // A line running south at x = 8 between its own loaders, crossed at z = 3 by a stretch east from x = 5 to 11.
    private static final int CROSSED_X = 8;
    private static final BlockPos CROSSED_SOURCE = new BlockPos(CROSSED_X, 1, 0);
    private static final BlockPos CROSSED_TARGET = new BlockPos(CROSSED_X, 1, 6);
    private static final BlockPos CROSSED_TILE = new BlockPos(CROSSED_X, 1, 3);
    private static final BlockPos CROSSING_SOURCE = new BlockPos(3, 1, 3);
    private static final BlockPos CROSSING_FIRST = CROSSING_SOURCE.east(2);
    private static final int[] CROSSING_RISE = {0, 0, 1, 1, 1, 0, 0};
    private static final List<BeltTileBlock.PitchState> CROSSING_PITCHES = List.of(
            BeltTileBlock.PitchState.LEVEL, BeltTileBlock.PitchState.FOOT_UP, BeltTileBlock.PitchState.TOP_UP,
            BeltTileBlock.PitchState.LEVEL, BeltTileBlock.PitchState.TOP_DOWN, BeltTileBlock.PitchState.FOOT_DOWN,
            BeltTileBlock.PitchState.LEVEL);
    private static final BlockPos CROSSING_TARGET = CROSSING_FIRST.east(CROSSING_RISE.length + 1);
    private static final int ITEMS = 64;
    private static final int LOAD_TICKS = 60;
    private static final int DELIVERY_TICKS = 400;

    private static void crossesALoadedLine(GameTestHelper helper) {
        helper.setBlock(CROSSED_TARGET, Blocks.CHEST);
        helper.setBlock(CROSSED_TARGET.north(), BeltTileTests.loader(BeltTier.BELT, Direction.NORTH));
        for (int z = 4; z >= 2; z--) helper.setBlock(new BlockPos(CROSSED_X, 1, z), tile(BeltTier.BELT, Direction.SOUTH));
        helper.setBlock(CROSSED_SOURCE.south(), BeltTileTests.loader(BeltTier.BELT, Direction.SOUTH));
        helper.setBlock(CROSSED_SOURCE, Blocks.CHEST);
        helper.setBlock(CROSSING_SOURCE, Blocks.CHEST);
        helper.setBlock(CROSSING_SOURCE.east(), BeltTileTests.loader(BeltTier.BELT, Direction.EAST));
        helper.setBlock(CROSSING_TARGET.west(), BeltTileTests.loader(BeltTier.BELT, Direction.WEST));
        helper.setBlock(CROSSING_TARGET, Blocks.CHEST);
        BeltTileTests.chest(helper, CROSSED_SOURCE).setItem(0, new ItemStack(Items.IRON_INGOT, ITEMS));

        helper.startSequence().thenIdle(LOAD_TICKS).thenExecute(() -> {
            Map<BlockPos, BlockState> crossed = new HashMap<>();
            for (int z = 2; z <= 4; z++) crossed.put(new BlockPos(CROSSED_X, 1, z), helper.getBlockState(new BlockPos(CROSSED_X, 1, z)));
            var line = helper.getBlockEntity(CROSSED_TILE, BeltTileBlockEntity.class).line();
            int carried = line == null ? 0 : line.size();
            if (carried == 0) helper.fail("the crossed line carries nothing before the stretch is laid", CROSSED_TILE);

            var player = started(helper, Direction.EAST, CROSSING_FIRST);
            if (accepted(helper, player, CROSSING_FIRST.east(CROSSING_RISE.length - 1), CROSSING_RISE.length) == null) return;
            crossed.forEach((pos, state) -> {
                if (!helper.getBlockState(pos).equals(state)) {
                    helper.fail("the crossed line's tile became " + helper.getBlockState(pos), pos);
                }
            });
            var after = helper.getBlockEntity(CROSSED_TILE, BeltTileBlockEntity.class).line();
            if (after == null || after.size() != carried) {
                helper.fail("the crossed line carried " + carried + " items and holds " + (after == null ? 0 : after.size()), CROSSED_TILE);
            }
            for (int i = 0; i < CROSSING_RISE.length; i++) {
                pitched(helper, CROSSING_FIRST.east(i).above(CROSSING_RISE[i]), CROSSING_PITCHES.get(i));
            }
            wedged(helper, CROSSING_FIRST.east(2), CROSSING_FIRST.east(4));
            BeltTileTests.chest(helper, CROSSING_SOURCE).setItem(0, new ItemStack(Items.COBBLESTONE, ITEMS));
        }).thenIdle(DELIVERY_TICKS).thenExecute(() -> {
            int over = count(helper, CROSSING_TARGET, Items.COBBLESTONE);
            int under = count(helper, CROSSED_TARGET, Items.IRON_INGOT);
            int mixed = count(helper, CROSSING_TARGET, Items.IRON_INGOT) + count(helper, CROSSED_TARGET, Items.COBBLESTONE);
            int onGround = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(2.0)).size();
            if (over != ITEMS || under != ITEMS || mixed != 0 || onGround != 0) {
                helper.fail("a stretch's crossing delivered " + over + " of " + ITEMS + " over and " + under + " of " + ITEMS
                        + " under, " + mixed + " into the other line's chest and " + onGround + " on the ground", CROSSED_TILE);
            }
        }).thenSucceed();
    }

    private static void joins(GameTestHelper helper) {
        BlockPos aimed = START.east(3);
        lineSouthAcross(helper, aimed);
        Map<BlockPos, BlockState> before = new HashMap<>();
        for (int dz = -1; dz <= 1; dz++) before.put(aimed.south(dz), helper.getBlockState(aimed.south(dz)));
        var player = started(helper, Direction.EAST);
        if (accepted(helper, player, aimed, 3) == null) return;
        before.forEach((pos, state) -> {
            if (!helper.getBlockState(pos).equals(state)) helper.fail("the aimed line's tile became " + helper.getBlockState(pos), pos);
        });
        for (int i = 0; i < 3; i++) {
            if (facing(helper, START.east(i)) != Direction.EAST) {
                helper.fail("tile " + i + " of a stretch aimed at a line faces " + facing(helper, START.east(i)), START.east(i));
            }
            pitched(helper, START.east(i), BeltTileBlock.PitchState.LEVEL);
        }
        helper.succeed();
    }

    private static int count(GameTestHelper helper, BlockPos chest, Item item) {
        var container = BeltTileTests.chest(helper, chest);
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).is(item)) total += container.getItem(slot).getCount();
        }
        return total;
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

    // A corner three ahead, then an end behind it: the plan still names the four tiles to the corner.
    private static void behindKeepsCorners(GameTestHelper helper) {
        var player = started(helper, Direction.EAST);
        player.setShiftKeyDown(true);
        click(helper, player, START.east(3));
        var plan = planOf(helper, player, START.east(1));
        if (plan == null || plan.refusal() != PlacementPlan.Refusal.BEHIND_LOOK || plan.blocks().size() != 4) {
            helper.fail("an end behind a corner was planned as " + (plan == null ? "nothing"
                    : plan.refusal() + " over " + plan.blocks().size() + " tiles"), START.east(1));
            return;
        }
        helper.succeed();
    }

    private static void startPreview(GameTestHelper helper) {
        var player = new ListeningPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemContent.tileFor(BeltTier.BELT), TILES));
        player.setYRot(Direction.SOUTH.toYRot());
        player.setShiftKeyDown(true);
        var plan = planOf(helper, player, START);
        if (plan == null || plan.isRefused() || plan.blocks().size() != 1
                || !plan.blocks().getFirst().pos().equals(helper.absolutePos(START))
                || plan.blocks().getFirst().state().getValue(BlockStateProperties.HORIZONTAL_FACING) != Direction.SOUTH) {
            helper.fail("a sneak with no start was planned as " + (plan == null ? "nothing" : plan.blocks()), START);
            return;
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

    private static ListeningPlayer started(GameTestHelper helper, Direction look) {
        return started(helper, look, START);
    }

    /** A survival player holding tier-1 tiles who has sneak-clicked a start at {@code start} looking {@code look}. */
    private static ListeningPlayer started(GameTestHelper helper, Direction look, BlockPos start) {
        var player = new ListeningPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemContent.tileFor(BeltTier.BELT), TILES));
        player.setYRot(look.toYRot());
        player.setShiftKeyDown(true);
        helper.useBlock(start.below(), player, hit(helper, start.below()));
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
        refused(helper, START, setUp, end, null, refusal, key);
    }

    /** As above, and a refusal at {@code named} draws the tile there, so the player sees what cannot be laid. */
    private static void refused(GameTestHelper helper, BlockPos start, Consumer<ListeningPlayer> setUp, BlockPos end,
                                @Nullable BlockPos named, PlacementPlan.Refusal refusal, String key) {
        var player = started(helper, Direction.EAST, start);
        setUp.accept(player);
        var plan = planOf(helper, player, end);
        if (plan == null || plan.refusal() != refusal) {
            helper.fail("the stretch was planned as " + (plan == null ? "nothing" : plan.refusal()) + ", not " + refusal, end);
            return;
        }
        if (named != null && plan.blocks().stream().noneMatch(placed -> placed.pos().equals(helper.absolutePos(named))
                && placed.state().getBlock() instanceof BeltTileBlock)) {
            helper.fail("the refused stretch's plan draws no tile here, where it cannot lay one", named);
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
