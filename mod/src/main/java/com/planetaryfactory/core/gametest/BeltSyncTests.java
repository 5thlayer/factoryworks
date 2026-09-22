package com.planetaryfactory.core.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import rearth.belts.blocks.ChuteBlockEntity;
import rearth.belts.model.BeltContents;
import rearth.belts.model.BeltTier;

/**
 * A belt tells its clients what it gains and loses, not its whole contents each tick (#351): a
 * belt moving items sends no block update, whether or not it is also loading and delivering, and
 * a saved belt restores every entry.
 *
 * <p>Whether the client's copy renders smoothly is a human check on delivery; the copy itself is
 * the fork's {@code BeltSyncTest}.
 */
final class BeltSyncTests {

    private static final BlockPos SOURCE = new BlockPos(2, 1, 3);
    private static final BlockPos FROM = new BlockPos(3, 1, 3);
    private static final BlockPos TO = new BlockPos(7, 1, 3);
    private static final BlockPos TARGET = new BlockPos(8, 1, 3);

    // Placing the belt sends its own updates; the item is loaded by then and still short of the end.
    private static final int SETTLE_TICKS = 5;
    private static final int MOVING_TICKS = 30;
    // Longer than an item takes to cross the five-block belt, so it is flowing end to end.
    private static final int FLOWING_WARMUP_TICKS = 80;
    private static final int FLOWING_TICKS = 100;
    private static final int SUPPLY = 27 * 64;

    private BeltSyncTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_moving_belt_sends_no_block_update", SETTLE_TICKS + MOVING_TICKS + 20, BeltSyncTests::movingSendsNothing);
        tests.test("a_loading_and_delivering_belt_sends_no_block_update", FLOWING_WARMUP_TICKS + FLOWING_TICKS + 20,
                BeltSyncTests::flowingSendsNothing);
        tests.test("a_saved_belt_restores_every_entry", FLOWING_WARMUP_TICKS + 20, BeltSyncTests::savedBeltRestores);
    }

    private static void movingSendsNothing(GameTestHelper helper) {
        ChuteBlockEntity belt = BeltHandoffTests.placeBelt(helper, SOURCE, FROM, TO, 1, BeltTier.BELT, BeltTier.BELT);

        double[] before = new double[1];
        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    before[0] = only(helper, belt).position();
                    watch(helper);
                })
                .thenIdle(MOVING_TICKS)
                .thenExecute(() -> {
                    int updates = stop(helper);
                    if (only(helper, belt).position() <= before[0]) {
                        helper.fail("the item did not move, so this proves nothing", FROM);
                    }
                    if (updates != 0) {
                        helper.fail("a belt moving one item sent " + updates + " block updates in "
                                + MOVING_TICKS + " ticks, expected none", FROM);
                    }
                })
                .thenSucceed();
    }

    private static void flowingSendsNothing(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.CHEST);
        BeltHandoffTests.placeBelt(helper, SOURCE, FROM, TO, SUPPLY, BeltTier.BELT, BeltTier.BELT);

        int[] arrivedBefore = new int[1];
        helper.startSequence()
                .thenIdle(FLOWING_WARMUP_TICKS)
                .thenExecute(() -> {
                    arrivedBefore[0] = BeltHandoffTests.count(BeltHandoffTests.chest(helper, TARGET));
                    watch(helper);
                })
                .thenIdle(FLOWING_TICKS)
                .thenExecute(() -> {
                    int updates = stop(helper);
                    if (BeltHandoffTests.count(BeltHandoffTests.chest(helper, TARGET)) == arrivedBefore[0]) {
                        helper.fail("nothing was delivered, so this proves nothing", TARGET);
                    }
                    if (updates != 0) {
                        helper.fail("a belt loading and delivering sent " + updates + " block updates in "
                                + FLOWING_TICKS + " ticks, expected none", FROM);
                    }
                })
                .thenSucceed();
    }

    // Through the block entity's own save and load, which is what a world save and reload runs.
    private static void savedBeltRestores(GameTestHelper helper) {
        ChuteBlockEntity belt = BeltHandoffTests.placeBelt(helper, SOURCE, FROM, TO, SUPPLY, BeltTier.BELT, BeltTier.BELT);

        helper.startSequence()
                .thenIdle(FLOWING_WARMUP_TICKS)
                .thenExecute(() -> {
                    var registries = helper.getLevel().registryAccess();
                    var saved = belt.saveWithFullMetadata(registries);
                    var loaded = (ChuteBlockEntity) BlockEntity.loadStatic(belt.getBlockPos(), belt.getBlockState(), saved, registries);
                    List<BeltContents.Entry<ItemStack>> before = belt.getBeltEntries();
                    List<BeltContents.Entry<ItemStack>> after = loaded.getBeltEntries();
                    if (before.size() < 2) {
                        helper.fail("the belt holds " + before.size() + " entries, so this proves little", FROM);
                    }
                    if (after.size() != before.size()) {
                        helper.fail("a belt saved with " + before.size() + " entries loaded with " + after.size(), FROM);
                    }
                    for (int i = 0; i < before.size(); i++) {
                        var was = before.get(i);
                        var is = after.get(i);
                        if (was.id() != is.id() || was.position() != is.position()
                                || !ItemStack.matches(was.payload(), is.payload())) {
                            helper.fail("entry " + i + " saved as " + describe(was) + " loaded as " + describe(is), FROM);
                        }
                    }
                })
                .thenSucceed();
    }

    private static BeltContents.Entry<ItemStack> only(GameTestHelper helper, ChuteBlockEntity belt) {
        var entries = belt.getBeltEntries();
        if (entries.size() != 1) helper.fail("the belt holds " + entries.size() + " entries, expected one", FROM);
        return entries.getFirst();
    }

    private static void watch(GameTestHelper helper) {
        BlockUpdateWatch.watch(helper.absolutePos(FROM));
        BlockUpdateWatch.watch(helper.absolutePos(TO));
    }

    private static int stop(GameTestHelper helper) {
        return BlockUpdateWatch.stop(helper.absolutePos(FROM)) + BlockUpdateWatch.stop(helper.absolutePos(TO));
    }

    private static String describe(BeltContents.Entry<ItemStack> entry) {
        return "#" + entry.id() + " " + entry.payload() + " at " + entry.position();
    }
}
