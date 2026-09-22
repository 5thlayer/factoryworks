package com.planetaryfactory.core.gametest;

import java.util.ArrayList;
import java.util.List;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.PFItems;
import com.planetaryfactory.core.energy.PoleColumn;
import com.planetaryfactory.core.energy.PoleTier;
import com.planetaryfactory.core.machine.AssemblingMachineBlockEntity;
import com.planetaryfactory.core.machine.AssemblingMachineFootprint;
import com.planetaryfactory.core.fluid.SteamEngineBlockEntity;
import com.planetaryfactory.core.fluid.SteamEngineFootprint;
import com.planetaryfactory.core.mining.rig.RigCorpus;
import com.planetaryfactory.core.mining.rig.RigGeometry;
import com.planetaryfactory.core.mining.rig.RigTier;
import com.planetaryfactory.core.placement.PlacementPlan;
import com.planetaryfactory.core.placement.Placements;
import com.planetaryfactory.core.radar.RadarBlockEntity;
import com.planetaryfactory.core.radar.RadarFootprint;
import rearth.belts.ItemContent;
import rearth.oritech.block.base.block.MultiblockMachine;
import com.planetaryfactory.core.smelting.FurnaceTier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * That a plan is what placing actually does (#297, ADR-0069).
 *
 * <p>This is the one check ADR-0069 asks for by name, and the only one that can exist. The geometry
 * underneath a plan is Minecraft-free and already tested -- {@code RigGeometryTest} for the
 * footprint, {@code PoleColumnTest} for the column -- and whether the preview <em>draws</em> right
 * is a human check on delivery. What is left is the seam the decision exists to protect: a plan
 * that disagrees with the click. That needs a world, a held stack and a real use gesture, so it is
 * here.
 *
 * <p>Each test is the same assertion applied to a different item: ask for the plan, then use the
 * block the way a player does ({@link GameTestHelper#useBlock}, which runs {@code useItemOn} and
 * falls through to {@code place} exactly as the server does), then check the world against what
 * the plan promised. An accepted plan must have put every one of its blocks down, in the state it
 * named; a refused plan must have changed nothing at all. Neither half is redundant: the preview's
 * two failure modes are promising a placement that does not happen and refusing one that does.
 */
final class PlacementPlanTests {

    /** Clear of the platform's edges, and clear of the other tests' fixtures. */
    private static final BlockPos FLOOR = new BlockPos(3, 0, 3);
    private static final BlockPos ABOVE_FLOOR = new BlockPos(3, 1, 3);

    private PlacementPlanTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("plan_matches_placement_for_a_furnace", 20,
                PlacementPlanTests::furnaceMatchesPlacement);
        tests.test("plan_matches_placement_for_a_pole", 20, PlacementPlanTests::poleMatchesPlacement);
        tests.test("plan_extends_a_pole_column", 20, PlacementPlanTests::poleExtendsColumn);
        tests.test("plan_refuses_a_full_pole_column", 20, PlacementPlanTests::poleColumnFull);
        tests.test("plan_refuses_a_blocked_pole_top", 20, PlacementPlanTests::poleBlockedTop);
        tests.test("plan_refuses_another_tier_at_a_pole", 20, PlacementPlanTests::poleWrongTier);
        tests.test("plan_matches_placement_for_a_rig", 20, PlacementPlanTests::rigMatchesPlacement);
        tests.test("plan_refuses_a_rig_whole", 20, PlacementPlanTests::rigRefusesWhole);
        tests.test("plan_matches_placement_for_an_assembling_machine", 20,
                PlacementPlanTests::assemblingMachineMatchesPlacement);
        tests.test("plan_refuses_an_assembling_machine_whole", 20,
                PlacementPlanTests::assemblingMachineRefusesWhole);
        tests.test("plan_matches_placement_for_a_steam_engine", 20,
                PlacementPlanTests::steamEngineMatchesPlacement);
        tests.test("plan_refuses_a_steam_engine_whole", 20,
                PlacementPlanTests::steamEngineRefusesWhole);
        tests.test("plan_matches_placement_for_a_radar", 20,
                PlacementPlanTests::radarMatchesPlacement);
        tests.test("plan_refuses_a_radar_whole", 20,
                PlacementPlanTests::radarRefusesWhole);
        tests.test("plan_matches_placement_for_a_boiler", 20,
                PlacementPlanTests::boilerMatchesPlacement);
        tests.test("plan_refuses_a_pump_on_a_dry_site", 20,
                PlacementPlanTests::pumpRefusesADrySite);
    }

    // ---- the cases -------------------------------------------------------------------------

    /** A plain BlockItem: no pack code at all, and the facing is the one vanilla just decided. */
    private static void furnaceMatchesPlacement(GameTestHelper helper) {
        check(helper, new ItemStack(PFBlocks.furnace(FurnaceTier.values()[0]).get()),
                FLOOR, Direction.UP, false);
        helper.succeed();
    }

    /** The pole placing normally, on the ground, which is still its own item's plan. */
    private static void poleMatchesPlacement(GameTestHelper helper) {
        check(helper, pole(PoleTier.SMALL), FLOOR, Direction.UP, false);
        helper.succeed();
    }

    /**
     * The extension: aimed at the <em>base</em>, and the plan must name the top, not the side.
     * Aiming at the base is the case that matters -- it is how a player raises a pole past their
     * own reach, and a preview drawn where vanilla would have put the block would describe a
     * placement the pack does not perform.
     */
    private static void poleExtendsColumn(GameTestHelper helper) {
        column(helper, 2);
        PlacementPlan plan = check(helper, pole(PoleTier.SMALL), ABOVE_FLOOR, Direction.NORTH, false);
        BlockPos placed = plan.blocks().getFirst().pos();
        if (placed.getY() != helper.absolutePos(ABOVE_FLOOR).getY() + 2) {
            helper.fail("a pole aimed at a column's base planned a segment somewhere other than "
                    + "the top of the column", ABOVE_FLOOR);
        }
        helper.succeed();
    }

    private static void poleColumnFull(GameTestHelper helper) {
        column(helper, PoleColumn.MAX_SEGMENTS);
        refusal(check(helper, pole(PoleTier.SMALL), ABOVE_FLOOR, Direction.NORTH, true),
                PlacementPlan.Refusal.COLUMN_FULL, helper);
        helper.succeed();
    }

    private static void poleBlockedTop(GameTestHelper helper) {
        column(helper, 1);
        helper.setBlock(ABOVE_FLOOR.above(), Blocks.STONE);
        refusal(check(helper, pole(PoleTier.SMALL), ABOVE_FLOOR, Direction.NORTH, true),
                PlacementPlan.Refusal.BLOCKED_TOP, helper);
        helper.succeed();
    }

    /**
     * Not fast replace (ADR-0069): a different tier aimed at a column is refused, not placed beside
     * it.
     *
     * <p>The column is three tall on purpose. The refusal is drawn at the top of the column the
     * player aimed at, and on a one-tall column "the top of that column" and "just above the block
     * I hit" are the same block -- so a one-tall fixture cannot tell a correct answer from a walk
     * that gave up.
     */
    private static void poleWrongTier(GameTestHelper helper) {
        column(helper, 3);
        PlacementPlan plan = check(helper, pole(PoleTier.MEDIUM), ABOVE_FLOOR, Direction.NORTH, true);
        refusal(plan, PlacementPlan.Refusal.WRONG_TIER, helper);
        BlockPos refusedAt = plan.blocks().getFirst().pos();
        if (refusedAt.getY() != helper.absolutePos(ABOVE_FLOOR).getY() + 3) {
            helper.fail("the wrong-tier refusal was drawn somewhere other than the top of the "
                    + "column that was aimed at", ABOVE_FLOOR);
        }
        helper.succeed();
    }

    /**
     * The multiblock: every position the plan names must be down, anchor and parts alike, and the
     * plan must name the <em>whole</em> footprint.
     *
     * <p>The size is checked against {@link RigGeometry} rather than against a number or a floor:
     * a plan naming two of a burner rig's eight positions would place two blocks, agree with itself
     * and pass every other assertion here, while the preview drew a quarter of the machine.
     */
    private static void rigMatchesPlacement(GameTestHelper helper) {
        PlacementPlan plan = check(helper, new ItemStack(PFItems.rig(RigTier.BURNER).get()),
                FLOOR, Direction.UP, false);
        RigCorpus.Row row = RigCorpus.get().rowOf(RigTier.BURNER);
        int expected = row.width() * row.height() * RigTier.BURNER.blocksTall();
        if (plan.blocks().size() != expected) {
            helper.fail("a burner rig's plan named " + plan.blocks().size()
                    + " blocks where its footprint is " + expected, FLOOR);
        }
        helper.succeed();
    }

    /**
     * One blocked position refuses the whole footprint, and nothing at all goes down. The
     * obstruction is placed a block <em>up</em>, which is the case a player cannot see: the rig's
     * vertical extent means it fails on a block above their line of sight.
     */
    private static void rigRefusesWhole(GameTestHelper helper) {
        helper.setBlock(ABOVE_FLOOR.above(), Blocks.STONE);
        refusal(check(helper, new ItemStack(PFItems.rig(RigTier.BURNER).get()),
                FLOOR, Direction.UP, true), PlacementPlan.Refusal.FOOTPRINT_BLOCKED, helper);
        helper.succeed();
    }

    /**
     * The Assembling Machine's footprint (#326): the plan names all of it, and placing puts every
     * block down in the state named -- the anchor already {@code ASSEMBLED}, each part numbered.
     *
     * <p>The size is compared against {@link AssemblingMachineFootprint} for the rig's reason: a
     * plan naming only the anchor would place one block and agree with itself. And the anchor's
     * block entity is asked whether it answers as assembled, because Oritech's tick returns early
     * on an unassembled machine and nothing else here would notice.
     */
    private static void assemblingMachineMatchesPlacement(GameTestHelper helper) {
        PlacementPlan plan = check(helper, new ItemStack(PFItems.ASSEMBLING_MACHINE.get()),
                FLOOR, Direction.UP, false);
        int expected = AssemblingMachineFootprint.FOOTPRINT.offsets().size();
        if (plan.blocks().size() != expected) {
            helper.fail("an Assembling Machine's plan named " + plan.blocks().size()
                    + " blocks where its footprint is " + expected, FLOOR);
        }
        BlockPos anchor = plan.blocks().getFirst().pos();
        if (!(helper.getLevel().getBlockEntity(anchor) instanceof AssemblingMachineBlockEntity)) {
            helper.fail("the placed anchor holds no Assembling Machine block entity", helper.relativePos(anchor));
        }
        // Read again ticks later, not only on the placing tick: Oritech's onLoad schedules a
        // rescan for the next tick, and a rescan that finds no cores clears ASSEMBLED -- after
        // which every right-click replays the setup animation and never opens the screen.
        helper.runAfterDelay(5, () -> {
            if (!helper.getLevel().getBlockState(anchor).getValue(MultiblockMachine.ASSEMBLED)) {
                helper.fail("the anchor lost ASSEMBLED after it was placed", helper.relativePos(anchor));
            }
            helper.succeed();
        });
    }

    /**
     * One taken position refuses the whole machine, and nothing goes down. The obstruction is the
     * block above the anchor, in the machine's upper row, rather than on the ground the player is
     * aiming at.
     */
    private static void assemblingMachineRefusesWhole(GameTestHelper helper) {
        helper.setBlock(ABOVE_FLOOR.above(AssemblingMachineFootprint.TALL - 1), Blocks.STONE);
        refusal(check(helper, new ItemStack(PFItems.ASSEMBLING_MACHINE.get()),
                FLOOR, Direction.UP, true), PlacementPlan.Refusal.FOOTPRINT_BLOCKED, helper);
        helper.succeed();
    }

    /**
     * The Steam Engine's footprint (ADR-0077), for the Assembling Machine's reasons: the whole of it,
     * and an anchor still {@code ASSEMBLED} after Oritech's next-tick rescan.
     */
    private static void steamEngineMatchesPlacement(GameTestHelper helper) {
        PlacementPlan plan = check(helper, new ItemStack(PFItems.STEAM_ENGINE.get()),
                FLOOR, Direction.UP, false);
        int expected = SteamEngineFootprint.FOOTPRINT.offsets().size();
        if (plan.blocks().size() != expected) {
            helper.fail("a Steam Engine's plan named " + plan.blocks().size()
                    + " blocks where its footprint is " + expected, FLOOR);
        }
        BlockPos anchor = plan.blocks().getFirst().pos();
        if (!(helper.getLevel().getBlockEntity(anchor) instanceof SteamEngineBlockEntity)) {
            helper.fail("the placed anchor holds no Steam Engine block entity", helper.relativePos(anchor));
        }
        helper.runAfterDelay(5, () -> {
            if (!helper.getLevel().getBlockState(anchor).getValue(MultiblockMachine.ASSEMBLED)) {
                helper.fail("the anchor lost ASSEMBLED after it was placed", helper.relativePos(anchor));
            }
            helper.succeed();
        });
    }

    /** One taken position, in the engine's upper row, refuses the whole engine. */
    private static void steamEngineRefusesWhole(GameTestHelper helper) {
        helper.setBlock(ABOVE_FLOOR.above(), Blocks.STONE);
        refusal(check(helper, new ItemStack(PFItems.STEAM_ENGINE.get()),
                FLOOR, Direction.UP, true), PlacementPlan.Refusal.FOOTPRINT_BLOCKED, helper);
        helper.succeed();
    }

    /** The Radar's 3x3x3 (#368): every block of it, the anchor holding the Radar's block entity. */
    private static void radarMatchesPlacement(GameTestHelper helper) {
        PlacementPlan plan = check(helper, new ItemStack(PFItems.RADAR.get()), FLOOR, Direction.UP, false);
        int expected = RadarFootprint.FOOTPRINT.offsets().size();
        if (plan.blocks().size() != expected) {
            helper.fail("a Radar's plan named " + plan.blocks().size()
                    + " blocks where its footprint is " + expected, FLOOR);
        }
        BlockPos anchor = plan.blocks().getFirst().pos();
        if (!(helper.getLevel().getBlockEntity(anchor) instanceof RadarBlockEntity)) {
            helper.fail("the placed anchor holds no Radar block entity", helper.relativePos(anchor));
        }
        helper.succeed();
    }

    /** One taken position, in the top layer's corner, refuses the whole Radar. */
    private static void radarRefusesWhole(GameTestHelper helper) {
        helper.setBlock(ABOVE_FLOOR.offset(1, 2, 1), Blocks.STONE);
        refusal(check(helper, new ItemStack(PFItems.RADAR.get()),
                FLOOR, Direction.UP, true), PlacementPlan.Refusal.FOOTPRINT_BLOCKED, helper);
        helper.succeed();
    }

    /**
     * The Boiler, the other plain BlockItem #297 names. It is not the furnace with a different id:
     * the furnace is a ladder of tiers off an enum and the Boiler is one block, and "no per-block
     * code" is a claim about both shapes.
     */
    private static void boilerMatchesPlacement(GameTestHelper helper) {
        check(helper, new ItemStack(PFBlocks.BOILER.get()), FLOOR, Direction.UP, false);
        helper.succeed();
    }

    /**
     * The pump's dry site, which is the refusal that used to be decided twice at two different
     * positions -- once in the plan and once in {@code place} (ADR-0069). Nothing here has water,
     * so the plan must refuse and the click must place nothing.
     */
    private static void pumpRefusesADrySite(GameTestHelper helper) {
        refusal(check(helper, new ItemStack(PFBlocks.OFFSHORE_PUMP.get()), FLOOR, Direction.UP, true),
                PlacementPlan.Refusal.NO_FLUID_SOURCE, helper);
        helper.succeed();
    }

    // ---- the one assertion -----------------------------------------------------------------

    /**
     * Ask for the plan, use the block, and hold the world to what the plan said.
     *
     * <p>The world is read <em>before</em> the gesture as well as after, because "a refused plan
     * changed nothing" is not the same claim as "the refused positions are empty": one of them is
     * already true of the block the player is aiming at.
     */
    private static PlacementPlan check(GameTestHelper helper, ItemStack stack, BlockPos target,
                                       Direction face, boolean expectRefused) {
        return check(helper, helper.makeMockPlayer(GameType.SURVIVAL), stack, target, face, expectRefused);
    }

    private static PlacementPlan check(GameTestHelper helper, Player player, ItemStack stack, BlockPos target,
                                       Direction face, boolean expectRefused) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos absolute = helper.absolutePos(target);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false);

        PlacementPlan plan = Placements.planFor(helper.getLevel(), player,
                InteractionHand.MAIN_HAND, stack, hit);
        if (plan == null) {
            helper.fail("no plan at all where one was expected", target);
            throw new IllegalStateException("unreachable");
        }
        if (plan.isRefused() != expectRefused) {
            helper.fail("the plan " + (plan.isRefused() ? "refused" : "accepted")
                    + " where the opposite was expected", target);
        }

        List<BlockState> before = new ArrayList<>(plan.blocks().size());
        for (PlacementPlan.Placed placed : plan.blocks()) {
            before.add(helper.getLevel().getBlockState(placed.pos()));
        }

        helper.useBlock(target, player, hit);

        for (int i = 0; i < plan.blocks().size(); i++) {
            PlacementPlan.Placed placed = plan.blocks().get(i);
            BlockState now = helper.getLevel().getBlockState(placed.pos());
            BlockPos relative = helper.relativePos(placed.pos());
            if (plan.isRefused()) {
                if (!now.equals(before.get(i))) {
                    helper.fail("a refused plan changed the world at one of its positions", relative);
                }
            } else if (!now.equals(placed.state())) {
                helper.fail("the plan promised " + placed.state() + " but placing left " + now,
                        relative);
            }
        }
        return plan;
    }

    private static void refusal(PlacementPlan plan, PlacementPlan.Refusal expected,
                                GameTestHelper helper) {
        if (plan.refusal() != expected) {
            helper.fail("expected the refusal " + expected + " but the plan gave " + plan.refusal(),
                    ABOVE_FLOOR);
        }
    }

    /** The fork's splitter (#355), registered only when the fork is loaded, since it names the fork's types. */
    static final class Splitters {

        private static final int HALVES = 2;

        private Splitters() {
        }

        static void register(PFGameTests.Registrar tests) {
            tests.test("plan_matches_placement_for_a_splitter", 20, helper -> {
                PlacementPlan plan = check(helper, facingSouth(helper), new ItemStack(ItemContent.SPLITTER.get()),
                        FLOOR, Direction.UP, false);
                if (plan.blocks().size() != HALVES) {
                    helper.fail("a splitter's plan named " + plan.blocks().size() + " blocks", FLOOR);
                }
                helper.succeed();
            });
            tests.test("plan_refuses_a_splitter_blocked_at_its_second_half", 20, helper -> {
                // A south-facing splitter's right half is west of its left.
                helper.setBlock(ABOVE_FLOOR.west(), Blocks.STONE);
                PlacementPlan plan = check(helper, facingSouth(helper), new ItemStack(ItemContent.SPLITTER.get()),
                        FLOOR, Direction.UP, true);
                refusal(plan, PlacementPlan.Refusal.FOOTPRINT_BLOCKED, helper);
                if (plan.blocks().size() != HALVES) {
                    helper.fail("a refused splitter's plan named " + plan.blocks().size() + " blocks", FLOOR);
                }
                helper.succeed();
            });
        }

        private static Player facingSouth(GameTestHelper helper) {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setYRot(0);
            return player;
        }
    }

    // ---- fixtures --------------------------------------------------------------------------

    private static ItemStack pole(PoleTier tier) {
        return new ItemStack(PFBlocks.pole(tier).get());
    }

    /** A small-pole column {@code segments} tall, standing on the floor. */
    private static void column(GameTestHelper helper, int segments) {
        for (int i = 0; i < segments; i++) {
            helper.setBlock(ABOVE_FLOOR.above(i), PFBlocks.pole(PoleTier.SMALL).get());
        }
    }
}
