package com.factoryworks.core.gametest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.portingdeadmods.researchd.api.ResearchdApi;
import com.portingdeadmods.researchd.api.team.ResearchTeam;
import com.portingdeadmods.researchd.api.team.ResearchTeamManager;
import io.github._5thlayer.craftworks.machine.AssemblerBlockEntity;
import io.github._5thlayer.craftworks.machine.AssemblerMenu;
import io.github._5thlayer.craftworks.machine.AssemblerSlots;
import io.github._5thlayer.craftworks.machine.AssemblerTier;
import io.github._5thlayer.craftworks.machine.Assemblers;
import io.github._5thlayer.craftworks.machine.HeldRecipes;
import io.github._5thlayer.craftworks.machine.HoldVerdict;
import io.github._5thlayer.craftworks.recipe.AssemblingRecipe;
import io.github._5thlayer.craftworks.recipe.CraftworksRecipes;
import io.github._5thlayer.groundworks.Footprint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * What the pack still owns of the Assemblers, which are Craftworks' (ADR-0118): that one on a pole
 * powers and crafts a pack recipe, that Fill Recipe is refused for a recipe the player's team has not
 * researched, and that every assembling recipe the pack ships is one some tier can hold.
 *
 * <p>The Assembler's own behaviour is Craftworks' GameTests. Craftworks names no Consumer API yet, so
 * these calls reach its internals until craftworks#37 does. Copper cable is the fixture: one plate
 * makes two wire in 0.5 s, which tier 1's speed 0.5 runs in 20 ticks. The figures are typed, for
 * {@code BoilerTests}' reason.
 */
final class AssemblingMachineTests {

    private static final BlockPos ORIGIN = new BlockPos(4, 1, 4);
    private static final Direction FACING = Direction.NORTH;
    private static final String PREFIX = "factoryworks:assembling/";
    private static final Identifier CABLE = Identifier.parse(PREFIX + "copper_cable");

    /** A research unlocks it, so a team that has researched nothing has it blocked. */
    private static final Identifier LOCKED_RECIPE = CABLE;

    /** Under no research's unlocks, so nothing locks it. */
    private static final Identifier FREE_RECIPE = Identifier.parse(PREFIX + "iron_gear_wheel");

    private static final int TICKS_PER_CRAFT = 20;

