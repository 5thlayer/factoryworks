package com.planetaryfactory.core.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import io.github._5thlayer.beltworks.blocks.BeltTileBlock;
import io.github._5thlayer.beltworks.blocks.BeltTileBlockEntity;
import io.github._5thlayer.beltworks.model.BeltTier;
import io.github._5thlayer.beltworks.model.TransportLine;

/**
 * The belt hand and riding on tiles (#396). A hand held on a tile takes whatever is on it at the
 * line's rate, fed once a tick as the client resends it; an item entity standing on a tile is
 * carried along it, round a corner and up and down a step (#417). Rates are typed.
 */
final class BeltTileHandTests {

    // A chest behind an east-facing loader, eight tiles east, a west-facing loader, a chest.
    private static final BlockPos SOURCE = new BlockPos(1, 1, 3);
    private static final BlockPos FROM = new BlockPos(2, 1, 3);
    private static final BlockPos FIRST_TILE = new BlockPos(3, 1, 3);
    private static final int TILES = 8;
    private static final BlockPos TO = FIRST_TILE.east(TILES);
    private static final BlockPos TARGET = TO.east();
    private static final BlockPos HELD = FIRST_TILE.east(4);
    private static final BlockPos LAST = FIRST_TILE.east(TILES - 1);
    private static final BlockPos STANDING = new BlockPos(6, 1, 5);

    private static final int SUPPLY = 27 * 64;
    private static final int WARMUP_TICKS = 120;
    // A multiple of four ticks: a tier-1 line moves a whole number of items only every four.
    private static final int HOLD_TICKS = 100;
    private static final int TIER_1_ITEMS_PER_SECOND = 15;
    private static final int HELD_ITEMS = TIER_1_ITEMS_PER_SECOND * HOLD_TICKS / 20;
    private static final int ROOM = 4;
    private static final int BACKUP_TICKS = 400;

    // A tier-1 tile carries 1.875 blocks/s; a rider is let off well short of that.
    private static final BlockPos RIDE_FIRST = new BlockPos(1, 1, 1);
    private static final int RIDE_TILES = 6;
    private static final int RIDE_TICKS = 40;
    private static final double RIDE_AT_LEAST = 1.0;

    // A row east, a corner, then a column south.
    private static final BlockPos CORNER = new BlockPos(4, 1, 1);
    private static final int CORNER_TICKS = 100;

    // Four tiles on the floor, then four a block up: the fourth is a foot and the fifth a top (#417).
    private static final int CLIMB_STEP = 4;
    private static final BlockPos HELD_FOOT = FIRST_TILE.east(CLIMB_STEP - 1);

    // Two level tiles, a foot, then a top and two level tiles a block up; and the same down.
    private static final BlockPos STEP_RIDE_FIRST = new BlockPos(1, 1, 1);
    private static final int STEP_RIDE_TILES = 6;
    private static final int STEP_RIDE_AT = 3;
    // Past the step, and short of the line's end, at a tier-1 tile's 1.875 blocks/s.
    private static final int STEP_RIDE_TICKS = 45;

