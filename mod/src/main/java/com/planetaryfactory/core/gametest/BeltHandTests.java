package com.planetaryfactory.core.gametest;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import rearth.belts.blocks.ChuteBlockEntity;
import rearth.belts.model.BeltTier;

/**
 * Holding a belt makes the held point its end for the player (#350): items reaching it go into
 * the inventory at the belt's rate, items already past it carry on to the real end, and the source
 * keeps loading. This is the server half, fed the belt and the point a client's ray would send,
 * once a tick as the client resends it.
 *
 * <p>The rate is typed rather than read off the fork: a tier-1 belt carries 15 items/s.
 */
final class BeltHandTests {

    private static final BlockPos SOURCE = new BlockPos(2, 1, 3);
    private static final BlockPos FROM = new BlockPos(3, 1, 3);
    private static final BlockPos TO = new BlockPos(7, 1, 3);
    private static final BlockPos TARGET = new BlockPos(8, 1, 3);
    // Beside the belt's middle, well within reach of it.
    private static final BlockPos STANDING = new BlockPos(5, 1, 5);

    private static final double MIDDLE = 0.5;
    private static final int SUPPLY = 27 * 64;
    // Longer than an item takes to cross the five-block belt, so it is flowing end to end.
    private static final int WARMUP_TICKS = 80;
    // A multiple of four ticks: a tier-1 belt moves a whole number of items only every four.
    private static final int HOLD_TICKS = 100;
    private static final int TIER_1_ITEMS_PER_SECOND = 15;
    private static final int HELD_ITEMS = TIER_1_ITEMS_PER_SECOND * HOLD_TICKS / 20;

    private static final int ROOM = 4;

    private BeltHandTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_held_belt_fills_the_inventory_at_its_rate", WARMUP_TICKS + HOLD_TICKS + 20,
                BeltHandTests::fillsTheInventory);
        tests.test("a_held_belt_stops_taking_when_the_inventory_is_full", WARMUP_TICKS + HOLD_TICKS + 20,
                BeltHandTests::stopsWhenFull);
    }

    private static void fillsTheInventory(GameTestHelper helper) {
        ChuteBlockEntity belt = placeBelt(helper);
        ServerPlayer player = player(helper, "pf_belt_hand");

        long[] pastTheHand = new long[1];
        int[] arrivedBefore = new int[1];
        int[] suppliedBefore = new int[1];
        helper.startSequence()
                .thenIdle(WARMUP_TICKS)
                .thenExecute(() -> {
                    double point = belt.handPoint(MIDDLE);
                    pastTheHand[0] = belt.getBeltEntries().stream().filter(entry -> entry.position() > point).count();
                    arrivedBefore[0] = BeltHandoffTests.count(BeltHandoffTests.chest(helper, TARGET));
                    suppliedBefore[0] = BeltHandoffTests.count(BeltHandoffTests.chest(helper, SOURCE));
                    if (pastTheHand[0] == 0) {
                        helper.fail("nothing is past the held point, so this proves nothing about it", TO);
                    }
                })
                .thenExecuteFor(HOLD_TICKS, () -> belt.holdHand(player, MIDDLE))
                .thenExecute(() -> {
                    int taken = cobblestone(player);
                    if (Math.abs(taken - HELD_ITEMS) > 1) {
                        helper.fail("holding a tier-1 belt for " + HOLD_TICKS + " ticks took " + taken
                                + " items, expected " + HELD_ITEMS, FROM);
                    }
                    int arrived = BeltHandoffTests.count(BeltHandoffTests.chest(helper, TARGET)) - arrivedBefore[0];
                    if (arrived != pastTheHand[0]) {
                        helper.fail(arrived + " items reached the belt's end while it was held, expected the "
                                + pastTheHand[0] + " already past the hand", TARGET);
                    }
                    int loaded = suppliedBefore[0] - BeltHandoffTests.count(BeltHandoffTests.chest(helper, SOURCE));
                    if (Math.abs(loaded - HELD_ITEMS) > 1) {
                        helper.fail("the source loaded " + loaded + " items while the belt was held, expected "
                                + HELD_ITEMS, SOURCE);
                    }
                    nothingOnTheGround(helper);
                })
                .thenSucceed();
    }

    // Past the refused item the belt runs to its end, so the chest there takes the rest of the flow.
    private static void stopsWhenFull(GameTestHelper helper) {
        ChuteBlockEntity belt = placeBelt(helper);
        ServerPlayer player = player(helper, "pf_belt_hand_full");
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getNonEquipmentItems().size(); slot++) {
            inventory.setItem(slot, new ItemStack(Items.DIRT, 64));
        }
        inventory.setItem(0, new ItemStack(Items.COBBLESTONE, 64 - ROOM));

        int[] arrivedBefore = new int[1];
        helper.startSequence()
                .thenIdle(WARMUP_TICKS)
                .thenExecute(() -> arrivedBefore[0] = BeltHandoffTests.count(BeltHandoffTests.chest(helper, TARGET)))
                .thenExecuteFor(HOLD_TICKS, () -> belt.holdHand(player, MIDDLE))
                .thenExecute(() -> {
                    int taken = cobblestone(player) - (64 - ROOM);
                    if (taken != ROOM) {
                        helper.fail("a hand with room for " + ROOM + " took " + taken, FROM);
                    }
                    int arrived = BeltHandoffTests.count(BeltHandoffTests.chest(helper, TARGET)) - arrivedBefore[0];
                    if (Math.abs(arrived - (HELD_ITEMS - ROOM)) > 1) {
                        helper.fail("once the hand was full " + arrived + " items reached the belt's end in "
                                + HOLD_TICKS + " ticks, expected " + (HELD_ITEMS - ROOM), TARGET);
                    }
                    int onBelt = belt.getBeltEntries().size();
                    int delivered = BeltHandoffTests.count(BeltHandoffTests.chest(helper, TARGET));
                    int loaded = SUPPLY - BeltHandoffTests.count(BeltHandoffTests.chest(helper, SOURCE));
                    if (loaded != onBelt + delivered + taken) {
                        helper.fail("the source loaded " + loaded + " items, but only " + (onBelt + delivered + taken)
                                + " are on the belt, at its end or in the hand", FROM);
                    }
                    nothingOnTheGround(helper);
                })
                .thenSucceed();
    }

    private static ChuteBlockEntity placeBelt(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.CHEST);
        return BeltHandoffTests.placeBelt(helper, SOURCE, FROM, TO, SUPPLY, BeltTier.BELT, BeltTier.BELT);
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