    private AssemblingMachineTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("an_assembler_on_a_pole_powers_and_crafts_a_pack_recipe", 160,
                AssemblingMachineTests::poweredByAPoleCrafts);
        tests.test("fill_recipe_is_refused_for_a_recipe_the_team_has_not_researched", 20,
                AssemblingMachineTests::refusesAnUnresearchedRecipe);
        tests.test("every_pack_assembling_recipe_can_be_held_by_some_tier", 20,
                AssemblingMachineTests::someTierHoldsEveryRecipe);
    }

    /** A creative pole reaching only the Assembler's far edge fills it, and the cable comes out. */
    private static void poweredByAPoleCrafts(GameTestHelper helper) {
        AssemblerBlockEntity machine = place(helper, AssemblerTier.ONE);
        BlockPos pole = ORIGIN.south(3);
        helper.startSequence()
                .thenExecute(() -> {
                    machine.setHeldRecipe(CABLE, player(helper));
                    machine.inventory().set(0, ItemResource.of(item("factoryworks:copper_plate")), 8);
                    helper.setBlock(pole, LibraryBlocks.creativePole());
                })
                .thenIdle(LibraryBlocks.POLE_RESCAN_INTERVAL + 5 + 3 * TICKS_PER_CRAFT)
                .thenExecute(() -> {
                    int wire = machine.inventory().getAmountAsInt(AssemblerSlots.PRODUCT);
                    if (wire < 2 || wire % 2 != 0) {
                        helper.fail("a powered Assembler made " + wire + " copper wire, expected whole crafts of 2",
                                ORIGIN);
                    }
                    if (!machine.inventory().getResource(AssemblerSlots.PRODUCT).equals(
                            ItemResource.of(item("factoryworks:copper_cable")))) {
                        helper.fail("a powered Assembler's product is "
                                + machine.inventory().getResource(AssemblerSlots.PRODUCT), ORIGIN);
                    }
                })
                .thenSucceed();
    }

    /** The Lock source is asked once, of the player who presses (Craftworks ADR-0013): a recipe a research unlocks is refused, one no research unlocks is held. */
    private static void refusesAnUnresearchedRecipe(GameTestHelper helper) {
        AssemblerBlockEntity machine = place(helper, AssemblerTier.ONE);
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        ResearchTeamManager teams = ResearchdApi.getTeamManager(helper.getLevel());
        if (teams == null) {
            helper.fail("Researchd has no team manager in this world, so nothing can lock a recipe", ORIGIN);
            return;
        }
        ResearchTeam team = teams.createDefaultTeam(player);
        teams.addTeam(team);
        try {
            AssemblerMenu menu = (AssemblerMenu) machine.createMenu(0, player.getInventory(), player);
            HoldVerdict locked = menu.request(player, LOCKED_RECIPE);
            if (locked != HoldVerdict.LOCKED || machine.heldRecipe().isPresent()) {
                helper.fail("Fill Recipe on an unresearched " + LOCKED_RECIPE + " was answered " + locked
                        + " and left " + machine.heldRecipe(), ORIGIN);
                return;
            }
            HoldVerdict free = menu.request(player, FREE_RECIPE);
            if (free != HoldVerdict.HELD || !machine.heldRecipe().equals(Optional.of(FREE_RECIPE))) {
                helper.fail("Fill Recipe on " + FREE_RECIPE + ", which no research unlocks, was answered " + free
                        + " and left " + machine.heldRecipe(), ORIGIN);
                return;
            }
        } finally {
            teams.removeTeam(team.getId());
            ResearchdApi.getResearchEffectManager(helper.getLevel()).clearTeam(team.getId());
        }
        helper.succeed();
    }

    /**
     * Against the manager, not the emitted files: a recipe the game rejected at load is
     * {@code check-datapack-load.py}'s.
     */
    private static void someTierHoldsEveryRecipe(GameTestHelper helper) {
        List<String> unheld = new ArrayList<>();
        int pack = 0;
        for (RecipeHolder<AssemblingRecipe> holder : helper.getLevel().getServer().getRecipeManager().recipeMap()
                .byType(CraftworksRecipes.ASSEMBLING_TYPE.get())) {
            String id = holder.id().identifier().toString();
            if (!id.startsWith(PREFIX)) {
                continue;
            }
            pack++;
            AssemblingRecipe recipe = holder.value();
            if (!HeldRecipes.canRun(recipe)) {
                unheld.add(id + " cannot run on an Assembler");
            } else if (Arrays.stream(AssemblerTier.values()).noneMatch(
                    tier -> HeldRecipes.takesCategory(tier, recipe) && HeldRecipes.takesFluids(tier, recipe))) {
                unheld.add(id + " is category " + recipe.category().id() + " with its fluids, which no tier holds");
            }
        }
        if (pack == 0) {
            helper.fail("the server loaded no " + PREFIX + " recipe, so this proves nothing");
            return;
        }
        if (!unheld.isEmpty()) {
            helper.fail("no Assembler tier can hold: " + unheld);
            return;
        }
        helper.succeed();
    }

    private static AssemblerBlockEntity place(GameTestHelper helper, AssemblerTier tier) {
        Footprint footprint = Assemblers.footprint(tier);
        List<BlockPos> positions = footprint.positions(helper.absolutePos(ORIGIN), FACING);
        for (int i = 0; i < positions.size(); i++) {
            helper.getLevel().setBlock(positions.get(i), footprint.stateAt(i, FACING), Block.UPDATE_ALL);
        }
        return helper.getBlockEntity(ORIGIN, AssemblerBlockEntity.class);
    }

    private static Player player(GameTestHelper helper) {
        return helper.makeMockPlayer(GameType.SURVIVAL);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }
}
