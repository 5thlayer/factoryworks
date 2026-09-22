package com.planetaryfactory.core.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.PFItems;
import com.planetaryfactory.core.machine.AssemblingTier;
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
import com.planetaryfactory.core.smelting.FurnaceTier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
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
        tests.test("plan_refuses_another_tier_at_a_pole", 20, PlacementPlanTests::poleWrongTier);
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

        private static List<ItemStack> inventory(Player player) {
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
