package com.planetaryfactory.core.placement;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * What a Fast Replace takes from the player and hands back (ADR-0082): one held item spent, the
 * replaced block's item into the slot that item freed or anywhere with room, and then the extras the
 * new block cannot hold. The plan asks {@link #fits} and the click {@link #execute}s, so a replace
 * the player has no room for is refused before anything changes.
 */
public final class ReplaceHandoff {

    private ReplaceHandoff() {
    }

    public static boolean fits(Player player, InteractionHand hand, ItemStack block, List<ItemStack> extras) {
        if (player.hasInfiniteMaterials()) {
            return true;
        }
        Inventory inventory = player.getInventory();
        List<ItemStack> slots = new ArrayList<>();
        for (ItemStack stack : inventory.getNonEquipmentItems()) {
            slots.add(stack.copy());
        }
        int offHand = slots.size();
        slots.add(player.getOffhandItem().copy());
        int freed = hand == InteractionHand.MAIN_HAND ? inventory.getSelectedSlot() : offHand;
        List<ItemStack> rest = new ArrayList<>(extras);
        if (player.getItemInHand(hand).getCount() == 1) {
            slots.set(freed, block.copy());
        } else {
            rest.addFirst(block);
        }
        for (ItemStack stack : rest) {
            if (!add(slots, offHand, stack.copy())) {
                return false;
            }
        }
        return true;
    }

    /** Spends the held item and hands the rest back. The caller has asked {@link #fits}. */
    public static void execute(Player player, InteractionHand hand, ItemStack block, List<ItemStack> extras) {
        if (player.hasInfiniteMaterials()) {
            extras.forEach(stack -> give(player, stack));
            return;
        }
        ItemStack held = player.getItemInHand(hand);
        held.shrink(1);
        if (held.isEmpty()) {
            player.setItemInHand(hand, block.copy());
        } else {
            give(player, block);
        }
        extras.forEach(stack -> give(player, stack));
    }

    private static void give(Player player, ItemStack stack) {
        ItemStack copy = stack.copy();
        if (!player.getInventory().add(copy) && !copy.isEmpty()) {
            player.drop(copy, false);
        }
    }

    /**
     * As {@code Inventory#add} places a stack: onto any matching stack with room, the off hand's
     * included, then into an empty slot that is not the off hand.
     */
    private static boolean add(List<ItemStack> slots, int offHand, ItemStack stack) {
        for (ItemStack slot : slots) {
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                int moved = Math.max(0, Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount()));
                slot.grow(moved);
                stack.shrink(moved);
                if (stack.isEmpty()) {
                    return true;
                }
            }
        }
        for (int i = 0; i < slots.size() && !stack.isEmpty(); i++) {
            if (i != offHand && slots.get(i).isEmpty()) {
                slots.set(i, stack.split(stack.getMaxStackSize()));
            }
        }
        return stack.isEmpty();
    }
}
