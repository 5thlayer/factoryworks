package com.planetaryfactory.core.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.PFDataComponents;
import com.planetaryfactory.core.dismantle.DismantlePlan;
import com.planetaryfactory.core.dismantle.DismantleSpan;
import com.planetaryfactory.core.dismantle.FamilyDismantle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import rearth.oritech.block.blocks.pipes.AbstractPipeBlock;
import rearth.oritech.block.blocks.pipes.GenericPipeBlock;

/**
 * A Dismantle of Oritech's fluid pipes (#431, ADR-0086): each test sneak-clicks a start with the
 * Engineer's Pick through the player's game mode, asks {@link FamilyDismantle#plan} for the end,
 * sneak-clicks it, and holds the world, the inventory and the stored start to the plan.
 */
final class PipeDismantleTests {

    private static final BlockPos START = new BlockPos(2, 1, 3);
    private static final Identifier PICK = Identifier.fromNamespaceAndPath("planetaryfactory", "engineers_iron_pick");
    private static final Identifier FLUID_PIPE = Identifier.fromNamespaceAndPath("oritech", "fluid_pipe");

    private PipeDismantleTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_pipe_dismantle_takes_up_a_straight_run", 20, helper -> {
            List<BlockPos> run = row(helper, 6);
            takesUp(helper, run.get(1), run.get(4), run.subList(1, 5), List.of(run.getFirst(), run.getLast()));
        });
        tests.test("a_pipe_dismantle_follows_a_bend", 20, helper -> {
            List<BlockPos> run = new ArrayList<>(row(helper, 3));
            for (int i = 1; i <= 3; i++) run.add(pipe(helper, START.east(2).south(i)));
            takesUp(helper, run.getFirst(), run.getLast(), run, List.of());
        });
        tests.test("a_pipe_dismantle_on_a_tee_takes_only_the_branch_between_its_ends", 20, helper -> {
            List<BlockPos> run = row(helper, 5);
            List<BlockPos> branch = List.of(pipe(helper, START.east(2).south(1)), pipe(helper, START.east(2).south(2)));
            takesUp(helper, run.getFirst(), run.getLast(), run, branch);
        });
        tests.test("a_one_pipe_dismantle_takes_that_pipe", 20, helper -> {
            List<BlockPos> run = row(helper, 3);
            takesUp(helper, run.get(1), run.get(1), List.of(run.get(1)), List.of(run.getFirst(), run.getLast()));
        });
        tests.test("a_pipe_dismantle_between_opposite_points_of_a_ring_changes_nothing", 20, helper -> {
            for (int x = 0; x < 3; x++) pipe(helper, START.east(x));
            for (int z = 1; z < 3; z++) pipe(helper, START.east(2).south(z));
            for (int x = 1; x >= 0; x--) pipe(helper, START.east(x).south(2));
            pipe(helper, START.south(1));
            refused(helper, START, START.east(2).south(2), DismantleSpan.Refusal.TIED, "message.planetaryfactory.dismantle.tied");
        });
        tests.test("a_pipe_dismantle_across_a_closed_connection_changes_nothing", 20, helper -> {
            List<BlockPos> run = row(helper, 4);
            close(helper, run.get(1), Direction.EAST);
            close(helper, run.get(2), Direction.WEST);
            refused(helper, run.getFirst(), run.getLast(), DismantleSpan.Refusal.NOT_JOINED, "message.planetaryfactory.dismantle.not_joined");
        });
        tests.test("a_pipe_dismantle_ending_on_a_machine_changes_nothing", 20, helper -> {
            helper.setBlock(START.east(3), PFBlocks.BOILER.get());
            row(helper, 3);
            refused(helper, START, START.east(3), DismantleSpan.Refusal.OUTSIDE_FAMILY, "message.planetaryfactory.dismantle.outside_family");
        });
        tests.test("a_pipe_dismantle_with_no_room_drops_the_rest_at_the_players_feet", 20, PipeDismantleTests::fullInventory);
        tests.test("a_creative_pipe_dismantle_hands_over_nothing", 20, PipeDismantleTests::creative);
        tests.test("a_click_after_the_start_pipe_broke_is_a_new_start", 20, PipeDismantleTests::staleStart);
        tests.test("a_sneak_use_in_the_air_clears_the_pipe_dismantle_start", 20, PipeDismantleTests::clears);
    }

    private static void fullInventory(GameTestHelper helper) {
        List<BlockPos> run = row(helper, 3);
        var player = started(helper, run.getFirst());
        for (int slot = 0; slot < player.getInventory().getNonEquipmentItems().size(); slot++) {
            if (player.getInventory().getItem(slot).isEmpty()) {
                player.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
            }
        }
        click(helper, player, run.getLast());
        if (standing(helper, run) != null) {
            helper.fail("a dismantle with no room left " + helper.getBlockState(standing(helper, run)), standing(helper, run));
            return;
        }
        Map<Item, Integer> dropped = new HashMap<>();
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(3))) {
            dropped.merge(entity.getItem().getItem(), entity.getItem().getCount(), Integer::sum);
        }
        if (!Map.of(pipeItem(), 3).equals(dropped)) {
            helper.fail("a dismantle with no room dropped " + dropped + " at the player's feet, not 3 pipes", START);
            return;
        }
        helper.succeed();
    }

    private static void creative(GameTestHelper helper) {
        List<BlockPos> run = row(helper, 3);
        var player = started(helper, run.getFirst());
        player.setGameMode(GameType.CREATIVE);
        click(helper, player, run.getLast());
        if (standing(helper, run) != null) {
            helper.fail("a creative dismantle left a pipe standing", standing(helper, run));
            return;
        }
        Map<Item, Integer> carried = inventory(player);
        if (!carried.isEmpty() || !helper.getEntities(EntityType.ITEM).isEmpty()) {
            helper.fail("a creative dismantle handed over " + carried, START);
            return;
        }
        helper.succeed();
    }

    private static void staleStart(GameTestHelper helper) {
        List<BlockPos> run = row(helper, 4);
        var player = started(helper, run.getFirst());
        helper.destroyBlock(run.getFirst());
        click(helper, player, run.get(2));
        if (!Objects.equals(stored(helper, run.get(2)), player.getMainHandItem().get(PFDataComponents.DISMANTLE_START.get()))) {
            helper.fail("a click after the start broke stored " + player.getMainHandItem().get(PFDataComponents.DISMANTLE_START.get())
                    + ", not the clicked pipe", run.get(2));
            return;
        }
        for (BlockPos pos : run.subList(1, run.size())) {
            if (FamilyDismantle.familyOf(helper.getBlockState(pos)).isEmpty()) {
                helper.fail("a click after the start broke took up a pipe", pos);
                return;
            }
        }
        helper.succeed();
    }

    private static void clears(GameTestHelper helper) {
        List<BlockPos> run = row(helper, 3);
        var player = started(helper, run.getFirst());
        player.setShiftKeyDown(true);
        player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
        if (player.getMainHandItem().has(PFDataComponents.DISMANTLE_START.get())) {
            helper.fail("a sneak-use in the air left the dismantle's start stored", run.getFirst());
            return;
        }
        helper.succeed();
    }

    /**
     * Asks the plan of a sneak-click at {@code end} from {@code start}, clicks, and holds the world to
     * it: exactly {@code taken} gone, {@code kept} still pipes, a pipe item for each taken block, and
     * the start cleared.
     */
    private static void takesUp(GameTestHelper helper, BlockPos start, BlockPos end, List<BlockPos> taken, List<BlockPos> kept) {
        var player = started(helper, start);
        DismantlePlan plan = planOf(helper, player, end);
        List<BlockPos> expected = taken.stream().map(helper::absolutePos).toList();
        if (plan == null || plan.refused() || !Objects.equals(Set.copyOf(expected), Set.copyOf(plan.blocks()))) {
            helper.fail("the dismantle to " + end + " was planned as "
                    + (plan == null ? "nothing" : plan.refused() ? plan.refusal() : plan.blocks()) + ", not " + expected, end);
            return;
        }
        click(helper, player, end);
        BlockPos left = standing(helper, taken);
        if (left != null) {
            helper.fail("the plan named " + left + " and the click left " + helper.getBlockState(left), left);
            return;
        }
        for (BlockPos pos : kept) {
            if (FamilyDismantle.familyOf(helper.getBlockState(pos)).isEmpty()) {
                helper.fail("the dismantle took " + pos + ", which is outside its span", pos);
                return;
            }
        }
        Map<Item, Integer> carried = inventory(player);
        if (!Map.of(pipeItem(), taken.size()).equals(carried)) {
            helper.fail("the dismantle handed over " + carried + ", not " + taken.size() + " pipes", end);
            return;
        }
        if (!helper.getEntities(EntityType.ITEM).isEmpty()) {
            helper.fail("the dismantle left items on the ground", end);
            return;
        }
        if (player.getMainHandItem().has(PFDataComponents.DISMANTLE_START.get())) {
            helper.fail("a dismantle left its start stored", end);
            return;
        }
        helper.succeed();
    }

    private static void refused(GameTestHelper helper, BlockPos start, BlockPos end, DismantleSpan.Refusal refusal, String message) {
        var player = started(helper, start);
        DismantlePlan plan = planOf(helper, player, end);
        if (plan == null || plan.refusal() != refusal || !plan.blocks().isEmpty()) {
            helper.fail("the dismantle to " + end + " was planned as "
                    + (plan == null ? "nothing" : plan.refused() ? plan.refusal() : plan.blocks()) + ", not " + refusal, end);
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
        if (!player.heard.equals(List.of(message))) {
            helper.fail("the player was told " + player.heard + ", not " + message, end);
            return;
        }
        helper.succeed();
    }

    /** {@code count} fluid pipes running east from {@link #START}. */
    private static List<BlockPos> row(GameTestHelper helper, int count) {
        List<BlockPos> run = new ArrayList<>();
        for (int i = 0; i < count; i++) run.add(pipe(helper, START.east(i)));
        return run;
    }

    /** A fluid pipe placed as its item places it, open to every pipe it touches. */
    private static BlockPos pipe(GameTestHelper helper, BlockPos at) {
        Block block = BuiltInRegistries.BLOCK.getValue(FLUID_PIPE);
        BlockPos absolute = helper.absolutePos(at);
        BlockState state = ((AbstractPipeBlock) block).addConnectionStates(block.defaultBlockState(), helper.getLevel(), absolute, true);
        helper.getLevel().setBlockAndUpdate(absolute, state);
        return at;
    }

    // The same block's state set in place leaves Oritech's network and its neighbours as they are.
    private static void close(GameTestHelper helper, BlockPos at, Direction side) {
        BlockState state = helper.getBlockState(at);
        GenericPipeBlock pipe = (GenericPipeBlock) state.getBlock();
        helper.setBlock(at, state.setValue(pipe.directionToProperty(side), GenericPipeBlock.NO_CONNECTION));
    }

    private static Item pipeItem() {
        return BuiltInRegistries.ITEM.getValue(FLUID_PIPE);
    }

    /** A survival player holding the Pick, standing on the platform, who has sneak-clicked a start at {@code start}. */
    private static ListeningPlayer started(GameTestHelper helper, BlockPos start) {
        var player = new ListeningPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(START.getX() + 0.5, START.getY(), START.getZ() - 1.5)));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.getValue(PICK)));
        click(helper, player, start);
        if (!Objects.equals(stored(helper, start), player.getMainHandItem().get(PFDataComponents.DISMANTLE_START.get()))) {
            helper.fail("a sneak-click on a pipe stored " + player.getMainHandItem().get(PFDataComponents.DISMANTLE_START.get())
                    + " as the start", start);
        }
        return player;
    }

    private static GlobalPos stored(GameTestHelper helper, BlockPos at) {
        return GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(at));
    }

    private static @Nullable DismantlePlan planOf(GameTestHelper helper, ListeningPlayer player, BlockPos end) {
        return FamilyDismantle.plan(helper.getLevel(), player.getMainHandItem(), helper.absolutePos(end));
    }

    private static void click(GameTestHelper helper, ListeningPlayer player, BlockPos at) {
        player.setShiftKeyDown(true);
        BlockPos absolute = helper.absolutePos(at);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
    }

    private static @Nullable BlockPos standing(GameTestHelper helper, List<BlockPos> positions) {
        for (BlockPos pos : positions) {
            if (!helper.getBlockState(pos).isAir()) return pos;
        }
        return null;
    }

    private static Map<BlockPos, BlockState> world(GameTestHelper helper) {
        return BlockPos.betweenClosedStream(helper.getBounds())
                .map(BlockPos::immutable)
                .collect(Collectors.toMap(Function.identity(), pos -> helper.getLevel().getBlockState(pos)));
    }

    /** What the player carries besides the Pick. */
    private static Map<Item, Integer> inventory(ListeningPlayer player) {
        Map<Item, Integer> counts = new HashMap<>();
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty()) counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        counts.remove(player.getMainHandItem().getItem());
        return counts;
    }
}
