package com.planetaryfactory.core.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import rearth.belts.blocks.BeltTileBlock;
import rearth.belts.blocks.BeltTileBlockEntity;
import rearth.belts.model.BeltTier;

/**
 * A side-load merges into the line it feeds (#409): the line from behind goes first, the side's
 * items fill its gaps, nothing is lost, and a ring loaded from its side fills to eight a tile and
 * keeps moving. Tiles are placed downstream first, as in {@link BeltCornerTests}.
 */
final class BeltSideLoadTests {

    // A row east from a chest to a chest, and a column south into the side of its middle tile.
    private static final BlockPos THROUGH_SOURCE = new BlockPos(1, 1, 5);
    private static final BlockPos THROUGH_FROM = THROUGH_SOURCE.east();
    private static final BlockPos ROW_FIRST = THROUGH_FROM.east();
    private static final BlockPos FED = ROW_FIRST.east();
    private static final BlockPos ROW_TO = ROW_FIRST.east(3);
    private static final BlockPos TARGET = ROW_TO.east();
    private static final int COLUMN = 3;
    private static final BlockPos SIDE_FROM = FED.north(COLUMN + 1);
    private static final BlockPos SIDE_SOURCE = SIDE_FROM.north();

    private static final int THROUGH_ITEMS = 64;
    private static final int SIDE_ITEMS = 32;
    // Before the through chest runs dry at 15 items/s, well after the side's first item could arrive.
    private static final int THROUGH_ONLY_TICKS = 70;

    // A ring round a 3x3, loaded from the north into the middle of its north side.
    private static final BlockPos RING_CORNER = new BlockPos(2, 1, 4);
    private static final int RING_TILES = 8;
    private static final BlockPos RING_FED = RING_CORNER.east();
    private static final BlockPos RING_SIDE_FROM = RING_FED.north(3);
    private static final BlockPos RING_SIDE_SOURCE = RING_SIDE_FROM.north();
    private static final int RING_SUPPLY = 100;
    private static final int RING_FILL_TICKS = 500;
    private static final double TIER_1_BLOCKS_PER_TICK = 0.09375;
    private static final int MOVE_TICKS = 5;

