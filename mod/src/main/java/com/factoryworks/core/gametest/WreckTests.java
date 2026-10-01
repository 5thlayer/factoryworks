package com.factoryworks.core.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.start.StartingKit;
import com.factoryworks.core.wreck.CargoHoldBlock;
import com.factoryworks.core.wreck.CargoHoldBlockEntity;
import com.factoryworks.core.wreck.CargoHoldCorpus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The wreck's blocks hold against a survival player, the cargo hold's face and save hook work, and
 * a spawn on the wreck's floor stays there (ADR-0107).
 */
final class WreckTests {

    private static final BlockPos AT = new BlockPos(2, 1, 3);
    private static final Identifier PICK = Identifier.fromNamespaceAndPath("factoryworks",
            "engineers_iron_pick");

    private WreckTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("wreck_hull_survives_a_survival_break", 20,
                helper -> survivesBreak(helper, PFBlocks.WRECK_HULL.get().defaultBlockState()));
        tests.test("wreck_hull_stairs_survives_a_survival_break", 20,
                helper -> survivesBreak(helper, PFBlocks.WRECK_HULL_STAIRS.get().defaultBlockState()));
        tests.test("wreck_hull_slab_survives_a_survival_break", 20,
                helper -> survivesBreak(helper, PFBlocks.WRECK_HULL_SLAB.get().defaultBlockState()));
        tests.test("wreck_hull_stairs_keeps_its_shape_beside_a_placed_block", 20,
                WreckTests::stairsKeepShape);
        for (var debris : List.of(PFBlocks.WRECK_DEBRIS_BIG, PFBlocks.WRECK_DEBRIS_MEDIUM,
                PFBlocks.WRECK_DEBRIS_SMALL)) {
            tests.test(debris.getId().getPath() + "_breaks_for_nothing", 60,
                    helper -> breaksForNothing(helper, debris.get().defaultBlockState()));
        }
        tests.test("wreck_window_survives_a_survival_break", 20,
                helper -> survivesBreak(helper, PFBlocks.WRECK_WINDOW.get().defaultBlockState()));
        tests.test("cargo_hold_part_survives_a_survival_break", 20,
                helper -> survivesBreak(helper, hold(false)));
        tests.test("cargo_hold_anchor_survives_a_survival_break", 20,
                helper -> survivesBreak(helper, hold(true)));
        tests.test("cargo_hold_shares_one_inventory_along_x", 20,
                helper -> sharedInventory(helper, true));
        tests.test("cargo_hold_shares_one_inventory_along_z", 20,
                helper -> sharedInventory(helper, false));
        tests.test("cargo_hold_any_part_resolves_the_anchor", 20, WreckTests::anyPartResolvesAnchor);
        tests.test("cargo_hold_without_an_anchor_answers_nothing", 20, WreckTests::noAnchor);
        tests.test("cargo_hold_with_two_anchors_answers_nothing", 20, WreckTests::twoAnchors);
        tests.test("cargo_hold_has_one_block_entity", 20, WreckTests::oneBlockEntity);
        tests.test("cargo_hold_face_takes_and_gives_on_every_side", 20, WreckTests::faceOnEverySide);
        tests.test("cargo_hold_contents_survive_its_save_hook", 20, WreckTests::survivesSave);
        tests.test("stamped_cargo_hold_holds_exactly_the_hold", 20, WreckTests::stampedHoldIsFilled);
        tests.test("spawn_finder_keeps_the_wreck_floor_under_its_roof", 100, WreckTests::spawnOnFloor);
    }

    private static BlockState hold(boolean anchor) {
        return PFBlocks.CARGO_HOLD.get().defaultBlockState().setValue(CargoHoldBlock.ANCHOR, anchor);
    }

    /** The hold's ten blocks, five along an axis and two up, the anchor at the bottom middle. */
    private static List<BlockPos> buildHold(GameTestHelper helper, boolean alongX, int anchors) {
        List<BlockPos> cells = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            for (int y = 0; y < 2; y++) {
                BlockPos at = alongX ? new BlockPos(1 + i, 1 + y, 1) : new BlockPos(1, 1 + y, 1 + i);
                boolean anchor = y == 0 && (i == 2 || (anchors == 2 && i == 4));
                if (anchors == 0) {
                    anchor = false;
                }
                helper.setBlock(at, hold(anchor));
                cells.add(at);
            }
        }
        return cells;
    }

    private static ResourceHandler<ItemResource> faceAt(GameTestHelper helper, BlockPos at, Direction side) {
        return helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(at), side);
    }

    private static void sharedInventory(GameTestHelper helper, boolean alongX) {
        List<BlockPos> cells = buildHold(helper, alongX, 1);
        List<ItemResource> items = distinctItems(2);
        for (int i = 0; i < cells.size(); i++) {
            Direction side = i % 2 == 0 ? null : Direction.UP;
            ResourceHandler<ItemResource> in = faceAt(helper, cells.get(i), side);
            ResourceHandler<ItemResource> out = faceAt(helper, cells.get((i + 3) % cells.size()), null);
            if (in == null || out == null) {
                helper.fail("a part of the hold has no item face", cells.get(i));
                return;
            }
            int last = CargoHoldCorpus.get().slots() - 1;
            helper.assertValueEqual(insert(in, items.get(0)), 1, "slot-less insert through part " + i);
            helper.assertValueEqual(extract(out, items.get(0)), 1, "slot-less extract from another part");
            helper.assertValueEqual(insertAt(in, last, items.get(1)), 1, "slot insert through part " + i);
            helper.assertValueEqual(extractAt(out, last, items.get(1)), 1, "slot extract from another part");
        }
        helper.succeed();
    }

    private static void anyPartResolvesAnchor(GameTestHelper helper) {
        for (boolean alongX : new boolean[] {true, false}) {
            List<BlockPos> cells = buildHold(helper, alongX, 1);
            for (BlockPos at : cells) {
                CargoHoldBlockEntity anchor = CargoHoldBlock.anchorOf(helper.getLevel(), helper.absolutePos(at));
                helper.assertTrue(anchor != null && anchor.getBlockState().getValue(CargoHoldBlock.ANCHOR),
                        "the part at " + at + " resolves to the anchor");
            }
            cells.forEach(at -> helper.setBlock(at, Blocks.AIR.defaultBlockState()));
        }
        helper.succeed();
    }

    private static void noAnchor(GameTestHelper helper) {
        for (BlockPos at : buildHold(helper, true, 0)) {
            helper.assertTrue(faceAt(helper, at, null) == null, "an anchorless hold answers at " + at);
            helper.assertTrue(CargoHoldBlock.anchorOf(helper.getLevel(), helper.absolutePos(at)) == null,
                    "an anchorless hold has an inventory at " + at);
        }
        helper.succeed();
    }

    private static void twoAnchors(GameTestHelper helper) {
        for (BlockPos at : buildHold(helper, true, 2)) {
            helper.assertTrue(faceAt(helper, at, null) == null, "a two-anchor hold answers at " + at);
            helper.assertTrue(CargoHoldBlock.anchorOf(helper.getLevel(), helper.absolutePos(at)) == null,
                    "a two-anchor hold has an inventory at " + at);
        }
        helper.succeed();
    }

    private static void oneBlockEntity(GameTestHelper helper) {
        int entities = 0;
        for (BlockPos at : buildHold(helper, false, 1)) {
            if (helper.getLevel().getBlockEntity(helper.absolutePos(at)) != null) {
                entities++;
            }
        }
        helper.assertValueEqual(entities, 1, "block entities in a ten-block hold");
        helper.succeed();
    }

    private static void survivesBreak(GameTestHelper helper, BlockState state) {
        Block block = state.getBlock();
        helper.setBlock(AT, state);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);
        var player = survivalPlayerWithPick(helper);

        player.gameMode.handleBlockBreakAction(pos, Action.START_DESTROY_BLOCK, Direction.SOUTH,
                level.getMaxY(), 0);
        for (int tick = 0; tick < 400; tick++) {
            player.gameMode.tick();
        }
        player.gameMode.handleBlockBreakAction(pos, Action.STOP_DESTROY_BLOCK, Direction.SOUTH,
                level.getMaxY(), 1);

        helper.assertTrue(level.getBlockState(pos).is(block), "the block was broken");
        AABB area = new AABB(pos).inflate(8);
        helper.assertValueEqual(level.getEntities(EntityType.ITEM, area, e -> true).size(), 0,
                "items dropped");
        helper.succeed();
    }

    private static FakePlayer survivalPlayerWithPick(
            GameTestHelper helper) {
        // Not makeMockServerPlayerInLevel: joining the level fires KubeJS's login sync, which
        // refuses the mock connection.
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        Vec3 feet = helper.absoluteVec(new Vec3(2.5, 1, 5.5));
        player.setPos(feet.x, feet.y, feet.z);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(BuiltInRegistries.ITEM.getValue(PICK)));
        return player;
    }

    private static void breaksForNothing(GameTestHelper helper, BlockState state) {
        helper.setBlock(AT, state);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);
        var player = survivalPlayerWithPick(helper);
        player.gameMode.handleBlockBreakAction(pos, Action.START_DESTROY_BLOCK, Direction.SOUTH,
                level.getMaxY(), 0);
        for (int tick = 0; tick < 400; tick++) {
            player.gameMode.tick();
        }
        player.gameMode.handleBlockBreakAction(pos, Action.STOP_DESTROY_BLOCK, Direction.SOUTH,
                level.getMaxY(), 1);

        helper.assertTrue(level.getBlockState(pos).isAir(), "the debris still stands");
        AABB area = new AABB(pos).inflate(8);
        helper.assertValueEqual(level.getEntities(EntityType.ITEM, area, e -> true).size(), 0,
                "items dropped");
        helper.succeed();
    }

    private static void stairsKeepShape(GameTestHelper helper) {
        BlockState corner = PFBlocks.WRECK_HULL_STAIRS.get().defaultBlockState()
                .setValue(StairBlock.FACING, Direction.EAST)
                .setValue(StairBlock.SHAPE, StairsShape.OUTER_RIGHT);
        helper.setBlock(AT, corner);
        helper.setBlock(AT.west(), Blocks.STONE);
        helper.assertValueEqual(helper.getBlockState(AT).getValue(StairBlock.SHAPE),
                StairsShape.OUTER_RIGHT, "the corner's shape");
        helper.succeed();
    }

    private static void faceOnEverySide(GameTestHelper helper) {
        helper.setBlock(AT, hold(true));
        List<ItemResource> items = distinctItems(2);
        List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
        sides.add(null);

        for (Direction side : sides) {
            ResourceHandler<ItemResource> face = helper.getLevel()
                    .getCapability(Capabilities.Item.BLOCK, helper.absolutePos(AT), side);
            if (face == null) {
                helper.fail("no item face on " + side, AT);
                return;
            }
            int last = CargoHoldCorpus.get().slots() - 1;
            helper.assertValueEqual(face.size(), CargoHoldCorpus.get().slots(), "slots on " + side);

            ItemResource anywhere = items.get(0);
            ItemResource named = items.get(1);
            helper.assertValueEqual(insert(face, anywhere), 1, "slot-less insert on " + side);
            helper.assertValueEqual(insertAt(face, last, named), 1, "slot insert on " + side);
            helper.assertValueEqual(extract(face, anywhere), 1, "slot-less extract on " + side);
            helper.assertValueEqual(extractAt(face, last, named), 1, "slot extract on " + side);
        }
        helper.succeed();
    }

    private static void survivesSave(GameTestHelper helper) {
        helper.setBlock(AT, hold(true));
        BlockPos absolute = helper.absolutePos(AT);
        BlockEntity original = helper.getLevel().getBlockEntity(absolute);
        if (!(original instanceof CargoHoldBlockEntity hold)) {
            helper.fail("the cargo hold has no block entity", AT);
            return;
        }

        int slots = hold.getContainerSize();
        List<ItemResource> items = distinctItems(slots + 1);
        for (int slot = 0; slot < slots; slot++) {
            ItemStack rest = hold.put(items.get(slot).toStack(slot + 1));
            helper.assertTrue(rest.isEmpty(), "slot " + slot + " took its stack");
        }
        ItemStack overflow = items.get(slots).toStack(1);
        helper.assertTrue(ItemStack.matches(hold.put(overflow), overflow), "a full hold took another stack");

        BlockState state = helper.getLevel().getBlockState(absolute);
        var registries = helper.getLevel().registryAccess();
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        hold.saveWithFullMetadata(out);
        BlockEntity reloaded = BlockEntity.loadStatic(absolute, state, out.buildResult(), registries);

        helper.assertTrue(reloaded != null, "the saved hold loads again");
        for (int slot = 0; slot < slots; slot++) {
            ItemStack want = items.get(slot).toStack(slot + 1);
            ItemStack got = ((net.minecraft.world.Container) reloaded).getItem(slot);
            if (!ItemStack.matches(want, got)) {
                helper.fail("slot " + slot + " reloaded as " + got + ", not " + want, AT);
                return;
            }
        }
        helper.succeed();
    }

    /**
     * The wreck the server stamped at start, not one built here: its hold is the stamp's work. The
     * GameTest server moves the respawn point to its own test site, so the hold is found among the
     * block entities of the chunks around the world origin, where the stamp put the hub.
     */
    private static void stampedHoldIsFilled(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<CargoHoldBlockEntity> holds = new ArrayList<>();
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                for (BlockEntity entity : level.getChunk(x, z).getBlockEntities().values()) {
                    if (entity instanceof CargoHoldBlockEntity found) {
                        holds.add(found);
                    }
                }
            }
        }
        helper.assertValueEqual(holds.size(), 1, "stamped cargo holds near the origin");
        CargoHoldBlockEntity hold = holds.getFirst();

        List<ItemStack> want = new ArrayList<>();
        for (StartingKit.Entry entry : StartingKit.HOLD) {
            want.add(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(entry.item())),
                    entry.count()));
        }
        List<ItemStack> got = new ArrayList<>();
        for (int slot = 0; slot < hold.getContainerSize(); slot++) {
            if (!hold.getItem(slot).isEmpty()) {
                got.add(hold.getItem(slot));
            }
        }
        helper.assertValueEqual(got.size(), want.size(), "stacks in the stamped cargo hold: " + got);
        for (int i = 0; i < want.size(); i++) {
            helper.assertTrue(ItemStack.matches(want.get(i), got.get(i)),
                    "slot " + i + " holds " + got.get(i) + ", not " + want.get(i));
        }
        helper.succeed();
    }

    /** A roofed wreck room with the level's spawn on its floor, which vanilla never answers. */
    private static void spawnOnFloor(GameTestHelper helper) {
        BlockState hull = PFBlocks.WRECK_HULL.get().defaultBlockState();
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                for (int y = 1; y <= 5; y++) {
                    boolean shell = y == 1 || y == 5 || x == 0 || x == 4 || z == 0 || z == 4;
                    helper.setBlock(new BlockPos(x, y, z), shell ? hull : Blocks.AIR.defaultBlockState());
                }
            }
        }
        ServerLevel level = helper.getLevel();
        BlockPos floor = helper.absolutePos(new BlockPos(2, 2, 2));

        // Radius 0 so vanilla searches this column alone.
        GameRules rules = level.getGameRules();
        int radius = rules.get(GameRules.RESPAWN_RADIUS);
        LevelData.RespawnData before = level.getRespawnData();
        CompletableFuture<Vec3> found;
        try {
            rules.set(GameRules.RESPAWN_RADIUS, 0, level.getServer());
            level.setRespawnData(LevelData.RespawnData.of(level.dimension(), floor, 0.0F, 0.0F));
            found = PlayerSpawnFinder.findSpawn(level, floor);
        } finally {
            level.setRespawnData(before);
            rules.set(GameRules.RESPAWN_RADIUS, radius, level.getServer());
        }

        Vec3 want = Vec3.atBottomCenterOf(floor);
        helper.succeedWhen(() -> {
            helper.assertTrue(found.isDone(), "the spawn search is still running");
            helper.assertValueEqual(found.join(), want, "spawn for a player with no respawn point");
        });
    }

    private static int insert(ResourceHandler<ItemResource> face, ItemResource item) {
        try (Transaction tx = Transaction.openRoot()) {
            int moved = face.insert(item, 1, tx);
            tx.commit();
            return moved;
        }
    }

    private static int insertAt(ResourceHandler<ItemResource> face, int slot, ItemResource item) {
        try (Transaction tx = Transaction.openRoot()) {
            int moved = face.insert(slot, item, 1, tx);
            tx.commit();
            return moved;
        }
    }

    private static int extract(ResourceHandler<ItemResource> face, ItemResource item) {
        try (Transaction tx = Transaction.openRoot()) {
            int moved = face.extract(item, 1, tx);
            tx.commit();
            return moved;
        }
    }

    private static int extractAt(ResourceHandler<ItemResource> face, int slot, ItemResource item) {
        try (Transaction tx = Transaction.openRoot()) {
            int moved = face.extract(slot, item, 1, tx);
            tx.commit();
            return moved;
        }
    }

    private static List<ItemResource> distinctItems(int count) {
        List<ItemResource> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR) {
                items.add(ItemResource.of(new ItemStack(item)));
            }
            if (items.size() == count) {
                break;
            }
        }
        return items;
    }
}
