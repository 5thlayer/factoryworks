package com.planetaryfactory.core.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import rearth.belts.BlockContent;
import rearth.belts.ComponentContent;
import rearth.belts.ItemContent;
import rearth.belts.blocks.BeltTileBlockEntity;
import rearth.belts.blocks.BeltWedgeBlock;
import rearth.belts.blocks.SplitterBlock;
import rearth.belts.items.DismantlePlan;
import rearth.belts.items.Dismantling;
import rearth.belts.model.BeltTier;
import rearth.belts.model.TransportLine;

/**
 * A Dismantle (#404): each test sneak-clicks a start with the Engineer's Pick through the player's
 * game mode, asks {@link Dismantling#plan} for the end, sneak-clicks it, and holds the world, the
 * inventory and the stored start to the plan. An accepted plan leaves none of its tiles or wedges
 * standing and hands the player a tile for each and every item they carried; a refused plan changes
 * no block, no slot and no stored start, and names its reason on the action bar.
 */
final class DismantleTests {

    private static final BlockPos START = new BlockPos(2, 1, 3);
    private static final Identifier PICK = Identifier.fromNamespaceAndPath("planetaryfactory", "engineers_iron_pick");
    private static final String OFF_LINE = "message.belts.dismantle_off_line";

    private DismantleTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_dismantle_takes_up_a_straight_span_and_its_items", 20, helper ->
                takesUp(helper, row(helper, 6), START.east(1), START.east(4)));
        tests.test("a_dismantle_taken_end_to_start_takes_the_same_span", 20, helper ->
                takesUp(helper, row(helper, 6), START.east(4), START.east(1)));
        tests.test("a_dismantle_on_one_tile_takes_that_tile", 20, helper ->
                takesUp(helper, row(helper, 3), START.east(1), START.east(1)));
        tests.test("a_dismantle_follows_its_line_round_a_corner", 20, DismantleTests::corner);
        tests.test("a_dismantle_follows_its_line_up_a_slope_and_takes_its_wedges", 20, DismantleTests::slope);
        tests.test("a_dismantle_out_of_a_line_leaves_both_sides_carrying_and_delivering", 120, DismantleTests::middle);
        tests.test("a_dismantle_with_no_room_drops_the_rest_at_the_players_feet", 20, DismantleTests::fullInventory);
        tests.test("a_creative_dismantle_hands_over_nothing", 20, DismantleTests::creative);
        tests.test("a_dismantle_ending_on_a_splitter_changes_nothing", 20, helper -> refused(helper,
                () -> splitterAt(helper, START.east(3)), START.east(3)));
        tests.test("a_dismantle_ending_beyond_a_splitter_changes_nothing", 20, helper -> refused(helper,
                () -> {
                    splitterAt(helper, START.east(3));
                    helper.setBlock(START.east(4), BeltTileTests.tile(BeltTier.BELT, Direction.EAST));
                }, START.east(4)));
        tests.test("a_dismantle_ending_on_a_loader_changes_nothing", 20, helper -> refused(helper,
                () -> helper.setBlock(START.east(3), BeltTileTests.loader(BeltTier.BELT, Direction.WEST)), START.east(3)));
        tests.test("a_dismantle_ending_on_another_line_changes_nothing", 20, helper -> refused(helper,
                () -> {
                    for (int i = 0; i < 3; i++) helper.setBlock(START.south(2).east(i), BeltTileTests.tile(BeltTier.BELT, Direction.EAST));
                }, START.south(2).east(1)));
        tests.test("a_click_after_the_start_tile_broke_is_a_new_start", 20, DismantleTests::staleStart);
        tests.test("a_sneak_use_in_the_air_clears_the_dismantle_start", 20, DismantleTests::clears);
    }

    private static void corner(GameTestHelper helper) {
        List<BlockPos> tiles = new ArrayList<>();
        for (int i = 2; i >= 1; i--) tiles.add(START.south(i));
        for (int i = 0; i < 3; i++) tiles.add(START.east(i));
        for (BlockPos tile : tiles) {
            Direction facing = tile.getX() == START.getX() && tile.getZ() > START.getZ() ? Direction.NORTH : Direction.EAST;
            helper.setBlock(tile, BeltTileTests.tile(BeltTier.BELT, facing));
        }
        carryOneEach(helper, tiles);
        takesUp(helper, tiles, tiles.getFirst(), tiles.getLast());
    }

    // Level, then a block up over air: the top stands on a wedge.
    private static void slope(GameTestHelper helper) {
        List<BlockPos> tiles = new ArrayList<>();
        int[] heights = {0, 0, 1, 1};
        for (int i = 0; i < heights.length; i++) {
            BlockPos at = START.east(i).above(heights[i]);
            helper.setBlock(at, BeltTileTests.tile(BeltTier.BELT, Direction.EAST));
            tiles.add(at);
        }
        if (!(helper.getBlockState(START.east(2)).getBlock() instanceof BeltWedgeBlock)) {
            helper.fail("the fixture's top stands on " + helper.getBlockState(START.east(2)) + ", not a wedge", START.east(2));
            return;
        }
        carryOneEach(helper, tiles);
        helper.runAfterDelay(2, () -> {
            var player = started(helper, tiles.getFirst());
            var plan = accepted(helper, player, tiles.getLast(), tiles.size(), tiles.size());
            if (plan == null) return;
            if (!plan.wedges().equals(List.of(helper.absolutePos(START.east(2))))) {
                helper.fail("the plan names wedges " + plan.wedges() + ", not the one under the top", START.east(2));
                return;
            }
            helper.succeed();
        });
    }

    // Six tiles, each carrying one ingot; the middle four taken up, then a loader and a chest put
    // past each side.
    private static void middle(GameTestHelper helper) {
        List<BlockPos> tiles = row(helper, 6);
        helper.startSequence().thenIdle(2).thenExecute(() -> {
            var player = started(helper, START.east(1));
            accepted(helper, player, START.east(4), 4, 4);
        }).thenIdle(3).thenExecute(() -> {
            int upstream = heldOn(helper, tiles.getFirst());
            int downstream = heldOn(helper, tiles.getLast());
            if (upstream != 1 || downstream != 1) {
                helper.fail("the tiles either side of the span hold " + upstream + " and " + downstream + ", not 1 and 1", START);
            }
            helper.setBlock(START.east(1), BeltTileTests.loader(BeltTier.BELT, Direction.WEST));
            helper.setBlock(START.east(2), Blocks.CHEST);
            helper.setBlock(START.east(6), BeltTileTests.loader(BeltTier.BELT, Direction.WEST));
            helper.setBlock(START.east(7), Blocks.CHEST);
        }).thenIdle(80).thenExecute(() -> {
            int upstream = BeltTileTests.count(BeltTileTests.chest(helper, START.east(2)));
            int downstream = BeltTileTests.count(BeltTileTests.chest(helper, START.east(7)));
            if (upstream != 1 || downstream != 1) {
                helper.fail("the two sides delivered " + upstream + " and " + downstream + ", not 1 and 1", START);
            }
        }).thenSucceed();
    }

    private static void fullInventory(GameTestHelper helper) {
        List<BlockPos> tiles = row(helper, 3);
        helper.runAfterDelay(2, () -> {
            var player = started(helper, tiles.getFirst());
            for (int slot = 0; slot < player.getInventory().getNonEquipmentItems().size(); slot++) {
                if (player.getInventory().getItem(slot).isEmpty()) {
                    player.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
                }
            }
            click(helper, player, tiles.getLast());
            for (BlockPos tile : tiles) {
                if (!helper.getBlockState(tile).isAir()) {
                    helper.fail("a dismantle with no room left " + helper.getBlockState(tile), tile);
                    return;
                }
            }
            Map<Item, Integer> dropped = new HashMap<>();
            for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new AABB(player.blockPosition()).inflate(3))) {
                dropped.merge(entity.getItem().getItem(), entity.getItem().getCount(), Integer::sum);
            }
            Map<Item, Integer> expected = Map.of(ItemContent.tileFor(BeltTier.BELT), 3, Items.IRON_INGOT, 3);
            if (!expected.equals(dropped)) {
                helper.fail("a dismantle with no room dropped " + dropped + " at the player's feet, not " + expected, START);
                return;
            }
            helper.succeed();
        });
    }

    private static void creative(GameTestHelper helper) {
        List<BlockPos> tiles = row(helper, 3);
        helper.runAfterDelay(2, () -> {
            var player = started(helper, tiles.getFirst());
            player.setGameMode(GameType.CREATIVE);
            click(helper, player, tiles.getLast());
            for (BlockPos tile : tiles) {
                if (!helper.getBlockState(tile).isAir()) {
                    helper.fail("a creative dismantle left " + helper.getBlockState(tile), tile);
                    return;
                }
            }
            Map<Item, Integer> carried = inventory(player);
            carried.remove(player.getMainHandItem().getItem());
            if (!carried.isEmpty()) {
                helper.fail("a creative dismantle handed over " + carried, START);
                return;
            }
            helper.succeed();
        });
    }

    private static void refused(GameTestHelper helper, Runnable setUp, BlockPos end) {
        row(helper, 3);
        setUp.run();
        helper.runAfterDelay(2, () -> {
            var player = started(helper, START);
            var plan = planOf(helper, player, end);
            if (plan == null || plan.refusal() == null || !plan.tiles().isEmpty()) {
                helper.fail("an end off the start's line was planned as " + (plan == null ? "nothing" : plan.tiles()), end);
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
                helper.fail("a refused dismantle changed " + changed.size() + " blocks, first at " + changed.getFirst(), end);
                return;
            }
            if (!carried.equals(inventory(player)) || !Objects.equals(stored, player.getMainHandItem().getComponents())) {
                helper.fail("a refused dismantle changed the player's inventory or the stored start", end);
                return;
            }
            if (!player.heard.equals(List.of(OFF_LINE))) {
                helper.fail("the player was told " + player.heard + ", not " + OFF_LINE, end);
                return;
            }
            helper.succeed();
        });
    }

    private static void staleStart(GameTestHelper helper) {
        List<BlockPos> tiles = row(helper, 4);
        var player = started(helper, tiles.getFirst());
        helper.destroyBlock(tiles.getFirst());
        click(helper, player, tiles.get(2));
        if (!helper.absolutePos(tiles.get(2)).equals(player.getMainHandItem().get(ComponentContent.DISMANTLE_START.get()))) {
            helper.fail("a click after the start broke stored " + player.getMainHandItem().get(ComponentContent.DISMANTLE_START.get())
                    + ", not the clicked tile", tiles.get(2));
            return;
        }
        for (BlockPos tile : tiles.subList(1, tiles.size())) {
            if (!(helper.getLevel().getBlockEntity(helper.absolutePos(tile)) instanceof BeltTileBlockEntity)) {
                helper.fail("a click after the start broke took up a tile", tile);
                return;
            }
        }
        helper.succeed();
    }

    private static void clears(GameTestHelper helper) {
        List<BlockPos> tiles = row(helper, 3);
        var player = started(helper, tiles.getFirst());
        player.setShiftKeyDown(true);
        player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
        if (player.getMainHandItem().has(ComponentContent.DISMANTLE_START.get())) {
            helper.fail("a sneak-use in the air left the dismantle's start stored", tiles.getFirst());
            return;
        }
        helper.succeed();
    }

    /**
     * Asks the plan of a sneak-click at {@code end}, clicks, and holds the world to it: none of its
     * tiles or wedges standing, {@code tiles} tile items and {@code items} ingots handed over, and the
     * start cleared.
     */
    private static void takesUp(GameTestHelper helper, List<BlockPos> tiles, BlockPos start, BlockPos end) {
        int span = Math.abs(tiles.indexOf(end) - tiles.indexOf(start)) + 1;
        helper.runAfterDelay(2, () -> {
            var player = started(helper, start);
            if (accepted(helper, player, end, span, span) != null) helper.succeed();
        });
    }

    private static @Nullable DismantlePlan accepted(GameTestHelper helper, ListeningPlayer player, BlockPos end, int tiles, int items) {
        var plan = planOf(helper, player, end);
        if (plan == null || plan.refused() || plan.tiles().size() != tiles) {
            helper.fail("the dismantle to " + end + " was planned as " + (plan == null ? "nothing"
                    : plan.refused() ? plan.refusal() : plan.tiles().size() + " tiles"), end);
            return null;
        }
        click(helper, player, end);
        List<BlockPos> gone = new ArrayList<>(plan.tiles());
        gone.addAll(plan.wedges());
        for (BlockPos pos : gone) {
            if (!helper.getLevel().getBlockState(pos).isAir()) {
                helper.fail("the plan named " + pos + " and the click left " + helper.getLevel().getBlockState(pos), helper.relativePos(pos));
                return null;
            }
        }
        Map<Item, Integer> expected = Map.of(ItemContent.tileFor(BeltTier.BELT), tiles, Items.IRON_INGOT, items);
        Map<Item, Integer> carried = inventory(player);
        carried.remove(player.getMainHandItem().getItem());
        if (!expected.equals(carried)) {
            helper.fail("the dismantle handed over " + carried + ", not " + expected, end);
            return null;
        }
        if (!helper.getEntities(EntityType.ITEM).isEmpty()) {
            helper.fail("the dismantle left items on the ground", end);
            return null;
        }
        if (player.getMainHandItem().has(ComponentContent.DISMANTLE_START.get())) {
            helper.fail("a dismantle left its start stored", end);
            return null;
        }
        return plan;
    }

    /** {@code count} tier-1 tiles running east from {@link #START}, each carrying one ingot. */
    private static List<BlockPos> row(GameTestHelper helper, int count) {
        List<BlockPos> tiles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            helper.setBlock(START.east(i), BeltTileTests.tile(BeltTier.BELT, Direction.EAST));
            tiles.add(START.east(i));
        }
        carryOneEach(helper, tiles);
        return tiles;
    }

    private static void carryOneEach(GameTestHelper helper, List<BlockPos> tiles) {
        for (BlockPos tile : tiles) {
            helper.getBlockEntity(tile, BeltTileBlockEntity.class)
                    .carry(List.of(new TransportLine.Share<>(0.5, new ItemStack(Items.IRON_INGOT))));
        }
    }

    private static void splitterAt(GameTestHelper helper, BlockPos left) {
        for (SplitterBlock.Side side : SplitterBlock.Side.values()) {
            helper.setBlock(side == SplitterBlock.Side.LEFT ? left : left.relative(Direction.EAST.getClockWise()),
                    BlockContent.splitterFor(BeltTier.BELT).defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)
                            .setValue(SplitterBlock.SIDE, side));
        }
    }

    /** A survival player holding the Pick, standing on the platform, who has sneak-clicked a start at {@code start}. */
    private static ListeningPlayer started(GameTestHelper helper, BlockPos start) {
        var player = new ListeningPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(START.getX() + 0.5, START.getY(), START.getZ() - 1.5)));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.getValue(PICK)));
        click(helper, player, start);
        if (!helper.absolutePos(start).equals(player.getMainHandItem().get(ComponentContent.DISMANTLE_START.get()))) {
            helper.fail("a sneak-click on a tile stored " + player.getMainHandItem().get(ComponentContent.DISMANTLE_START.get())
                    + " as the start", start);
        }
        return player;
    }

    private static @Nullable DismantlePlan planOf(GameTestHelper helper, ListeningPlayer player, BlockPos end) {
        return Dismantling.plan(helper.getLevel(), player.getMainHandItem(), helper.absolutePos(end));
    }

    private static void click(GameTestHelper helper, ListeningPlayer player, BlockPos at) {
        player.setShiftKeyDown(true);
        BlockPos absolute = helper.absolutePos(at);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
    }

    private static int heldOn(GameTestHelper helper, BlockPos tile) {
        int held = 0;
        for (ItemStack stack : helper.getBlockEntity(tile, BeltTileBlockEntity.class).heldHere()) held += stack.getCount();
        return held;
    }

    private static Map<BlockPos, BlockState> world(GameTestHelper helper) {
        return BlockPos.betweenClosedStream(helper.getBounds())
                .map(BlockPos::immutable)
                .collect(Collectors.toMap(Function.identity(), pos -> helper.getLevel().getBlockState(pos)));
    }

    private static Map<Item, Integer> inventory(ListeningPlayer player) {
        Map<Item, Integer> counts = new HashMap<>();
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty()) counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        return counts;
    }
}
