package com.rngtech.client.screen;

import com.rngtech.content.item.GlyphItem;
import com.rngtech.content.item.JobChipItem;
import com.rngtech.content.menu.DispatchTerminalMenu;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.workorder.WorkOrderRewards;
import com.rngtech.rpg.workorder.WorkOrder;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The Dispatch Terminal: a job display in a slate console frame. Each row shows a listed Job Chip's tier, machine, stage,
 * job, and the reward it pays, with a Download button; hovering a row shows the chip's full tooltip. Refresh below the
 * display spends one Requisition.
 */
public class DispatchTerminalScreen extends AbstractContainerScreen<DispatchTerminalMenu> {
    private static final int WIDTH = 254;
    private static final int HEIGHT = 226;
    private static final int FOOTER_Y = 208;
    private static final int SLATE = 0xFF2E3642;
    private static final int SLATE_LIGHT = 0xFF46505E;
    private static final int SLATE_DARK = 0xFF1E232B;
    private static final int DISPLAY = 0xFF0C1418;
    private static final int SCANLINE = 0xFF0A1115;
    private static final int TEAL = 0xFF2FB3C2;
    private static final int TEAL_DIM = 0xFF16606A;
    private static final int TEAL_TEXT = 0xFF8EE3EC;
    private static final int TEAL_MUTED = 0xFF4E8C94;
    private static final int AMBER = 0xFFE3B866;
    private static final int HEADER_TEXT = 0xFFE7DDC0;
    private static final int HEADER_MUTED = 0xFFA9A08A;
    private static final int SOCKET = 0xFF05090B;
    private static final int BUTTON_HEIGHT = 12;

    public DispatchTerminalScreen(DispatchTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (overRefresh(mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, List.of(Component.translatable("rngtech.dispatch_terminal.reroll_hint")), mouseX, mouseY);
            return;
        }
        for (int slot = 0; slot < 6; slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.isEmpty()) {
                continue;
            }
            if (overDownload(slot, mouseX, mouseY)) {
                guiGraphics.renderComponentTooltip(font, List.of(Component.translatable("rngtech.dispatch_terminal.take_hint")), mouseX, mouseY);
                return;
            }
            if (overRow(slot, mouseX, mouseY)) {
                guiGraphics.renderTooltip(font, stack, mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        guiGraphics.fill(left, top, left + imageWidth, top + imageHeight, SLATE);
        guiGraphics.fill(left, top, left + imageWidth, top + 1, SLATE_LIGHT);
        guiGraphics.fill(left, top, left + 1, top + imageHeight, SLATE_LIGHT);
        guiGraphics.fill(left + imageWidth - 1, top, left + imageWidth, top + imageHeight, SLATE_DARK);
        guiGraphics.fill(left, top + imageHeight - 1, left + imageWidth, top + imageHeight, SLATE_DARK);

        int displayLeft = left + 6;
        int displayTop = top + 16;
        int displayRight = left + imageWidth - 6;
        int displayBottom = top + FOOTER_Y - 3;
        guiGraphics.fill(displayLeft - 1, displayTop - 1, displayRight + 1, displayBottom + 1, TEAL_DIM);
        guiGraphics.fill(displayLeft, displayTop, displayRight, displayBottom, DISPLAY);
        for (int y = displayTop + 1; y < displayBottom; y += 2) {
            guiGraphics.fill(displayLeft, y, displayRight, y + 1, SCANLINE);
        }

        for (int slot = 0; slot < 6; slot++) {
            int rowTop = top + DispatchTerminalMenu.rowTop(slot);
            int rowLeft = left + DispatchTerminalMenu.ROW_LEFT;
            if (slot > 0) {
                guiGraphics.fill(rowLeft, rowTop - 2, rowLeft + DispatchTerminalMenu.ROW_WIDTH, rowTop - 1, TEAL_DIM);
            }
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.isEmpty()) {
                continue;
            }
            int socketX = rowLeft + DispatchTerminalMenu.ICON_X - 1;
            int socketY = rowTop + DispatchTerminalMenu.ICON_Y - 1;
            guiGraphics.fill(socketX - 1, socketY - 1, socketX + 19, socketY + 19, TEAL_DIM);
            guiGraphics.fill(socketX, socketY, socketX + 18, socketY + 18, SOCKET);

            int[] button = downloadBox(slot);
            boolean hovered = mouseX >= button[0] && mouseX < button[0] + button[2] && mouseY >= button[1] && mouseY < button[1] + button[3];
            guiGraphics.fill(button[0], button[1], button[0] + button[2], button[1] + button[3], TEAL);
            guiGraphics.fill(button[0] + 1, button[1] + 1, button[0] + button[2] - 1, button[1] + button[3] - 1, hovered ? TEAL_DIM : DISPLAY);
        }

        boolean ready = requisitions() > 0;
        int x = left + 8;
        int y = top + FOOTER_Y + 2;
        int width = refreshWidth();
        guiGraphics.fill(x, y, x + width, y + BUTTON_HEIGHT + 2, ready ? TEAL : TEAL_DIM);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + BUTTON_HEIGHT + 1, ready && overRefresh(mouseX, mouseY) ? TEAL_DIM : DISPLAY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 5, HEADER_TEXT, false);
        for (int slot = 0; slot < 6; slot++) {
            renderRowText(guiGraphics, slot);
        }
        boolean ready = requisitions() > 0;
        guiGraphics.drawString(font, Component.translatable("rngtech.dispatch_terminal.reroll"), 13, FOOTER_Y + 5, ready ? TEAL_TEXT : TEAL_MUTED, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.dispatch_terminal.requisitions", requisitions()),
                8 + refreshWidth() + 6, FOOTER_Y + 5, ready ? HEADER_TEXT : HEADER_MUTED, false);
    }

