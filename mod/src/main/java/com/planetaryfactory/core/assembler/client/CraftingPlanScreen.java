package com.planetaryfactory.core.assembler.client;

import com.planetaryfactory.core.assembler.AssemblerQueueView;
import com.planetaryfactory.core.assembler.CraftButtons;
import com.planetaryfactory.core.assembler.CraftingPlanMenu;
import com.planetaryfactory.core.assembler.ItemAmount;
import com.planetaryfactory.core.assembler.PlanDisplay;
import com.planetaryfactory.core.network.PlanCraftPacket;
import com.planetaryfactory.core.network.QueueSyncPacket;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The Crafting Plan: what one craft spends, makes and cannot get, three buttons that queue, and the
 * queue running underneath (#287).
 *
 * <p>There is no Start and no Cancel. {@code +1}, {@code +5} and {@code all} each queue at once, the
 * plan re-resolves in place as the inventory is spent, and cancelling stays on the panel, which
 * already lists the queue. Close leaves the queue running.
 *
 * <p>{@code Locked} is its own column beside {@code Missing} because the two ask different things of
 * the player: research one, mine the other.
 */
public final class CraftingPlanScreen extends AssemblerScreen<CraftingPlanMenu> {

    /**
     * Consume, To Craft, Missing, Locked.
     *
     * <p>Consume comes first because it is what a press spends. ADR-0038 pays the whole raw cost in
     * one go, so the list of what leaves the inventory is the one the player is actually being asked
     * to agree to -- reading it after the list of what arrives puts the price after the purchase.
     */
    private static final int COLUMN = 4;

    /**
     * How many lines a column shows before it says how many it did not.
     *
     * <p>A flattened plan can be long and the dialog does not scroll. Truncating with a count is the
     * honest failure: a column that silently stopped would tell a player they have everything.
     */
    private static final int MAX_LINES = 6;

    /** Tall enough for a 16-pixel icon and the count beside it. */
    private static final int ROW_HEIGHT = 18;

    /**
     * The item under the cursor, or empty.
     *
     * <p>Collected while the panel draws and spent after the rest of the screen has, because a
     * tooltip painted from inside the background would be drawn over by everything after it.
     */
    private ItemStack hovered = ItemStack.EMPTY;

    /** Queue rows drawn under the plan; the panel shows the rest. */
    private static final int QUEUE_ROWS = 3;

    private static final int QUEUE_TOP = 156;

    private static final int BAR = 0xFF4FA84F;

    private Button one;
    private Button five;
    private Button all;

    public CraftingPlanScreen(CraftingPlanMenu menu, Inventory inventory, Component title) {
        // Wider than the panel: four columns of item names at 3-space-per-character do not fit in its
        // width, and a name that elides is a name the player cannot shop for.
        super(menu, inventory, title, 340, 270);
    }

    @Override
    protected void init() {
        super.init();
        int y = topPos + imageHeight - 26;
        one = addRenderableWidget(Button.builder(Component.literal("+1"), b -> craft(1))
                .bounds(leftPos + 8, y, 36, 20).build());
        five = addRenderableWidget(Button.builder(Component.literal("+5"), b -> craft(5))
                .bounds(leftPos + 48, y, 36, 20).build());
        all = addRenderableWidget(Button.builder(Component.translatable("planetaryfactory_core.assembler.all"),
                        b -> craft(menu.buttons().allCount()))
                .bounds(leftPos + 88, y, 40, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("planetaryfactory_core.assembler.close"),
                        b -> onClose())
                .bounds(leftPos + imageWidth - 76, y, 70, 20).build());
        refreshButtons();
    }

    private void craft(int amount) {
        ClientPacketDistributor.sendToServer(new PlanCraftPacket(menu.display().recipe(), amount));
    }

    /** Read off the menu every frame, since a {@code PlanUpdatePacket} can change it at any time. */
    private void refreshButtons() {
        CraftButtons buttons = menu.buttons();
        one.active = buttons.one();
        five.active = buttons.five();
        all.active = buttons.all();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        hovered = ItemStack.EMPTY;
        refreshButtons();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (!hovered.isEmpty()) {
            graphics.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
        }
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        PlanDisplay display = menu.display();
        int width = (imageWidth - 16) / COLUMN;
        column(graphics, leftPos + 8, "consume", display.consume(), ChatFormatting.AQUA, mouseX, mouseY);
        column(graphics, leftPos + 8 + width, "to_craft", display.toCraft(), ChatFormatting.WHITE, mouseX, mouseY);
        column(graphics, leftPos + 8 + 2 * width, "missing", display.missing(), ChatFormatting.RED, mouseX, mouseY);
        column(graphics, leftPos + 8 + 3 * width, "locked", display.locked(), ChatFormatting.GOLD, mouseX, mouseY);
        if (!display.complete()) {
            // Its own line above the buttons, not beside them: the reason a plan cannot be queued is
            // a translated sentence of unknown width, and sharing the row with the buttons means the
            // longest translation is the one that gets cut in half.
            //
            // Three empty columns is not "you are short of something" -- it is the resolver saying
            // it could not read the recipe at all, which happens to one carrying a tag ingredient,
            // a fluid or a chanced output. Telling the player they are missing nothing while
            // refusing to queue would be the worst of both.
            boolean nothingToShow = display.consume().isEmpty()
                    && display.toCraft().isEmpty()
                    && display.missing().isEmpty()
                    && display.locked().isEmpty();
            // Locked before short: research is the fix there, and "not enough" would send the player
            // mining for an item no amount of mining lets them craft.
            String reason = nothingToShow ? "unplannable"
                    : display.locked().isEmpty() ? "incomplete" : "locked_plan";
            graphics.text(font,
                    Component.translatable("planetaryfactory_core.assembler." + reason)
                            .withStyle(ChatFormatting.RED),
                    leftPos + 8, topPos + imageHeight - 38, 0xFFFF5555, false);
        }
        renderQueue(graphics);
    }

    /**
     * The first few queued plans, so a press can be watched running without leaving the dialog.
     *
     * <p>No cancel here: the panel owns that, and a cancel beside three buttons that queue would be
     * one misclick from refunding what was just paid for.
     */
    private void renderQueue(GuiGraphicsExtractor graphics) {
        int y = topPos + QUEUE_TOP;
        graphics.text(font,
                Component.translatable("planetaryfactory_core.assembler.panel").withStyle(ChatFormatting.GRAY),
                leftPos + 8, y, 0xFFAAAAAA, false);
        y += 12;
        List<QueueSyncPacket.Entry> entries = AssemblerQueueView.entries();
        if (entries.isEmpty()) {
            graphics.text(font,
                    Component.translatable("planetaryfactory_core.assembler.queue_empty").withStyle(ChatFormatting.GRAY),
                    leftPos + 10, y + 4, 0xFFAAAAAA, false);
            return;
        }
        int trackWidth = imageWidth - 16;
        for (QueueSyncPacket.Entry entry : entries.subList(0, Math.min(QUEUE_ROWS, entries.size()))) {
            graphics.item(itemStack(entry.rootItem()), leftPos + 8, y);
            graphics.text(font, "x" + entry.amount(), leftPos + 28, y + 5, 0xFFFFFFFF, false);
            int barLeft = leftPos + 60;
            int barWidth = (int) ((trackWidth - 52) * Math.max(0.0f, Math.min(1.0f, entry.progress())));
            graphics.fill(barLeft, y + 7, leftPos + 8 + trackWidth, y + 9, ROW);
            graphics.fill(barLeft, y + 7, barLeft + barWidth, y + 9, BAR);
            y += ROW_HEIGHT;
        }
        if (entries.size() > QUEUE_ROWS) {
            graphics.text(font,
                    Component.translatable("planetaryfactory_core.assembler.and_more", entries.size() - QUEUE_ROWS),
                    leftPos + 8, y, 0xFF888888, false);
        }
    }

    private void column(GuiGraphicsExtractor graphics, int x, String key, List<ItemAmount> amounts,
            ChatFormatting colour, int mouseX, int mouseY) {
        graphics.text(font,
                Component.translatable("planetaryfactory_core.assembler." + key).withStyle(colour),
                x, topPos + 22, 0xFFFFFFFF, false);
        int y = topPos + 34;
        for (ItemAmount amount : amounts.subList(0, Math.min(MAX_LINES, amounts.size()))) {
            // The icon and not the name: four columns of "8 Steel Plate" is more text than the
            // dialog has room for, and the sprite is the thing a player already recognises from
            // their own inventory. The name is one hover away, from vanilla's own tooltip, so
            // nothing is lost -- including the id, which F3+H puts back for anyone who wants it.
            ItemStack stack = itemStack(amount.item());
            graphics.item(stack, x, y);
            graphics.text(font, "x " + amount.count(), x + 20, y + 5, 0xFFCCCCCC, false);
            if (!stack.isEmpty() && mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                hovered = stack;
            }
            y += ROW_HEIGHT;
        }
        if (amounts.size() > MAX_LINES) {
            graphics.text(font,
                    Component.translatable("planetaryfactory_core.assembler.and_more",
                            amounts.size() - MAX_LINES),
                    x, y, 0xFF888888, false);
        }
    }
}
