package com.rngtech.client.screen;

import com.rngtech.content.item.RefinementConsumableItem;
import com.rngtech.content.menu.RefinementMenuSupport;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.corruption.CorruptionCatalog;
import com.rngtech.rpg.corruption.CorruptionPreview;
import com.rngtech.rpg.corruption.CorruptionText;
import com.rngtech.rpg.refinement.RefinementOperation;
import com.rngtech.rpg.refinement.RefinementTargets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

final class RefinementScreenStyle {
    static final int REFINEMENT_TAB_INDEX = 3;
    static final int TARGET_SLOT_X = RefinementMenuSupport.REFINEMENT_TARGET_SLOT_X;
    static final int CONSUMABLE_SLOT_X = RefinementMenuSupport.REFINEMENT_CONSUMABLE_SLOT_X;
    static final int SLOT_Y = RefinementMenuSupport.REFINEMENT_SLOT_Y;
    static final int PROCESSING_TARGET_SLOT_X = 204;
    static final int PROCESSING_TARGET_SLOT_Y = 42;

    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int BUTTON = 0xFF9A9A9A;
    private static final int BUTTON_DARK = 0xFF5F5F5F;
    private static final int BUTTON_LIGHT = 0xFFE0E0E0;
    private static final int APPLY_X = 23;
    private static final int APPLY_Y = 68;
    private static final int APPLY_WIDTH = 62;
    private static final int APPLY_HEIGHT = 18;
    private static final int AFFIX_PANEL_X = 96;
    private static final int AFFIX_PANEL_Y = 22;
    private static final int AFFIX_PANEL_WIDTH = 136;
    private static final int MAX_AFFIX_LINES = 7;

    static void render(GuiGraphics guiGraphics, int leftPos, int topPos, MachineTraits traits, int accent) {
        MachineScreenStyle.AffixLine[] affixLines = affixLines(traits);
        renderSlotFrame(guiGraphics, leftPos, topPos, TARGET_SLOT_X, SLOT_Y);
        renderSlotFrame(guiGraphics, leftPos, topPos, CONSUMABLE_SLOT_X, SLOT_Y);
        renderApplyButton(guiGraphics, leftPos, topPos);
        MachineScreenStyle.renderAffixListPanel(
                guiGraphics,
                leftPos,
                topPos,
                AFFIX_PANEL_X,
                AFFIX_PANEL_Y,
                AFFIX_PANEL_WIDTH,
                affixLines,
                accent
        );
    }

    static void drawLabels(GuiGraphics guiGraphics, Font font, MachineTraits traits, int accent) {
        MachineScreenStyle.AffixLine[] affixLines = affixLines(traits);
        drawCentered(guiGraphics, font, Component.translatable("rngtech.refinement.target"), TARGET_SLOT_X + 8, 28);
        drawCentered(guiGraphics, font, Component.translatable("rngtech.refinement.catalyst.short"), CONSUMABLE_SLOT_X + 8, 28);
        Component apply = Component.translatable("rngtech.refinement.apply");
        guiGraphics.drawString(font, apply, APPLY_X + (APPLY_WIDTH - font.width(apply)) / 2, APPLY_Y + 6, TEXT, false);
        MachineScreenStyle.drawAffixListPanelLabels(
                guiGraphics,
                font,
                traits.isCorrupted()
                        ? Component.translatable(
                                "rngtech.refinement.summary_corrupted",
                                Component.translatable(traits.rarity().translationKey())
                        )
                        : Component.translatable(
                                "rngtech.refinement.summary",
                                Component.translatable(traits.rarity().translationKey()),
                                traits.refinementPotential()
                        ),
                affixLines,
                AFFIX_PANEL_X,
                AFFIX_PANEL_Y,
                AFFIX_PANEL_WIDTH,
                accent
        );
    }

    static void renderTooltips(
            GuiGraphics guiGraphics,
            Font font,
            int leftPos,
            int topPos,
            int mouseX,
            int mouseY,
            boolean active,
            MachineTraits traits,
            AbstractContainerMenu menu
    ) {
        if (!active) {
            return;
        }
        List<Component> corruptionTable = corruptionTable(menu, traits);
        if (!corruptionTable.isEmpty() && menu.getCarried().isEmpty() && overApplyButton(leftPos, topPos, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, corruptionTable, mouseX, mouseY);
            return;
        }
        MachineScreenStyle.renderAffixListPanelTooltip(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                AFFIX_PANEL_X,
                AFFIX_PANEL_Y,
                AFFIX_PANEL_WIDTH,
                affixLines(traits)
        );
    }

