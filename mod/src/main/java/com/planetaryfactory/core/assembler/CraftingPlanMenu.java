package com.planetaryfactory.core.assembler;

import com.planetaryfactory.core.PFMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Crafting Plan: the dialog EMI's {@code + Fill Recipe} opens on a middle-click, or when what was
 * asked for cannot be queued (#287, ADR-0065).
 *
 * <p>The plan itself never comes here. What the client gets is a {@link PlanDisplay} for one craft
 * and the resolver's {@code largestAffordable}, which decides which of {@code +1}, {@code +5} and
 * {@code all} are lit ({@link CraftButtons}). Both are replaced in place by {@code PlanUpdatePacket}
 * while the dialog is up, because each press spends the inventory the next one is resolved against.
 */
public final class CraftingPlanMenu extends DialogMenu {

    private PlanDisplay display;
    private int largestAffordable;

    public CraftingPlanMenu(int containerId, Inventory inventory, PlanDisplay display, int largestAffordable) {
        super(PFMenus.CRAFTING_PLAN.get(), containerId);
        this.display = display;
        this.largestAffordable = largestAffordable;
    }

    public CraftingPlanMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, PlanDisplay.STREAM_CODEC.decode(buffer), buffer.readVarInt());
    }

    public PlanDisplay display() {
        return display;
    }

    public CraftButtons buttons() {
        return CraftButtons.of(largestAffordable);
    }

    /** The client taking a re-resolved plan. */
    public void update(PlanDisplay display, int largestAffordable) {
        this.display = display;
        this.largestAffordable = largestAffordable;
    }
}