    private void renderRowText(GuiGraphics guiGraphics, int slot) {
        int x = DispatchTerminalMenu.ROW_LEFT;
        int y = DispatchTerminalMenu.rowTop(slot);
        ItemStack stack = menu.getSlot(slot).getItem();
        WorkOrder order = JobChipItem.isWorkOrder(stack) ? JobChipItem.data(stack) : null;
        if (order == null) {
            guiGraphics.drawString(font, Component.translatable("rngtech.dispatch_terminal.taken"), x + 26, y + 10, TEAL_MUTED, false);
            return;
        }
        int textLeft = x + 26;
        int[] button = downloadBox(slot);
        int rewardLeft = button[0] - leftPos - 22;
        String tag = "[" + GlyphItem.numeral(order.tier()) + "]";
        guiGraphics.drawString(font, tag, textLeft, y + 4, AMBER, false);
        int nameLeft = textLeft + font.width(tag) + 4;
        Component heading = Component.translatable("rngtech.dispatch_terminal.heading", JobChipItem.familyName(order.family()), order.minStage());
        guiGraphics.drawString(font, font.plainSubstrByWidth(heading.getString(), rewardLeft - nameLeft - 2), nameLeft, y + 4, TEAL_TEXT, false);
        String job = font.plainSubstrByWidth(JobChipItem.objectiveText(order.objective()).getString(), rewardLeft - textLeft - 2);
        guiGraphics.drawString(font, job, textLeft, y + 15, TEAL_MUTED, false);

        List<ItemStack> rewards = WorkOrderRewards.items(order.rewards());
        if (!rewards.isEmpty()) {
            guiGraphics.renderItem(rewards.getFirst(), rewardLeft, y + 6);
            guiGraphics.renderItemDecorations(font, rewards.getFirst(), rewardLeft, y + 6);
        }
        guiGraphics.drawString(font, Component.translatable("rngtech.dispatch_terminal.take"), button[0] - leftPos + 4, button[1] - topPos + 2, TEAL_TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            if (overRefresh(mouseX, mouseY) && requisitions() > 0) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, DispatchTerminalMenu.BUTTON_REROLL);
                return true;
            }
            for (int slot = 0; slot < 6; slot++) {
                if (!menu.getSlot(slot).getItem().isEmpty() && overDownload(slot, mouseX, mouseY)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, DispatchTerminalMenu.BUTTON_TAKE + slot);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** The Download button of a row, as screen x, y, width, and height. */
    private int[] downloadBox(int slot) {
        int width = font.width(Component.translatable("rngtech.dispatch_terminal.take")) + 8;
        int x = leftPos + DispatchTerminalMenu.ROW_LEFT + DispatchTerminalMenu.ROW_WIDTH - width - 2;
        int y = topPos + DispatchTerminalMenu.rowTop(slot) + (DispatchTerminalMenu.ROW_HEIGHT - BUTTON_HEIGHT) / 2;
        return new int[] {x, y, width, BUTTON_HEIGHT};
    }

    private boolean overRow(int slot, double mouseX, double mouseY) {
        int x = leftPos + DispatchTerminalMenu.ROW_LEFT;
        int y = topPos + DispatchTerminalMenu.rowTop(slot);
        return mouseX >= x && mouseX < x + DispatchTerminalMenu.ROW_WIDTH && mouseY >= y && mouseY < y + DispatchTerminalMenu.ROW_HEIGHT;
    }

    private boolean overDownload(int slot, double mouseX, double mouseY) {
        int[] box = downloadBox(slot);
        return mouseX >= box[0] && mouseX < box[0] + box[2] && mouseY >= box[1] && mouseY < box[1] + box[3];
    }

    private boolean overRefresh(double mouseX, double mouseY) {
        int x = leftPos + 8;
        int y = topPos + FOOTER_Y + 2;
        return mouseX >= x && mouseX < x + refreshWidth() && mouseY >= y && mouseY < y + BUTTON_HEIGHT + 2;
    }

    private int refreshWidth() {
        return font.width(Component.translatable("rngtech.dispatch_terminal.reroll")) + 10;
    }

    private int requisitions() {
        if (minecraft == null || minecraft.player == null) {
            return 0;
        }
        int count = 0;
        Inventory inventory = minecraft.player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.REQUISITION.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
