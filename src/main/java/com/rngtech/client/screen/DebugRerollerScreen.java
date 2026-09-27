package com.rngtech.client.screen;

import com.rngtech.content.menu.DebugRerollerMenu;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.refinement.RefinementTargets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class DebugRerollerScreen extends AbstractContainerScreen<DebugRerollerMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int BUTTON = 0xFF6C7658;
    private static final int BUTTON_DARK = 0xFF3E4634;
    private static final int BUTTON_LIGHT = 0xFFA5B17E;
    private static final int STAT_ACCENT = 0xFFE09B48;
    private static final int TARGET_SLOT_X = 48;
    private static final int TARGET_SLOT_Y = 42;
    private static final int BUTTON_X = 24;
    private static final int BUTTON_Y = 68;
    private static final int BUTTON_WIDTH = 66;
    private static final int BUTTON_HEIGHT = 18;
    private static final int AFFIX_PANEL_X = 96;
    private static final int AFFIX_PANEL_Y = 22;
    private static final int AFFIX_PANEL_WIDTH = 136;
    private static final int MAX_AFFIX_LINES = 7;

    public DebugRerollerScreen(DebugRerollerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 240;
        imageHeight = 200;
        inventoryLabelX = 39;
        inventoryLabelY = 104;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
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
                affixLines()
        );
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        renderSlotFrame(guiGraphics, TARGET_SLOT_X - 1, TARGET_SLOT_Y - 1);
        renderRerollButton(guiGraphics);
        MachineScreenStyle.renderAffixListPanel(
                guiGraphics,
                leftPos,
                topPos,
                AFFIX_PANEL_X,
                AFFIX_PANEL_Y,
                AFFIX_PANEL_WIDTH,
                affixLines(),
                STAT_ACCENT
        );
        renderPlayerInventoryFrames(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.debug_reroller.target"), 40, 28, TEXT_MUTED, false);

        Component reroll = Component.translatable("rngtech.debug_reroller.reroll");
        guiGraphics.drawString(
                font,
                reroll,
                BUTTON_X + (BUTTON_WIDTH - font.width(reroll)) / 2,
                BUTTON_Y + 6,
                0xFFFFFFFF,
                false
        );
        MachineScreenStyle.drawAffixListPanelLabels(
                guiGraphics,
                font,
                Component.translatable(
                        "rngtech.debug_reroller.summary",
                        Component.translatable(targetTraits().rarity().translationKey()),
                        targetTraits().refinementPotential()
                ),
                affixLines(),
                AFFIX_PANEL_X,
                AFFIX_PANEL_Y,
                AFFIX_PANEL_WIDTH,
                STAT_ACCENT
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && handleRerollClick(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleRerollClick(double mouseX, double mouseY) {
        int x = leftPos + BUTTON_X;
        int y = topPos + BUTTON_Y;
        if (mouseX < x || mouseX >= x + BUTTON_WIDTH || mouseY < y || mouseY >= y + BUTTON_HEIGHT) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.gameMode != null
                && menu.clickMenuButton(minecraft.player, DebugRerollerMenu.BUTTON_REROLL)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, DebugRerollerMenu.BUTTON_REROLL);
        }
        return true;
    }

    private MachineTraits targetTraits() {
        ItemStack target = menu.getSlot(0).getItem();
        return RefinementTargets.canRefine(target) ? RefinementTargets.traits(target) : MachineTraits.EMPTY;
    }

    private MachineScreenStyle.AffixLine[] affixLines() {
        return MachineScreenStyle.fitAffixLines(MachineScreenStyle.machineAffixLines(targetTraits()), MAX_AFFIX_LINES);
    }

    private void renderPanel(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        guiGraphics.fill(x, y, x + imageWidth, y + 1, PANEL_LIGHT);
        guiGraphics.fill(x, y, x + 1, y + imageHeight, PANEL_LIGHT);
        guiGraphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, PANEL_DARK);
        guiGraphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, PANEL_DARK);
    }

    private void renderRerollButton(GuiGraphics guiGraphics) {
        int x = leftPos + BUTTON_X;
        int y = topPos + BUTTON_Y;
        guiGraphics.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, BUTTON_DARK);
        guiGraphics.fill(x + 1, y + 1, x + BUTTON_WIDTH - 1, y + BUTTON_HEIGHT - 1, BUTTON);
        guiGraphics.fill(x + 1, y + 1, x + BUTTON_WIDTH - 1, y + 2, BUTTON_LIGHT);
    }

    private void renderPlayerInventoryFrames(GuiGraphics guiGraphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlotFrame(guiGraphics, 38 + column * 18, 115 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderSlotFrame(guiGraphics, 38 + column * 18, 173);
        }
    }

    private void renderSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + 18, top + 18, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + 17, top + 17, 0xFFE0E0E0);
        guiGraphics.fill(left + 2, top + 2, left + 16, top + 16, 0xFF8B8B8B);
    }
}
