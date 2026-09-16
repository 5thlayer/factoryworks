package com.planetaryfactory.core.assembler.client;

import com.planetaryfactory.core.assembler.AssemblerQueueView;
import com.planetaryfactory.core.network.QueueSyncPacket;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * One queue row, in Factorio's order: the craft under way first, with the clock-hand wipe when it is
 * the head, and what it is for after it. A step that makes the row's own item is named once.
 *
 * <p>Shared by the hotbar overlay and the inventory screen so the two cannot drift.
 */
final class QueueRow {

    static final int ICON = 16;

    private QueueRow() {
    }

    /** Where a row's icons landed, so a click can be tested against them. {@code second} is -1 when absent. */
    record Icons(int first, int second) {

        boolean hit(double mouseX, double mouseY, int y) {
            return mouseY >= y && mouseY < y + ICON
                    && (inIcon(mouseX, first) || second >= 0 && inIcon(mouseX, second));
        }

        private static boolean inIcon(double mouseX, int x) {
            return mouseX >= x && mouseX < x + ICON;
        }
    }

    static Icons draw(GuiGraphicsExtractor graphics, Font font, QueueSyncPacket.Entry entry, boolean head,
            int x, int y, int textColor, boolean shadow) {
        Icons icons = layout(font, entry, x);
        boolean split = icons.second() >= 0;
        graphics.item(PlanItems.stack(split ? entry.stepItem() : entry.rootItem()), x, y);
        if (head) RadialWipeRenderer.over(graphics, x, y, AssemblerQueueView.liveProgress(entry));
        graphics.text(font, firstAmount(entry), x + ICON + 1, y + 4, textColor, shadow);
        if (!split) return icons;
        graphics.item(PlanItems.stack(entry.rootItem()), icons.second(), y);
        graphics.text(font, "x" + entry.amount(), icons.second() + ICON + 1, y + 4, textColor, shadow);
        return icons;
    }

    /** Where {@link #draw} puts a row's icons, without drawing: what a click is tested against. */
    static Icons layout(Font font, QueueSyncPacket.Entry entry, int x) {
        if (!entry.hasStep() || entry.stepIsRoot()) return new Icons(x, -1);
        return new Icons(x, x + ICON + 1 + font.width(firstAmount(entry)) + 3);
    }

    private static String firstAmount(QueueSyncPacket.Entry entry) {
        return "x" + (entry.hasStep() && !entry.stepIsRoot() ? entry.stepAmount() : entry.amount());
    }
}
