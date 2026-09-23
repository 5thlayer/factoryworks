package com.planetaryfactory.core.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.PFItems;
import com.planetaryfactory.core.machine.AssemblingTier;
import com.planetaryfactory.core.energy.LevelWires;
import com.planetaryfactory.core.energy.PoleColumn;
import com.planetaryfactory.core.energy.PoleLinks;
import com.planetaryfactory.core.energy.SupplyAreaPoleBlockEntity;
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
import com.planetaryfactory.core.oil.PumpjackBlockEntity;
import com.planetaryfactory.core.oil.PumpjackFootprint;
import com.planetaryfactory.core.radar.RadarBlockEntity;
import com.planetaryfactory.core.radar.RadarFootprint;
import rearth.belts.BlockContent;
import rearth.belts.ItemContent;
import rearth.belts.blocks.ChuteBlockEntity;
import rearth.belts.items.SplitterItem;
import rearth.belts.model.BeltPath;
import rearth.belts.model.BeltTier;
import rearth.oritech.block.base.block.MultiblockMachine;
import com.planetaryfactory.core.smelting.FurnaceBlock;
import com.planetaryfactory.core.smelting.FurnaceBlockEntity;
import com.planetaryfactory.core.smelting.FurnaceSlots;
import com.planetaryfactory.core.smelting.FurnaceTier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
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
        tests.test("plan_refuses_another_group_at_a_pole", 20, PlacementPlanTests::poleOtherGroup);
        tests.test("plan_matches_placement_for_a_rig", 20, PlacementPlanTests::rigMatchesPlacement);
        tests.test("plan_refuses_a_rig_whole", 20, PlacementPlanTests::rigRefusesWhole);
        tests.test("plan_matches_placement_for_an_assembling_machine", 20,
                helper -> assemblingMachineMatchesPlacement(helper, AssemblingTier.ONE));
        tests.test("plan_matches_placement_for_an_assembling_machine_2", 20,
                helper -> assemblingMachineMatchesPlacement(helper, AssemblingTier.TWO));
        tests.test("plan_matches_placement_for_an_assembling_machine_3", 20,
                helper -> assemblingMachineMatchesPlacement(helper, AssemblingTier.THREE));
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
        tests.test("plan_matches_placement_for_a_pumpjack_on_a_well", 20,
                PlacementPlanTests::pumpjackMatchesPlacementOnAWell);
        tests.test("plan_refuses_a_pumpjack_off_a_well", 20,
                PlacementPlanTests::pumpjackRefusesOffAWell);
        tests.test("plan_matches_placement_for_a_boiler", 20,
                PlacementPlanTests::boilerMatchesPlacement);
        tests.test("plan_refuses_a_pump_on_a_dry_site", 20,
                PlacementPlanTests::pumpRefusesADrySite);
        Replaces.register(tests);
        PoleReplaces.register(tests);
    }

    // ---- the cases -------------------------------------------------------------------------

    /** Aimed at the floor, a furnace's plan is vanilla's, and the facing the one vanilla decided. */
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
     * A substation is alone in its Replace Group (ADR-0082), so aimed at a small column it is
     * refused, not placed beside it.
     *
     * <p>The column is three tall on purpose. The refusal is drawn at the top of the column the
     * player aimed at, and on a one-tall column "the top of that column" and "just above the block
     * I hit" are the same block -- so a one-tall fixture cannot tell a correct answer from a walk
     * that gave up.
     */
    private static void poleOtherGroup(GameTestHelper helper) {
        column(helper, 3);
        PlacementPlan plan = check(helper, pole(PoleTier.SUBSTATION), ABOVE_FLOOR, Direction.NORTH, true);
        refusal(plan, PlacementPlan.Refusal.OTHER_REPLACE_GROUP, helper);
        BlockPos refusedAt = plan.blocks().getFirst().pos();
        if (refusedAt.getY() != helper.absolutePos(ABOVE_FLOOR).getY() + 3) {
            helper.fail("the other-group refusal was drawn somewhere other than the top of the "
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
    private static void assemblingMachineMatchesPlacement(GameTestHelper helper, AssemblingTier tier) {
        PlacementPlan plan = check(helper, new ItemStack(PFItems.assemblingMachine(tier).get()),
                FLOOR, Direction.UP, false);
        int expected = AssemblingMachineFootprint.FOOTPRINT.offsets().size();
        if (plan.blocks().size() != expected) {
            helper.fail("an Assembling Machine's plan named " + plan.blocks().size()
                    + " blocks where its footprint is " + expected, FLOOR);
        }
        BlockPos anchor = plan.blocks().getFirst().pos();
        if (!(helper.getLevel().getBlockEntity(anchor) instanceof AssemblingMachineBlockEntity machine)
                || machine.tier() != tier) {
            helper.fail("the placed anchor holds no tier " + tier + " Assembling Machine", helper.relativePos(anchor));
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
        refusal(check(helper, new ItemStack(PFItems.assemblingMachine(AssemblingTier.ONE).get()),
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

    /** A Pumpjack's 3x3x3 over an oil well (ADR-0081), the anchor on the well holding its block entity. */
    private static void pumpjackMatchesPlacementOnAWell(GameTestHelper helper) {
        helper.setBlock(FLOOR, PFBlocks.OIL_WELL.get());
        PlacementPlan plan = check(helper, new ItemStack(PFItems.PUMPJACK.get()), FLOOR, Direction.UP, false);
        if (plan.blocks().size() != PumpjackFootprint.FOOTPRINT.offsets().size()) {
            helper.fail("a Pumpjack's plan named " + plan.blocks().size() + " blocks", FLOOR);
        }
        BlockPos anchor = plan.blocks().getFirst().pos();
        if (!(helper.getLevel().getBlockEntity(anchor) instanceof PumpjackBlockEntity pumpjack)
                || pumpjack.well() == null) {
            helper.fail("the placed anchor is not a Pumpjack over the well", helper.relativePos(anchor));
        }
        helper.succeed();
    }

    /** The same click on stone refuses the whole Pumpjack and changes nothing. */
    private static void pumpjackRefusesOffAWell(GameTestHelper helper) {
        refusal(check(helper, new ItemStack(PFItems.PUMPJACK.get()),
                FLOOR, Direction.UP, true), PlacementPlan.Refusal.NOT_ON_WELL, helper);
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

    /**
     * The fork's splitter (#355), registered only when the fork is loaded, since it names the fork's
     * types. Placed across a belt, its plan names the belts it cuts, or why it cuts none (#361).
     */
    static final class Splitters {

        private static final int HALVES = 2;
        private static final BlockPos CUT = new BlockPos(6, 1, 2);
        private static final BlockPos FROM = new BlockPos(2, 1, 2);
        private static final BlockPos TO = new BlockPos(10, 1, 2);

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
            registerCuts(tests);
        }

        private static void registerCuts(PFGameTests.Registrar tests) {
            tests.test("plan_cuts_exactly_the_belts_a_splitter_names", 20, helper -> {
                belt(helper, FROM, Direction.EAST, TO, Direction.WEST);
                BlockPos besideFrom = new BlockPos(2, 1, 4);
                BlockPos besideTo = new BlockPos(10, 1, 4);
                belt(helper, besideFrom, Direction.EAST, besideTo, Direction.WEST);
                helper.startSequence().thenIdle(2).thenExecute(() -> {
                    ListeningPlayer player = facingEast(helper);
                    SplitterItem.Plan plan = forkPlan(helper, player, CUT);
                    List<BlockPos> named = plan.cuts().stream().map(SplitterItem.Cut::source).toList();
                    if (!named.equals(List.of(helper.absolutePos(FROM)))
                            || !plan.cuts().getFirst().half().equals(helper.absolutePos(CUT))) {
                        helper.fail("the plan names the cuts " + plan.cuts() + ", not only the belt from " + FROM, CUT);
                    }
                    check(helper, player, player.getMainHandItem(), CUT.below(), Direction.UP, false);
                    if (!helper.absolutePos(CUT).equals(chute(helper, FROM).getTarget())
                            || !helper.absolutePos(TO).equals(chute(helper, CUT).getTarget())) {
                        helper.fail("the belt the plan named was not cut at the splitter", CUT);
                    }
                    if (!helper.absolutePos(besideTo).equals(chute(helper, besideFrom).getTarget())) {
                        helper.fail("a belt the plan did not name was cut", besideFrom);
                    }
                    BlockPos right = CUT.relative(Direction.EAST.getClockWise());
                    if (chute(helper, right).getTarget() != null) {
                        helper.fail("the half no belt crosses starts one", right);
                    }
                }).thenSucceed();
            });
            tests.test("plan_refuses_a_splitter_against_a_belts_flow", 20, helper -> {
                belt(helper, TO, Direction.WEST, FROM, Direction.EAST);
                refusesCut(helper, CUT, TO, FROM, BeltPath.CrossingRefusal.AGAINST_FACING);
            });
            tests.test("plan_refuses_a_splitter_a_belt_crosses_at_an_angle", 20, helper -> {
                // A lane change through the right half, wide of the left.
                BlockPos from = new BlockPos(2, 1, 3);
                BlockPos aside = new BlockPos(10, 1, 4);
                belt(helper, from, Direction.EAST, aside, Direction.WEST);
                refusesCut(helper, CUT, from, aside, BeltPath.CrossingRefusal.AT_ANGLE);
            });
            tests.test("plan_refuses_a_splitter_a_belt_crosses_through_its_side", 20, helper -> {
                BlockPos north = new BlockPos(6, 1, 0);
                BlockPos south = new BlockPos(6, 1, 5);
                belt(helper, north, Direction.SOUTH, south, Direction.NORTH);
                refusesCut(helper, CUT, north, south, BeltPath.CrossingRefusal.THROUGH_SIDE);
            });
            tests.test("plan_refuses_a_splitter_on_a_curved_belt", 20, helper -> {
                BlockPos turned = new BlockPos(6, 1, 6);
                belt(helper, FROM, Direction.EAST, turned, Direction.NORTH);
                refusesCut(helper, new BlockPos(3, 1, 2), FROM, turned, BeltPath.CrossingRefusal.CURVED);
            });
        }

        /**
         * The belt stays as it was, no block and no slot changes, and the player is told why. The
         * world is read before the click as well as after, since "nothing changed" is not "empty".
         */
        private static void refusesCut(GameTestHelper helper, BlockPos left, BlockPos from, BlockPos to,
                                       BeltPath.CrossingRefusal expected) {
            helper.startSequence().thenIdle(2).thenExecute(() -> {
                ListeningPlayer player = facingEast(helper);
                SplitterItem.Plan plan = forkPlan(helper, player, left);
                if (plan.crossing() != expected) {
                    helper.fail("the splitter's plan refuses with " + plan.crossing() + ", not " + expected, left);
                }
                ChuteBlockEntity source = chute(helper, from);
                int cost = source.getCost();
                Map<BlockPos, BlockState> before = world(helper);
                List<ItemStack> carried = inventory(player);
                player.heard.clear();

                PlacementPlan placement = check(helper, player, player.getMainHandItem(), left.below(), Direction.UP, true);
                refusal(placement, PlacementPlan.Refusal.BELT_CROSSING, helper);

                Map<BlockPos, BlockState> after = world(helper);
                if (!before.equals(after)) helper.fail("a refused splitter changed a block", left);
                List<ItemStack> now = inventory(player);
                for (int slot = 0; slot < carried.size(); slot++) {
                    if (!ItemStack.matches(carried.get(slot), now.get(slot))) {
                        helper.fail("a refused splitter changed the player's slot " + slot, left);
                    }
                }
                if (!helper.absolutePos(to).equals(source.getTarget()) || source.getCost() != cost) {
                    helper.fail("a refused splitter changed the belt it crosses", from);
                }
                if (!player.heard.equals(List.of(expected.messageKey()))) {
                    helper.fail("the player was told " + player.heard + ", not " + expected.messageKey(), left);
                }
            }).thenSucceed();
        }

        private static void belt(GameTestHelper helper, BlockPos from, Direction fromFacing, BlockPos to, Direction toFacing) {
            helper.setBlock(from, BlockContent.loaderFor(BeltTier.BELT).defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, fromFacing));
            helper.setBlock(to, BlockContent.loaderFor(BeltTier.BELT).defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, toFacing));
            chute(helper, from).assignFromBeltItem(helper.absolutePos(to), List.of(), BeltTier.BELT, 9)
                    .ifPresent(refusal -> helper.fail("the fixture's belt is refused: " + refusal, from));
        }

        private static SplitterItem.Plan forkPlan(GameTestHelper helper, Player player, BlockPos left) {
            ItemStack stack = player.getMainHandItem();
            BlockPos absolute = helper.absolutePos(left.below());
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
            SplitterItem.Plan plan = ((SplitterItem) stack.getItem())
                    .plan(new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
            if (plan == null) {
                helper.fail("the splitter planned nothing", left);
                throw new IllegalStateException("unreachable");
            }
            return plan;
        }

        private static ListeningPlayer facingEast(GameTestHelper helper) {
            ListeningPlayer player = new ListeningPlayer(helper);
            player.setGameMode(GameType.SURVIVAL);
            player.setYRot(-90);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemContent.SPLITTER.get()));
            return player;
        }

        private static ChuteBlockEntity chute(GameTestHelper helper, BlockPos pos) {
            return helper.getBlockEntity(pos, ChuteBlockEntity.class);
        }

        private static Map<BlockPos, BlockState> world(GameTestHelper helper) {
            return BlockPos.betweenClosedStream(helper.getBounds())
                    .map(BlockPos::immutable)
                    .collect(Collectors.toMap(Function.identity(), pos -> helper.getLevel().getBlockState(pos)));
        }

        static List<ItemStack> inventory(Player player) {
            List<ItemStack> slots = new ArrayList<>();
            for (ItemStack stack : player.getInventory()) slots.add(stack.copy());
            return slots;
        }

        private static Player facingSouth(GameTestHelper helper) {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setYRot(0);
            return player;
        }
    }

    /**
     * Fast Replace on the furnaces (#388, ADR-0082): the plan names the furnace it replaces, the
     * click swaps it in place, and the world and the inventory are held to what the plan said.
     */
    static final class Replaces {

        private static final BlockPos AT = new BlockPos(3, 1, 3);
        private static final Direction FACING = Direction.EAST;
        private static final int STONE_PROGRESS = 40;
        private static final int STONE_DURATION = 64;
        private static final int STEEL_PROGRESS = 20;
        private static final int STEEL_DURATION = 32;
        private static final int JOULES = 1_000_000;
        private static final int LIT_JOULES = 4_000_000;

        private Replaces() {
        }

        static void register(PFGameTests.Registrar tests) {
            tests.test("replace_stone_furnace_with_steel_and_back", 20, Replaces::burnerAndBack);
            tests.test("replace_burner_furnace_with_electric_and_back", 20, Replaces::electricAndBack);
            tests.test("replace_with_the_last_item_returns_into_the_freed_slot", 20, Replaces::lastItem);
            tests.test("replace_refused_with_no_room_changes_nothing", 20,
                    helper -> noRoom(helper, stack(FurnaceTier.STEEL, 2)));
            tests.test("replace_refused_with_no_room_for_the_fuel", 20, Replaces::noRoomForFuel);
            tests.test("replace_ignores_a_block_of_another_group", 20, Replaces::otherGroup);
            tests.test("replace_sneak_places_beside", 20, Replaces::sneakPlacesBeside);
            tests.test("replace_same_tier_places_nothing", 20, Replaces::sameTier);
        }

        private static void burnerAndBack(GameTestHelper helper) {
            burner(helper, FurnaceTier.STONE);
            ListeningPlayer player = new ListeningPlayer(helper);
            replace(helper, player, stack(FurnaceTier.STEEL, 2), FurnaceTier.STEEL);
            holds(helper, 5, STEEL_PROGRESS, STEEL_DURATION, JOULES);
            spent(helper, player, FurnaceTier.STEEL, 1);
            returned(helper, player, FurnaceTier.STONE, 1);

            player.getInventory().clearContent();
            replace(helper, player, stack(FurnaceTier.STONE, 2), FurnaceTier.STONE);
            holds(helper, 5, STONE_PROGRESS, STONE_DURATION, JOULES);
            spent(helper, player, FurnaceTier.STONE, 1);
            returned(helper, player, FurnaceTier.STEEL, 1);
            helper.succeed();
        }

        /** The fuel slot has no counterpart on the Electric tier, and neither buffer crosses. */
        private static void electricAndBack(GameTestHelper helper) {
            burner(helper, FurnaceTier.STONE);
            ListeningPlayer player = new ListeningPlayer(helper);
            replace(helper, player, stack(FurnaceTier.ELECTRIC, 2), FurnaceTier.ELECTRIC);
            holds(helper, 0, STEEL_PROGRESS, STEEL_DURATION, 0);
            spent(helper, player, FurnaceTier.ELECTRIC, 1);
            returned(helper, player, FurnaceTier.STONE, 1);
            if (player.getInventory().countItem(Items.COAL) != 5) {
                helper.fail("the fuel slot's 5 coal did not come back to the player", AT);
            }

            furnace(helper).data().set(FurnaceBlockEntity.DATA_ENERGY, 500);
            player.getInventory().clearContent();
            replace(helper, player, stack(FurnaceTier.STONE, 2), FurnaceTier.STONE);
            holds(helper, 0, STONE_PROGRESS, STONE_DURATION, 0);
            spent(helper, player, FurnaceTier.STONE, 1);
            returned(helper, player, FurnaceTier.ELECTRIC, 1);
            helper.succeed();
        }

        private static void lastItem(GameTestHelper helper) {
            burner(helper, FurnaceTier.STONE);
            ListeningPlayer player = new ListeningPlayer(helper);
            fill(player);
            replace(helper, player, stack(FurnaceTier.STEEL, 1), FurnaceTier.STEEL);
            ItemStack hand = player.getMainHandItem();
            if (!hand.is(PFBlocks.furnace(FurnaceTier.STONE).get().asItem()) || hand.getCount() != 1) {
                helper.fail("the freed slot holds " + hand + " rather than the stone furnace", AT);
            }
            helper.succeed();
        }

        /** The last Electric Furnace frees a slot for the stone one, but the coal has none. */
        private static void noRoomForFuel(GameTestHelper helper) {
            noRoom(helper, stack(FurnaceTier.ELECTRIC, 1));
        }

        private static void noRoom(GameTestHelper helper, ItemStack held) {
            burner(helper, FurnaceTier.STONE);
            ListeningPlayer player = new ListeningPlayer(helper);
            fill(player);
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            BlockHitResult hit = hit(helper, Direction.NORTH);
            PlacementPlan plan = plan(helper, player, hit);
            if (plan.refusal() != PlacementPlan.Refusal.NO_ROOM_TO_RETURN || !plan.isReplace()) {
                helper.fail("a full inventory planned " + plan, AT);
            }
            BlockState before = helper.getBlockState(AT);
            BlockState beside = helper.getBlockState(AT.north());
            List<ItemStack> contents = contents(furnace(helper));
            List<ItemStack> inventory = inventory(player);

            helper.useBlock(AT, player, hit);

            if (!helper.getBlockState(AT).equals(before)
                    || !helper.getBlockState(AT.north()).equals(beside)
                    || !ItemStack.listMatches(contents(furnace(helper)), contents)
                    || !ItemStack.listMatches(inventory(player), inventory)) {
                helper.fail("a refused replace changed the world or the inventory", AT);
            }
            if (!player.heard.contains("message.planetaryfactory.replace.no_room")) {
                helper.fail("a refused replace named no reason on the action bar", AT);
            }
            helper.succeed();
        }

        private static void otherGroup(GameTestHelper helper) {
            burner(helper, FurnaceTier.STONE);
            ListeningPlayer player = new ListeningPlayer(helper);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PFBlocks.BOILER.get(), 2));
            BlockHitResult hit = hit(helper, Direction.NORTH);
            PlacementPlan plan = Placements.planFor(helper.getLevel(), player,
                    InteractionHand.MAIN_HAND, player.getMainHandItem(), hit);
            if (plan != null && plan.isReplace()) {
                helper.fail("a Boiler planned to replace a furnace", AT);
            }
            unchangedAfterClick(helper, player, hit, 2);
            helper.succeed();
        }

        private static void sameTier(GameTestHelper helper) {
            burner(helper, FurnaceTier.STONE);
            ListeningPlayer player = new ListeningPlayer(helper);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack(FurnaceTier.STONE, 2));
            BlockHitResult hit = hit(helper, Direction.NORTH);
            if (plan(helper, player, hit).isReplace()) {
                helper.fail("a stone furnace planned to replace a stone furnace", AT);
            }
            unchangedAfterClick(helper, player, hit, 2);
            helper.succeed();
        }

        /** Through the game mode, which is where a sneak skips the block's use. */
        private static void sneakPlacesBeside(GameTestHelper helper) {
            burner(helper, FurnaceTier.STONE);
            ListeningPlayer player = new ListeningPlayer(helper);
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack(FurnaceTier.STEEL, 2));
            BlockHitResult hit = hit(helper, Direction.UP);
            PlacementPlan plan = plan(helper, player, hit);
            BlockPos above = helper.absolutePos(AT.above());
            if (plan.isReplace() || plan.isRefused() || !plan.blocks().getFirst().pos().equals(above)) {
                helper.fail("a sneaking player's plan was not a placement on top: " + plan, AT);
            }
            player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(),
                    InteractionHand.MAIN_HAND, hit);
            player.setShiftKeyDown(false);
            helper.assertBlockPresent(PFBlocks.furnace(FurnaceTier.STONE).get(), AT);
            if (!helper.getLevel().getBlockState(above).equals(plan.blocks().getFirst().state())) {
                helper.fail("sneak-placing left " + helper.getLevel().getBlockState(above), AT.above());
            }
            helper.succeed();
        }

        // ---- the gesture -------------------------------------------------------------------

        private static void replace(GameTestHelper helper, ListeningPlayer player, ItemStack stack,
                                    FurnaceTier to) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            BlockHitResult hit = hit(helper, Direction.NORTH);
            PlacementPlan plan = plan(helper, player, hit);
            BlockPos absolute = helper.absolutePos(AT);
            BlockState expected = PFBlocks.furnace(to).get().defaultBlockState()
                    .setValue(FurnaceBlock.FACING, FACING);
            if (plan.isRefused() || !plan.replaces().equals(List.of(absolute))
                    || plan.blocks().size() != 1
                    || !plan.blocks().getFirst().equals(new PlacementPlan.Placed(absolute, expected))) {
                helper.fail("the plan was not a replace of the furnace by " + to + ": " + plan, AT);
            }
            helper.useBlock(AT, player, hit);
            if (!helper.getBlockState(AT).equals(expected)) {
                helper.fail("the replace left " + helper.getBlockState(AT) + ", not " + expected, AT);
            }
        }

        private static void unchangedAfterClick(GameTestHelper helper, ListeningPlayer player,
                                                BlockHitResult hit, int count) {
            BlockState before = helper.getBlockState(AT);
            BlockState north = helper.getBlockState(AT.north());
            helper.useBlock(AT, player, hit);
            if (!helper.getBlockState(AT).equals(before) || !helper.getBlockState(AT.north()).equals(north)
                    || player.getMainHandItem().getCount() != count) {
                helper.fail("clicking a furnace changed the world or spent the held item", AT);
            }
        }

        private static PlacementPlan plan(GameTestHelper helper, ListeningPlayer player, BlockHitResult hit) {
            PlacementPlan plan = Placements.planFor(helper.getLevel(), player,
                    InteractionHand.MAIN_HAND, player.getMainHandItem(), hit);
            if (plan == null) {
                helper.fail("no plan at all where one was expected", AT);
                throw new IllegalStateException("unreachable");
            }
            return plan;
        }

        private static BlockHitResult hit(GameTestHelper helper, Direction face) {
            BlockPos absolute = helper.absolutePos(AT);
            return new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false);
        }

        // ---- fixtures and reads ------------------------------------------------------------

        /** A furnace mid-smelt: cobblestone in, coal banked and in the slot, bricks out. */
        private static void burner(GameTestHelper helper, FurnaceTier tier) {
            helper.setBlock(AT, PFBlocks.furnace(tier).get().defaultBlockState()
                    .setValue(FurnaceBlock.FACING, FACING));
            FurnaceBlockEntity furnace = furnace(helper);
            furnace.setItem(FurnaceSlots.INPUT, new ItemStack(Items.COBBLESTONE, 10));
            furnace.setItem(FurnaceSlots.FUEL, new ItemStack(Items.COAL, 5));
            furnace.setItem(FurnaceSlots.OUTPUT, new ItemStack(Items.STONE_BRICKS, 3));
            furnace.data().set(FurnaceBlockEntity.DATA_PROGRESS, STONE_PROGRESS);
            furnace.data().set(FurnaceBlockEntity.DATA_DURATION, STONE_DURATION);
            furnace.data().set(FurnaceBlockEntity.DATA_ENERGY_CAPACITY, LIT_JOULES);
            furnace.data().set(FurnaceBlockEntity.DATA_ENERGY, JOULES);
        }

        private static void holds(GameTestHelper helper, int coal, int progress, int duration, int energy) {
            FurnaceBlockEntity furnace = furnace(helper);
            ContainerData data = furnace.data();
            if (furnace.getItem(FurnaceSlots.INPUT).getCount() != 10
                    || furnace.getItem(FurnaceSlots.FUEL).getCount() != coal
                    || furnace.getItem(FurnaceSlots.OUTPUT).getCount() != 3) {
                helper.fail("the new furnace holds " + contents(furnace), AT);
            }
            if (data.get(FurnaceBlockEntity.DATA_PROGRESS) != progress
                    || data.get(FurnaceBlockEntity.DATA_DURATION) != duration) {
                helper.fail("the smelt stood at " + data.get(FurnaceBlockEntity.DATA_PROGRESS) + "/"
                        + data.get(FurnaceBlockEntity.DATA_DURATION) + ", not " + progress + "/" + duration, AT);
            }
            int gauge = energy == JOULES ? LIT_JOULES : data.get(FurnaceBlockEntity.DATA_ENERGY_CAPACITY);
            if (data.get(FurnaceBlockEntity.DATA_ENERGY) != energy
                    || data.get(FurnaceBlockEntity.DATA_ENERGY_CAPACITY) != gauge) {
                helper.fail("the new furnace's buffer is " + data.get(FurnaceBlockEntity.DATA_ENERGY)
                        + ", not " + energy, AT);
            }
        }

        private static void spent(GameTestHelper helper, ListeningPlayer player, FurnaceTier tier, int left) {
            ItemStack hand = player.getMainHandItem();
            if (!hand.is(PFBlocks.furnace(tier).get().asItem()) || hand.getCount() != left) {
                helper.fail("the hand holds " + hand + " where " + left + " " + tier + " should be left", AT);
            }
        }

        private static void returned(GameTestHelper helper, ListeningPlayer player, FurnaceTier tier, int count) {
            int got = player.getInventory().countItem(PFBlocks.furnace(tier).get().asItem());
            if (got != count) {
                helper.fail(got + " of the replaced " + tier + " furnace came back, not " + count, AT);
            }
        }

        /** Every main-inventory slot but the hand's full of dirt. */
        static void fill(ListeningPlayer player) {
            Inventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getNonEquipmentItems().size(); slot++) {
                if (slot != inventory.getSelectedSlot()) {
                    inventory.setItem(slot, new ItemStack(Items.DIRT, 64));
                }
            }
        }

        private static ItemStack stack(FurnaceTier tier, int count) {
            return new ItemStack(PFBlocks.furnace(tier).get(), count);
        }

        private static FurnaceBlockEntity furnace(GameTestHelper helper) {
            return helper.getBlockEntity(AT, FurnaceBlockEntity.class);
        }

        private static List<ItemStack> contents(FurnaceBlockEntity furnace) {
            List<ItemStack> items = new ArrayList<>();
            for (int slot = 0; slot < FurnaceSlots.SIZE; slot++) {
                items.add(furnace.getItem(slot).copy());
            }
            return items;
        }

        static List<ItemStack> inventory(Player player) {
            List<ItemStack> slots = new ArrayList<>();
            for (ItemStack stack : player.getInventory()) {
                slots.add(stack.copy());
            }
            return slots;
        }
    }

    /**
     * Fast Replace on pole columns (#389, ADR-0082): the plan names every segment, the click swaps
     * the column in place for one item, and the wires stay where they were.
     */
    static final class PoleReplaces {

        /** Within a small pole's 7.5 of the column's base, so the two are wired on placement. */
        private static final BlockPos NEIGHBOUR = ABOVE_FLOOR.east(5);
        /**
         * Nearer the base than {@link #NEIGHBOUR}, but wired only to it, since the two ends of a
         * placed pole's wires never share a neighbour. A base wired afresh would take it first.
         */
        private static final BlockPos BESIDE = ABOVE_FLOOR.east(3);
        private static final String OTHER_GROUP_KEY = "message.planetaryfactory.replace.other_group";
        private static final String NO_ROOM_KEY = "message.planetaryfactory.replace.no_room";

        private PoleReplaces() {
        }

        static void register(PFGameTests.Registrar tests) {
            tests.test("replace_small_pole_column_with_medium", 20,
                    helper -> replaces(helper, PoleTier.SMALL, PoleTier.MEDIUM, 1));
            tests.test("replace_medium_pole_column_with_small", 20,
                    helper -> replaces(helper, PoleTier.MEDIUM, PoleTier.SMALL, 0));
            tests.test("replace_small_pole_column_with_medium_at_its_top", 20,
                    helper -> replaces(helper, PoleTier.SMALL, PoleTier.MEDIUM, 2));
            tests.test("replace_pole_refuses_a_substation_on_a_small_column", 20,
                    helper -> refuses(helper, PoleTier.SMALL, 3, PoleTier.SUBSTATION, 2, false,
                            PlacementPlan.Refusal.OTHER_REPLACE_GROUP, OTHER_GROUP_KEY));
            tests.test("replace_pole_refuses_a_small_pole_on_a_substation", 20,
                    helper -> refuses(helper, PoleTier.SUBSTATION, 1, PoleTier.SMALL, 0, false,
                            PlacementPlan.Refusal.OTHER_REPLACE_GROUP, OTHER_GROUP_KEY));
            tests.test("replace_pole_refused_with_no_room_changes_nothing", 20,
                    helper -> refuses(helper, PoleTier.SMALL, 3, PoleTier.MEDIUM, 2, true,
                            PlacementPlan.Refusal.NO_ROOM_TO_RETURN, NO_ROOM_KEY));
        }

        /** A three-segment column of {@code from}, clicked at segment {@code aimed} with {@code to}. */
        private static void replaces(GameTestHelper helper, PoleTier from, PoleTier to, int aimed) {
            standing(helper, from, 3);
            BlockPos base = helper.absolutePos(ABOVE_FLOOR);
            Set<PoleLinks.Wire> wires = wires(helper);
            LevelWires levelWires = LevelWires.of(helper.getLevel());
            if (!levelWires.contains(base, helper.absolutePos(NEIGHBOUR))
                    || levelWires.contains(base, helper.absolutePos(BESIDE))) {
                helper.fail("the fixture was not wired base to neighbour only", ABOVE_FLOOR);
            }
            ListeningPlayer player = new ListeningPlayer(helper);
            player.setItemInHand(InteractionHand.MAIN_HAND, pole(to, 2));
            BlockPos target = ABOVE_FLOOR.above(aimed);
            BlockHitResult hit = hit(helper, target);
            PlacementPlan plan = plan(helper, player, hit, target);

            List<BlockPos> column = List.of(base, base.above(), base.above(2));
            BlockState segment = PFBlocks.pole(to).get().defaultBlockState();
            List<PlacementPlan.Placed> expected = column.stream()
                    .map(pos -> new PlacementPlan.Placed(pos, segment)).toList();
            if (plan.isRefused() || !plan.replaces().equals(column) || !plan.blocks().equals(expected)) {
                helper.fail("the plan was not a replace of the whole column by " + to + ": " + plan, target);
            }
            BlockState above = helper.getBlockState(ABOVE_FLOOR.above(3));

            helper.useBlock(target, player, hit);

            for (PlacementPlan.Placed placed : plan.blocks()) {
                if (!helper.getLevel().getBlockState(placed.pos()).equals(placed.state())) {
                    helper.fail("the replace left " + helper.getLevel().getBlockState(placed.pos()),
                            helper.relativePos(placed.pos()));
                }
            }
            if (!helper.getBlockState(ABOVE_FLOOR.above(3)).equals(above)) {
                helper.fail("the replace changed the height of the column", ABOVE_FLOOR.above(3));
            }
            if (!wires(helper).equals(wires)) {
                helper.fail("the replace changed the wires from " + wires + " to " + wires(helper), ABOVE_FLOOR);
            }
            if (helper.getBlockEntity(ABOVE_FLOOR, SupplyAreaPoleBlockEntity.class).tier() != to) {
                helper.fail("the column's base is not a " + to + " pole", ABOVE_FLOOR);
            }
            ItemStack hand = player.getMainHandItem();
            if (!hand.is(PFBlocks.pole(to).get().asItem()) || hand.getCount() != 1) {
                helper.fail("the hand holds " + hand + " where one " + to + " pole should be left", target);
            }
            int returned = player.getInventory().countItem(PFBlocks.pole(from).get().asItem());
            if (returned != 1) {
                helper.fail(returned + " " + from + " poles came back, not one", target);
            }
            helper.succeed();
        }

        private static void refuses(GameTestHelper helper, PoleTier standing, int height, PoleTier held,
                                    int aimed, boolean full, PlacementPlan.Refusal expected, String reason) {
            standing(helper, standing, height);
            ListeningPlayer player = new ListeningPlayer(helper);
            if (full) {
                Replaces.fill(player);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, pole(held, 2));
            BlockPos target = ABOVE_FLOOR.above(aimed);
            BlockHitResult hit = hit(helper, target);
            PlacementPlan plan = plan(helper, player, hit, target);
            if (plan.refusal() != expected || plan.isReplace() != (expected == PlacementPlan.Refusal.NO_ROOM_TO_RETURN)) {
                helper.fail("expected the refusal " + expected + " but the plan was " + plan, target);
            }
            Map<BlockPos, BlockState> world = world(helper);
            Set<PoleLinks.Wire> wires = wires(helper);
            List<ItemStack> inventory = Replaces.inventory(player);

            helper.useBlock(target, player, hit);

            if (!world(helper).equals(world) || !wires(helper).equals(wires)
                    || !ItemStack.listMatches(Replaces.inventory(player), inventory)) {
                helper.fail("a refused replace changed the world, the wires or the inventory", target);
            }
            if (!player.heard.contains(reason)) {
                helper.fail("a refused replace did not name " + reason + " on the action bar", target);
            }
            helper.succeed();
        }

        /** A column of {@code tier} on the floor, a small pole wired to its base, and one wired past it. */
        private static void standing(GameTestHelper helper, PoleTier tier, int height) {
            for (int i = 0; i < height; i++) {
                helper.setBlock(ABOVE_FLOOR.above(i), PFBlocks.pole(tier).get());
            }
            helper.setBlock(NEIGHBOUR, PFBlocks.pole(PoleTier.SMALL).get());
            helper.setBlock(BESIDE, PFBlocks.pole(PoleTier.SMALL).get());
        }

        private static PlacementPlan plan(GameTestHelper helper, Player player, BlockHitResult hit, BlockPos target) {
            PlacementPlan plan = Placements.planFor(helper.getLevel(), player,
                    InteractionHand.MAIN_HAND, player.getMainHandItem(), hit);
            if (plan == null) {
                helper.fail("no plan at all where one was expected", target);
                throw new IllegalStateException("unreachable");
            }
            return plan;
        }

        private static BlockHitResult hit(GameTestHelper helper, BlockPos target) {
            BlockPos absolute = helper.absolutePos(target);
            return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.NORTH, 0.5),
                    Direction.NORTH, absolute, false);
        }

        /** The column's positions, one above it, and the block north of each, where a fall-through would place. */
        private static Map<BlockPos, BlockState> world(GameTestHelper helper) {
            Map<BlockPos, BlockState> states = new HashMap<>();
            for (int i = 0; i <= PoleColumn.MAX_SEGMENTS; i++) {
                BlockPos pos = ABOVE_FLOOR.above(i);
                states.put(pos, helper.getBlockState(pos));
                states.put(pos.north(), helper.getBlockState(pos.north()));
            }
            return states;
        }

        /** The wires with an end in this fixture: the level's are shared with every test beside it. */
        private static Set<PoleLinks.Wire> wires(GameTestHelper helper) {
            Set<PoleLinks.Pos> ends = new HashSet<>();
            for (BlockPos pos : List.of(ABOVE_FLOOR, NEIGHBOUR, BESIDE)) {
                BlockPos absolute = helper.absolutePos(pos);
                ends.add(new PoleLinks.Pos(absolute.getX(), absolute.getY(), absolute.getZ()));
            }
            return LevelWires.of(helper.getLevel()).wires().all().stream()
                    .filter(wire -> ends.contains(wire.a()) || ends.contains(wire.b()))
                    .collect(Collectors.toSet());
        }

        private static ItemStack pole(PoleTier tier, int count) {
            return new ItemStack(PFBlocks.pole(tier).get(), count);
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
