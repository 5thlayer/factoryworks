package com.planetaryfactory.core.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import rearth.belts.BlockContent;
import rearth.belts.ItemContent;
import rearth.belts.blocks.ChuteBlock;
import rearth.belts.blocks.ChuteBlock.Floor;
import rearth.belts.model.BeltTier;

/**
 * The belt floor a loader draws to meet the tile in front of it (#399), as a player builds and
 * breaks a line: loading behind a line's first tile, unloading past its last, none with no tile or a
 * tile running across, whichever of the loader and the tile went down first.
 *
 * <p>Every block but the chests is placed and broken through the player's game mode, since the
 * floor is kept by neighbour updates a {@code setBlock} fixture would stand in for.
 */
final class LoaderFloorTests {

    private static final int SOURCE_X = 2;
    private static final int TILES = 3;

    private LoaderFloorTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("loaders_placed_after_their_tiles_draw_the_line_floor_at_all_four_tiers", 20,
                LoaderFloorTests::loadersAfterTiles);
        tests.test("a_loader_floor_follows_tiles_placed_and_broken_after_it", 20,
                LoaderFloorTests::tilesAfterLoaders);
        tests.test("a_loader_with_no_tile_or_a_crossing_tile_draws_no_floor", 20,
                LoaderFloorTests::noFloor);
    }

    private static void loadersAfterTiles(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        for (BeltTier tier : BeltTier.values()) {
            Row row = new Row(2 * tier.ordinal());
            row.chests(helper);
            for (int tile = 0; tile < TILES; tile++) {
                placeTile(helper, player, tier, row.tile(tile), Direction.EAST);
            }
            row.loaders(helper, player, tier);
            if (!expect(helper, row.from(), Floor.LOADING, tier + " loader behind the line")
                    || !expect(helper, row.into(), Floor.UNLOADING, tier + " loader past the line")) {
                return;
            }
        }
        helper.succeed();
    }

    private static void tilesAfterLoaders(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Row row = new Row(3);
        row.chests(helper);
        row.loaders(helper, player, BeltTier.BELT);
        if (!expect(helper, row.from(), Floor.NONE, "loading loader before its tile")
                || !expect(helper, row.into(), Floor.NONE, "unloading loader before its tile")) {
            return;
        }
        for (int tile = 0; tile < TILES; tile++) {
            placeTile(helper, player, BeltTier.BELT, row.tile(tile), Direction.EAST);
        }
        if (!expect(helper, row.from(), Floor.LOADING, "loading loader once its tile is placed")
                || !expect(helper, row.into(), Floor.UNLOADING, "unloading loader once its tile is placed")) {
            return;
        }
        breakBlock(helper, player, row.tile(0));
        breakBlock(helper, player, row.tile(TILES - 1));
        if (!expect(helper, row.from(), Floor.NONE, "loading loader once its tile is broken")
                || !expect(helper, row.into(), Floor.NONE, "unloading loader once its tile is broken")) {
            return;
        }
        helper.succeed();
    }

    private static void noFloor(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Row alone = new Row(1);
        alone.chests(helper);
        alone.loaders(helper, player, BeltTier.BELT);
        if (!expect(helper, alone.from(), Floor.NONE, "loading loader with no tile")
                || !expect(helper, alone.into(), Floor.NONE, "unloading loader with no tile")) {
            return;
        }

        Row after = new Row(3);
        after.chests(helper);
        placeTile(helper, player, BeltTier.BELT, after.tile(0), Direction.SOUTH);
        placeTile(helper, player, BeltTier.BELT, after.tile(TILES - 1), Direction.NORTH);
        after.loaders(helper, player, BeltTier.BELT);
        if (!expect(helper, after.from(), Floor.NONE, "loader placed beside a crossing tile")
                || !expect(helper, after.into(), Floor.NONE, "loader placed beside a crossing tile")) {
            return;
        }

        Row before = new Row(5);
        before.chests(helper);
        before.loaders(helper, player, BeltTier.BELT);
        placeTile(helper, player, BeltTier.BELT, before.tile(0), Direction.NORTH);
        placeTile(helper, player, BeltTier.BELT, before.tile(TILES - 1), Direction.SOUTH);
        if (!expect(helper, before.from(), Floor.NONE, "loader with a crossing tile placed after it")
                || !expect(helper, before.into(), Floor.NONE, "loader with a crossing tile placed after it")) {
            return;
        }
        helper.succeed();
    }

    /** Chest, loader, three tiles running east, loader, chest, along x on row {@code z}. */
    private record Row(int z) {

        BlockPos source() {
            return new BlockPos(SOURCE_X, 1, z);
        }

        BlockPos from() {
            return source().east();
        }

        BlockPos tile(int index) {
            return from().east(1 + index);
        }

        BlockPos into() {
            return tile(TILES);
        }

        BlockPos target() {
            return into().east();
        }

        void chests(GameTestHelper helper) {
            helper.setBlock(source(), Blocks.CHEST);
            helper.setBlock(target(), Blocks.CHEST);
        }

        /** Sneak-clicked onto each chest's inner face, as a player places a loader. */
        void loaders(GameTestHelper helper, ServerPlayer player, BeltTier tier) {
            player.setShiftKeyDown(true);
            use(helper, player, new ItemStack(BlockContent.loaderFor(tier).asItem()), source(), Direction.EAST);
            use(helper, player, new ItemStack(BlockContent.loaderFor(tier).asItem()), target(), Direction.WEST);
            player.setShiftKeyDown(false);
        }
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void placeTile(GameTestHelper helper, ServerPlayer player, BeltTier tier, BlockPos at, Direction runs) {
        player.setYRot(runs.toYRot());
        use(helper, player, new ItemStack(ItemContent.tileFor(tier)), at.below(), Direction.UP);
    }

    private static void breakBlock(GameTestHelper helper, ServerPlayer player, BlockPos at) {
        player.gameMode.destroyBlock(helper.absolutePos(at));
    }

    private static void use(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos on, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos absolute = helper.absolutePos(on);
        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false));
    }

    private static boolean expect(GameTestHelper helper, BlockPos loader, Floor expected, String what) {
        var state = helper.getBlockState(loader);
        if (!(state.getBlock() instanceof ChuteBlock)) {
            helper.fail("no loader where the " + what + " should stand: " + state, loader);
            return false;
        }
        Floor floor = state.getValue(ChuteBlock.FLOOR);
        if (floor != expected) {
            helper.fail("the " + what + " draws floor " + floor + ", expected " + expected, loader);
            return false;
        }
        return true;
    }
}
