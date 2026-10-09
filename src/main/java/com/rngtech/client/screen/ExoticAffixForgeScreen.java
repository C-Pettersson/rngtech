package com.rngtech.client.screen;

import com.rngtech.content.blockentity.ExoticAffixForgeBlockEntity;
import com.rngtech.content.menu.ExoticAffixForgeMenu;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineModifierText;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.refinement.ExoticAffixForgeAction;
import com.rngtech.rpg.refinement.RefinementEngine;
import com.rngtech.rpg.refinement.RefinementSelection;
import com.rngtech.rpg.refinement.RefinementTargets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExoticAffixForgeScreen extends AbstractContainerScreen<ExoticAffixForgeMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int TEXT_GOOD = 0xFF2F6B3F;
    private static final int TEXT_WARNING = 0xFF8A6A22;
    private static final int TEXT_BAD = 0xFF8A3C32;
    private static final int ENERGY = 0xFFB43B28;
    private static final int FAILURE = 0xFF8A3C32;
    private static final int PROGRESS = 0xFF667F51;
    private static final int CATALYST = 0xFFB58F52;
    private static final int HISTORY = 0xFF4D92A3;
    private static final int ROW = 0xFFE0E0E0;
    private static final int ROW_DISABLED = 0xFFB8B8B8;
    private static final int ROW_SELECTED = 0xFFE1D7B0;
    private static final int STAT_ACCENT = 0xFF4D92A3;
    private static final int TAB_WIDTH = 54;
    private static final int TAB_SPACING = 56;
    private static final int ENERGY_X = 16;
    private static final int ENERGY_Y = 70;
    private static final int ENERGY_WIDTH = 10;
    private static final int ENERGY_HEIGHT = 50;
    private static final int FAILURE_X = 32;
    private static final int FAILURE_Y = 70;
    private static final int FAILURE_WIDTH = 10;
    private static final int FAILURE_HEIGHT = 50;
    private static final int PROGRESS_X = 96;
    private static final int PROGRESS_Y = 105;
    private static final int PROGRESS_WIDTH = 112;
    private static final int PROGRESS_HEIGHT = 8;
    private static final int STATUS_Y = 116;
    private static final int STATUS_ICON_SIZE = 12;
    private static final int STATUS_ICON_X = 96;
    private static final int CATALYST_ICON_X = 112;
    private static final int HISTORY_ICON_X = 128;
    private static final int ACTION_X = 96;
    private static final int ACTION_Y = 28;
    private static final int ACTION_WIDTH = 112;
    private static final int ACTION_ROW_HEIGHT = 12;
    private static final int APPLY_BUTTON_X = 44;
    private static final int APPLY_BUTTON_Y = 102;
    private static final int APPLY_BUTTON_WIDTH = 48;
    private static final int APPLY_BUTTON_HEIGHT = 16;
    private static final int SELECTION_X = 224;
    private static final int SELECTION_Y = 28;
    private static final int SELECTION_WIDTH = 104;
    private static final int SELECTION_HEIGHT = 96;
    private static final int SELECTION_ROW_HEIGHT = 12;
    private static final int MAX_SELECTION_ROWS = 7;
    private static final int STAT_PANEL_X = 14;
    private static final int STAT_PANEL_Y = 28;
    private static final int STAT_PANEL_WIDTH = 312;

    public ExoticAffixForgeScreen(ExoticAffixForgeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 342;
        imageHeight = 226;
        titleLabelX = 8;
        titleLabelY = 8;
        inventoryLabelX = 61;
        inventoryLabelY = 130;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderPanelTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        renderTabs(guiGraphics);
        if (menu.selectedTab() == ExoticAffixForgeMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == ExoticAffixForgeMenu.TAB_GEAR) {
            renderGear(guiGraphics);
        } else {
            renderStats(guiGraphics);
        }
        renderPlayerInventoryFrames(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        if (menu.selectedTab() == ExoticAffixForgeMenu.TAB_PROCESSING) {
            guiGraphics.drawString(font, Component.translatable("rngtech.refinement.target.short"), 13, 20, TEXT_MUTED, false);
            guiGraphics.drawString(font, Component.translatable("rngtech.refinement.catalyst.short"), 43, 20, TEXT_MUTED, false);
            guiGraphics.drawString(font, Component.translatable("rngtech.processing.output.tiny"), 72, 20, TEXT_MUTED, false);
            guiGraphics.drawString(font, Component.translatable("rngtech.exotic_affix_forge.energy"), ENERGY_X - 1, ENERGY_Y - 11, TEXT_MUTED, false);
            drawCentered(guiGraphics, Component.translatable("rngtech.exotic_affix_forge.actions"), ACTION_X + ACTION_WIDTH / 2, 18, TEXT_MUTED);
            drawCentered(guiGraphics, Component.translatable("rngtech.refinement.modifiers"), SELECTION_X + SELECTION_WIDTH / 2, 18, TEXT_MUTED);
            drawProcessingLabels(guiGraphics);
        } else if (menu.selectedTab() == ExoticAffixForgeMenu.TAB_GEAR) {
            drawCentered(guiGraphics, Component.translatable("rngtech.gear.battery_cell.short"), 52, 20, TEXT_MUTED);
            drawGearLabels(guiGraphics);
        } else {
            drawStatsLabels(guiGraphics);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (handleTabClick(mouseX, mouseY)) {
                return true;
            }
            if (menu.selectedTab() == ExoticAffixForgeMenu.TAB_PROCESSING
                    && (handleApplyClick(mouseX, mouseY) || handleActionClick(mouseX, mouseY) || handleSelectionClick(mouseX, mouseY))) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        renderSlotFrame(guiGraphics, 13, 31);
        renderSlotFrame(guiGraphics, 42, 31);
        renderSlotFrame(guiGraphics, 71, 31);
        renderVerticalBar(guiGraphics, ENERGY_X, ENERGY_Y, ENERGY_WIDTH, ENERGY_HEIGHT, menu.energyProgress(), ENERGY);
        renderVerticalBar(guiGraphics, FAILURE_X, FAILURE_Y, FAILURE_WIDTH, FAILURE_HEIGHT, menu.powerFailureProgress(), FAILURE);
        renderHorizontalBar(guiGraphics, PROGRESS_X, PROGRESS_Y, PROGRESS_WIDTH, PROGRESS_HEIGHT, menu.processingProgress(), PROGRESS);
        renderApplyButton(guiGraphics);
        renderActionRows(guiGraphics);
        renderSelectionRows(guiGraphics);
        renderStatusIcons(guiGraphics);
    }

    private void renderGear(GuiGraphics guiGraphics) {
        renderSlotFrame(guiGraphics, 42, 47);
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

    private void renderActionRows(GuiGraphics guiGraphics) {
        int index = 0;
        for (ExoticAffixForgeAction action : ExoticAffixForgeAction.values()) {
            int rowX = leftPos + ACTION_X;
            int rowY = topPos + ACTION_Y + index * ACTION_ROW_HEIGHT;
            int color = menu.craftActive() ? ROW_DISABLED : action == menu.selectedAction() ? ROW_SELECTED : ROW;
            guiGraphics.fill(rowX, rowY, rowX + ACTION_WIDTH, rowY + ACTION_ROW_HEIGHT - 1, color);
            index++;
        }
    }

    private void drawProcessingLabels(GuiGraphics guiGraphics) {
        drawCentered(
                guiGraphics,
                applyButtonLabel(),
                APPLY_BUTTON_X + APPLY_BUTTON_WIDTH / 2,
                APPLY_BUTTON_Y + 4,
                menu.canApplyCraft() ? TEXT : TEXT_MUTED
        );

        int index = 0;
        for (ExoticAffixForgeAction action : ExoticAffixForgeAction.values()) {
            int color = menu.craftActive() ? TEXT_MUTED : action == menu.selectedAction() ? TEXT : TEXT_MUTED;
            drawClipped(guiGraphics, Component.translatable(action.translationKey()), ACTION_X + 4, ACTION_Y + 2 + index * ACTION_ROW_HEIGHT, ACTION_WIDTH - 8, color);
            index++;
        }

        List<SelectionRow> rows = selectionRows();
        if (rows.isEmpty()) {
            drawClipped(
                    guiGraphics,
                    Component.translatable("rngtech.exotic_affix_forge.no_focus"),
                    SELECTION_X + 3,
                    SELECTION_Y + 4,
                    SELECTION_WIDTH - 6,
                    TEXT_MUTED
            );
            return;
        }
        for (SelectionRow row : rows) {
            int color = row.enabled() && !menu.craftActive() ? TEXT : TEXT_MUTED;
            drawClipped(guiGraphics, row.title(), SELECTION_X + 3, row.y() + 1, SELECTION_WIDTH - 6, color);
        }
    }

    private void drawGearLabels(GuiGraphics guiGraphics) {
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

    private void renderSelectionRows(GuiGraphics guiGraphics) {
        int x = leftPos + SELECTION_X - 2;
        int y = topPos + SELECTION_Y - 2;
        guiGraphics.fill(x, y, x + SELECTION_WIDTH + 4, y + SELECTION_HEIGHT, 0xFF9C9C9C);
        guiGraphics.fill(x + 1, y + 1, x + SELECTION_WIDTH + 3, y + SELECTION_HEIGHT - 1, 0xFFD5D5D5);

        for (SelectionRow row : selectionRows()) {
            int color = menu.craftActive() ? ROW_DISABLED : row.selected() ? ROW_SELECTED : row.enabled() ? ROW : ROW_DISABLED;
            int rowX = leftPos + SELECTION_X;
            int rowY = topPos + row.y();
            guiGraphics.fill(rowX, rowY, rowX + SELECTION_WIDTH, rowY + SELECTION_ROW_HEIGHT - 1, color);
        }
    }

    private void renderApplyButton(GuiGraphics guiGraphics) {
        int x = leftPos + APPLY_BUTTON_X;
        int y = topPos + APPLY_BUTTON_Y;
        int color = menu.canApplyCraft() ? ROW_SELECTED : ROW_DISABLED;
        guiGraphics.fill(x, y, x + APPLY_BUTTON_WIDTH, y + APPLY_BUTTON_HEIGHT, 0xFF5F5F5F);
        guiGraphics.fill(x + 1, y + 1, x + APPLY_BUTTON_WIDTH - 1, y + APPLY_BUTTON_HEIGHT - 1, color);
    }

    private void renderStatusIcons(GuiGraphics guiGraphics) {
        renderIconBox(guiGraphics, STATUS_ICON_X, STATUS_Y);
        guiGraphics.fill(
                leftPos + STATUS_ICON_X + 3,
                topPos + STATUS_Y + 3,
                leftPos + STATUS_ICON_X + 9,
                topPos + STATUS_Y + 9,
                statusColor()
        );

        renderIconBox(guiGraphics, CATALYST_ICON_X, STATUS_Y);
        guiGraphics.fill(
                leftPos + CATALYST_ICON_X + 3,
                topPos + STATUS_Y + 3,
                leftPos + CATALYST_ICON_X + 9,
                topPos + STATUS_Y + 9,
                CATALYST
        );

        renderIconBox(guiGraphics, HISTORY_ICON_X, STATUS_Y);
        guiGraphics.fill(
                leftPos + HISTORY_ICON_X + 3,
                topPos + STATUS_Y + 3,
                leftPos + HISTORY_ICON_X + 9,
                topPos + STATUS_Y + 9,
                HISTORY
        );
    }

    private void renderTabs(GuiGraphics guiGraphics) {
        renderTab(guiGraphics, 0, Component.translatable("rngtech.tab.processing.short"), menu.selectedTab() == ExoticAffixForgeMenu.TAB_PROCESSING, true);
        renderTab(guiGraphics, 1, Component.translatable("rngtech.tab.gear"), menu.selectedTab() == ExoticAffixForgeMenu.TAB_GEAR, true);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.tab.stats"), menu.selectedTab() == ExoticAffixForgeMenu.TAB_STATS, true);
        renderTab(guiGraphics, 3, Component.translatable("rngtech.tab.refinement.short"), false, false);
    }

    private void renderTab(GuiGraphics guiGraphics, int index, Component label, boolean selected, boolean enabled) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        int color = selected ? PANEL : enabled ? 0xFF9A9A9A : 0xFF777777;
        guiGraphics.fill(x, y, x + TAB_WIDTH, y + 21, color);
        guiGraphics.fill(x, y, x + TAB_WIDTH, y + 1, PANEL_LIGHT);
        guiGraphics.fill(x, y, x + 1, y + 21, PANEL_LIGHT);
        guiGraphics.fill(x + TAB_WIDTH - 1, y, x + TAB_WIDTH, y + 21, PANEL_DARK);
        if (!selected) {
            guiGraphics.fill(x, y + 20, x + TAB_WIDTH, y + 21, PANEL_DARK);
        }
        guiGraphics.drawString(
                font,
                label,
                x + (TAB_WIDTH - font.width(label)) / 2,
                y + 7,
                selected ? TEXT : enabled ? 0xFF2F2F2F : 0xFF4F4F4F,
                false
        );
    }

    private void renderHorizontalBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float progress, int fillColor) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + width, top + height, 0xFF5F5F5F);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, 0xFF8B8B8B);
        int fill = Math.round((width - 2) * progress);
        if (fill > 0) {
            guiGraphics.fill(left + 1, top + 1, left + 1 + fill, top + height - 1, fillColor);
        }
    }

    private void renderVerticalBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float progress, int fillColor) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + width, top + height, 0xFF5F5F5F);
        int fill = Math.round((height - 2) * progress);
        if (fill > 0) {
            guiGraphics.fill(left + 1, top + height - 1 - fill, left + width - 1, top + height - 1, fillColor);
        }
    }

    private boolean handleTabClick(double mouseX, double mouseY) {
        for (int tab = 0; tab < 3; tab++) {
            int x = leftPos + 8 + tab * TAB_SPACING;
            int y = topPos - 20;
            if (mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + 21) {
                menu.selectTab(tab);
                return true;
            }
        }
        return false;
    }

    private boolean handleActionClick(double mouseX, double mouseY) {
        if (menu.craftActive()) {
            return false;
        }
        int index = 0;
        for (ExoticAffixForgeAction action : ExoticAffixForgeAction.values()) {
            int x = leftPos + ACTION_X;
            int y = topPos + ACTION_Y + index * ACTION_ROW_HEIGHT;
            if (mouseX >= x && mouseX < x + ACTION_WIDTH && mouseY >= y && mouseY < y + ACTION_ROW_HEIGHT) {
                sendButton(menu.actionButtonId(action));
                return true;
            }
            index++;
        }
        return false;
    }

    private boolean handleSelectionClick(double mouseX, double mouseY) {
        if (menu.craftActive()) {
            return false;
        }
        for (SelectionRow row : selectionRows()) {
            int x = leftPos + SELECTION_X;
            int y = topPos + row.y();
            if (mouseX >= x && mouseX < x + SELECTION_WIDTH && mouseY >= y && mouseY < y + SELECTION_ROW_HEIGHT) {
                if (row.kind() == RefinementSelection.Kind.EXISTING_MODIFIER) {
                    menu.selectExistingModifier(row.affixIndex());
                } else if (row.kind() == RefinementSelection.Kind.EMPTY_SLOT) {
                    menu.selectEmptySlot(row.emptySlot());
                }
                sendButton(menu.selectionButtonId());
                return true;
            }
        }
        return false;
    }

    private boolean handleApplyClick(double mouseX, double mouseY) {
        if (!menu.canApplyCraft()) {
            return false;
        }
        int x = leftPos + APPLY_BUTTON_X;
        int y = topPos + APPLY_BUTTON_Y;
        if (mouseX >= x && mouseX < x + APPLY_BUTTON_WIDTH && mouseY >= y && mouseY < y + APPLY_BUTTON_HEIGHT) {
            sendButton(menu.applyButtonId());
            return true;
        }
        return false;
    }

    private void sendButton(int buttonId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.gameMode != null && menu.clickMenuButton(minecraft.player, buttonId)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    private List<SelectionRow> selectionRows() {
        List<SelectionRow> rows = new ArrayList<>();
        ItemStack target = targetStack();
        if (!RefinementTargets.canRefine(target)) {
            return rows;
        }

        int y = SELECTION_Y + 4;
        List<MachineModifier> affixes = affixes(targetTraits());
        for (int affixIndex = 0; affixIndex < affixes.size() && rows.size() < MAX_SELECTION_ROWS; affixIndex++) {
            MachineModifier modifier = affixes.get(affixIndex);
            rows.add(new SelectionRow(
                    RefinementSelection.Kind.EXISTING_MODIFIER,
                    affixIndex,
                    modifier.slot(),
                    MachineModifierText.displayName(modifier),
                    MachineModifierText.tooltipLine(modifier),
                    !menu.selectedAction().requiresEmptySlot(),
                    selectedExisting(affixIndex),
                    y
            ));
            y += SELECTION_ROW_HEIGHT;
        }

        for (ModifierSlot slot : List.of(ModifierSlot.PREFIX, ModifierSlot.SUFFIX)) {
            if (rows.size() >= MAX_SELECTION_ROWS || !hasOpenSlot(slot)) {
                continue;
            }
            rows.add(new SelectionRow(
                    RefinementSelection.Kind.EMPTY_SLOT,
                    -1,
                    slot,
                    Component.translatable("rngtech.refinement.empty_slot", MachineModifierText.slotLabel(slot)),
                    Component.translatable("rngtech.refinement.select_empty_slot", MachineModifierText.slotLabel(slot)),
                    !menu.selectedAction().requiresExistingModifier(),
                    selectedEmpty(slot),
                    y
            ));
            y += SELECTION_ROW_HEIGHT;
        }
        return rows;
    }

    private boolean hasOpenSlot(ModifierSlot slot) {
        return RefinementEngine.hasOpenAffixSlot(targetTraits(), slot);
    }

    private boolean selectedExisting(int affixIndex) {
        RefinementSelection selection = menu.selectedRefinement();
        return selection.kind() == RefinementSelection.Kind.EXISTING_MODIFIER && selection.affixIndex() == affixIndex;
    }

    private boolean selectedEmpty(ModifierSlot slot) {
        RefinementSelection selection = menu.selectedRefinement();
        return selection.kind() == RefinementSelection.Kind.EMPTY_SLOT && selection.emptySlot() == slot;
    }

    private ItemStack targetStack() {
        return menu.getSlot(ExoticAffixForgeBlockEntity.SLOT_TARGET).getItem();
    }

    private MachineTraits targetTraits() {
        ItemStack target = targetStack();
        return RefinementTargets.canRefine(target) ? RefinementTargets.traits(target) : MachineTraits.EMPTY;
    }

    private List<MachineModifier> affixes(MachineTraits traits) {
        return traits.modifiers().stream().filter(modifier -> modifier.slot().isAffix()).toList();
    }

    private Component statusLabel() {
        return Component.translatable(statusKey(menu.status()));
    }

    private int statusColor() {
        return switch (menu.status()) {
            case ExoticAffixForgeBlockEntity.STATUS_READY, ExoticAffixForgeBlockEntity.STATUS_WORKING -> TEXT_GOOD;
            case ExoticAffixForgeBlockEntity.STATUS_NO_POWER -> TEXT_WARNING;
            default -> TEXT_BAD;
        };
    }

    private String statusKey(int status) {
        return switch (status) {
            case ExoticAffixForgeBlockEntity.STATUS_NO_TARGET -> "rngtech.exotic_affix_forge.status.no_target";
            case ExoticAffixForgeBlockEntity.STATUS_INVALID_TARGET -> "rngtech.exotic_affix_forge.status.invalid_target";
            case ExoticAffixForgeBlockEntity.STATUS_UNIQUE_TARGET -> "rngtech.exotic_affix_forge.status.unique";
            case ExoticAffixForgeBlockEntity.STATUS_MISSING_CATALYST -> "rngtech.exotic_affix_forge.status.missing_catalyst";
            case ExoticAffixForgeBlockEntity.STATUS_INVALID_RECIPE -> "rngtech.exotic_affix_forge.status.invalid_recipe";
            case ExoticAffixForgeBlockEntity.STATUS_INSUFFICIENT_CATALYST -> "rngtech.exotic_affix_forge.status.insufficient_catalyst";
            case ExoticAffixForgeBlockEntity.STATUS_MISSING_SELECTION -> "rngtech.exotic_affix_forge.status.missing_selection";
            case ExoticAffixForgeBlockEntity.STATUS_INSUFFICIENT_POTENTIAL -> "rngtech.exotic_affix_forge.status.insufficient_potential";
            case ExoticAffixForgeBlockEntity.STATUS_ILLEGAL_OPERATION -> "rngtech.exotic_affix_forge.status.illegal_operation";
            case ExoticAffixForgeBlockEntity.STATUS_NO_POWER -> "rngtech.exotic_affix_forge.status.no_power";
            case ExoticAffixForgeBlockEntity.STATUS_WORKING -> "rngtech.exotic_affix_forge.status.working";
            case ExoticAffixForgeBlockEntity.STATUS_OUTPUT_FULL -> "rngtech.exotic_affix_forge.status.output_full";
            case ExoticAffixForgeBlockEntity.STATUS_FAILED -> "rngtech.exotic_affix_forge.status.failed";
            default -> "rngtech.exotic_affix_forge.status.ready";
        };
    }

    private MachineScreenStyle.StatLine[] statLines() {
        return new MachineScreenStyle.StatLine[] {
                MachineScreenStyle.statLine(
                        Component.translatable("rngtech.exotic_affix_forge.stat.label.energy"),
                        formatLong(menu.energy()) + " / " + formatLong(menu.energyCapacity()),
                        1.0,
                        false,
                        false,
                        Component.translatable("rngtech.exotic_affix_forge.stat.energy", formatLong(menu.energy()), formatLong(menu.energyCapacity()))
                ),
                MachineScreenStyle.statLine(
                        Component.translatable("rngtech.exotic_affix_forge.stat.label.action"),
                        Component.translatable(menu.selectedAction().translationKey()).getString(),
                        1.0,
                        false,
                        false,
                        Component.translatable("rngtech.exotic_affix_forge.stat.action", Component.translatable(menu.selectedAction().translationKey()))
                ),
                MachineScreenStyle.statLine(
                        Component.translatable("rngtech.exotic_affix_forge.stat.label.cost"),
                        formatLong(menu.effectiveEnergyCost()) + " FE / " + menu.requiredCatalysts(),
                        1.0,
                        false,
                        false,
                        Component.translatable("rngtech.exotic_affix_forge.stat.cost", formatLong(menu.effectiveEnergyCost()), menu.requiredCatalysts())
                ),
                MachineScreenStyle.statLine(
                        Component.translatable("rngtech.exotic_affix_forge.stat.label.history"),
                        menu.totalUses() + " / " + menu.actionUses(),
                        1.0,
                        false,
                        false,
                        Component.translatable("rngtech.exotic_affix_forge.stat.history", menu.totalUses(), menu.actionUses())
                ),
                MachineScreenStyle.statLine(
                        Component.translatable("rngtech.exotic_affix_forge.stat.label.rp_cost"),
                        String.valueOf(menu.refinementPotentialCost()),
                        1.0,
                        false,
                        false,
                        Component.translatable("rngtech.exotic_affix_forge.stat.rp_cost", menu.refinementPotentialCost())
                )
        };
    }

    private void renderPanelTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() == ExoticAffixForgeMenu.TAB_GEAR) {
            if (hovered(mouseX, mouseY, 42, 47, 18, 18) && (hoveredSlot == null || !hoveredSlot.hasItem())) {
                guiGraphics.renderComponentTooltip(
                        font,
                        List.of(
                                Component.translatable("rngtech.exotic_affix_forge.gear.battery_cell"),
                                Component.translatable("rngtech.exotic_affix_forge.gear.battery_cell.tooltip")
                        ),
                        mouseX,
                        mouseY
                );
            }
            return;
        }
        if (menu.selectedTab() == ExoticAffixForgeMenu.TAB_STATS) {
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
            return;
        }
        if (menu.selectedTab() != ExoticAffixForgeMenu.TAB_PROCESSING) {
            return;
        }
        if (hovered(mouseX, mouseY, ENERGY_X, ENERGY_Y, ENERGY_WIDTH, ENERGY_HEIGHT)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable("rngtech.exotic_affix_forge.tooltip.energy", formatLong(menu.energy()), formatLong(menu.energyCapacity())),
                    mouseX,
                    mouseY
            );
            return;
        }
        if (hovered(mouseX, mouseY, FAILURE_X, FAILURE_Y, FAILURE_WIDTH, FAILURE_HEIGHT)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable("rngtech.exotic_affix_forge.tooltip.failure", menu.powerFailure()),
                    mouseX,
                    mouseY
            );
            return;
        }
        if (hovered(mouseX, mouseY, PROGRESS_X, PROGRESS_Y, PROGRESS_WIDTH, PROGRESS_HEIGHT)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable("rngtech.exotic_affix_forge.tooltip.progress", menu.progress(), menu.processingTicks()),
                    mouseX,
                    mouseY
            );
            return;
        }
        if (hovered(mouseX, mouseY, APPLY_BUTTON_X, APPLY_BUTTON_Y, APPLY_BUTTON_WIDTH, APPLY_BUTTON_HEIGHT)) {
            guiGraphics.renderTooltip(font, applyButtonTooltip(), mouseX, mouseY);
            return;
        }
        if (hovered(mouseX, mouseY, STATUS_ICON_X, STATUS_Y, STATUS_ICON_SIZE, STATUS_ICON_SIZE)) {
            guiGraphics.renderTooltip(font, statusLabel(), mouseX, mouseY);
            return;
        }
        if (hovered(mouseX, mouseY, CATALYST_ICON_X, STATUS_Y, STATUS_ICON_SIZE, STATUS_ICON_SIZE)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable(
                            "rngtech.exotic_affix_forge.tooltip.cost",
                            formatLong(menu.effectiveEnergyCost()),
                            menu.requiredCatalysts()
                    ),
                    mouseX,
                    mouseY
            );
            return;
        }
        if (hovered(mouseX, mouseY, HISTORY_ICON_X, STATUS_Y, STATUS_ICON_SIZE, STATUS_ICON_SIZE)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable("rngtech.exotic_affix_forge.tooltip.history", menu.totalUses(), menu.actionUses()),
                    mouseX,
                    mouseY
            );
            return;
        }
        ExoticAffixForgeAction action = hoveredActionRow(mouseX, mouseY);
        if (action != null) {
            guiGraphics.renderComponentTooltip(
                    font,
                    List.of(
                            Component.translatable(action.translationKey()),
                            Component.translatable("rngtech.exotic_affix_forge.tooltip.action_select")
                    ),
                    mouseX,
                    mouseY
            );
            return;
        }
        SelectionRow row = hoveredSelectionRow(mouseX, mouseY);
        if (row != null) {
            guiGraphics.renderTooltip(font, row.tooltip(), mouseX, mouseY);
        }
    }

    private ExoticAffixForgeAction hoveredActionRow(int mouseX, int mouseY) {
        int index = 0;
        for (ExoticAffixForgeAction action : ExoticAffixForgeAction.values()) {
            if (hovered(mouseX, mouseY, ACTION_X, ACTION_Y + index * ACTION_ROW_HEIGHT, ACTION_WIDTH, ACTION_ROW_HEIGHT)) {
                return action;
            }
            index++;
        }
        return null;
    }

    private SelectionRow hoveredSelectionRow(int mouseX, int mouseY) {
        for (SelectionRow row : selectionRows()) {
            if (hovered(mouseX, mouseY, SELECTION_X, row.y(), SELECTION_WIDTH, SELECTION_ROW_HEIGHT)) {
                return row;
            }
        }
        return null;
    }

    private boolean hovered(int mouseX, int mouseY, int x, int y, int width, int height) {
        int left = leftPos + x;
        int top = topPos + y;
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private Component applyButtonLabel() {
        if (menu.craftActive()) {
            return Component.translatable("rngtech.exotic_affix_forge.button.working");
        }
        return Component.translatable(menu.craftFailed()
                ? "rngtech.exotic_affix_forge.button.retry"
                : "rngtech.exotic_affix_forge.button.apply");
    }

    private Component applyButtonTooltip() {
        if (menu.craftActive()) {
            return Component.translatable("rngtech.exotic_affix_forge.tooltip.apply.active");
        }
        if (menu.craftFailed()) {
            return Component.translatable("rngtech.exotic_affix_forge.tooltip.apply.retry");
        }
        if (!menu.canApplyCraft()) {
            return statusLabel();
        }
        return Component.translatable("rngtech.exotic_affix_forge.tooltip.apply.ready");
    }

    private void drawClipped(GuiGraphics guiGraphics, Component component, int x, int y, int width, int color) {
        String clipped = font.plainSubstrByWidth(component.getString(), width);
        guiGraphics.drawString(font, Component.literal(clipped), x, y, color, false);
    }

    private void drawCentered(GuiGraphics guiGraphics, Component component, int centerX, int y, int color) {
        guiGraphics.drawString(font, component, centerX - font.width(component) / 2, y, color, false);
    }

    private String formatLong(long value) {
        return String.format(Locale.ROOT, "%,d", value);
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

    private void renderPlayerInventoryFrames(GuiGraphics guiGraphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlotFrame(guiGraphics, 60 + column * 18, 142 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderSlotFrame(guiGraphics, 60 + column * 18, 200);
        }
    }

    private void renderSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + 18, top + 18, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + 17, top + 17, 0xFFE0E0E0);
        guiGraphics.fill(left + 2, top + 2, left + 16, top + 16, 0xFF8B8B8B);
    }

    private void renderIconBox(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + STATUS_ICON_SIZE, top + STATUS_ICON_SIZE, 0xFF5F5F5F);
        guiGraphics.fill(left + 1, top + 1, left + STATUS_ICON_SIZE - 1, top + STATUS_ICON_SIZE - 1, 0xFFD5D5D5);
    }

    private record SelectionRow(
            RefinementSelection.Kind kind,
            int affixIndex,
            ModifierSlot emptySlot,
            Component title,
            Component tooltip,
            boolean enabled,
            boolean selected,
            int y
    ) {
    }
}
