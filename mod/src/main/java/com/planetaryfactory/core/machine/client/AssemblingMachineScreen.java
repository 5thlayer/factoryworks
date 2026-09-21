package com.planetaryfactory.core.machine.client;

import com.planetaryfactory.core.machine.AssemblingMachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The Assembling Machine's screen (#327): what it holds, its five slots, and a clear button.
 *
 * <p>No recipe is picked here: the recipe viewer is the only picker, through EMI's Fill Recipe
 * (ADR-0073, #336). The clear button empties the Held recipe through vanilla's menu button click,
 * and the machine hands back what its inputs held. A recipe the team has not researched is named
 * as locked -- the Lock annotation policy is to annotate, never to hide.
 *
 * <p>Whether this draws correctly is a human check on delivery; no check here claims it.
 */
public class AssemblingMachineScreen extends AbstractContainerScreen<AssemblingMachineMenu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int SLOT_EDGE = 0xFF373737;
    private static final int TEXT = 0xFF404040;
    private static final int LOCKED_TEXT = 0xFFA02020;

    private static final int HELD_Y = 17;
    private static final int CLEAR_X = 150;
    private static final int CLEAR_SIZE = 18;

    public AssemblingMachineScreen(AssemblingMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
        inventoryLabelY = AssemblingMachineMenu.INVENTORY_Y - 11;
    }

    private Button clear;

    @Override
    protected void init() {
        super.init();
        clear = addRenderableWidget(Button.builder(Component.literal("\u2715"),
                        button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                                AssemblingMachineMenu.CLEAR))
                .bounds(leftPos + CLEAR_X, topPos + HELD_Y - 1, CLEAR_SIZE, CLEAR_SIZE)
                .tooltip(Tooltip.create(Component.translatable("gui.planetaryfactory.assembling_machine.clear")))
                .build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        clear.active = menu.held() != null || menu.holdsUnknown();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        for (Slot slot : menu.slots) {
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17,
                    SLOT_EDGE);
            graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, SLOT);
        }
    }

    /** What the machine holds -- drawn whether or not it has anything to make it with. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        AssemblingMachineMenu.Entry held = menu.held();
        if (held == null) {
            String key = menu.holdsUnknown()
                    ? "gui.planetaryfactory.assembling_machine.unknown_recipe"
                    : "gui.planetaryfactory.assembling_machine.no_recipe";
            graphics.text(font, Component.translatable(key),
                    8, HELD_Y + 4, TEXT, false);
            return;
        }
        graphics.item(held.icon(), 8, HELD_Y);
        graphics.text(font, held.icon().getHoverName(), 28, HELD_Y + 4, TEXT, false);
        if (held.choice().locked()) {
            graphics.text(font, Component.translatable("gui.planetaryfactory.assembling_machine.locked"),
                    100, HELD_Y + 4, LOCKED_TEXT, false);
        }
    }
}
