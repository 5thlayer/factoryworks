package com.planetaryfactory.core.gametest;

import java.util.List;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.energy.SupplyAreaPoleBlockEntity;
import com.planetaryfactory.core.machine.AssemblingMachineBlockEntity;
import com.planetaryfactory.core.machine.AssemblingMachineMenu;
import com.planetaryfactory.core.machine.AssemblingStall;
import com.planetaryfactory.core.machine.AssemblingTier;
import com.planetaryfactory.core.machine.HeldRecipe;
import com.planetaryfactory.core.machine.HoldVerdict;
import com.planetaryfactory.core.machine.footprint.FootprintMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The helpers every machine on the crafting chassis (ADR-0096) tests the same way. Each machine's
 * recipes and figures stay in its own file, typed, for {@code BoilerTests}' reason.
 */
record ChassisFixture(String name, FootprintMachine footprint, BlockPos anchor, Direction facing) {

    /** A pole rescans at most this many ticks after a machine appears (EnergyFaceTests' figure). */
    static final int RESCAN_INTERVAL = 40;

    static ChassisFixture assembling(AssemblingTier tier, BlockPos anchor, Direction facing) {
        return new ChassisFixture("Assembling Machine " + tier, PFBlocks.assemblingFootprint(tier), anchor, facing);
    }

    /** A footprint block other than the anchor, on the anchor's layer when there is one. */
    BlockPos hullBlock() {
        List<BlockPos> blocks = footprint.positions(anchor, facing);
        return blocks.stream().filter(pos -> !pos.equals(anchor) && pos.getY() == anchor.getY()).findFirst()
                .orElseGet(() -> blocks.stream().filter(pos -> !pos.equals(anchor)).findFirst().orElseThrow());
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

    ResourceHandler<FluidResource> fluidFace(GameTestHelper helper, BlockPos at) {
        return helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(at), null);
    }

    ResourceHandler<ItemResource> itemFace(GameTestHelper helper, BlockPos at) {
        return helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(at), null);
    }

    interface Move<R extends Resource> {
        int apply(ResourceHandler<R> face, Transaction tx);
    }

    static <R extends Resource> void expectMoved(GameTestHelper helper, BlockPos at, String what, int expected,
            ResourceHandler<R> face, Move<R> move) {
        int moved;
        try (Transaction tx = Transaction.openRoot()) {
            moved = move.apply(face, tx);
            tx.commit();
        }
        if (moved != expected) {
            helper.fail(what + " moved " + moved + ", expected " + expected, at);
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

    /** A pole reaching {@code reach} counts one machine and fills it. */
    void isFedByAPole(GameTestHelper helper, AssemblingMachineBlockEntity machine, BlockPos pole, String reach) {
        helper.startSequence()
                .thenExecute(() -> {
                    machine.energyStorage.set(0L);
                    helper.setBlock(pole, PFBlocks.CREATIVE_POLE.get());
                })
                .thenIdle(RESCAN_INTERVAL + 5)
                .thenExecute(() -> {
                    int found = helper.getBlockEntity(pole, SupplyAreaPoleBlockEntity.class).machineCount();
                    if (found != 1) {
                        helper.fail("a pole reaching " + reach + " counts " + found + " machines", pole);
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
