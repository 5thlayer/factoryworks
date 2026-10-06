package com.factoryworks.core.gametest;

import com.factoryworks.core.mining.rig.RigMiningArea;
import com.factoryworks.core.mining.rig.RigBlock;
import com.factoryworks.core.ore.OreResource;
import java.util.ArrayList;
import java.util.List;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.PFItems;
import com.factoryworks.core.machine.OilRefineryBlockEntity;
import com.factoryworks.core.machine.OilRefineryFootprint;
import com.factoryworks.core.fluid.SteamEngineBlockEntity;
import com.factoryworks.core.fluid.SteamEngineFootprint;
import com.factoryworks.core.mining.rig.RigCorpus;
import com.factoryworks.core.mining.rig.RigGeometry;
import com.factoryworks.core.mining.rig.RigTier;
import com.factoryworks.core.placement.PackRefusal;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import com.factoryworks.core.oil.PumpjackBlockEntity;
import com.factoryworks.core.oil.PumpjackFootprint;
import com.factoryworks.core.radar.RadarBlockEntity;
import com.factoryworks.core.radar.RadarFootprint;
import rearth.oritech.block.base.block.MultiblockMachine;
import net.minecraft.world.item.Item;
import com.factoryworks.core.smelting.FurnaceBlock;
import com.factoryworks.core.smelting.FurnaceBlockEntity;
import com.factoryworks.core.smelting.FurnaceSlots;
import com.factoryworks.core.smelting.FurnaceTier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Pack's claim: for every item the Pack owns, and for the vanilla blocks it opts into Groundworks'
 * plan, the {@link Placements#planFor} plan is what placing does; and the Pack states the furnace and
 * electric-pole Replace groups (#297, #388, #450, #627, ADR-0069, ADR-0082). Groundworks' own tests
 * hold the plan, the click and Fast Replace for its own test blocks.
 *
 * <p>This is the one check ADR-0069 asks for by name, and the only one that can exist. The geometry
 * underneath a plan is Minecraft-free and already tested -- {@code RigGeometryTest} for the
 * footprint; a pole column's plan is Wireworks' -- and whether the preview <em>draws</em> right
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
        tests.test("plan_matches_placement_for_a_rig", 20, PlacementPlanTests::rigMatchesPlacement);
        tests.test("plan_refuses_a_rig_whole", 20, PlacementPlanTests::rigRefusesWhole);
        tests.test("plan_refuses_a_burner_rig_over_no_ore", 20,
                helper -> rigRefusesOverNoOre(helper, RigTier.BURNER));
        tests.test("plan_refuses_an_electric_rig_over_no_ore", 20,
                helper -> rigRefusesOverNoOre(helper, RigTier.ELECTRIC));
        tests.test("plan_accepts_a_burner_rig_over_ore_at_its_area_edge", 20,
                helper -> rigAcceptsOreAtAreaEdge(helper, RigTier.BURNER));
        tests.test("plan_accepts_an_electric_rig_over_ore_outside_its_footprint", 20,
                helper -> rigAcceptsOreAtAreaEdge(helper, RigTier.ELECTRIC));
        tests.test("plan_matches_placement_for_an_oil_refinery", 20,
                PlacementPlanTests::oilRefineryMatchesPlacement);
        tests.test("plan_refuses_an_oil_refinery_blocked_in_a_chamber", 20,
                PlacementPlanTests::oilRefineryRefusesWhole);
        tests.test("plan_refuses_an_oil_refinery_blocked_off_its_anchor_column", 20,
                PlacementPlanTests::oilRefineryRefusesOffColumn);
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
        tests.test("plan_matches_placement_for_vanilla_stairs", 20,
                helper -> vanillaMatchesPlacement(helper, Items.OAK_STAIRS));
        tests.test("plan_matches_placement_for_a_vanilla_chest", 20,
                helper -> vanillaMatchesPlacement(helper, Items.CHEST));
        tests.test("plan_matches_placement_for_a_vanilla_log", 20,
                helper -> vanillaMatchesPlacement(helper, Items.OAK_LOG));
        tests.test("plan_is_none_for_an_unoriented_foreign_block", 20,
                PlacementPlanTests::unorientedHasNoPlan);
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

    /**
     * The multiblock: every position the plan names must be down, anchor and parts alike, and the
     * plan must name the <em>whole</em> footprint.
     *
     * <p>The size is checked against {@link RigGeometry} rather than against a number or a floor:
     * a plan naming two of a burner rig's eight positions would place two blocks, agree with itself
     * and pass every other assertion here, while the preview drew a quarter of the machine.
     */
    private static void rigMatchesPlacement(GameTestHelper helper) {
        helper.setBlock(FLOOR, PFBlocks.ore(OreResource.IRON).get());
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
                FLOOR, Direction.UP, true), PackRefusal.FOOTPRINT_BLOCKED, helper);
        helper.succeed();
    }

    /** Bare ground under a drill's area refuses the plan, and the click places nothing (#589). */
    private static void rigRefusesOverNoOre(GameTestHelper helper, RigTier tier) {
        refusal(check(helper, new ItemStack(PFItems.rig(tier).get()),
                FLOOR, Direction.UP, true), PackRefusal.NO_ORE_IN_AREA, helper);
        helper.succeed();
    }

    /**
     * One ore block on a tile of the area that lies outside the footprint is enough (#589). The
     * facing comes from the bare-ground plan, since the mock player's look decides the area.
     */
    private static void rigAcceptsOreAtAreaEdge(GameTestHelper helper, RigTier tier) {
        Player probe = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(PFItems.rig(tier).get());
        probe.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos floor = helper.absolutePos(FLOOR);
        PlacementPlan bare = Placements.planFor(helper.getLevel(), probe, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(floor).relative(Direction.UP, 0.5), Direction.UP, floor, false));
        if (bare == null) {
            helper.fail("no plan at all where one was expected", FLOOR);
            throw new IllegalStateException("unreachable");
        }
        BlockPos anchor = bare.blocks().getFirst().pos();
        Direction facing = bare.blocks().getFirst().state().getValue(RigBlock.FACING);
        List<BlockPos> footprint = bare.blocks().stream().map(PlacementPlan.Placed::pos).toList();
        BlockPos edge = RigMiningArea.positions(anchor, tier, facing).stream()
                .filter(pos -> footprint.stream().noneMatch(
                        part -> part.getX() == pos.getX() && part.getZ() == pos.getZ()))
                .findFirst()
                .orElse(null);
        if (edge == null) {
            // The burner area is its own footprint's 2x2, so its edge is a footprint column.
            edge = RigMiningArea.positions(anchor, tier, facing).getLast();
        }
        helper.getLevel().setBlockAndUpdate(edge, PFBlocks.ore(OreResource.IRON).get().defaultBlockState());
        check(helper, new ItemStack(PFItems.rig(tier).get()), FLOOR, Direction.UP, false);
        helper.succeed();
    }

    /** The Oil Refinery's base and both chamber layers (ADR-0096), placed as one. */
    private static void oilRefineryMatchesPlacement(GameTestHelper helper) {
        PlacementPlan plan = check(helper, new ItemStack(PFItems.OIL_REFINERY.get()), FLOOR, Direction.UP, false);
        int expected = OilRefineryFootprint.FOOTPRINT.offsets().size();
        if (plan.blocks().size() != expected) {
            helper.fail("an Oil Refinery's plan named " + plan.blocks().size() + " blocks, expected " + expected, FLOOR);
        }
        long layers = plan.blocks().stream().map(block -> block.pos().getY()).distinct().count();
        if (layers != OilRefineryFootprint.BASE_HEIGHT + OilRefineryFootprint.CHAMBERS) {
            helper.fail("an Oil Refinery's plan spans " + layers + " layers", FLOOR);
        }
        BlockPos anchor = plan.blocks().getFirst().pos();
        if (!(helper.getLevel().getBlockEntity(anchor) instanceof OilRefineryBlockEntity)) {
            helper.fail("the placed anchor holds no Oil Refinery", helper.relativePos(anchor));
        }
        helper.runAfterDelay(5, () -> {
            if (!helper.getLevel().getBlockState(anchor).getValue(MultiblockMachine.ASSEMBLED)) {
                helper.fail("the anchor lost ASSEMBLED after it was placed", helper.relativePos(anchor));
            }
            helper.succeed();
        });
    }

    /** A stone in the top chamber layer, above the anchor where a player cannot see it, refuses the whole. */
    private static void oilRefineryRefusesWhole(GameTestHelper helper) {
        helper.setBlock(ABOVE_FLOOR.above(OilRefineryFootprint.BASE_HEIGHT + OilRefineryFootprint.CHAMBERS - 1),
                Blocks.STONE);
        refusal(check(helper, new ItemStack(PFItems.OIL_REFINERY.get()), FLOOR, Direction.UP, true),
                PackRefusal.FOOTPRINT_BLOCKED, helper);
        helper.succeed();
    }

    /**
     * Stones in the lower chamber layer beside the anchor's column, one on each side, so whichever
     * way the machine faces, one stands in a chamber and not on the anchor's column.
     */
    private static void oilRefineryRefusesOffColumn(GameTestHelper helper) {
        BlockPos layer = ABOVE_FLOOR.above(OilRefineryFootprint.BASE_HEIGHT);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            helper.setBlock(layer.relative(side), Blocks.STONE);
        }
        refusal(check(helper, new ItemStack(PFItems.OIL_REFINERY.get()), FLOOR, Direction.UP, true),
                PackRefusal.FOOTPRINT_BLOCKED, helper);
        helper.succeed();
    }

    /**
     * The Steam Engine's footprint (ADR-0116): the whole of it, its anchor holding the engine.
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
        helper.succeed();
    }

    /** One taken position, in the engine's upper row, refuses the whole engine. */
    private static void steamEngineRefusesWhole(GameTestHelper helper) {
        helper.setBlock(ABOVE_FLOOR.above(), Blocks.STONE);
        refusal(check(helper, new ItemStack(PFItems.STEAM_ENGINE.get()),
                FLOOR, Direction.UP, true), PackRefusal.FOOTPRINT_BLOCKED, helper);
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
                FLOOR, Direction.UP, true), PackRefusal.NOT_ON_WELL, helper);
        helper.succeed();
    }

    /** One taken position, in the top layer's corner, refuses the whole Radar. */
    private static void radarRefusesWhole(GameTestHelper helper) {
        helper.setBlock(ABOVE_FLOOR.offset(1, 2, 1), Blocks.STONE);
        refusal(check(helper, new ItemStack(PFItems.RADAR.get()),
                FLOOR, Direction.UP, true), PackRefusal.FOOTPRINT_BLOCKED, helper);
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
                PackRefusal.NO_FLUID_SOURCE, helper);
        helper.succeed();
    }

    /** A block from outside the pack is drawn when it has a facing, an axis or a rotation (#450). */
    private static void vanillaMatchesPlacement(GameTestHelper helper, Item item) {
        check(helper, new ItemStack(item), FLOOR, Direction.UP, false);
        helper.succeed();
    }

    private static void unorientedHasNoPlan(GameTestHelper helper) {
        ItemStack stone = new ItemStack(Items.STONE);
        BlockPos absolute = helper.absolutePos(FLOOR);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
        if (Placements.planFor(helper.getLevel(), helper.makeMockPlayer(GameType.SURVIVAL),
                InteractionHand.MAIN_HAND, stone, hit) != null) {
            helper.fail("stone, which has no orientation, got a plan", FLOOR);
        }
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

    private static void refusal(PlacementPlan plan, PackRefusal expected,
                                GameTestHelper helper) {
        if (plan.refusal() != expected) {
            helper.fail("expected the refusal " + expected + " but the plan gave " + plan.refusal(),
                    ABOVE_FLOOR);
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
            if (plan.refusal() != PackRefusal.NO_ROOM_TO_RETURN || !plan.isReplace()) {
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
            if (!player.heard.contains("message.factoryworks.replace.no_room")) {
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
     * The Pack's pole Replace group (ADR-0082): small and medium replace each other through
     * Wireworks' column builder, and the substation is alone. The replace itself is Wireworks'
     * {@code PoleReplaceTests}; these hold only the group the Pack states.
     */
    static final class PoleReplaces {

        private PoleReplaces() {
        }

        static void register(PFGameTests.Registrar tests) {
            tests.test("pole_group_replaces_small_with_medium", 20, PoleReplaces::smallWithMedium);
            tests.test("pole_group_leaves_the_substation_alone", 20, PoleReplaces::substationAlone);
        }

        private static void smallWithMedium(GameTestHelper helper) {
            column(helper, LibraryBlocks.smallPole(), 2);
            ListeningPlayer player = clicked(helper, LibraryBlocks.mediumPole());
            for (int i = 0; i < 2; i++) {
                if (!helper.getBlockState(ABOVE_FLOOR.above(i)).is(LibraryBlocks.mediumPole())) {
                    helper.fail("a medium pole did not replace the small column", ABOVE_FLOOR.above(i));
                }
            }
            if (player.getInventory().countItem(LibraryBlocks.smallPole().asItem()) != 1) {
                helper.fail("the replace did not hand back one small pole", ABOVE_FLOOR);
            }
            helper.succeed();
        }

        private static void substationAlone(GameTestHelper helper) {
            column(helper, LibraryBlocks.smallPole(), 2);
            clicked(helper, LibraryBlocks.largePole());
            for (int i = 0; i < 2; i++) {
                if (!helper.getBlockState(ABOVE_FLOOR.above(i)).is(LibraryBlocks.smallPole())) {
                    helper.fail("a substation replaced a small column", ABOVE_FLOOR.above(i));
                }
            }
            helper.succeed();
        }

        private static void column(GameTestHelper helper, Block pole, int height) {
            for (int i = 0; i < height; i++) {
                helper.setBlock(ABOVE_FLOOR.above(i), pole);
            }
        }

        /** A plain click through the player's game mode, where Groundworks takes a Fast Replace. */
        private static ListeningPlayer clicked(GameTestHelper helper, Block held) {
            ListeningPlayer player = new ListeningPlayer(helper);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(held, 2));
            BlockPos absolute = helper.absolutePos(ABOVE_FLOOR);
            player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.NORTH, 0.5), Direction.NORTH, absolute, false));
            return player;
        }
    }
}
