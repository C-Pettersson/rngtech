package com.rngtech.client.screen;

import com.rngtech.content.item.GlyphItem;
import com.rngtech.content.item.JobChipItem;
import com.rngtech.content.menu.DriveBaySlot;
import com.rngtech.content.menu.WorkOrderMenuSupport;
import com.rngtech.content.menu.WorkOrderMenuView;
import com.rngtech.content.network.WorkOrderActionPayload;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.workorder.MachineWorkOrders;
import com.rngtech.rpg.workorder.WorkOrder;
import com.rngtech.rpg.workorder.WorkOrderRules;
import com.rngtech.rpg.workorder.WorkPart;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Orders tab inside a machine screen's panel: three drive bays, each a real chip slot with the chip's tier, job, and
 * progress beside it, and a button column for the selected bay (Activate or Pause, and Abandon). The player's inventory
 * shows below, so chips go in and out of the bays by hand. It draws and hit-tests only inside the panel, so it does not
 * depend on where the screen puts its tabs.
 */
final class WorkOrdersTab {
    private static final int ROW_LEFT = DriveBaySlot.X - 2;
    private static final int ROW_HEIGHT = 24;
    private static final int TEXT_LEFT = DriveBaySlot.X + 21;
    private static final int MARGIN = 8;
    private static final int BUTTON_HEIGHT = 13;
    private static final int BUTTON_GAP = 3;
    private static final int COUNT_WIDTH = 52;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int DONE = 0xFF2F7F3F;
    private static final int RUSH = 0xFFB0661A;
    private static final int ROW = 0xFFB4B4B4;
    private static final int ROW_SELECTED = 0xFFC9C9C9;
    private static final int ROW_BORDER = 0xFF8B8B8B;
    private static final int ROW_BORDER_SELECTED = 0xFF3F3F3F;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int BAR_EMPTY = 0xFF7A7A7A;
    private static final int BUTTON = 0xFF9A9A9A;
    private static final int BUTTON_HOVER = 0xFFB0B0B0;
    private static final int BUTTON_DISABLED = 0xFFAFAFAF;
    private static final int BUTTON_DARK = 0xFF5F5F5F;
    private static final int BUTTON_LIGHT = 0xFFE0E0E0;

    private final WorkOrderMenuView view;
    private final int accent;
    private int selected = MachineWorkOrders.NONE;

    private enum Action { ROW, ACTIVATE, PAUSE, RUSH, ABANDON }

    /** A clickable or hoverable part of the tab, in screen coordinates. {@code enabled} is false for a greyed-out button. */
    private record Part(Action action, int bay, int left, int top, int width, int height, boolean enabled) {
        boolean contains(double x, double y) {
            return x >= left && x < left + width && y >= top && y < top + height;
        }
    }

    WorkOrdersTab(WorkOrderMenuView view, int accent) {
        this.view = view;
        this.accent = accent;
    }