    static boolean handleApplyClick(
            AbstractContainerScreen<? extends AbstractContainerMenu> screen,
            AbstractContainerMenu menu,
            double mouseX,
            double mouseY
    ) {
        int x = screen.getGuiLeft() + APPLY_X;
        int y = screen.getGuiTop() + APPLY_Y;
        if (mouseX < x || mouseX >= x + APPLY_WIDTH || mouseY < y || mouseY >= y + APPLY_HEIGHT) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.gameMode != null
                && menu.clickMenuButton(minecraft.player, RefinementMenuSupport.BUTTON_APPLY)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, RefinementMenuSupport.BUTTON_APPLY);
        }
        return true;
    }

    /** The Volatile Catalyst outcome table for the placed machine, shown over Apply while a catalyst is inserted. */
    private static List<Component> corruptionTable(AbstractContainerMenu menu, MachineTraits traits) {
        ItemStack consumable = slotStack(menu, CONSUMABLE_SLOT_X);
        ItemStack machine = slotStack(menu, TARGET_SLOT_X);
        if (!(consumable.getItem() instanceof RefinementConsumableItem item)
                || item.operation() != RefinementOperation.CORRUPT
                || !RefinementTargets.canRefine(machine)) {
            return List.of();
        }
        if (traits.isCorrupted()) {
            return List.of(Component.translatable("rngtech.refinement.failure.corrupted"));
        }
        String host = RefinementTargets.eligibilityProfile(machine).id();
        CorruptionCatalog catalog = CorruptionCatalog.active();
        if (!catalog.canCorrupt(host)) {
            return List.of(Component.translatable("rngtech.refinement.failure.cannot_corrupt"));
        }
        return CorruptionText.table(CorruptionPreview.rows(catalog, host, traits, false), false);
    }

    private static ItemStack slotStack(AbstractContainerMenu menu, int x) {
        for (Slot slot : menu.slots) {
            if (slot.isActive() && slot.x == x && slot.y == SLOT_Y) {
                return slot.getItem();
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean overApplyButton(int leftPos, int topPos, int mouseX, int mouseY) {
        int x = leftPos + APPLY_X;
        int y = topPos + APPLY_Y;
        return mouseX >= x && mouseX < x + APPLY_WIDTH && mouseY >= y && mouseY < y + APPLY_HEIGHT;
    }

    private static MachineScreenStyle.AffixLine[] affixLines(MachineTraits traits) {
        return MachineScreenStyle.fitAffixLines(MachineScreenStyle.machineAffixLines(traits), MAX_AFFIX_LINES);
    }

    private static void renderApplyButton(GuiGraphics guiGraphics, int leftPos, int topPos) {
        int x = leftPos + APPLY_X;
        int y = topPos + APPLY_Y;
        guiGraphics.fill(x, y, x + APPLY_WIDTH, y + APPLY_HEIGHT, BUTTON_DARK);
        guiGraphics.fill(x + 1, y + 1, x + APPLY_WIDTH - 1, y + APPLY_HEIGHT - 1, BUTTON);
        guiGraphics.fill(x + 1, y + 1, x + APPLY_WIDTH - 1, y + 2, BUTTON_LIGHT);
    }

    private static void drawCentered(GuiGraphics guiGraphics, Font font, Component text, int centerX, int y) {
        guiGraphics.drawString(font, text, centerX - font.width(text) / 2, y, TEXT_MUTED, false);
    }

    private static void renderSlotFrame(GuiGraphics guiGraphics, int leftPos, int topPos, int x, int y) {
        int left = leftPos + x - 1;
        int top = topPos + y - 1;
        guiGraphics.fill(left, top, left + 18, top + 18, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + 17, top + 17, 0xFFE0E0E0);
        guiGraphics.fill(left + 2, top + 2, left + 16, top + 16, 0xFF8B8B8B);
    }

    private RefinementScreenStyle() {
    }
}