    private BeltTileHandTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_held_tile_fills_the_inventory_at_its_lines_rate", WARMUP_TICKS + HOLD_TICKS + 20,
                BeltTileHandTests::fillsTheInventory);
        tests.test("a_held_tile_stops_taking_when_the_inventory_is_full", WARMUP_TICKS + HOLD_TICKS + 20,
                BeltTileHandTests::stopsWhenFull);
        tests.test("a_backed_up_lines_last_tile_held_gives_up_its_items", BACKUP_TICKS + 20 + 20,
                BeltTileHandTests::lastTileOfABackedUpLine);
        tests.test("an_item_on_a_tile_rides_it", RIDE_TICKS + 20, BeltTileHandTests::itemRides);
        tests.test("an_item_on_a_tile_rides_round_a_corner", CORNER_TICKS + 20, BeltTileHandTests::itemRidesRoundACorner);
        tests.test("a_held_slope_takes_at_its_lines_rate", WARMUP_TICKS + HOLD_TICKS + 20,
                BeltTileHandTests::slopeFillsTheInventory);
        tests.test("an_item_rides_up_a_step", STEP_RIDE_TICKS + 20, helper -> itemRidesAStep(helper, true));
        tests.test("an_item_rides_down_a_step", STEP_RIDE_TICKS + 20, helper -> itemRidesAStep(helper, false));
    }

    // A hand on a foot of a line that climbs to its unloader (#417).
    private static void slopeFillsTheInventory(GameTestHelper helper) {
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.setBlock(FROM, BeltTileTests.loader(BeltTier.BELT, Direction.EAST));
        BeltTileTests.climb(helper, BeltTier.BELT, FIRST_TILE, TILES, CLIMB_STEP);
        for (BlockPos at : List.of(TO, TARGET)) helper.setBlock(at, Blocks.STONE);
        helper.setBlock(TO.above(), BeltTileTests.loader(BeltTier.BELT, Direction.WEST));
        helper.setBlock(TARGET.above(), Blocks.CHEST);
        BeltTileTests.fill(helper, SOURCE, SUPPLY);
        ServerPlayer player = player(helper, "pf_slope_hand");
        BeltTileBlockEntity held = helper.getBlockEntity(HELD_FOOT, BeltTileBlockEntity.class);

        helper.startSequence()
                .thenIdle(WARMUP_TICKS)
                .thenExecute(() -> {
                    if (BeltTileTests.pitch(helper, HELD_FOOT) != BeltTileBlock.PitchState.FOOT_UP) {
                        helper.fail("the held tile is " + BeltTileTests.pitch(helper, HELD_FOOT) + ", not a foot", HELD_FOOT);
                    }
                    if (BeltTileTests.count(BeltTileTests.chest(helper, TARGET.above())) == 0) {
                        helper.fail("nothing reached the top of the climb, so this proves little", TARGET.above());
                    }
                })
                .thenExecuteFor(HOLD_TICKS, () -> held.holdHand(player))
                .thenExecute(() -> {
                    int taken = cobblestone(player);
                    if (Math.abs(taken - HELD_ITEMS) > 1) {
                        helper.fail("holding a tier-1 foot for " + HOLD_TICKS + " ticks took " + taken
                                + " items, expected " + HELD_ITEMS, HELD_FOOT);
                    }
                    nothingOnTheGround(helper);
                })
                .thenSucceed();
    }

    // Read by height as well as distance: a rider stuck at the foot of a climb has moved east too.
    private static void itemRidesAStep(GameTestHelper helper, boolean up) {
        List<BlockPos> tiles = new ArrayList<>();
        for (int tile = 0; tile < STEP_RIDE_TILES; tile++) {
            BlockPos at = STEP_RIDE_FIRST.east(tile);
            if (up == tile >= STEP_RIDE_AT) {
                helper.setBlock(at, Blocks.STONE);
                at = at.above();
            }
            helper.setBlock(at, BeltTileTests.tile(BeltTier.BELT, Direction.EAST));
            tiles.add(at);
        }
        ItemEntity item = rider(helper, tiles.getFirst());
        BlockPos last = tiles.getLast();
        Vec3 end = helper.absoluteVec(last.getBottomCenter());
        helper.startSequence()
                .thenIdle(STEP_RIDE_TICKS)
                .thenExecute(() -> {
                    Vec3 step = helper.absoluteVec(tiles.get(STEP_RIDE_AT).getBottomCenter());
                    if (item.getX() < step.x) {
                        helper.fail("an item riding " + (up ? "up" : "down") + " a step is at x " + item.getX()
                                + ", short of the step's far side at " + step.x, tiles.get(STEP_RIDE_AT));
                    }
                    if (item.getY() < end.y || item.getY() > end.y + 0.6) {
                        helper.fail("an item ridden " + (up ? "up" : "down") + " a step is at y " + item.getY()
                                + ", expected on the tiles past it at " + end.y, last);
                    }
                    if (Math.abs(item.getZ() - end.z) > 0.2) helper.fail("the rider drifted off the line", last);
                })
                .thenSucceed();
    }

    private static void fillsTheInventory(GameTestHelper helper) {
        place(helper, false);
        ServerPlayer player = player(helper, "pf_tile_hand");
        BeltTileBlockEntity held = helper.getBlockEntity(HELD, BeltTileBlockEntity.class);

        int[] pastTheTile = new int[1];
        int[] arrivedBefore = new int[1];
        int[] suppliedBefore = new int[1];
        helper.startSequence()
                .thenIdle(WARMUP_TICKS)
                .thenExecute(() -> {
                    pastTheTile[0] = onTilesPast(helper);
                    arrivedBefore[0] = BeltTileTests.count(BeltTileTests.chest(helper, TARGET));
                    suppliedBefore[0] = BeltTileTests.count(BeltTileTests.chest(helper, SOURCE));
                    if (pastTheTile[0] == 0) helper.fail("nothing is past the held tile, so this proves nothing", TO);
                })
                .thenExecuteFor(HOLD_TICKS, () -> held.holdHand(player))
                .thenExecute(() -> {
                    int taken = cobblestone(player);
                    if (Math.abs(taken - HELD_ITEMS) > 1) {
                        helper.fail("holding a tier-1 tile for " + HOLD_TICKS + " ticks took " + taken
                                + " items, expected " + HELD_ITEMS, HELD);
                    }
                    int arrived = BeltTileTests.count(BeltTileTests.chest(helper, TARGET)) - arrivedBefore[0];
                    if (arrived != pastTheTile[0]) {
                        helper.fail(arrived + " items reached the line's end while a tile was held, expected the "
                                + pastTheTile[0] + " already past it", TARGET);
                    }
                    int loaded = suppliedBefore[0] - BeltTileTests.count(BeltTileTests.chest(helper, SOURCE));
                    if (Math.abs(loaded - HELD_ITEMS) > 1) {
                        helper.fail("the source loaded " + loaded + " items while the tile was held, expected "
                                + HELD_ITEMS, SOURCE);
                    }
                    nothingOnTheGround(helper);
                })
                .thenSucceed();
    }

    private static void stopsWhenFull(GameTestHelper helper) {
        place(helper, false);
        ServerPlayer player = player(helper, "pf_tile_hand_full");
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getNonEquipmentItems().size(); slot++) {
            inventory.setItem(slot, new ItemStack(Items.DIRT, 64));
        }
        inventory.setItem(0, new ItemStack(Items.COBBLESTONE, 64 - ROOM));
        BeltTileBlockEntity held = helper.getBlockEntity(HELD, BeltTileBlockEntity.class);

        int[] arrivedBefore = new int[1];
        helper.startSequence()
                .thenIdle(WARMUP_TICKS)
                .thenExecute(() -> arrivedBefore[0] = BeltTileTests.count(BeltTileTests.chest(helper, TARGET)))
                .thenExecuteFor(HOLD_TICKS, () -> held.holdHand(player))
                .thenExecute(() -> {
                    int taken = cobblestone(player) - (64 - ROOM);
                    if (taken != ROOM) helper.fail("a hand with room for " + ROOM + " took " + taken, HELD);
                    int arrived = BeltTileTests.count(BeltTileTests.chest(helper, TARGET)) - arrivedBefore[0];
                    if (Math.abs(arrived - (HELD_ITEMS - ROOM)) > 1) {
                        helper.fail("once the hand was full " + arrived + " items reached the line's end in "
                                + HOLD_TICKS + " ticks, expected " + (HELD_ITEMS - ROOM), TARGET);
                    }
                    var line = held.line();
                    int onLine = line == null ? 0 : line.size();
                    int delivered = BeltTileTests.count(BeltTileTests.chest(helper, TARGET));
                    int loaded = SUPPLY - BeltTileTests.count(BeltTileTests.chest(helper, SOURCE));
                    if (loaded != onLine + delivered + taken) {
                        helper.fail("the source loaded " + loaded + " items, but only " + (onLine + delivered + taken)
                                + " are on the line, at its end or in the hand", HELD);
                    }
                    nothingOnTheGround(helper);
                })
                .thenSucceed();
    }

    // The head sits flush with the last tile's front, so it is on the tile like any other item (#408).
    private static void lastTileOfABackedUpLine(GameTestHelper helper) {
        place(helper, true);
        ServerPlayer player = player(helper, "pf_tile_hand_end");
        BeltTileBlockEntity last = helper.getBlockEntity(LAST, BeltTileBlockEntity.class);

        helper.startSequence()
                .thenIdle(BACKUP_TICKS)
                .thenExecute(() -> {
                    var line = last.line();
                    if (line == null || line.size() < line.capacity()) {
                        helper.fail("the line has not backed up, so this proves nothing about its end", LAST);
                    }
                })
                .thenExecuteFor(20, () -> last.holdHand(player))
                .thenExecute(() -> {
                    int taken = cobblestone(player);
                    if (Math.abs(taken - TIER_1_ITEMS_PER_SECOND) > 1) {
                        helper.fail("holding a backed-up line's last tile for a second took " + taken
                                + " items, expected " + TIER_1_ITEMS_PER_SECOND, LAST);
                    }
                    nothingOnTheGround(helper);
                })
                .thenSucceed();
    }

    private static void itemRides(GameTestHelper helper) {
        for (int tile = 0; tile < RIDE_TILES; tile++) {
            helper.setBlock(RIDE_FIRST.east(tile), BeltTileTests.tile(BeltTier.BELT, Direction.EAST));
        }
        ItemEntity item = rider(helper, RIDE_FIRST);
        double startX = item.getX();
        double startZ = item.getZ();
        helper.startSequence()
                .thenIdle(RIDE_TICKS)
                .thenExecute(() -> {
                    double moved = item.getX() - startX;
                    if (moved < RIDE_AT_LEAST) {
                        helper.fail("an item on a tier-1 tile moved " + moved + " blocks east in " + RIDE_TICKS
                                + " ticks, expected at least " + RIDE_AT_LEAST, RIDE_FIRST);
                    }
                    if (Math.abs(item.getZ() - startZ) > 0.2) helper.fail("the rider drifted off the line", RIDE_FIRST);
                })
                .thenSucceed();
    }

    private static void itemRidesRoundACorner(GameTestHelper helper) {
        // Downstream first, since a tile's shape is set when what feeds it is placed.
        for (int tile = 3; tile >= 1; tile--) {
            helper.setBlock(CORNER.south(tile), BeltTileTests.tile(BeltTier.BELT, Direction.SOUTH));
        }
        helper.setBlock(CORNER, BeltTileTests.tile(BeltTier.BELT, Direction.SOUTH));
        for (int tile = 1; tile <= 2; tile++) {
            helper.setBlock(CORNER.west(tile), BeltTileTests.tile(BeltTier.BELT, Direction.EAST));
        }
        ItemEntity item = rider(helper, CORNER.west(2));
        Vec3 corner = helper.absoluteVec(CORNER.getCenter());
        helper.startSequence()
                .thenIdle(CORNER_TICKS)
                .thenExecute(() -> {
                    if (item.getZ() < corner.z + 0.75) {
                        helper.fail("an item ridden into a corner is at z " + item.getZ() + ", expected it past the corner southward", CORNER);
                    }
                    if (Math.abs(item.getX() - corner.x) > 0.3) {
                        helper.fail("an item round the corner is at x " + item.getX() + ", off the column at " + corner.x, CORNER);
                    }
                })
                .thenSucceed();
    }

    private static ItemEntity rider(GameTestHelper helper, BlockPos tile) {
        // Dropped from above, so it lands on the belt's surface rather than inside it.
        Vec3 at = helper.absoluteVec(tile.getCenter()).add(0, 0.3, 0);
        // Not cobblestone: tests run side by side, and a neighbour counts the cobblestone lying near it.
        ItemEntity item = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, new ItemStack(Items.STICK));
        item.setDeltaMovement(Vec3.ZERO);
        item.setNeverPickUp();
        helper.getLevel().addFreshEntity(item);
        return item;
    }

    private static void place(GameTestHelper helper, boolean targetFull) {
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.setBlock(FROM, BeltTileTests.loader(BeltTier.BELT, Direction.EAST));
        for (int tile = 0; tile < TILES; tile++) {
            helper.setBlock(FIRST_TILE.east(tile), BeltTileTests.tile(BeltTier.BELT, Direction.EAST));
        }
        helper.setBlock(TO, BeltTileTests.loader(BeltTier.BELT, Direction.WEST));
        helper.setBlock(TARGET, Blocks.CHEST);
        BeltTileTests.fill(helper, SOURCE, SUPPLY);
        if (targetFull) {
            var target = BeltTileTests.chest(helper, TARGET);
            for (int slot = 0; slot < target.getContainerSize(); slot++) target.setItem(slot, new ItemStack(Items.DIRT, 64));
        }
    }

    // By the hand's own point, since an item straddling the held tile's front is already past it.
    private static int onTilesPast(GameTestHelper helper) {
        var line = helper.getBlockEntity(FIRST_TILE, BeltTileBlockEntity.class).line();
        if (line == null) return 0;
        double point = TransportLine.handPoint(HELD.getX() - FIRST_TILE.getX());
        return (int) line.contents().entries().stream().filter(entry -> entry.position() > point).count();
    }

    // A player of its own: the shared fake player's inventory is every test's at once.
    private static ServerPlayer player(GameTestHelper helper, String name) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.setGameMode(GameType.SURVIVAL);
        var at = helper.absoluteVec(STANDING.getBottomCenter());
        player.setPos(at.x, at.y, at.z);
        return player;
    }

    private static int cobblestone(ServerPlayer player) {
        return ContainerHelper.clearOrCountMatchingItems(player.getInventory(),
                stack -> stack.is(Items.COBBLESTONE), 0, true);
    }

    private static void nothingOnTheGround(GameTestHelper helper) {
        int lying = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(2.0))
                .stream().mapToInt(entity -> entity.getItem().getCount()).sum();
        if (lying != 0) helper.fail(lying + " items lie on the ground");
    }
}
