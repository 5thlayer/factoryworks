package com.planetaryfactory.core.assembler;

import com.planetaryfactory.core.PFAttachments;
import com.planetaryfactory.core.network.PFNetwork;
import com.planetaryfactory.core.network.PlanUpdatePacket;
import com.planetaryfactory.core.network.QueueSyncPacket;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;

/**
 * The server's side of the Personal Assembler: what each packet actually does.
 *
 * <p>Everything here runs on the server, which is the point. A plan is server truth (ADR-0038), so
 * the client never holds one -- it holds a {@link PlanDisplay} and a queue view, and every decision
 * that spends or refunds an item is made on this side of the wire.
 */
public final class PersonalAssembler {

    private PersonalAssembler() {
    }

    public static AssemblerQueue queueOf(Player player) {
        return player.getData(PFAttachments.ASSEMBLER_QUEUE.get());
    }

    /** Opens the panel. This is the tab on the inventory screen, and EMI's precondition. */
    public static void openPanel(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, who) -> new AssemblerPanelMenu(id, inventory),
                Component.translatable("planetaryfactory_core.assembler.panel")));
        sync(player);
    }

    /**
     * EMI's {@code + Fill Recipe}: the Crafting Plan for one craft, and the ceiling beside it (#287).
     *
     * <p>Opening queues nothing. The plan shown is the price of the next {@code +1}, so a stray click
     * on EMI's button spends nothing, and the plan is still never empty on arrival.
     */
    public static void openPlan(ServerPlayer player, Identifier recipe) {
        PlanView view = planView(player, recipe);
        player.openMenu(
                new SimpleMenuProvider(
                        (id, inventory, who) -> new CraftingPlanMenu(id, inventory, view.display(), view.all()),
                        Component.translatable("planetaryfactory_core.assembler.plan")),
                buffer -> {
                    PlanDisplay.STREAM_CODEC.encode(buffer, view.display());
                    buffer.writeVarInt(view.all());
                });
        sync(player);
    }

    /**
     * {@code +1}, {@code +5} or {@code all}: resolve that many and queue it in the same step (#287).
     *
     * <p>Resolved here rather than trusted from the dialog, because a packet is not a button: the
     * count may be stale or invented. An incomplete plan queues nothing, and so does one whose
     * reservation the inventory can no longer cover. Either way the dialog stays up and is re-sent,
     * so what the player sees is the inventory as it now is.
     */
    public static boolean craft(ServerPlayer player, Identifier recipe, int amount) {
        int wanted = Math.min(Math.max(1, amount), PlanResolver.MAX_CRAFTS);
        PlanSource.ResolvedPlan resolved = PlanSource.ACTIVE.resolve(player, recipe, wanted);
        boolean queued = false;
        if (resolved.complete()) {
            AssemblerQueue queue = queueOf(player);
            queued = queue.enqueue(resolved.plan(), new InventoryPlayerItems(player.getInventory()));
            if (queued) player.setData(PFAttachments.ASSEMBLER_QUEUE.get(), queue);
        }
        sync(player);
        refreshPlan(player);
        return queued;
    }

    /**
     * Re-sends the open Crafting Plan, if one is open.
     *
     * <p>Called after a press and on the queue's sync cadence: a queue under way spends and returns
     * items, and a lit {@code +5} that the inventory stopped covering would be a promise broken.
     */
    public static void refreshPlan(ServerPlayer player) {
        if (!(player.containerMenu instanceof CraftingPlanMenu menu)) return;
        PlanView view = planView(player, menu.display().recipe());
        PFNetwork.sendToPlayer(player, new PlanUpdatePacket(menu.containerId, view.display(), view.all()));
    }

    /** What the dialog shows, resolved one way for the open and every update so the two cannot drift. */
    private static PlanView planView(ServerPlayer player, Identifier recipe) {
        return new PlanView(PlanSource.ACTIVE.resolve(player, recipe, 1).display(),
                PlanSource.ACTIVE.largestAffordable(player, recipe));
    }

    private record PlanView(PlanDisplay display, int all) {
    }

    /**
     * Cancels a plan and refunds its buffer. Anything that will not fit goes to the player the way
     * a closed container's contents do -- a cancellation is their own action, unlike a finished
     * craft, which pauses rather than drops.
     */
    public static boolean cancel(ServerPlayer player, UUID planId) {
        AssemblerQueue queue = queueOf(player);
        AssemblerQueue.CancelResult result = queue.cancel(planId, new InventoryPlayerItems(player.getInventory()));
        if (!result.cancelled()) return false;
        for (ItemAmount leftover : result.notReturned()) {
            player.getInventory().placeItemBackInInventory(ItemKeys.toStack(leftover, player.registryAccess()));
        }
        player.setData(PFAttachments.ASSEMBLER_QUEUE.get(), queue);
        sync(player);
        return true;
    }

    /** One tick of one player's queue, run whether or not any screen is open. */
    public static void tick(ServerPlayer player) {
        AssemblerQueue queue = queueOf(player);
        if (queue.isEmpty()) return;
        queue.tick(new InventoryPlayerItems(player.getInventory()));
        player.setData(PFAttachments.ASSEMBLER_QUEUE.get(), queue);
    }

    /** Sends the queue's display view. The plan itself never crosses. */
    public static void sync(ServerPlayer player) {
        PFNetwork.sendToPlayer(player, QueueSyncPacket.of(queueOf(player)));
    }
}