    void render(GuiGraphics graphics, int leftPos, int topPos, int width, int height, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        WorkOrderMenuSupport.View orders = view.workOrderView();
        int chosen = selection(orders);
        int right = leftPos + rowRight(width, font);
        for (int bay = 0; bay < MachineWorkOrders.SLOTS; bay++) {
            int top = topPos + rowTop(bay);
            boolean loaded = bay < orders.slots().size();
            boolean active = bay == orders.active();
            boolean picked = loaded && bay == chosen;
            graphics.fill(leftPos + ROW_LEFT, top, right, top + ROW_HEIGHT, active ? accent : picked ? ROW_BORDER_SELECTED : ROW_BORDER);
            graphics.fill(leftPos + ROW_LEFT + 1, top + 1, right - 1, top + ROW_HEIGHT - 1, picked ? ROW_SELECTED : ROW);
            int slotX = leftPos + DriveBaySlot.X - 1;
            int slotY = topPos + DriveBaySlot.Y + bay * DriveBaySlot.STEP - 1;
            graphics.fill(slotX, slotY, slotX + 18, slotY + 18, SLOT_LIGHT);
            graphics.fill(slotX, slotY, slotX + 17, slotY + 17, SLOT_DARK);
            graphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, SLOT_FILL);
            if (!loaded) {
                graphics.drawString(font, Component.translatable("rngtech.work_order.tab.empty_bay"), leftPos + TEXT_LEFT, top + 8, TEXT_MUTED, false);
                continue;
            }
            renderRow(graphics, font, orders.slots().get(bay), active, leftPos + TEXT_LEFT, top, right - 4);
        }
        for (Part part : parts(leftPos, topPos, width, font)) {
            if (part.action() != Action.ROW) {
                renderButton(graphics, font, part, label(part.action()), part.enabled() && part.contains(mouseX, mouseY));
            }
        }
    }

    boolean mouseClicked(double mouseX, double mouseY, int button, int leftPos, int topPos, int width, int height) {
        if (button != 0) {
            return false;
        }
        for (Part part : parts(leftPos, topPos, width, Minecraft.getInstance().font)) {
            if (!part.contains(mouseX, mouseY)) {
                continue;
            }
            if (part.action() == Action.ROW) {
                selected = part.bay();
                return false;
            }
            if (part.enabled()) {
                switch (part.action()) {
                    case ACTIVATE -> send("activate", part.bay());
                    case PAUSE -> send("deactivate", part.bay());
                    case RUSH -> send("rush", part.bay());
                    case ABANDON -> {
                        if (Screen.hasShiftDown()) { send("abandon", part.bay()); }
                    }
                    default -> { }
                }
            }
            return true;
        }
        return false;
    }

    boolean mouseScrolled(double scrollY) {
        return false;
    }

    List<Component> tooltip(double mouseX, double mouseY, int leftPos, int topPos, int width, int height) {
        for (Part part : parts(leftPos, topPos, width, Minecraft.getInstance().font)) {
            if (!part.contains(mouseX, mouseY)) {
                continue;
            }
            return switch (part.action()) {
                case ROW -> overSlot(part.bay(), mouseX, mouseY, leftPos, topPos) ? List.of() : rowTooltip(part.bay());
                case ACTIVATE -> List.of(Component.translatable("rngtech.work_order.tab.activate_hint"));
                case PAUSE -> List.of(Component.translatable("rngtech.work_order.tab.pause_hint"));
                case RUSH -> rushTooltip(part);
                case ABANDON -> List.of(Component.translatable("rngtech.work_order.tab.abandon_hint"));
            };
        }
        return List.of();
    }

    private void renderRow(GuiGraphics graphics, Font font, WorkOrderMenuSupport.Slot slot, boolean active, int left, int top, int right) {
        String tier = GlyphItem.numeral(slot.tier());
        graphics.drawString(font, tier, left, top + 3, active ? accent : TEXT, false);
        int jobLeft = left + font.width(tier) + 4;
        int jobRight = right;
        if (active || slot.done() || slot.rushed()) {
            Component tag = slot.done() ? Component.translatable("rngtech.work_order.tab.done")
                    : slot.rushed() ? Component.translatable("rngtech.work_order.tab.rushed", JobChipItem.clock(slot.rushSeconds()))
                    : Component.translatable("rngtech.work_order.tab.active");
            jobRight -= font.width(tag) + 4;
            graphics.drawString(font, tag, right - font.width(tag), top + 3, slot.done() ? DONE : slot.rushed() ? RUSH : accent, false);
        }
        String job = font.plainSubstrByWidth(JobChipItem.shortObjectiveText(slot.objective()).getString(), jobRight - jobLeft);
        graphics.drawString(font, job, jobLeft, top + 3, TEXT, false);
        int barTop = top + 15;
        int barRight = right - COUNT_WIDTH;
        long target = Math.max(1, slot.totalTarget());
        int filled = (int) Math.round((barRight - left - 2) * Math.min(1.0, slot.totalProgress() / (double) target));
        graphics.fill(left, barTop, barRight, barTop + 5, ROW_BORDER);
        graphics.fill(left + 1, barTop + 1, barRight - 1, barTop + 4, BAR_EMPTY);
        graphics.fill(left + 1, barTop + 1, left + 1 + filled, barTop + 4, slot.done() ? DONE : accent);
        Component count = JobChipItem.progressText(slot.objective(), slot.totalProgress());
        graphics.drawString(font, count, right - font.width(count), barTop - 2, active ? TEXT : TEXT_MUTED, false);
    }

    /** Rows and buttons, laid out the same for drawing, clicks, and tooltips. */
    private List<Part> parts(int leftPos, int topPos, int width, Font font) {
        List<Part> parts = new ArrayList<>();
        WorkOrderMenuSupport.View orders = view.workOrderView();
        int right = rowRight(width, font);
        for (int bay = 0; bay < orders.slots().size(); bay++) {
            parts.add(new Part(Action.ROW, bay, leftPos + ROW_LEFT, topPos + rowTop(bay), right - ROW_LEFT, ROW_HEIGHT, true));
        }
        int chosen = selection(orders);
        boolean any = chosen != MachineWorkOrders.NONE && !orders.slots().get(chosen).done();
        boolean active = any && chosen == orders.active();
        int buttonWidth = buttonWidth(font);
        int top = topPos + rowTop(0);
        boolean rushed = any && orders.slots().get(chosen).rushed();
        for (Action action : new Action[] {active ? Action.PAUSE : Action.ACTIVATE, Action.RUSH, Action.ABANDON}) {
            boolean enabled = any && (action != Action.RUSH || !rushed);
            parts.add(new Part(action, chosen, leftPos + width - MARGIN - buttonWidth, top, buttonWidth, BUTTON_HEIGHT, enabled));
            top += BUTTON_HEIGHT + BUTTON_GAP;
        }
        return parts;
    }

    /** Rush's terms for the selected chip, shown before the player commits; a chip under Rush shows its time left. */
    private List<Component> rushTooltip(Part part) {
        WorkOrderMenuSupport.View orders = view.workOrderView();
        if (part.bay() < 0 || part.bay() >= orders.slots().size()) {
            return List.of(Component.translatable("rngtech.work_order.tab.rush_none"));
        }
        WorkOrderMenuSupport.Slot slot = orders.slots().get(part.bay());
        if (slot.rushed()) {
            return List.of(Component.translatable("rngtech.work_order.tab.rush_running", JobChipItem.clock(slot.rushSeconds())));
        }
        ItemStack chip = view.workOrderChip(part.bay());
        WorkOrder order = JobChipItem.data(chip);
        MachineTraits traits = JobChipItem.traits(chip);
        long ticks = order == null ? WorkOrder.rushTicks(slot.tier()) : WorkOrderRules.rushTicks(order, traits);
        return List.of(Component.translatable("rngtech.work_order.tab.rush_hint", JobChipItem.clock(ticks / 20), WorkOrderRules.rushBonus(traits)),
                Component.translatable("rngtech.work_order.tab.rush_warning").withStyle(ChatFormatting.RED));
    }

    /** The bay the buttons act on: the one the player clicked, else the active chip, else the first. */
    private int selection(WorkOrderMenuSupport.View orders) {
        if (selected >= 0 && selected < orders.slots().size()) {
            return selected;
        }
        selected = MachineWorkOrders.NONE;
        if (orders.active() != MachineWorkOrders.NONE) {
            return orders.active();
        }
        return orders.slots().isEmpty() ? MachineWorkOrders.NONE : 0;
    }

    /** The chip's own tooltip, with each part's progress for a job of several parts, and what clicking the row does. */
    private List<Component> rowTooltip(int bay) {
        WorkOrderMenuSupport.Slot slot = view.workOrderView().slots().get(bay);
        ItemStack chip = view.workOrderChip(bay);
        List<Component> lines = new ArrayList<>(chip.isEmpty() ? List.of() : Screen.getTooltipFromItem(Minecraft.getInstance(), chip));
        if (slot.objective().parts().size() > 1) {
            for (int part = 0; part < slot.objective().parts().size(); part++) {
                WorkPart value = slot.objective().parts().get(part);
                lines.add(Component.translatable("rngtech.work_order.tab.part_progress", JobChipItem.itemName(value.item()),
                        slot.progress().get(part), value.target()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        lines.add(Component.translatable(slot.done() ? "rngtech.work_order.tab.done_hint" : "rngtech.work_order.tab.select_hint")
                .withStyle(slot.done() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        return lines;
    }

    private static boolean overSlot(int bay, double mouseX, double mouseY, int leftPos, int topPos) {
        int x = leftPos + DriveBaySlot.X;
        int y = topPos + DriveBaySlot.Y + bay * DriveBaySlot.STEP;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    private static int rowTop(int bay) {
        return DriveBaySlot.Y - 4 + bay * DriveBaySlot.STEP;
    }

    private static int rowRight(int width, Font font) {
        return width - MARGIN - buttonWidth(font) - 4;
    }

    private static Component label(Action action) {
        return Component.translatable("rngtech.work_order.tab." + action.name().toLowerCase(Locale.ROOT));
    }

    private static int buttonWidth(Font font) {
        int widest = 0;
        for (Action action : new Action[] {Action.ACTIVATE, Action.PAUSE, Action.RUSH, Action.ABANDON}) {
            widest = Math.max(widest, font.width(label(action)));
        }
        return widest + 10;
    }

    private static void renderButton(GuiGraphics graphics, Font font, Part part, Component label, boolean hovered) {
        int x = part.left();
        int y = part.top();
        graphics.fill(x, y, x + part.width(), y + part.height(), BUTTON_DARK);
        graphics.fill(x + 1, y + 1, x + part.width() - 1, y + part.height() - 1,
                !part.enabled() ? BUTTON_DISABLED : hovered ? BUTTON_HOVER : BUTTON);
        if (part.enabled()) {
            graphics.fill(x + 1, y + 1, x + part.width() - 1, y + 2, BUTTON_LIGHT);
        }
        graphics.drawString(font, label, x + (part.width() - font.width(label)) / 2, y + 3, part.enabled() ? TEXT : TEXT_MUTED, false);
    }

    private void send(String action, int bay) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            PacketDistributor.sendToServer(new WorkOrderActionPayload(player.containerMenu.containerId, action, Integer.toString(bay)));
        }
    }
}
