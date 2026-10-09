package com.rngtech.client.screen;

import com.rngtech.content.item.ModularToolItem;
import com.rngtech.content.item.RefinementConsumableItem;
import com.rngtech.content.menu.ToolBenchMenu;
import com.rngtech.content.tool.ToolBaseStatCatalog;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineModifierText;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierDefinition;
import com.rngtech.rpg.ModifierEligibilityProfile;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.refinement.RefinementEngine;
import com.rngtech.rpg.refinement.RefinementOperation;
import com.rngtech.rpg.refinement.RefinementTargets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ToolBenchScreen extends AbstractContainerScreen<ToolBenchMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int TEXT_GOOD = 0xFF2F6B3F;
    private static final int TEXT_BAD = 0xFF8A3C32;
    private static final int ROW = 0xFFE0E0E0;
    private static final int ROW_DISABLED = 0xFFB8B8B8;
    private static final int ROW_SELECTED = 0xFFB58F52;
    private static final int BUTTON = 0xFF5E6E73;
    private static final int BUTTON_DARK = 0xFF354246;
    private static final int BUTTON_LIGHT = 0xFF93A7AD;
    private static final int APPLY_X = 154;
    private static final int APPLY_Y = 35;
    private static final int REMOVE_X = 154;
    private static final int REMOVE_Y = 59;
    private static final int DISASSEMBLE_X = 154;
    private static final int DISASSEMBLE_Y = 83;
    private static final int BUTTON_WIDTH = 72;
    private static final int BUTTON_HEIGHT = 18;
    private static final int TAB_WIDTH = 54;
    private static final int TAB_SPACING = 56;
    private static final int REFINE_SLOT_Y = 31;
    private static final int REFINE_LABEL_Y = 20;
    private static final int REFINE_TOOL_FRAME_X = 13;
    private static final int REFINE_HEAD_FRAME_X = 42;
    private static final int REFINE_ROD_FRAME_X = 71;
    private static final int REFINE_CONSUMABLE_FRAME_X = 100;
    private static final int TARGET_BUTTON_Y = 60;
    private static final int TARGET_BUTTON_WIDTH = 36;
    private static final int TARGET_BUTTON_HEIGHT = 16;
    private static final int TARGET_HEAD_BUTTON_X = 13;
    private static final int TARGET_ROD_BUTTON_X = 56;
    private static final int REFINE_BUTTON_X = 13;
    private static final int REFINE_BUTTON_Y = 84;
    private static final int REFINE_BUTTON_WIDTH = 79;
    private static final int REFINE_RP_X = 13;
    private static final int REFINE_RP_Y = 108;
    private static final int REFINE_RP_WIDTH = 79;
    private static final int REFINE_RP_HEIGHT = 16;
    private static final int PLAYER_INVENTORY_X = 132;
    private static final int PLAYER_INVENTORY_Y = 143;
    private static final int HOTBAR_Y = 201;
    private static final int MOD_PANEL_X = 124;
    private static final int MOD_PANEL_Y = 22;
    private static final int MOD_PANEL_WIDTH = 92;
    private static final int OUTCOME_PANEL_X = 224;
    private static final int OUTCOME_PANEL_Y = 22;
    private static final int OUTCOME_PANEL_WIDTH = 124;
    private static final int REFINEMENT_PANEL_HEIGHT = 100;
    private static final int ROW_HEIGHT = 14;
    private static final int MAX_ROWS = 6;
    private static final int MAX_OUTCOME_LINES = 6;
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 14;
    private static final int STAT_PANEL_WIDTH = 216;
    private static final int STAT_ACCENT = 0xFF7F9ED7;
    private static final MachineStat[] TOOL_STATS = {
            MachineStat.MINING_LEVEL,
            MachineStat.MINING_SPEED,
            MachineStat.ATTACK_SPEED,
            MachineStat.DURABILITY,
            MachineStat.SELF_REPAIR,
            MachineStat.BATTERY_SUPPORT,
            MachineStat.FE_TRANSFER,
            MachineStat.FE_USAGE,
            MachineStat.AREA_WIDTH,
            MachineStat.AREA_HEIGHT,
            MachineStat.TREE_FELL_LIMIT,
            MachineStat.VEIN_MINE_LIMIT,
            MachineStat.VEIN_MINE_FE_USAGE,
            MachineStat.ORE_BURST_SPEED,
            MachineStat.ORE_BURST_FE_USAGE,
            MachineStat.LUCK,
            MachineStat.STABILITY,
            MachineStat.CONTROL
    };

    public ToolBenchScreen(ToolBenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 360;
        imageHeight = 226;
        inventoryLabelX = PLAYER_INVENTORY_X;
        inventoryLabelY = 130;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderStatTooltips(guiGraphics, mouseX, mouseY);
        renderRefinementTooltips(guiGraphics, mouseX, mouseY);
        GearSlotTooltips.render(this, guiGraphics, font, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        renderTabs(guiGraphics);
        renderSlotFrames(guiGraphics);
        if (menu.selectedTab() == ToolBenchMenu.TAB_STATS) {
            renderStats(guiGraphics);
        } else if (menu.selectedTab() == ToolBenchMenu.TAB_ASSEMBLY) {
            renderAssembly(guiGraphics);
        } else if (menu.selectedTab() == ToolBenchMenu.TAB_REFINEMENT) {
            renderRefinement(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() == ToolBenchMenu.TAB_STATS) {
            drawStatsLabels(guiGraphics);
        } else if (menu.selectedTab() == ToolBenchMenu.TAB_GEAR) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
            drawGearLabel(guiGraphics, Component.translatable("rngtech.tool_bench.anvil"), 82);
            drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.energy_connector.short"), 118);
        } else if (menu.selectedTab() == ToolBenchMenu.TAB_REFINEMENT) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
            drawSlotLabel(guiGraphics, Component.translatable("rngtech.tool_bench.tool"), REFINE_TOOL_FRAME_X);
            drawSlotLabel(guiGraphics, Component.translatable("rngtech.tool_bench.head"), REFINE_HEAD_FRAME_X);
            drawSlotLabel(guiGraphics, Component.translatable("rngtech.tool_bench.rod"), REFINE_ROD_FRAME_X);
            drawSlotLabel(guiGraphics, Component.translatable("rngtech.refinement.catalyst.short"), REFINE_CONSUMABLE_FRAME_X);
            drawTargetButtonLabel(guiGraphics, Component.translatable("rngtech.tool_bench.head"), TARGET_HEAD_BUTTON_X);
            drawTargetButtonLabel(guiGraphics, Component.translatable("rngtech.tool_bench.rod"), TARGET_ROD_BUTTON_X);
            drawRefineButtonLabel(guiGraphics, Component.translatable("rngtech.tool_bench.apply"));
            drawRpLabel(guiGraphics);
            guiGraphics.drawString(font, Component.translatable("rngtech.refinement.modifiers"), MOD_PANEL_X, 12, TEXT_MUTED, false);
            guiGraphics.drawString(font, Component.translatable("rngtech.refinement.outcome"), OUTCOME_PANEL_X, 12, TEXT_MUTED, false);
            drawSelectionLabels(guiGraphics);
            drawOutcomeLabels(guiGraphics);
        } else {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
            guiGraphics.drawString(font, Component.translatable("rngtech.tool_bench.tool"), 21, 30, TEXT_MUTED, false);
            guiGraphics.drawString(font, Component.translatable("rngtech.tool_bench.head"), 69, 18, TEXT_MUTED, false);
            guiGraphics.drawString(font, Component.translatable("rngtech.tool_bench.rod"), 72, 82, TEXT_MUTED, false);
            guiGraphics.drawString(font, Component.translatable("rngtech.tool_bench.cell"), 118, 30, TEXT_MUTED, false);
            guiGraphics.drawString(font, Component.translatable("rngtech.tool_bench.repair"), 112, 60, TEXT_MUTED, false);
            drawCentered(guiGraphics, Component.translatable("rngtech.tool_bench.apply"), APPLY_X, APPLY_Y);
            drawCentered(guiGraphics, Component.translatable("rngtech.tool_bench.remove_cell"), REMOVE_X, REMOVE_Y);
            drawCentered(guiGraphics, Component.translatable("rngtech.tool_bench.disassemble"), DISASSEMBLE_X, DISASSEMBLE_Y);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOverTab(mouseX, mouseY, 0)) {
                selectTab(ToolBenchMenu.TAB_ASSEMBLY);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 1)) {
                selectTab(ToolBenchMenu.TAB_GEAR);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 2)) {
                selectTab(ToolBenchMenu.TAB_STATS);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 3)) {
                selectTab(ToolBenchMenu.TAB_REFINEMENT);
                return true;
            }
            if (menu.selectedTab() == ToolBenchMenu.TAB_ASSEMBLY) {
                if (handleButton(mouseX, mouseY, APPLY_X, APPLY_Y, ToolBenchMenu.BUTTON_APPLY)) {
                    return true;
                }
                if (handleButton(mouseX, mouseY, REMOVE_X, REMOVE_Y, ToolBenchMenu.BUTTON_REMOVE_BATTERY)) {
                    return true;
                }
                if (handleButton(mouseX, mouseY, DISASSEMBLE_X, DISASSEMBLE_Y, ToolBenchMenu.BUTTON_DISASSEMBLE)) {
                    return true;
                }
            } else if (menu.selectedTab() == ToolBenchMenu.TAB_REFINEMENT) {
                if (handleTargetButton(mouseX, mouseY, TARGET_HEAD_BUTTON_X, ToolBenchMenu.BUTTON_SELECT_HEAD)) {
                    return true;
                }
                if (handleTargetButton(mouseX, mouseY, TARGET_ROD_BUTTON_X, ToolBenchMenu.BUTTON_SELECT_ROD)) {
                    return true;
                }
                if (handleRefineButton(mouseX, mouseY)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void selectTab(int tab) {
        menu.selectTab(tab);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ToolBenchMenu.BUTTON_TAB_BASE + tab);
        }
    }

    private boolean handleTargetButton(double mouseX, double mouseY, int buttonX, int id) {
        int x = leftPos + buttonX;
        int y = topPos + TARGET_BUTTON_Y;
        if (mouseX < x || mouseX >= x + TARGET_BUTTON_WIDTH || mouseY < y || mouseY >= y + TARGET_BUTTON_HEIGHT) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.gameMode != null && menu.clickMenuButton(minecraft.player, id)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
        return true;
    }

    private boolean handleRefineButton(double mouseX, double mouseY) {
        int x = leftPos + REFINE_BUTTON_X;
        int y = topPos + REFINE_BUTTON_Y;
        if (mouseX < x
                || mouseX >= x + REFINE_BUTTON_WIDTH
                || mouseY < y
                || mouseY >= y + BUTTON_HEIGHT) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null
                && minecraft.gameMode != null
                && menu.clickMenuButton(minecraft.player, ToolBenchMenu.BUTTON_REFINE)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ToolBenchMenu.BUTTON_REFINE);
        }
        return true;
    }

    private boolean handleButton(double mouseX, double mouseY, int buttonX, int buttonY, int id) {
        int x = leftPos + buttonX;
        int y = topPos + buttonY;
        if (mouseX < x || mouseX >= x + BUTTON_WIDTH || mouseY < y || mouseY >= y + BUTTON_HEIGHT) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.gameMode != null && menu.clickMenuButton(minecraft.player, id)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
        return true;
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

    private void renderTabs(GuiGraphics guiGraphics) {
        renderTab(guiGraphics, 0, Component.translatable("rngtech.tab.assembly"), menu.selectedTab() == ToolBenchMenu.TAB_ASSEMBLY);
        renderTab(guiGraphics, 1, Component.translatable("rngtech.tab.gear"), menu.selectedTab() == ToolBenchMenu.TAB_GEAR);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.tab.stats"), menu.selectedTab() == ToolBenchMenu.TAB_STATS);
        renderTab(guiGraphics, 3, Component.translatable("rngtech.tab.refinement.short"), menu.selectedTab() == ToolBenchMenu.TAB_REFINEMENT);
    }

    private void renderTab(GuiGraphics guiGraphics, int index, Component label, boolean selected) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        int color = selected ? PANEL : 0xFF9A9A9A;
        guiGraphics.fill(x, y, x + TAB_WIDTH, y + 21, color);
        guiGraphics.fill(x, y, x + TAB_WIDTH, y + 1, PANEL_LIGHT);
        guiGraphics.fill(x, y, x + 1, y + 21, PANEL_LIGHT);
        guiGraphics.fill(x + TAB_WIDTH - 1, y, x + TAB_WIDTH, y + 21, PANEL_DARK);
        if (!selected) {
            guiGraphics.fill(x, y + 20, x + TAB_WIDTH, y + 21, PANEL_DARK);
        }
        guiGraphics.drawString(font, label, x + (TAB_WIDTH - font.width(label)) / 2, y + 7, selected ? TEXT : 0xFF2F2F2F, false);
    }

    private void renderSlotFrames(GuiGraphics guiGraphics) {
        if (menu.selectedTab() == ToolBenchMenu.TAB_ASSEMBLY) {
            renderSlotFrame(guiGraphics, 24, 42);
            renderSlotFrame(guiGraphics, 71, 29);
            renderSlotFrame(guiGraphics, 71, 55);
            renderSlotFrame(guiGraphics, 118, 42);
            renderSlotFrame(guiGraphics, 118, 68);
            renderPlayerInventoryFrames(guiGraphics);
        } else if (menu.selectedTab() == ToolBenchMenu.TAB_GEAR) {
            renderSlotFrame(guiGraphics, 82, 47);
            renderSlotFrame(guiGraphics, 118, 47);
            renderPlayerInventoryFrames(guiGraphics);
        } else if (menu.selectedTab() == ToolBenchMenu.TAB_REFINEMENT) {
            renderSlotFrame(guiGraphics, REFINE_TOOL_FRAME_X, REFINE_SLOT_Y);
            renderSlotFrame(guiGraphics, REFINE_HEAD_FRAME_X, REFINE_SLOT_Y);
            renderSlotFrame(guiGraphics, REFINE_ROD_FRAME_X, REFINE_SLOT_Y);
            renderSlotFrame(guiGraphics, REFINE_CONSUMABLE_FRAME_X, REFINE_SLOT_Y);
            renderPlayerInventoryFrames(guiGraphics);
        }
    }

    private void renderAssembly(GuiGraphics guiGraphics) {
        renderButton(guiGraphics, APPLY_X, APPLY_Y);
        renderButton(guiGraphics, REMOVE_X, REMOVE_Y);
        renderButton(guiGraphics, DISASSEMBLE_X, DISASSEMBLE_Y);
    }

    private void renderRefinement(GuiGraphics guiGraphics) {
        renderTargetButton(guiGraphics, TARGET_HEAD_BUTTON_X, menu.selectedHeadTarget());
        renderTargetButton(guiGraphics, TARGET_ROD_BUTTON_X, !menu.selectedHeadTarget());
        renderRefineButton(guiGraphics);
        renderRpReadout(guiGraphics);
        renderSelectionPanel(guiGraphics);
        renderOutcomePanel(guiGraphics);
        if (menu.hasValidToolForRefinement()) {
            renderRefinementTarget(guiGraphics, menu.headRefinementTarget(), REFINE_HEAD_FRAME_X, REFINE_SLOT_Y);
            renderRefinementTarget(guiGraphics, menu.rodRefinementTarget(), REFINE_ROD_FRAME_X, REFINE_SLOT_Y);
        }
    }

    private void renderRefinementTarget(GuiGraphics guiGraphics, ItemStack stack, int frameX, int frameY) {
        if (stack.isEmpty()) {
            return;
        }
        guiGraphics.renderItem(stack, leftPos + frameX + 1, topPos + frameY + 1);
    }

    private void renderStats(GuiGraphics guiGraphics) {
        MachineScreenStyle.renderStatPanel(
                guiGraphics,
                leftPos,
                topPos,
                STAT_PANEL_X,
                STAT_PANEL_Y,
                STAT_PANEL_WIDTH,
                statLines().length,
                STAT_ACCENT
        );
    }

    private void drawStatsLabels(GuiGraphics guiGraphics) {
        MachineScreenStyle.drawStatPanelLabels(
                guiGraphics,
                font,
                Component.translatable("rngtech.tab.stats"),
                statLines(),
                STAT_PANEL_X,
                STAT_PANEL_Y,
                STAT_PANEL_WIDTH,
                STAT_ACCENT
        );
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != ToolBenchMenu.TAB_STATS) {
            return;
        }
        MachineScreenStyle.renderStatPanelTooltip(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                STAT_PANEL_X,
                STAT_PANEL_Y,
                STAT_PANEL_WIDTH,
                statLines()
        );
    }

    private void renderRefinementTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != ToolBenchMenu.TAB_REFINEMENT) {
            return;
        }
        if (isOverTargetButton(mouseX, mouseY, TARGET_HEAD_BUTTON_X)) {
            guiGraphics.renderTooltip(font, Component.translatable("rngtech.tool_bench.refine_head.tooltip"), mouseX, mouseY);
            return;
        }
        if (isOverTargetButton(mouseX, mouseY, TARGET_ROD_BUTTON_X)) {
            guiGraphics.renderTooltip(font, Component.translatable("rngtech.tool_bench.refine_rod.tooltip"), mouseX, mouseY);
            return;
        }
        if (isOverRefineButton(mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, refineButtonTooltip(), mouseX, mouseY);
            return;
        }
        if (hoveredRpReadout(mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable("rngtech.tooltip.refinement_potential", selectedTargetTraits().refinementPotential()),
                    mouseX,
                    mouseY
            );
            return;
        }
        ItemStack head = menu.headRefinementTarget();
        if (menu.hasValidToolForRefinement()
                && !head.isEmpty()
                && isOverRefinementFrame(mouseX, mouseY, REFINE_HEAD_FRAME_X, REFINE_SLOT_Y)) {
            guiGraphics.renderTooltip(font, head, mouseX, mouseY);
            return;
        }
        ItemStack rod = menu.rodRefinementTarget();
        if (menu.hasValidToolForRefinement()
                && !rod.isEmpty()
                && isOverRefinementFrame(mouseX, mouseY, REFINE_ROD_FRAME_X, REFINE_SLOT_Y)) {
            guiGraphics.renderTooltip(font, rod, mouseX, mouseY);
            return;
        }
        if (isOverRefinementFrame(mouseX, mouseY, REFINE_TOOL_FRAME_X, REFINE_SLOT_Y) && menu.toolStack().isEmpty()) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable("rngtech.tool_bench.refine_tool_slot.tooltip"),
                    mouseX,
                    mouseY
            );
            return;
        }
        if (isOverRefinementFrame(mouseX, mouseY, REFINE_HEAD_FRAME_X, REFINE_SLOT_Y) && head.isEmpty()) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable("rngtech.tool_bench.refine_head_slot.tooltip"),
                    mouseX,
                    mouseY
            );
            return;
        }
        if (isOverRefinementFrame(mouseX, mouseY, REFINE_ROD_FRAME_X, REFINE_SLOT_Y) && rod.isEmpty()) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable("rngtech.tool_bench.refine_rod_slot.tooltip"),
                    mouseX,
                    mouseY
            );
            return;
        }
        SelectionRow selectionRow = hoveredSelectionRow(mouseX, mouseY);
        if (selectionRow != null) {
            guiGraphics.renderTooltip(font, selectionRow.tooltip(), mouseX, mouseY);
            return;
        }
        OutcomeLine outcomeLine = hoveredOutcomeLine(mouseX, mouseY);
        if (outcomeLine != null) {
            guiGraphics.renderTooltip(font, outcomeLine.tooltip(), mouseX, mouseY);
        }
    }

    private List<Component> refineButtonTooltip() {
        return List.of(
                Component.translatable("rngtech.tool_bench.refine_selected.tooltip"),
                refinementSummary(menu.selectedRefinementTarget())
        );
    }

    private boolean isOverRefinementFrame(int mouseX, int mouseY, int frameX, int frameY) {
        int x = leftPos + frameX;
        int y = topPos + frameY;
        return mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
    }

    private boolean isOverTargetButton(int mouseX, int mouseY, int buttonX) {
        int x = leftPos + buttonX;
        int y = topPos + TARGET_BUTTON_Y;
        return mouseX >= x
                && mouseX < x + TARGET_BUTTON_WIDTH
                && mouseY >= y
                && mouseY < y + TARGET_BUTTON_HEIGHT;
    }

    private boolean isOverRefineButton(int mouseX, int mouseY) {
        int x = leftPos + REFINE_BUTTON_X;
        int y = topPos + REFINE_BUTTON_Y;
        return mouseX >= x && mouseX < x + REFINE_BUTTON_WIDTH && mouseY >= y && mouseY < y + BUTTON_HEIGHT;
    }

    private void renderTargetButton(GuiGraphics guiGraphics, int buttonX, boolean selected) {
        int x = leftPos + buttonX;
        int y = topPos + TARGET_BUTTON_Y;
        int body = selected ? ROW_SELECTED : BUTTON;
        guiGraphics.fill(x, y, x + TARGET_BUTTON_WIDTH, y + TARGET_BUTTON_HEIGHT, BUTTON_DARK);
        guiGraphics.fill(x + 1, y + 1, x + TARGET_BUTTON_WIDTH - 1, y + TARGET_BUTTON_HEIGHT - 1, body);
        guiGraphics.fill(x + 1, y + 1, x + TARGET_BUTTON_WIDTH - 1, y + 2, BUTTON_LIGHT);
    }

    private void renderRefineButton(GuiGraphics guiGraphics) {
        int x = leftPos + REFINE_BUTTON_X;
        int y = topPos + REFINE_BUTTON_Y;
        guiGraphics.fill(x, y, x + REFINE_BUTTON_WIDTH, y + BUTTON_HEIGHT, BUTTON_DARK);
        guiGraphics.fill(x + 1, y + 1, x + REFINE_BUTTON_WIDTH - 1, y + BUTTON_HEIGHT - 1, BUTTON);
        guiGraphics.fill(x + 1, y + 1, x + REFINE_BUTTON_WIDTH - 1, y + 2, BUTTON_LIGHT);
    }

    private void renderRpReadout(GuiGraphics guiGraphics) {
        int x = leftPos + REFINE_RP_X;
        int y = topPos + REFINE_RP_Y;
        guiGraphics.fill(x, y, x + REFINE_RP_WIDTH, y + REFINE_RP_HEIGHT, 0xFF595959);
        guiGraphics.fill(x + 1, y + 1, x + REFINE_RP_WIDTH - 1, y + REFINE_RP_HEIGHT - 1, 0xFFD5D5D5);
        guiGraphics.fill(x + 1, y + 1, x + REFINE_RP_WIDTH - 1, y + 2, PANEL_LIGHT);
    }

    private void renderSelectionPanel(GuiGraphics guiGraphics) {
        int x = leftPos + MOD_PANEL_X - 2;
        int y = topPos + MOD_PANEL_Y - 2;
        guiGraphics.fill(x, y, x + MOD_PANEL_WIDTH + 4, y + REFINEMENT_PANEL_HEIGHT, 0xFF9C9C9C);
        guiGraphics.fill(x + 1, y + 1, x + MOD_PANEL_WIDTH + 3, y + REFINEMENT_PANEL_HEIGHT - 1, 0xFFD5D5D5);

        for (SelectionRow row : selectionRows()) {
            int color = row.enabled() ? ROW : ROW_DISABLED;
            int rowX = leftPos + MOD_PANEL_X;
            int rowY = topPos + row.y();
            guiGraphics.fill(rowX, rowY, rowX + MOD_PANEL_WIDTH, rowY + ROW_HEIGHT - 1, color);
        }
    }

    private void drawSelectionLabels(GuiGraphics guiGraphics) {
        List<SelectionRow> rows = selectionRows();
        if (rows.isEmpty()) {
            drawClipped(
                    guiGraphics,
                    Component.translatable("rngtech.refinement.no_target_modifiers"),
                    MOD_PANEL_X + 3,
                    MOD_PANEL_Y + 4,
                    MOD_PANEL_WIDTH - 6,
                    TEXT_MUTED
            );
            return;
        }

        for (SelectionRow row : rows) {
            drawClipped(
                    guiGraphics,
                    row.title(),
                    MOD_PANEL_X + 3,
                    row.y() + 2,
                    MOD_PANEL_WIDTH - 6,
                    row.enabled() ? TEXT : TEXT_MUTED
            );
        }
    }

    private void renderOutcomePanel(GuiGraphics guiGraphics) {
        int x = leftPos + OUTCOME_PANEL_X - 2;
        int y = topPos + OUTCOME_PANEL_Y - 2;
        guiGraphics.fill(x, y, x + OUTCOME_PANEL_WIDTH + 4, y + REFINEMENT_PANEL_HEIGHT, 0xFF9C9C9C);
        guiGraphics.fill(x + 1, y + 1, x + OUTCOME_PANEL_WIDTH + 3, y + REFINEMENT_PANEL_HEIGHT - 1, 0xFFD5D5D5);
    }

    private void drawOutcomeLabels(GuiGraphics guiGraphics) {
        List<OutcomeLine> lines = outcomeLines();
        for (int index = 0; index < lines.size() && index < MAX_OUTCOME_LINES; index++) {
            OutcomeLine line = lines.get(index);
            drawClipped(
                    guiGraphics,
                    line.text(),
                    OUTCOME_PANEL_X + 3,
                    OUTCOME_PANEL_Y + 4 + index * ROW_HEIGHT,
                    OUTCOME_PANEL_WIDTH - 6,
                    line.color()
            );
        }
    }

    private List<SelectionRow> selectionRows() {
        ItemStack target = selectedTargetStack();
        if (!RefinementTargets.canRefine(target)) {
            return List.of();
        }

        List<SelectionRow> rows = new java.util.ArrayList<>();
        int y = MOD_PANEL_Y + 4;
        for (MachineModifier modifier : affixes(selectedTargetTraits())) {
            if (rows.size() >= MAX_ROWS) {
                break;
            }
            rows.add(new SelectionRow(
                    MachineModifierText.displayName(modifier),
                    MachineModifierText.tooltipLine(modifier),
                    true,
                    y
            ));
            y += ROW_HEIGHT;
        }

        for (ModifierSlot slot : List.of(ModifierSlot.PREFIX, ModifierSlot.SUFFIX)) {
            if (rows.size() >= MAX_ROWS || !hasOpenSlot(slot)) {
                continue;
            }
            rows.add(new SelectionRow(
                    Component.translatable("rngtech.refinement.empty_slot", MachineModifierText.slotLabel(slot)),
                    Component.translatable("rngtech.refinement.select_empty_slot", MachineModifierText.slotLabel(slot)),
                    !candidateDefinitions(slot).isEmpty(),
                    y
            ));
            y += ROW_HEIGHT;
        }
        return rows;
    }

    private List<OutcomeLine> outcomeLines() {
        ItemStack target = selectedTargetStack();
        if (!RefinementTargets.canRefine(target)) {
            return List.of(new OutcomeLine(Component.translatable("rngtech.refinement.failure.invalid_target"), TEXT_BAD));
        }

        RefinementOperation operation = operation();
        if (operation == null) {
            return List.of(new OutcomeLine(Component.translatable("rngtech.refinement.failure.invalid_consumable"), TEXT_BAD));
        }

        return switch (operation.action()) {
            case RANDOM_ADD -> candidateLines(Component.translatable("rngtech.refinement.preview.add"), candidateDefinitions());
            case TARGETED_ADD_OR_UPGRADE -> targetedOutcomeLines(operation);
            case ASCEND_RARITY -> List.of(
                    new OutcomeLine(Component.translatable("rngtech.refinement.preview.ascend"), TEXT_GOOD),
                    new OutcomeLine(Component.translatable("rngtech.refinement.preview.upgrade_existing"), TEXT_MUTED),
                    new OutcomeLine(Component.translatable("rngtech.refinement.preview.cost_all"), TEXT_MUTED)
            );
            case CATALYZE_ASCENSION -> List.of(
                    new OutcomeLine(Component.translatable("rngtech.refinement.preview.catalyze_ascension"), TEXT_GOOD),
                    costFixedLine(RefinementEngine.ascensionCatalystPotentialCost())
            );
            default -> List.of(new OutcomeLine(Component.translatable("rngtech.tool_bench.failure.invalid_refinement_consumable"), TEXT_BAD));
        };
    }

    private List<OutcomeLine> targetedOutcomeLines(RefinementOperation operation) {
        List<MachineModifier> matching = affixes(selectedTargetTraits()).stream()
                .filter(modifier -> operation.targetStats().stream().anyMatch(modifier::matchesTargetStat))
                .filter(modifier -> targetableDefinition(modifier) != null)
                .sorted(java.util.Comparator.comparingInt(MachineModifier::tier))
                .toList();
        if (!matching.isEmpty()) {
            MachineModifier modifier = matching.get(0);
            return List.of(
                    new OutcomeLine(Component.translatable("rngtech.refinement.preview.upgrade"), TEXT_GOOD),
                    new OutcomeLine(MachineModifierText.displayName(modifier), MachineModifierText.tooltipLine(modifier), TEXT),
                    costRangeLine()
            );
        }
        return candidateLines(Component.translatable("rngtech.refinement.preview.can_roll"), candidateDefinitions(operation));
    }

    private List<OutcomeLine> candidateLines(Component header, List<ModifierDefinition> definitions) {
        if (definitions.isEmpty()) {
            return List.of(noAddableAffixLine());
        }

        List<OutcomeLine> lines = new java.util.ArrayList<>();
        lines.add(new OutcomeLine(header, TEXT_GOOD));
        lines.add(costRangeLine());
        for (ModifierDefinition definition : definitions) {
            if (lines.size() >= MAX_OUTCOME_LINES) {
                break;
            }
            lines.add(new OutcomeLine(candidateName(definition), candidateTooltip(definition), TEXT));
        }
        return lines;
    }

    private List<ModifierDefinition> candidateDefinitions() {
        List<ModifierDefinition> definitions = new java.util.ArrayList<>();
        definitions.addAll(candidateDefinitions(ModifierSlot.PREFIX));
        definitions.addAll(candidateDefinitions(ModifierSlot.SUFFIX));
        return definitions;
    }

    private List<ModifierDefinition> candidateDefinitions(RefinementOperation operation) {
        List<ModifierDefinition> definitions = new java.util.ArrayList<>();
        definitions.addAll(candidateDefinitions(ModifierSlot.PREFIX, operation));
        definitions.addAll(candidateDefinitions(ModifierSlot.SUFFIX, operation));
        return definitions;
    }

    private List<ModifierDefinition> candidateDefinitions(ModifierSlot slot) {
        return candidateDefinitions(slot, operation());
    }

    private List<ModifierDefinition> candidateDefinitions(ModifierSlot slot, RefinementOperation operation) {
        ItemStack target = selectedTargetStack();
        if (!RefinementTargets.canRefine(target) || !hasOpenSlot(slot)) {
            return List.of();
        }

        ModifierEligibilityProfile profile = RefinementTargets.eligibilityProfile(target);
        return profile.rollableDefinitions(slot).stream()
                .filter(definition -> operation == null
                        || !operation.hasTargetStats()
                        || definition.canTargetWithLens() && operation.targetStats().contains(definition.stat()))
                .filter(definition -> affixes(selectedTargetTraits()).stream().noneMatch(definition::conflictsWith))
                .toList();
    }

    private ModifierDefinition targetableDefinition(MachineModifier modifier) {
        ItemStack target = selectedTargetStack();
        if (!RefinementTargets.canRefine(target)) {
            return null;
        }
        ModifierEligibilityProfile profile = RefinementTargets.eligibilityProfile(target);
        return profile.definitions().stream()
                .filter(ModifierDefinition::canTargetWithLens)
                .filter(definition -> definition.matches(modifier))
                .findFirst()
                .orElse(null);
    }

    private boolean hasOpenSlot(ModifierSlot slot) {
        return RefinementEngine.hasOpenAffixSlot(selectedTargetTraits(), slot);
    }

    private OutcomeLine noAddableAffixLine() {
        return new OutcomeLine(
                Component.translatable(RefinementEngine.noAddableAffixFailureKey(selectedTargetTraits(), true)),
                TEXT_BAD
        );
    }

    private Component candidateName(ModifierDefinition definition) {
        return Component.translatable(
                "rngtech.refinement.candidate",
                MachineModifierText.slotLabel(definition.slot()),
                MachineModifierText.displayName(definition)
        );
    }

    private Component candidateTooltip(ModifierDefinition definition) {
        return Component.translatable(
                "rngtech.refinement.candidate",
                MachineModifierText.slotLabel(definition.slot()),
                MachineModifierText.definitionStats(definition)
        );
    }

    private OutcomeLine costRangeLine() {
        RefinementOperation operation = operation();
        return new OutcomeLine(
                Component.translatable(
                        "rngtech.refinement.preview.cost_range",
                        RefinementEngine.minPotentialCost(operation),
                        RefinementEngine.maxPotentialCost(operation)
                ),
                TEXT_MUTED
        );
    }

    private OutcomeLine costFixedLine(int cost) {
        return new OutcomeLine(Component.translatable("rngtech.refinement.preview.cost_rp", cost), TEXT_MUTED);
    }

    private RefinementOperation operation() {
        ItemStack consumable = menu.refinementConsumableStack();
        return consumable.getItem() instanceof RefinementConsumableItem item ? item.operation() : null;
    }

    private ItemStack selectedTargetStack() {
        return menu.selectedRefinementTarget();
    }

    private MachineTraits selectedTargetTraits() {
        ItemStack target = selectedTargetStack();
        return RefinementTargets.canRefine(target) ? RefinementTargets.traits(target) : MachineTraits.EMPTY;
    }

    private List<MachineModifier> affixes(MachineTraits traits) {
        return traits.modifiers().stream().filter(modifier -> modifier.slot().isAffix()).toList();
    }

    private SelectionRow hoveredSelectionRow(int mouseX, int mouseY) {
        for (SelectionRow row : selectionRows()) {
            int x = leftPos + MOD_PANEL_X;
            int y = topPos + row.y();
            if (mouseX >= x && mouseX < x + MOD_PANEL_WIDTH && mouseY >= y && mouseY < y + ROW_HEIGHT) {
                return row;
            }
        }
        return null;
    }

    private OutcomeLine hoveredOutcomeLine(int mouseX, int mouseY) {
        List<OutcomeLine> lines = outcomeLines();
        for (int index = 0; index < lines.size() && index < MAX_OUTCOME_LINES; index++) {
            int x = leftPos + OUTCOME_PANEL_X;
            int y = topPos + OUTCOME_PANEL_Y + 4 + index * ROW_HEIGHT;
            if (mouseX >= x && mouseX < x + OUTCOME_PANEL_WIDTH && mouseY >= y && mouseY < y + font.lineHeight) {
                return lines.get(index);
            }
        }
        return null;
    }

    private boolean hoveredRpReadout(int mouseX, int mouseY) {
        int x = leftPos + REFINE_RP_X;
        int y = topPos + REFINE_RP_Y;
        return mouseX >= x
                && mouseX < x + REFINE_RP_WIDTH
                && mouseY >= y
                && mouseY < y + REFINE_RP_HEIGHT;
    }

    private MachineScreenStyle.StatLine[] statLines() {
        ItemStack tool = menu.toolStack();
        if (!ModularToolItem.isModularTool(tool)) {
            return new MachineScreenStyle.StatLine[] {
                    MachineScreenStyle.statLine(
                            Component.translatable("rngtech.tool_bench.stats.empty"),
                            "--",
                            0.0,
                            false
                    )
            };
        }

        MachineStatAccumulator baseStats = ToolBaseStatCatalog.baseStats(tool);
        MachineStatAccumulator effectiveStats = ToolBaseStatCatalog.effectiveStats(tool);
        MachineStat[] statsToShow = displayedToolStats(tool, effectiveStats);
        MachineScreenStyle.StatLine[] statLines = new MachineScreenStyle.StatLine[statsToShow.length];
        for (int index = 0; index < statsToShow.length; index++) {
            MachineStat stat = statsToShow[index];
            double value = statValue(tool, effectiveStats, stat);
            statLines[index] = MachineScreenStyle.statLine(
                    Component.translatable(stat.translationKey()),
                    statText(tool, effectiveStats, stat),
                    value,
                    integralStat(stat),
                    isEnhancedStat(baseStats, value, stat),
                    statTooltip(tool, stat)
            );
        }
        return MachineScreenStyle.fitStatLines(statLines, MachineScreenStyle.maxStatRows(STAT_PANEL_Y, imageHeight));
    }

    private MachineStat[] displayedToolStats(ItemStack tool, MachineStatAccumulator stats) {
        return Arrays.stream(TOOL_STATS)
                .filter(stat -> shouldShowToolStat(tool, stats, stat))
                .toArray(MachineStat[]::new);
    }

    private boolean shouldShowToolStat(ItemStack tool, MachineStatAccumulator stats, MachineStat stat) {
        double value = statValue(tool, stats, stat);
        return switch (stat) {
            case AREA_WIDTH, AREA_HEIGHT, TREE_FELL_LIMIT, VEIN_MINE_LIMIT -> value > 1.0001;
            case VEIN_MINE_FE_USAGE, ORE_BURST_SPEED, ORE_BURST_FE_USAGE, LUCK, SELF_REPAIR -> value > 0.0001;
            default -> true;
        };
    }

    private String statText(ItemStack tool, MachineStatAccumulator stats, MachineStat stat) {
        return switch (stat) {
            case MINING_LEVEL -> Integer.toString(ToolBaseStatCatalog.miningLevel(tool));
            case MINING_SPEED -> MachineModifierText.formatValue(ToolBaseStatCatalog.miningSpeed(tool));
            case ATTACK_SPEED -> MachineModifierText.formatValue(ToolBaseStatCatalog.attackSpeed(tool));
            case DURABILITY -> Integer.toString(ToolBaseStatCatalog.maxDurability(tool));
            case SELF_REPAIR -> Integer.toString(ToolBaseStatCatalog.selfRepairAmount(tool));
            case BATTERY_SUPPORT -> Integer.toString(ToolBaseStatCatalog.batterySupport(tool));
            case FE_TRANSFER -> CompactValueText.energyRate(ToolBaseStatCatalog.feTransfer(tool));
            case FE_USAGE -> CompactValueText.energyAmount(ToolBaseStatCatalog.feUsage(tool));
            case AREA_WIDTH -> Integer.toString(ToolBaseStatCatalog.areaWidth(tool));
            case AREA_HEIGHT -> Integer.toString(ToolBaseStatCatalog.areaHeight(tool));
            case TREE_FELL_LIMIT -> Integer.toString(ToolBaseStatCatalog.treeFellLimit(tool));
            case VEIN_MINE_LIMIT -> Integer.toString(ToolBaseStatCatalog.veinMineLimit(tool));
            case VEIN_MINE_FE_USAGE -> CompactValueText.energyAmount(ToolBaseStatCatalog.veinMineFeUsage(tool));
            case ORE_BURST_SPEED -> percent(ToolBaseStatCatalog.oreBurstSpeed(tool));
            case ORE_BURST_FE_USAGE -> CompactValueText.energyAmount(ToolBaseStatCatalog.oreBurstFeUsage(tool));
            case LUCK -> Integer.toString(ToolBaseStatCatalog.luck(tool));
            case STABILITY -> MachineModifierText.formatValue(ToolBaseStatCatalog.stability(tool));
            case CONTROL -> MachineModifierText.formatValue(ToolBaseStatCatalog.control(tool));
            default -> MachineModifierText.formatValue(stats.value(stat));
        };
    }

    private double statValue(ItemStack tool, MachineStatAccumulator stats, MachineStat stat) {
        return switch (stat) {
            case MINING_LEVEL -> ToolBaseStatCatalog.miningLevel(tool);
            case MINING_SPEED -> ToolBaseStatCatalog.miningSpeed(tool);
            case ATTACK_SPEED -> ToolBaseStatCatalog.attackSpeed(tool);
            case DURABILITY -> ToolBaseStatCatalog.maxDurability(tool);
            case SELF_REPAIR -> ToolBaseStatCatalog.selfRepairAmount(tool);
            case BATTERY_SUPPORT -> ToolBaseStatCatalog.batterySupport(tool);
            case FE_TRANSFER -> ToolBaseStatCatalog.feTransfer(tool);
            case FE_USAGE -> ToolBaseStatCatalog.feUsage(tool);
            case AREA_WIDTH -> ToolBaseStatCatalog.areaWidth(tool);
            case AREA_HEIGHT -> ToolBaseStatCatalog.areaHeight(tool);
            case TREE_FELL_LIMIT -> ToolBaseStatCatalog.treeFellLimit(tool);
            case VEIN_MINE_LIMIT -> ToolBaseStatCatalog.veinMineLimit(tool);
            case VEIN_MINE_FE_USAGE -> ToolBaseStatCatalog.veinMineFeUsage(tool);
            case ORE_BURST_SPEED -> ToolBaseStatCatalog.oreBurstSpeed(tool);
            case ORE_BURST_FE_USAGE -> ToolBaseStatCatalog.oreBurstFeUsage(tool);
            case LUCK -> ToolBaseStatCatalog.luck(tool);
            case STABILITY -> ToolBaseStatCatalog.stability(tool);
            case CONTROL -> ToolBaseStatCatalog.control(tool);
            default -> stats.value(stat);
        };
    }

    private boolean integralStat(MachineStat stat) {
        return stat == MachineStat.MINING_LEVEL
                || stat == MachineStat.DURABILITY
                || stat == MachineStat.SELF_REPAIR
                || stat == MachineStat.BATTERY_SUPPORT
                || stat == MachineStat.FE_TRANSFER
                || stat == MachineStat.FE_USAGE
                || stat == MachineStat.AREA_WIDTH
                || stat == MachineStat.AREA_HEIGHT
                || stat == MachineStat.TREE_FELL_LIMIT
                || stat == MachineStat.VEIN_MINE_LIMIT
                || stat == MachineStat.VEIN_MINE_FE_USAGE
                || stat == MachineStat.ORE_BURST_FE_USAGE
                || stat == MachineStat.LUCK;
    }

    private boolean isEnhancedStat(MachineStatAccumulator baseStats, double value, MachineStat stat) {
        double baseValue = baseStats == null ? 0.0 : baseStats.value(stat);
        if (stat == MachineStat.FE_USAGE || stat == MachineStat.ORE_BURST_FE_USAGE) {
            return value < baseValue - 0.0001;
        }
        return value > baseValue + 0.0001;
    }

    private Component statTooltip(ItemStack tool, MachineStat stat) {
        if (stat == MachineStat.STABILITY) {
            return Component.translatable(
                    "rngtech.tool_bench.stat_tooltip.stability",
                    percent(ToolBaseStatCatalog.stabilityCostAvoidanceChance(tool))
            );
        }
        if (stat == MachineStat.CONTROL) {
            return Component.translatable(
                    "rngtech.tool_bench.stat_tooltip.control",
                    percent(ToolBaseStatCatalog.controlDurabilityProtectionShare(tool)),
                    ToolBaseStatCatalog.controlDurabilityProtectionFeCost(tool)
            );
        }
        return Component.translatable("rngtech.tool_bench.stat_tooltip." + stat.getSerializedName());
    }

    private Component refinementSummary(ItemStack stack) {
        if (!RefinementTargets.canRefine(stack)) {
            return Component.literal("--");
        }
        MachineTraits traits = RefinementTargets.storedTraits(stack);
        return Component.translatable(
                "rngtech.refinement.summary",
                Component.translatable(traits.rarity().translationKey()),
                traits.refinementPotential()
        );
    }

    private String percent(double value) {
        return String.format(Locale.ROOT, "%.0f%%", value * 100.0);
    }

    private void renderButton(GuiGraphics guiGraphics, int buttonX, int buttonY) {
        int x = leftPos + buttonX;
        int y = topPos + buttonY;
        guiGraphics.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, BUTTON_DARK);
        guiGraphics.fill(x + 1, y + 1, x + BUTTON_WIDTH - 1, y + BUTTON_HEIGHT - 1, BUTTON);
        guiGraphics.fill(x + 1, y + 1, x + BUTTON_WIDTH - 1, y + 2, BUTTON_LIGHT);
    }

    private void drawCentered(GuiGraphics guiGraphics, Component label, int buttonX, int buttonY) {
        String clipped = font.plainSubstrByWidth(label.getString(), BUTTON_WIDTH - 8);
        guiGraphics.drawString(
                font,
                clipped,
                buttonX + (BUTTON_WIDTH - font.width(clipped)) / 2,
                buttonY + 6,
                0xFFFFFFFF,
                false
        );
    }

    private void drawTargetButtonLabel(GuiGraphics guiGraphics, Component label, int buttonX) {
        drawClippedCentered(guiGraphics, label, buttonX, TARGET_BUTTON_Y + 4, TARGET_BUTTON_WIDTH, 0xFFFFFFFF);
    }

    private void drawRefineButtonLabel(GuiGraphics guiGraphics, Component label) {
        drawClippedCentered(guiGraphics, label, REFINE_BUTTON_X, REFINE_BUTTON_Y + 6, REFINE_BUTTON_WIDTH, 0xFFFFFFFF);
    }

    private void drawRpLabel(GuiGraphics guiGraphics) {
        ItemStack target = selectedTargetStack();
        Component label = RefinementTargets.canRefine(target)
                ? Component.literal(Integer.toString(RefinementTargets.storedTraits(target).refinementPotential()))
                : Component.literal("--");
        drawClippedCentered(guiGraphics, label, REFINE_RP_X, REFINE_RP_Y + 5, REFINE_RP_WIDTH, TEXT);
    }

    private void drawClippedCentered(
            GuiGraphics guiGraphics,
            Component label,
            int x,
            int y,
            int width,
            int color
    ) {
        String clipped = font.plainSubstrByWidth(label.getString(), width - 8);
        guiGraphics.drawString(font, clipped, x + (width - font.width(clipped)) / 2, y, color, false);
    }

    private void drawClipped(GuiGraphics guiGraphics, Component label, int x, int y, int width, int color) {
        guiGraphics.drawString(font, font.plainSubstrByWidth(label.getString(), width), x, y, color, false);
    }

    private void drawSlotLabel(GuiGraphics guiGraphics, Component label, int frameX) {
        guiGraphics.drawString(font, label, frameX + (18 - font.width(label)) / 2, REFINE_LABEL_Y, TEXT_MUTED, false);
    }

    private void drawGearLabel(GuiGraphics guiGraphics, Component label, int frameX) {
        guiGraphics.drawString(font, label, frameX + (18 - font.width(label)) / 2, 35, TEXT_MUTED, false);
    }

    private void renderPlayerInventoryFrames(GuiGraphics guiGraphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlotFrame(guiGraphics, PLAYER_INVENTORY_X - 1 + column * 18, PLAYER_INVENTORY_Y - 1 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderSlotFrame(guiGraphics, PLAYER_INVENTORY_X - 1 + column * 18, HOTBAR_Y - 1);
        }
    }

    private void renderSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + 18, top + 18, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + 17, top + 17, 0xFFE0E0E0);
        guiGraphics.fill(left + 2, top + 2, left + 16, top + 16, 0xFF8B8B8B);
    }

    private boolean isOverTab(double mouseX, double mouseY, int index) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        return mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + 21;
    }

    private record SelectionRow(Component title, Component tooltip, boolean enabled, int y) {}

    private record OutcomeLine(Component text, Component tooltip, int color) {
        private OutcomeLine(Component text, int color) {
            this(text, text, color);
        }
    }
}
