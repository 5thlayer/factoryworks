package com.factoryworks.core.gametest;

import java.util.List;

import io.github._5thlayer.wireworks.EnergyOwner;
import com.factoryworks.core.machine.AssemblingMachineBlockEntity;
import com.factoryworks.core.machine.AssemblingMachineMenu;
import com.factoryworks.core.machine.AssemblingStall;
import com.factoryworks.core.machine.HeldRecipe;
import com.factoryworks.core.machine.HoldVerdict;
import com.factoryworks.core.machine.footprint.FootprintMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;

/**
 * The helpers every machine on the crafting chassis (ADR-0096) tests the same way. What they hold is
 * the Pack's: the chassis's Held recipe, its stalls and its parts' energy owner. Each machine's
 * recipes and figures stay in its own file, typed, for {@code BoilerTests}' reason.
 */
record ChassisFixture(String name, FootprintMachine footprint, BlockPos anchor, Direction facing) {

    /** How far up and down a pole supplies: two blocks, as Wireworks documents it. */
    static final int POLE_VERTICAL_REACH = 2;

    /** A pole rescans at most this many ticks after a machine appears (#271). */
    static final int RESCAN_INTERVAL = 40;

    BlockPos hullBlock() {
        return footprint.positions(anchor, facing).stream().filter(pos -> !pos.equals(anchor)).findFirst()
                .orElseThrow();
    }

    <T extends AssemblingMachineBlockEntity> T placeWhole(GameTestHelper helper, Class<T> type) {
        footprint.placeAll(helper.getLevel(), helper.absolutePos(anchor), facing);
        return type.cast(helper.getLevel().getBlockEntity(helper.absolutePos(anchor)));
    }

    void hold(GameTestHelper helper, AssemblingMachineBlockEntity machine, String id) {
        machine.setHeldRecipe(HeldRecipe.of(id), helper.makeMockPlayer(GameType.SURVIVAL));
        if (!machine.heldRecipeResolves()) {
            helper.fail(id + " does not resolve on the " + name + ", so this proves nothing", anchor);
        }
    }

    int outputTank(AssemblingMachineBlockEntity machine, int output) {
        int seen = 0;
        for (int index = 0; index < machine.tank().size(); index++) {
            if (machine.isOutputTank(index) && seen++ == output) {
                return index;
            }
        }
        throw new IllegalStateException("the " + name + " has no output tank " + output);
    }

    /** A stall draws no FE, makes no progress and keeps its recipe; which inputs it kept is the caller's. */
    void assertStalled(GameTestHelper helper, AssemblingMachineBlockEntity machine, AssemblingStall expected,
            long charge, String recipe) {
        if (machine.stall() != expected) {
            helper.fail("the machine reports " + machine.stall() + ", expected " + expected, anchor);
        }
        long spent = charge - machine.energyStorage.getAmountAsLong();
        if (spent != 0) {
            helper.fail("a machine stalled on " + expected + " drew " + spent + " FE", anchor);
        }
        if (machine.progress.get() != 0) {
            helper.fail("a machine stalled on " + expected + " made progress " + machine.progress.get(), anchor);
        }
        if (!machine.heldRecipe().equals(HeldRecipe.of(recipe))) {
            helper.fail("a machine stalled on " + expected + " let go of its recipe", anchor);
        }
    }

    /** Fill Recipe's setter holds {@code own} and refuses each of {@code others} with a message. */
    void refusesOtherRecipes(GameTestHelper helper, AssemblingMachineBlockEntity machine, String own,
            List<String> others) {
        ListeningPlayer player = new ListeningPlayer(helper);
        AssemblingMachineMenu menu = AssemblingMachineMenu.open(0, player.getInventory(), machine);
        HoldVerdict held = menu.request(player, own);
        if (held != HoldVerdict.HELD) {
            helper.fail(own + " was answered " + held, anchor);
            return;
        }
        for (String other : others) {
            player.heard.clear();
            HoldVerdict verdict = menu.request(player, other);
            if (verdict != HoldVerdict.NOT_THIS_TYPE || !machine.heldRecipe().equals(HeldRecipe.of(own))
                    || !player.heard.equals(List.of(HoldVerdict.NOT_THIS_TYPE.messageKey()))) {
                helper.fail(other + " was answered " + verdict + " with " + player.heard + " and left "
                        + machine.heldRecipe(), anchor);
                return;
            }
        }
        helper.succeed();
    }

    void keepsItsRecipeOverAReload(GameTestHelper helper, AssemblingMachineBlockEntity machine, String recipe) {
        hold(helper, machine, recipe);
        CompoundTag saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(machine.getBlockPos(), machine.getBlockState(), saved,
                helper.getLevel().registryAccess());
        if (!(loaded instanceof AssemblingMachineBlockEntity reloaded) || reloaded.getClass() != machine.getClass()
                || !reloaded.heldRecipe().equals(HeldRecipe.of(recipe))) {
            helper.fail("the " + name + " held " + recipe + " and reloaded as " + loaded, anchor);
            return;
        }
        reloaded.setLevel(helper.getLevel());
        if (!reloaded.heldRecipeResolves()) {
            helper.fail("the reloaded recipe does not resolve against the recipe manager", anchor);
            return;
        }
        helper.succeed();
    }

    /**
     * A creative pole reaching {@code reach} fills the machine, and every block of the footprint
     * answers for the anchor, which Wireworks documents as what makes a pole count and feed a machine
     * once.
     */
    void isFedByAPole(GameTestHelper helper, AssemblingMachineBlockEntity machine, BlockPos pole, String reach) {
        helper.startSequence()
                .thenExecute(() -> {
                    machine.energyStorage.set(0L);
                    helper.setBlock(pole, LibraryBlocks.creativePole());
                })
                .thenIdle(RESCAN_INTERVAL + 5)
                .thenExecute(() -> {
                    BlockPos absoluteAnchor = helper.absolutePos(anchor);
                    for (BlockPos block : footprint.positions(absoluteAnchor, facing)) {
                        if (block.equals(absoluteAnchor)) {
                            continue;
                        }
                        BlockPos owner = EnergyOwner.of(helper.getLevel(), block);
                        if (!absoluteAnchor.equals(owner)) {
                            helper.fail("the block at " + helper.relativePos(block) + " answers for " + owner
                                    + " to a pole reaching " + reach + ", not the anchor", anchor);
                        }
                    }
                    if (machine.energyStorage.getAmountAsLong() <= 0L) {
                        helper.fail("a pole reaching " + reach + " left the machine unpowered", anchor);
                    }
                })
                .thenSucceed();
    }

    static Fluid fluid(String id) {
        return BuiltInRegistries.FLUID.getValue(Identifier.parse(id));
    }
}
