package com.planetaryfactory.core.assembler.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * What the Assembler's screens share: a flat panel, a title, and no texture.
 *
 * <p>Drawn from fills rather than from a sprite sheet because the jar's only asset is its lang file
 * (see {@code mod/README.md}) -- and because what the Assembler looks like is #161's to settle, so a
 * texture written now would be a texture drawn twice.
 */
abstract class AssemblerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    protected static final int PANEL = 0xFF2B2B2B;
    protected static final int ROW = 0xFF3C3C3C;
    private static final int SLOT = 0xFF1A1A1A;
    private static final int SLOT_EDGE = 0xFF4A4A4A;

    protected AssemblerScreen(T menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // The world dims behind the panel as it does behind the inventory; skipping super left it bright.
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        renderPanel(graphics, mouseX, mouseY);
    }

    /** The player-facing name of an item id. See {@link PlanItems}. */
    protected static Component itemName(String id) {
        return PlanItems.name(id);
    }

    /** One stack of an item id, for drawing and for its tooltip. See {@link PlanItems}. */
    protected static ItemStack itemStack(String id) {
        return PlanItems.stack(id);
    }

    /** What this particular screen puts on the panel. */
    protected abstract void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY);

    /**
     * A well for every slot the menu carries.
     *
     * <p>{@code AbstractContainerScreen} draws what is <em>in</em> a slot and nothing else -- the
     * empty grid is part of the background texture in vanilla. With no texture, an empty inventory
     * renders as blank panel, so the wells are drawn here from the menu's own slot positions rather
     * than from a second copy of the layout.
     */
    protected void renderSlots(GuiGraphicsExtractor graphics) {
        for (net.minecraft.world.inventory.Slot slot : menu.slots) {
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
            graphics.fill(x, y, x + 16, y + 16, SLOT);
        }
    }

    /** The title only. The inherited second label names a player inventory these screens draw themselves. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, 8, 6, 0xFFFFFFFF, false);
    }
}
