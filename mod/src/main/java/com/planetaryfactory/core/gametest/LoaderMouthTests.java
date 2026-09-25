package com.planetaryfactory.core.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import io.github._5thlayer.beltworks.BlockContent;
import io.github._5thlayer.beltworks.ItemContent;
import io.github._5thlayer.beltworks.blocks.BeltTileBlockEntity;
import io.github._5thlayer.beltworks.model.BeltTier;
import io.github._5thlayer.beltworks.model.TransportLine;

/**
 * A loader carries no items (#408): a loading line takes each item on at its first tile's back
 * edge, where the loader's mouth is, and an unloading line holds its head against its last tile's
 * front edge and lets it go from there.
 */
final class LoaderMouthTests {

    private static final int TILES = 3;
    // A tile is one block, and an item spans an eighth of it.
    private static final double ITEM_LENGTH = 1 / 8d;
    private static final double EPSILON = 1e-9;
    private static final int BACKED_UP = 8 * TILES;

    private static final BlockPos SOURCE = new BlockPos(2, 1, 3);
    private static final BlockPos FIRST_TILE = SOURCE.east(2);
    private static final BlockPos LAST_TILE = FIRST_TILE.east(TILES - 1);
    private static final BlockPos TARGET = LAST_TILE.east(2);

    private LoaderMouthTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_loading_line_takes_an_item_on_at_its_first_tile_back_edge", 60,
                LoaderMouthTests::loadsAtBackEdge);
        tests.test("an_unloading_line_lets_its_items_go_at_its_last_tile_front_edge", 400,
                LoaderMouthTests::unloadsAtFrontEdge);
    }

    private static void loadsAtBackEdge(GameTestHelper helper) {
        build(helper);
        chest(helper, SOURCE).setItem(0, new ItemStack(Items.COBBLESTONE));
        String[] firstSeen = {null};
        helper.onEachTick(() -> {
            for (int index = 0; firstSeen[0] == null && index < TILES; index++) {
                List<TransportLine.Share<ItemStack>> held = tile(helper, index).held();
                if (held.isEmpty()) continue;
                double offset = held.getFirst().offset();
                firstSeen[0] = index == 0 && offset < ITEM_LENGTH ? ""
                        : "the loaded item was first seen on tile " + index + " at " + offset
                                + ", not at the first tile's back edge";
            }
        });
        helper.succeedWhen(() -> {
            if (firstSeen[0] == null) throw helper.assertionException(FIRST_TILE, "no item was loaded");
            if (!firstSeen[0].isEmpty()) throw helper.assertionException(FIRST_TILE, firstSeen[0]);
        });
    }

    private static void unloadsAtFrontEdge(GameTestHelper helper) {
        build(helper);
        Container target = chest(helper, TARGET);
        for (int slot = 0; slot < target.getContainerSize(); slot++) {
            target.setItem(slot, new ItemStack(Items.STONE, 64));
        }
        chest(helper, SOURCE).setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        boolean[] freed = {false};
        helper.succeedWhen(() -> {
            if (!freed[0]) {
                int onLine = 0;
                for (int index = 0; index < TILES; index++) onLine += tile(helper, index).held().size();
                if (onLine < BACKED_UP) {
                    throw helper.assertionException(LAST_TILE, "the line holds " + onLine + " of " + BACKED_UP);
                }
                List<TransportLine.Share<ItemStack>> last = tile(helper, TILES - 1).held();
                double front = last.getLast().offset() + ITEM_LENGTH;
                if (Math.abs(front - 1) > EPSILON) {
                    throw helper.assertionException(LAST_TILE,
                            "the backed-up head's front is at " + front + " of the last tile, not its front edge");
                }
                target.setItem(0, ItemStack.EMPTY);
                freed[0] = true;
            }
            if (!target.getItem(0).is(Items.COBBLESTONE)) {
                throw helper.assertionException(TARGET, "the head was not delivered once the chest had room");
            }
        });
    }

    /** Chest, loader, three tiles running east, loader, chest, placed through the player's game mode. */
    private static void build(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.setBlock(TARGET, Blocks.CHEST);
        player.setYRot(Direction.EAST.toYRot());
        for (int index = 0; index < TILES; index++) {
            use(helper, player, new ItemStack(ItemContent.tileFor(BeltTier.BELT)), FIRST_TILE.east(index).below(),
                    Direction.UP);
        }
        player.setShiftKeyDown(true);
        use(helper, player, new ItemStack(BlockContent.loaderFor(BeltTier.BELT).asItem()), SOURCE, Direction.EAST);
        use(helper, player, new ItemStack(BlockContent.loaderFor(BeltTier.BELT).asItem()), TARGET, Direction.WEST);
        player.setShiftKeyDown(false);
    }

    private static void use(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos on, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos absolute = helper.absolutePos(on);
        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false));
    }

    private static BeltTileBlockEntity tile(GameTestHelper helper, int index) {
        return helper.getBlockEntity(FIRST_TILE.east(index), BeltTileBlockEntity.class);
    }

    private static Container chest(GameTestHelper helper, BlockPos at) {
        return helper.getBlockEntity(at, ChestBlockEntity.class);
    }
}
