package com.planetaryfactory.core.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import rearth.belts.BlockContent;
import rearth.belts.ItemContent;
import rearth.belts.blocks.ChuteBlockEntity;
import rearth.belts.model.BeltPath.Bound;
import rearth.belts.model.BeltTier;

/**
 * A belt laid through the belt item in a shape outside one of ADR-0078's four bounds is refused:
 * no block changes, no loader is linked, no belt item is spent, and the player is told which bound.
 * The world is read before the gesture as well as after, as ADR-0069's plan tests read it.
 *
 * <p>Each layout is an east-facing loader clicked, then a second loader clicked. Where each bound
 * falls is the fork's {@code BeltPathTest}.
 */
final class BeltShapeTests {

    private static final BlockPos FROM = new BlockPos(2, 1, 3);
    private static final BlockPos LONG_FROM = new BlockPos(1, 1, 1);
    private static final int HELD = 64;

    private BeltShapeTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        // A curve three blocks long rising one: 35.4° at its steepest.
        tests.test("a_belt_climbing_past_35_degrees_is_refused", 20,
                helper -> refused(helper, FROM, new BlockPos(4, 2, 3), Direction.WEST, Bound.TOO_STEEP));
        tests.test("a_belt_that_climbs_and_sidesteps_is_refused", 20,
                helper -> refused(helper, FROM, new BlockPos(8, 2, 4), Direction.WEST, Bound.TURNS_WHILE_CLIMBING));
        // A quarter turn over a block and a half: a radius of 0.8.
        tests.test("a_belt_turning_tighter_than_a_block_is_refused", 20,
                helper -> refused(helper, FROM, new BlockPos(3, 1, 4), Direction.NORTH, Bound.TURN_TOO_TIGHT));
        tests.test("a_belt_reaching_33_blocks_is_refused", 20, PFGameTests.LONG_PLATFORM,
                helper -> refused(helper, LONG_FROM, LONG_FROM.east(33), Direction.WEST, Bound.SPAN_TOO_LONG));
    }

    private static void refused(GameTestHelper helper, BlockPos from, BlockPos to, Direction toFacing, Bound bound) {
        helper.setBlock(from, loader(Direction.EAST));
        helper.setBlock(to, loader(toFacing));
        var player = new ListeningPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemContent.beltFor(BeltTier.BELT), HELD));

        Map<BlockPos, BlockState> before = world(helper);
        click(helper, player, from);
        click(helper, player, to);

        Map<BlockPos, BlockState> after = world(helper);
        List<BlockPos> changed = before.keySet().stream().filter(pos -> before.get(pos) != after.get(pos)).toList();
        if (!changed.isEmpty()) {
            helper.fail("a refused belt changed " + changed.size() + " blocks, first at " + changed.getFirst(), from);
        }
        for (BlockPos end : List.of(from, to)) {
            if (helper.getBlockEntity(end, ChuteBlockEntity.class).isUsed()) {
                helper.fail("a refused belt linked this loader", end);
            }
        }
        int left = ContainerHelper.clearOrCountMatchingItems(player.getInventory(),
                stack -> stack.is(ItemContent.beltFor(BeltTier.BELT)), 0, true);
        if (left != HELD) {
            helper.fail("a refused belt charged " + (HELD - left) + " belt items", from);
        }
        if (!player.heard.contains(bound.messageKey())) {
            helper.fail("the player was told " + player.heard + ", not " + bound.messageKey(), to);
        }
        helper.succeed();
    }

    private static Map<BlockPos, BlockState> world(GameTestHelper helper) {
        return BlockPos.betweenClosedStream(helper.getBounds())
                .map(BlockPos::immutable)
                .collect(Collectors.toMap(Function.identity(), pos -> helper.getLevel().getBlockState(pos)));
    }

    private static BlockState loader(Direction facing) {
        return BlockContent.CHUTE_BLOCK.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    private static void click(GameTestHelper helper, FakePlayer player, BlockPos target) {
        BlockPos absolute = helper.absolutePos(target);
        helper.useBlock(target, player, new BlockHitResult(
                Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false));
    }

    /** A player of its own, keeping the translation key of every message it is sent. */
    private static final class ListeningPlayer extends FakePlayer {
        final List<String> heard = new ArrayList<>();

        ListeningPlayer(GameTestHelper helper) {
            super(helper.getLevel(), new GameProfile(UUID.randomUUID(), "pf_belt_shaper"));
        }

        @Override
        public void sendSystemMessage(Component message, boolean actionBar) {
            if (message.getContents() instanceof TranslatableContents translatable) heard.add(translatable.getKey());
        }
    }
}