    private BeltSideLoadTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_t_junction_delivers_both_inputs_through_line_first", 400, BeltSideLoadTests::tJunction);
        tests.test("a_side_loaded_ring_holds_8_a_tile_and_moves", RING_FILL_TICKS + MOVE_TICKS + 20, BeltSideLoadTests::sideLoadedRing);
    }

    private static void tJunction(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.CHEST);
        helper.setBlock(ROW_TO, BeltTileTests.loader(BeltTier.BELT, Direction.WEST));
        for (int tile = 2; tile >= 0; tile--) helper.setBlock(ROW_FIRST.east(tile), BeltTileTests.tile(BeltTier.BELT, Direction.EAST));
        helper.setBlock(THROUGH_FROM, BeltTileTests.loader(BeltTier.BELT, Direction.EAST));
        helper.setBlock(THROUGH_SOURCE, Blocks.CHEST);
        for (int tile = 1; tile <= COLUMN; tile++) helper.setBlock(FED.north(tile), BeltTileTests.tile(BeltTier.BELT, Direction.SOUTH));
        helper.setBlock(SIDE_FROM, BeltTileTests.loader(BeltTier.BELT, Direction.SOUTH));
        helper.setBlock(SIDE_SOURCE, Blocks.CHEST);
        fill(helper, THROUGH_SOURCE, Items.COBBLESTONE, THROUGH_ITEMS);
        fill(helper, SIDE_SOURCE, Items.IRON_INGOT, SIDE_ITEMS);
        helper.startSequence().thenIdle(THROUGH_ONLY_TICKS).thenExecute(() -> {
            int side = count(helper, TARGET, Items.IRON_INGOT);
            if (side != 0 || count(helper, THROUGH_SOURCE, Items.COBBLESTONE) == 0) {
                helper.fail("the side got " + side + " items in while the line from behind was still full", TARGET);
            }
        }).thenIdle(300).thenExecute(() -> {
            int through = count(helper, TARGET, Items.COBBLESTONE);
            int side = count(helper, TARGET, Items.IRON_INGOT);
            var dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(2.0)).size();
            if (through != THROUGH_ITEMS || side != SIDE_ITEMS || dropped != 0) {
                helper.fail("a T-junction delivered " + through + " of " + THROUGH_ITEMS + " through and " + side
                        + " of " + SIDE_ITEMS + " from the side, with " + dropped + " on the ground", TARGET);
            }
        }).thenSucceed();
    }

    private static void sideLoadedRing(GameTestHelper helper) {
        // Clockwise seen from above: east along the north side, south, west, north.
        var travel = new Direction[] {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};
        var at = RING_CORNER;
        var ring = new BlockPos[RING_TILES];
        for (int tile = 0; tile < RING_TILES; tile++) {
            ring[tile] = at;
            at = at.relative(travel[tile / 2]);
        }
        for (int tile = RING_TILES - 1; tile >= 1; tile--) {
            helper.setBlock(ring[tile], BeltTileTests.tile(BeltTier.BELT, travel[tile / 2]));
        }
        // A ring has no downstream to place first: what feeds its last tile is already down, so it
        // is placed in the shape that feeder gives it.
        helper.setBlock(ring[0], BeltTileTests.tile(BeltTier.BELT, travel[0]).setValue(BeltTileBlock.CORNER, BeltTileBlock.Shape.FROM_RIGHT));
        for (int tile = 1; tile <= 2; tile++) helper.setBlock(RING_FED.north(tile), BeltTileTests.tile(BeltTier.BELT, Direction.SOUTH));
        helper.setBlock(RING_SIDE_FROM, BeltTileTests.loader(BeltTier.BELT, Direction.SOUTH));
        helper.setBlock(RING_SIDE_SOURCE, Blocks.CHEST);
        fill(helper, RING_SIDE_SOURCE, Items.COBBLESTONE, RING_SUPPLY);
        double[] before = new double[1];
        int[] id = new int[1];
        helper.startSequence().thenIdle(RING_FILL_TICKS).thenExecute(() -> {
            var line = helper.getBlockEntity(RING_FED, BeltTileBlockEntity.class).line();
            if (line == null || !line.ring() || line.tileCount() != RING_TILES) {
                helper.fail("the eight tiles are not one ring", RING_FED);
            }
            if (line.size() != RING_TILES * 8) {
                helper.fail("a side-loaded ring of " + RING_TILES + " holds " + line.size() + ", expected " + RING_TILES * 8, RING_FED);
            }
            var first = line.contents().entries().getFirst();
            id[0] = first.id();
            before[0] = first.position();
        }).thenIdle(MOVE_TICKS).thenExecute(() -> {
            var line = helper.getBlockEntity(RING_FED, BeltTileBlockEntity.class).line();
            var entry = line.contents().entries().stream().filter(held -> held.id() == id[0]).findFirst().orElseThrow();
            double expected = (before[0] + MOVE_TICKS * TIER_1_BLOCKS_PER_TICK) % RING_TILES;
            if (Math.abs(entry.position() - expected) > 1e-6) {
                helper.fail("a full ring's item moved from " + before[0] + " to " + entry.position() + ", expected " + expected, RING_FED);
            }
        }).thenSucceed();
    }

    private static void fill(GameTestHelper helper, BlockPos chest, Item item, int items) {
        for (int slot = 0; items > 0; slot++, items -= 64) {
            BeltHandoffTests.chest(helper, chest).setItem(slot, new ItemStack(item, Math.min(items, 64)));
        }
    }

    private static int count(GameTestHelper helper, BlockPos chest, Item item) {
        var container = BeltHandoffTests.chest(helper, chest);
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            var stack = container.getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }
}
