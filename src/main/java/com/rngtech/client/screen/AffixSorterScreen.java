package com.rngtech.client.screen;

import com.rngtech.content.affixsorter.AffixSorterCosts;
import com.rngtech.content.affixsorter.SorterRule;
import com.rngtech.content.affixsorter.SorterRules;
import com.rngtech.content.blockentity.AffixSorterBlockEntity;
import com.rngtech.content.menu.AffixSorterMenu;
import com.rngtech.content.menu.WorkOrderMenuSupport;
import com.rngtech.content.network.SorterRulesPayload;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.MachineModifierText;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.ModifierDefinition;
import com.rngtech.rpg.ModifierEligibilityProfile;
import com.rngtech.rpg.ModifierEligibilityProfiles;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.refinement.RefinementTargets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The Affix Sorter screen. Process, Gear, Stats, and Refinement follow the machine screen rules; the Rules tab is the
 * one place that may use text, because building conditions needs names and numbers.
 */
public class AffixSorterScreen extends AbstractContainerScreen<AffixSorterMenu> implements MachineTabs.Host {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFFB43B28;
    private static final int PROGRESS = 0xFF6A4F8C;
    private static final int STAT_ACCENT = 0xFF6A4F8C;
    private static final int PASS = 0xFF4E8A4A;
    private static final int REJECT = 0xFFA6473E;
    private static final int INPUT_FACE = 0xFF5F5F5F;
    private static final int ROW = 0xFFB5B5B5;
    private static final int ROW_SELECTED = 0xFFEDEDED;
    private static final int WELL = 0xFF2F2F2F;
    private static final int[] RARITY_COLORS = {0xFFC8C8C8, 0xFF8888FF, 0xFFFFFF77, 0xFFAF6025};
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 18;
    private static final int STAT_PANEL_WIDTH = 224;
    private static final int ENERGY_BAR_X = 28;
    private static final int ENERGY_BAR_Y = 30;
    private static final int ENERGY_BAR_WIDTH = 12;
    private static final int ENERGY_BAR_HEIGHT = 52;
    private static final int INPUT_SLOT_X = 44;
    private static final int INPUT_SLOT_Y = 51;
    private static final int BUFFER_X = 140;
    private static final int PASS_Y = 29;
    private static final int REJECT_Y = 73;
    private static final int PROGRESS_BAR_X = 70;
    private static final int PROGRESS_BAR_Y = 57;
    private static final int PROGRESS_BAR_WIDTH = 60;
    private static final int PROGRESS_BAR_HEIGHT = 7;
    private static final int STATUS_ICON_X = 70;
    private static final int ENERGY_ICON_X = 86;
    private static final int ICON_Y = 40;
    private static final int ICON_SIZE = 12;
    private static final int FACE_CELL = 8;
    private static final int FACE_NET_X = 80;
    private static final int FACE_NET_Y = 71;
    private static final int LIST_X = 8;
    private static final int LIST_Y = 20;
    private static final int LIST_WIDTH = 90;
    private static final int ROW_HEIGHT = 11;
    private static final int VISIBLE_ROWS = 12;
    private static final int EDITOR_X = 102;
    private static final int EDITOR_WIDTH = 130;
    private static final int LINE = 12;
    private static final int PICKER_ROWS = 11;
    private static final String[] STAT_LABEL_KEYS = {
            "rngtech.stat.processing_speed",
            "rngtech.stat.energy_usage",
            "rngtech.stat.energy_capacity",
            "rngtech.stat.rule_capacity"
    };
    private static final MachineStat[] STAT_TYPES = {
            MachineStat.PROCESSING_SPEED,
            MachineStat.ENERGY_USAGE,
            MachineStat.ENERGY_CAPACITY,
            MachineStat.RULE_CAPACITY
    };

    /** A clickable area built while drawing the Rules tab. */
    private record Hit(int x, int y, int width, int height, Runnable action, Component tooltip) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private final MachineTabs tabs;
    private final List<Hit> hits = new ArrayList<>();
    private EditBox search;
    private int selectedRule = -1;
    private int listScroll;
    private boolean picking;
    private int pickerScroll;
    private int tagCursor;

    public AffixSorterScreen(AffixSorterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 240;
        imageHeight = 200;
        inventoryLabelX = 39;
        inventoryLabelY = 104;
        tabs = MachineTabs.builder(this)
                .processing(AffixSorterMenu.TAB_PROCESSING, ModBlocks.AFFIX_SORTER.get())
                .gear(AffixSorterMenu.TAB_GEAR)
                .stats(AffixSorterMenu.TAB_STATS)
                .refinement(AffixSorterMenu.TAB_REFINEMENT)
                .tab(AffixSorterMenu.TAB_RULES, "rngtech.tab.rules", ModItems.RULE_CARD.get())
                .build();
    }

    @Override
    public MachineTabs machineTabs() {
        return tabs;
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font, leftPos + EDITOR_X + 2, topPos + 20, EDITOR_WIDTH - 4, 12, Component.translatable("rngtech.affix_sorter.search"));
        search.setMaxLength(40);
        search.setResponder(text -> pickerScroll = 0);
        search.setVisible(false);
        addWidget(search);
    }

    private boolean rulesTab() {
        return menu.selectedTab() == AffixSorterMenu.TAB_RULES;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        search.setVisible(rulesTab() && picking);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (search.isVisible()) {
            search.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        renderTooltip(guiGraphics, mouseX, mouseY);
        tabs.renderTooltip(guiGraphics, mouseX, mouseY);
        if (menu.selectedTab() == AffixSorterMenu.TAB_PROCESSING) {
            renderProcessingTooltips(guiGraphics, mouseX, mouseY);
        } else if (menu.selectedTab() == AffixSorterMenu.TAB_STATS) {
            MachineScreenStyle.renderStatPanelTooltip(guiGraphics, font, leftPos, topPos, mouseX, mouseY,
                    STAT_PANEL_X, STAT_PANEL_Y, STAT_PANEL_WIDTH, statLines());
        } else if (rulesTab()) {
            for (Hit hit : hits) {
                if (hit.tooltip() != null && hit.contains(mouseX, mouseY)) {
                    guiGraphics.renderTooltip(font, hit.tooltip(), mouseX, mouseY);
                    break;
                }
            }
        }
        RefinementScreenStyle.renderTooltips(guiGraphics, font, leftPos, topPos, mouseX, mouseY,
                menu.selectedTab() == RefinementScreenStyle.REFINEMENT_TAB_INDEX, menu.machineTraits(), menu);
        GearSlotTooltips.render(this, guiGraphics, font, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 1, PANEL_LIGHT);
        guiGraphics.fill(leftPos, topPos, leftPos + 1, topPos + imageHeight, PANEL_LIGHT);
        guiGraphics.fill(leftPos + imageWidth - 1, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL_DARK);
        guiGraphics.fill(leftPos, topPos + imageHeight - 1, leftPos + imageWidth, topPos + imageHeight, PANEL_DARK);
        tabs.render(guiGraphics, mouseX, mouseY, menu.selectedTab());
        renderSlotFrames(guiGraphics);
        switch (menu.selectedTab()) {
            case AffixSorterMenu.TAB_PROCESSING -> renderProcessing(guiGraphics);
            case AffixSorterMenu.TAB_STATS -> MachineScreenStyle.renderStatPanel(guiGraphics, leftPos, topPos, STAT_PANEL_X, STAT_PANEL_Y,
                    STAT_PANEL_WIDTH, statLines().length, STAT_ACCENT);
            case AffixSorterMenu.TAB_REFINEMENT -> RefinementScreenStyle.render(guiGraphics, leftPos, topPos, menu.machineTraits(), STAT_ACCENT);
            case AffixSorterMenu.TAB_RULES -> renderRules(guiGraphics, mouseX, mouseY);
            default -> {
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        tabs.renderSelectedLabel(guiGraphics, menu.selectedTab(), titleLabelX, titleLabelY, TEXT_MUTED);
        int tab = menu.selectedTab();
        if (tab != AffixSorterMenu.TAB_STATS && tab != AffixSorterMenu.TAB_RULES) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }
        if (tab == AffixSorterMenu.TAB_PROCESSING) {
            drawCentered(guiGraphics, Component.translatable("rngtech.processing.input.tiny"), INPUT_SLOT_X + 9, 39);
        } else if (tab == AffixSorterMenu.TAB_GEAR) {
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.gear.battery_cell.short"), 64, 35, 34, TEXT_MUTED);
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.gear.rule_core.short"), 100, 35, 34, TEXT_MUTED);
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.gear.servo.short"), 136, 35, 34, TEXT_MUTED);
        } else if (tab == AffixSorterMenu.TAB_STATS) {
            MachineScreenStyle.drawStatPanelLabels(guiGraphics, font, Component.translatable("rngtech.tab.stats"), statLines(),
                    STAT_PANEL_X, STAT_PANEL_Y, STAT_PANEL_WIDTH, STAT_ACCENT);
        } else if (tab == AffixSorterMenu.TAB_REFINEMENT) {
            RefinementScreenStyle.drawLabels(guiGraphics, font, menu.machineTraits(), STAT_ACCENT);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int tab = tabs.tabAt(mouseX, mouseY);
            if (tab >= 0) {
                menu.selectTab(tab);
                picking = false;
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, WorkOrderMenuSupport.BUTTON_SELECT_TAB + menu.selectedTab());
                }
                return true;
            }
            if (menu.selectedTab() == AffixSorterMenu.TAB_REFINEMENT && RefinementScreenStyle.handleApplyClick(this, menu, mouseX, mouseY)) {
                return true;
            }
            if (menu.selectedTab() == AffixSorterMenu.TAB_PROCESSING) {
                int face = faceAt(mouseX, mouseY);
                if (face >= 0 && minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AffixSorterMenu.BUTTON_FACE_BASE + face);
                    return true;
                }
            }
        }
        if (rulesTab()) {
            if (search.isVisible() && search.mouseClicked(mouseX, mouseY, button)) {
                setFocused(search);
                return true;
            }
            for (Hit hit : List.copyOf(hits)) {
                if (hit.contains(mouseX, mouseY)) {
                    hit.action().run();
                    return true;
                }
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (rulesTab()) {
            int step = scrollY > 0 ? -1 : 1;
            if (picking && mouseX >= leftPos + EDITOR_X) {
                pickerScroll = Math.max(0, pickerScroll + step);
            } else {
                listScroll = Math.max(0, Math.min(Math.max(0, rules().rules().size() - VISIBLE_ROWS), listScroll + step));
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (search.isVisible() && search.isFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                picking = false;
                search.setFocused(false);
                return true;
            }
            return search.keyPressed(key, scan, modifiers) || true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (search.isVisible() && search.isFocused()) {
            return search.charTyped(character, modifiers);
        }
        return super.charTyped(character, modifiers);
    }

    // Process tab

    private void renderSlotFrames(GuiGraphics guiGraphics) {
        int tab = menu.selectedTab();
        if (tab == AffixSorterMenu.TAB_PROCESSING) {
            renderSlotFrame(guiGraphics, INPUT_SLOT_X, INPUT_SLOT_Y);
            for (int slot = 0; slot < AffixSorterBlockEntity.BUFFER_SLOTS; slot++) {
                renderSlotFrame(guiGraphics, BUFFER_X + slot * 18, PASS_Y);
                renderSlotFrame(guiGraphics, BUFFER_X + slot * 18, REJECT_Y);
            }
        } else if (tab == AffixSorterMenu.TAB_GEAR) {
            renderSlotFrame(guiGraphics, 55, 47);
            renderSlotFrame(guiGraphics, 91, 47);
            renderSlotFrame(guiGraphics, 127, 47);
        } else if (tab == AffixSorterMenu.TAB_REFINEMENT) {
            renderSlotFrame(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            renderSlotFrame(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
        }
        if (tab == AffixSorterMenu.TAB_STATS || tab == AffixSorterMenu.TAB_RULES) {
            return;
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlotFrame(guiGraphics, 38 + column * 18, 115 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderSlotFrame(guiGraphics, 38 + column * 18, 173);
        }
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(x + ENERGY_BAR_X, y + ENERGY_BAR_Y, x + ENERGY_BAR_X + ENERGY_BAR_WIDTH, y + ENERGY_BAR_Y + ENERGY_BAR_HEIGHT, 0xFF5F5F5F);
        int energyHeight = Math.round((ENERGY_BAR_HEIGHT - 4) * menu.energyProgress());
        int energyBottom = y + ENERGY_BAR_Y + ENERGY_BAR_HEIGHT - 2;
        guiGraphics.fill(x + ENERGY_BAR_X + 2, energyBottom - energyHeight, x + ENERGY_BAR_X + ENERGY_BAR_WIDTH - 2, energyBottom, ENERGY);

        guiGraphics.fill(x + PROGRESS_BAR_X, y + PROGRESS_BAR_Y, x + PROGRESS_BAR_X + PROGRESS_BAR_WIDTH, y + PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT, 0xFF5F5F5F);
        guiGraphics.fill(x + PROGRESS_BAR_X + 1, y + PROGRESS_BAR_Y + 1,
                x + PROGRESS_BAR_X + 1 + Math.round((PROGRESS_BAR_WIDTH - 2) * menu.processingProgress()),
                y + PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT - 1, PROGRESS);

        // Route stripes beside each buffer row, the last route lit.
        guiGraphics.fill(x + BUFFER_X - 5, y + PASS_Y + 1, x + BUFFER_X - 2, y + PASS_Y + 17, menu.lastRoute() == 0 ? PASS : darker(PASS));
        guiGraphics.fill(x + BUFFER_X - 5, y + REJECT_Y + 1, x + BUFFER_X - 2, y + REJECT_Y + 17, menu.lastRoute() == 1 ? REJECT : darker(REJECT));

        renderIconBox(guiGraphics, STATUS_ICON_X, ICON_Y);
        guiGraphics.fill(x + STATUS_ICON_X + 3, y + ICON_Y + 3, x + STATUS_ICON_X + 9, y + ICON_Y + 9, statusColor());
        renderIconBox(guiGraphics, ENERGY_ICON_X, ICON_Y);
        int boltX = x + ENERGY_ICON_X;
        int boltY = y + ICON_Y;
        guiGraphics.fill(boltX + 6, boltY + 2, boltX + 9, boltY + 3, ENERGY);
        guiGraphics.fill(boltX + 5, boltY + 3, boltX + 8, boltY + 6, ENERGY);
        guiGraphics.fill(boltX + 4, boltY + 6, boltX + 7, boltY + 7, ENERGY);
        guiGraphics.fill(boltX + 3, boltY + 7, boltX + 6, boltY + 10, ENERGY);

        for (AffixSorterBlockEntity.RelativeFace face : AffixSorterBlockEntity.RelativeFace.values()) {
            int[] cell = faceCell(face);
            int color = switch (menu.faceRoute(face)) {
                case PASS -> PASS;
                case REJECT -> REJECT;
                case NONE -> INPUT_FACE;
            };
            guiGraphics.fill(x + cell[0], y + cell[1], x + cell[0] + FACE_CELL, y + cell[1] + FACE_CELL, 0xFF373737);
            guiGraphics.fill(x + cell[0] + 1, y + cell[1] + 1, x + cell[0] + FACE_CELL - 1, y + cell[1] + FACE_CELL - 1, color);
        }
        // A dot marks the front face.
        int[] front = faceCell(AffixSorterBlockEntity.RelativeFace.FRONT);
        guiGraphics.fill(x + front[0] + 3, y + front[1] + 3, x + front[0] + 5, y + front[1] + 5, PANEL_LIGHT);
    }

    /** The face net, unfolded around the front: top above, bottom below, then left, front, right, back across. */
    private static int[] faceCell(AffixSorterBlockEntity.RelativeFace face) {
        int step = FACE_CELL + 1;
        return switch (face) {
            case TOP -> new int[] {FACE_NET_X + step, FACE_NET_Y};
            case LEFT -> new int[] {FACE_NET_X, FACE_NET_Y + step};
            case FRONT -> new int[] {FACE_NET_X + step, FACE_NET_Y + step};
            case RIGHT -> new int[] {FACE_NET_X + 2 * step, FACE_NET_Y + step};
            case BACK -> new int[] {FACE_NET_X + 3 * step, FACE_NET_Y + step};
            case BOTTOM -> new int[] {FACE_NET_X + step, FACE_NET_Y + 2 * step};
        };
    }

    private int faceAt(double mouseX, double mouseY) {
        for (AffixSorterBlockEntity.RelativeFace face : AffixSorterBlockEntity.RelativeFace.values()) {
            int[] cell = faceCell(face);
            if (mouseX >= leftPos + cell[0] && mouseX < leftPos + cell[0] + FACE_CELL
                    && mouseY >= topPos + cell[1] && mouseY < topPos + cell[1] + FACE_CELL) {
                return face.ordinal();
            }
        }
        return -1;
    }

    private void renderProcessingTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int face = faceAt(mouseX, mouseY);
        if (face >= 0) {
            AffixSorterBlockEntity.RelativeFace relative = AffixSorterBlockEntity.RelativeFace.values()[face];
            guiGraphics.renderComponentTooltip(font, List.of(
                    Component.translatable("rngtech.affix_sorter.face", Component.translatable("rngtech.affix_sorter.face." + relative.name().toLowerCase(Locale.ROOT)),
                            Component.translatable("rngtech.affix_sorter.route." + menu.faceRoute(relative).name().toLowerCase(Locale.ROOT))),
                    Component.translatable("rngtech.affix_sorter.face.hint")
            ), mouseX, mouseY);
            return;
        }
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY,
                ENERGY_BAR_X, ENERGY_BAR_Y, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT,
                CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity()));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY,
                STATUS_ICON_X, ICON_Y, ICON_SIZE, ICON_SIZE,
                Component.translatable("rngtech.recycler.tooltip.status", statusText()));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY,
                ENERGY_ICON_X, ICON_Y, ICON_SIZE, ICON_SIZE,
                Component.translatable("rngtech.affix_sorter.tooltip.energy", CompactValueText.energyRate(menu.energyPerTick()),
                        CompactValueText.energyAmount(menu.fePerItem()), menu.enabledRules()));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY,
                BUFFER_X - 5, PASS_Y, 3, 18, Component.translatable("rngtech.affix_sorter.route.pass"));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY,
                BUFFER_X - 5, REJECT_Y, 3, 18, Component.translatable("rngtech.affix_sorter.route.reject"));
    }

    private Component statusText() {
        String key = AffixSorterBlockEntity.statusKey(menu.statusCode());
        return Component.translatable(key.isEmpty() ? "rngtech.affix_sorter.status.ready" : key);
    }

    private int statusColor() {
        return switch (menu.statusCode()) {
            case AffixSorterBlockEntity.STATUS_READY -> STAT_ACCENT;
            case AffixSorterBlockEntity.STATUS_NO_INPUT, AffixSorterBlockEntity.STATUS_MISSING_CORE -> PANEL_DARK;
            case AffixSorterBlockEntity.STATUS_NO_POWER -> 0xFFD3A33A;
            default -> 0xFFB45B4A;
        };
    }

    private MachineScreenStyle.StatLine[] statLines() {
        int[] indices = {
                AffixSorterBlockEntity.DATA_PROCESSING_SPEED,
                AffixSorterBlockEntity.DATA_ENERGY_USAGE,
                AffixSorterBlockEntity.DATA_ENERGY_CAPACITY_STAT,
                -1
        };
        MachineScreenStyle.StatLine[] lines = new MachineScreenStyle.StatLine[STAT_TYPES.length];
        for (int index = 0; index < lines.length; index++) {
            double value = indices[index] < 0 ? menu.ruleCapacity() : menu.statValue(indices[index]);
            boolean integral = STAT_TYPES[index] == MachineStat.ENERGY_CAPACITY || STAT_TYPES[index] == MachineStat.RULE_CAPACITY;
            boolean enhanced = STAT_TYPES[index] == MachineStat.ENERGY_USAGE ? value < 0.999 : !integral && value > 1.001;
            lines[index] = MachineScreenStyle.statLine(
                    Component.translatable(STAT_LABEL_KEYS[index]),
                    MachineScreenStyle.statValue(STAT_TYPES[index], value),
                    value,
                    integral,
                    enhanced,
                    MachineScreenStyle.statLayerTooltip(menu.getSlot(menu.refinementTargetSlot()).getItem(), STAT_TYPES[index], value)
            ).withStat(STAT_TYPES[index]);
        }
        return lines;
    }

    // Rules tab

    private SorterRules rules() {
        return menu.sorterRules();
    }

    private SorterRule selected() {
        List<SorterRule> list = rules().rules();
        return selectedRule >= 0 && selectedRule < list.size() ? list.get(selectedRule) : null;
    }

    private void commit(SorterRules next) {
        menu.applySorterRules(next);
        PacketDistributor.sendToServer(new SorterRulesPayload(menu.containerId, next));
    }

    private void edit(SorterRule rule) {
        if (selectedRule >= 0) {
            commit(rules().withRule(selectedRule, rule));
        }
    }

    private void renderRules(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        hits.clear();
        renderRuleList(guiGraphics);
        if (picking && selected() != null) {
            renderPicker(guiGraphics);
        } else {
            renderEditor(guiGraphics);
        }
        renderFooter(guiGraphics);
    }

    private void renderRuleList(GuiGraphics guiGraphics) {
        int x = leftPos + LIST_X;
        int y = topPos + LIST_Y;
        guiGraphics.fill(x - 1, y - 1, x + LIST_WIDTH + 1, y + VISIBLE_ROWS * ROW_HEIGHT + 1, WELL);
        List<SorterRule> list = rules().rules();
        int capacity = menu.ruleCapacity();
        int enabledSeen = 0;
        for (int index = 0; index < list.size(); index++) {
            SorterRule rule = list.get(index);
            if (rule.enabled()) {
                enabledSeen++;
            }
            int row = index - listScroll;
            if (row < 0 || row >= VISIBLE_ROWS) {
                continue;
            }
            int rowY = y + row * ROW_HEIGHT;
            int rowIndex = index;
            guiGraphics.fill(x, rowY, x + LIST_WIDTH, rowY + ROW_HEIGHT - 1, index == selectedRule ? ROW_SELECTED : ROW);
            guiGraphics.fill(x + 1, rowY + 1, x + 4, rowY + ROW_HEIGHT - 2, rule.route() == SorterRule.Route.PASS ? PASS : REJECT);
            if (index == menu.lastRule()) {
                guiGraphics.fill(x + LIST_WIDTH - 3, rowY + 1, x + LIST_WIDTH - 1, rowY + ROW_HEIGHT - 2, STAT_ACCENT);
            }
            int textColor = rule.enabled() && enabledSeen <= capacity ? TEXT : TEXT_MUTED;
            String label = (index + 1) + " " + summary(rule).getString();
            guiGraphics.drawString(font, clip(label, LIST_WIDTH - 20), x + 6, rowY + 2, textColor, false);
            renderCheck(guiGraphics, x + LIST_WIDTH - 14, rowY + 1, rule.enabled());
            hits.add(new Hit(x + LIST_WIDTH - 14, rowY, 10, ROW_HEIGHT - 1, () -> toggleEnabled(rowIndex),
                    Component.translatable(rule.enabled() ? "rngtech.affix_sorter.rule.disable" : "rngtech.affix_sorter.rule.enable")));
            hits.add(new Hit(x, rowY, LIST_WIDTH - 14, ROW_HEIGHT - 1, () -> {
                selectedRule = rowIndex;
                picking = false;
            }, describe(rule)));
        }
        int buttonY = y + VISIBLE_ROWS * ROW_HEIGHT + 4;
        button(guiGraphics, x, buttonY, 14, "+", Component.translatable("rngtech.affix_sorter.rule.add"), this::addRule);
        button(guiGraphics, x + 16, buttonY, 14, "-", Component.translatable("rngtech.affix_sorter.rule.remove"), () -> {
            if (selected() != null) {
                commit(rules().withRemoved(selectedRule));
                selectedRule = Math.min(selectedRule, rules().rules().size() - 1);
            }
        });
        button(guiGraphics, x + 32, buttonY, 14, "↑", Component.translatable("rngtech.affix_sorter.rule.up"), () -> move(-1));
        button(guiGraphics, x + 48, buttonY, 14, "↓", Component.translatable("rngtech.affix_sorter.rule.down"), () -> move(1));
        SorterRule.Route fallback = rules().fallback();
        Component fallbackLabel = Component.translatable("rngtech.affix_sorter.fallback." + fallback.getSerializedName());
        int fallbackX = x;
        int fallbackY = buttonY + 14;
        int width = LIST_WIDTH;
        guiGraphics.fill(fallbackX, fallbackY, fallbackX + width, fallbackY + 11, fallback == SorterRule.Route.PASS ? PASS : REJECT);
        guiGraphics.drawString(font, clip(fallbackLabel.getString(), width - 4), fallbackX + 3, fallbackY + 2, 0xFFFFFFFF, false);
        hits.add(new Hit(fallbackX, fallbackY, width, 11, () -> commit(rules().withFallback(fallback.toggled())),
                Component.translatable("rngtech.affix_sorter.fallback.hint")));
    }

    private void addRule() {
        if (rules().rules().size() >= SorterRules.MAX_RULES) {
            return;
        }
        SorterRule rule = rules().enabledCount() < menu.ruleCapacity() ? SorterRule.DEFAULT : SorterRule.DEFAULT.withEnabled(false);
        commit(rules().withAdded(rule));
        selectedRule = rules().rules().size() - 1;
        listScroll = Math.max(0, selectedRule - VISIBLE_ROWS + 1);
    }

    private void move(int direction) {
        if (selected() != null) {
            commit(rules().withMoved(selectedRule, direction));
            selectedRule = Math.max(0, Math.min(rules().rules().size() - 1, selectedRule + direction));
        }
    }

    /** Enabling a rule past Rule Capacity is refused, so a click never silently switches off another rule. */
    private void toggleEnabled(int index) {
        SorterRule rule = rules().rules().get(index);
        if (!rule.enabled() && rules().enabledCount() >= menu.ruleCapacity()) {
            return;
        }
        commit(rules().withRule(index, rule.withEnabled(!rule.enabled())));
    }

    private void renderEditor(GuiGraphics guiGraphics) {
        int x = leftPos + EDITOR_X;
        int y = topPos + LIST_Y;
        SorterRule rule = selected();
        if (rule == null) {
            guiGraphics.drawWordWrap(font, Component.translatable("rngtech.affix_sorter.rule.none"), x, y + 4, EDITOR_WIDTH, TEXT_MUTED);
            return;
        }
        int row = y;
        // Route, pulse
        chip(guiGraphics, x, row, 50, Component.translatable("rngtech.affix_sorter.route." + rule.route().getSerializedName()),
                rule.route() == SorterRule.Route.PASS ? PASS : REJECT, Component.translatable("rngtech.affix_sorter.route.hint"),
                () -> edit(rule.withRoute(rule.route().toggled())));
        chip(guiGraphics, x + 54, row, 50, Component.translatable("rngtech.affix_sorter.pulse"), rule.pulse() ? STAT_ACCENT : PANEL_DARK,
                Component.translatable("rngtech.affix_sorter.pulse.hint"), () -> edit(rule.withPulse(!rule.pulse())));
        row += LINE + 1;

        // Items and tag
        label(guiGraphics, "rngtech.affix_sorter.cond.items", x, row);
        int itemX = x + 30;
        for (int index = 0; index < rule.items().size(); index++) {
            Item item = rule.items().get(index);
            int slotX = itemX + index * 16;
            guiGraphics.renderFakeItem(new ItemStack(item), slotX, row - 4);
            int removed = index;
            hits.add(new Hit(slotX, row - 4, 16, 16, () -> {
                List<Item> next = new ArrayList<>(rule.items());
                next.remove(removed);
                edit(rule.withItems(next, rule.tag()));
            }, Component.translatable("rngtech.affix_sorter.cond.item_remove", new ItemStack(item).getHoverName())));
        }
        if (rule.items().size() < SorterRule.MAX_ITEMS) {
            button(guiGraphics, itemX + rule.items().size() * 16 + 2, row - 1, 12, "+",
                    Component.translatable("rngtech.affix_sorter.cond.item_add"), () -> addInputItem(rule));
        }
        row += LINE + 2;
        label(guiGraphics, "rngtech.affix_sorter.cond.tag", x, row);
        String tagText = rule.tag().map(ResourceLocation::toString).orElseGet(() -> Component.translatable("rngtech.affix_sorter.any").getString());
        chip(guiGraphics, x + 30, row, EDITOR_WIDTH - 30, Component.literal(tagText), PANEL_DARK,
                Component.translatable("rngtech.affix_sorter.cond.tag_hint"), () -> cycleTag(rule));
        row += LINE;

        // Stage range
        label(guiGraphics, "rngtech.affix_sorter.cond.stage", x, row);
        stepper(guiGraphics, x + 30, row, String.valueOf(rule.minStage()), () -> edit(rule.withStages(rule.minStage() - 1, rule.maxStage())),
                () -> edit(rule.withStages(Math.min(rule.maxStage(), rule.minStage() + 1), rule.maxStage())));
        stepper(guiGraphics, x + 78, row, String.valueOf(rule.maxStage()), () -> edit(rule.withStages(rule.minStage(), Math.max(rule.minStage(), rule.maxStage() - 1))),
                () -> edit(rule.withStages(rule.minStage(), rule.maxStage() + 1)));
        row += LINE;

        // Rarity
        label(guiGraphics, "rngtech.affix_sorter.cond.rarity", x, row);
        for (Rarity rarity : Rarity.values()) {
            boolean on = rule.rarities().contains(rarity);
            int chipX = x + 30 + rarity.ordinal() * 25;
            chip(guiGraphics, chipX, row, 23, Component.translatable(rarity.translationKey()).copy(), on ? RARITY_COLORS[rarity.ordinal()] : PANEL_DARK,
                    Component.translatable("rngtech.affix_sorter.cond.rarity_hint"), () -> {
                        List<Rarity> next = new ArrayList<>(rule.rarities());
                        if (on) {
                            next.remove(rarity);
                        } else {
                            next.add(rarity);
                        }
                        edit(rule.withRarities(next));
                    });
        }
        row += LINE + 1;

        // Affixes
        label(guiGraphics, "rngtech.affix_sorter.cond.affixes", x, row);
        if (!rule.affixes().isEmpty()) {
            stepper(guiGraphics, x + 46, row, Component.translatable("rngtech.affix_sorter.cond.at_least", rule.minAffixMatches()).getString(),
                    () -> edit(rule.withAffixes(rule.affixes(), rule.minAffixMatches() - 1)),
                    () -> edit(rule.withAffixes(rule.affixes(), rule.minAffixMatches() + 1)));
        }
        if (rule.affixes().size() < SorterRule.MAX_AFFIXES) {
            button(guiGraphics, x + EDITOR_WIDTH - 12, row - 1, 12, "+", Component.translatable("rngtech.affix_sorter.cond.affix_add"), () -> {
                picking = true;
                pickerScroll = 0;
                search.setValue("");
                setFocused(search);
                search.setFocused(true);
            });
        }
        row += LINE;
        Map<String, ModifierDefinition> known = affixCatalog(rule);
        for (int index = 0; index < rule.affixes().size(); index++) {
            SorterRule.AffixCondition condition = rule.affixes().get(index);
            int conditionIndex = index;
            ModifierDefinition definition = known.get(condition.affixId());
            String name = definition == null ? condition.affixId() : affixLabel(definition).getString();
            guiGraphics.drawString(font, clip(name, EDITOR_WIDTH - 52), x + 2, row, TEXT, false);
            String tier = condition.minTier() <= 0
                    ? Component.translatable("rngtech.affix_sorter.any").getString()
                    : "T" + condition.minTier() + "+";
            stepper(guiGraphics, x + EDITOR_WIDTH - 50, row, tier,
                    () -> editAffix(rule, conditionIndex, new SorterRule.AffixCondition(condition.affixId(), condition.minTier() - 1)),
                    () -> editAffix(rule, conditionIndex, new SorterRule.AffixCondition(condition.affixId(), condition.minTier() + 1)));
            button(guiGraphics, x + EDITOR_WIDTH - 10, row - 1, 10, "x", Component.translatable("rngtech.affix_sorter.cond.affix_remove"), () -> {
                List<SorterRule.AffixCondition> next = new ArrayList<>(rule.affixes());
                next.remove(conditionIndex);
                edit(rule.withAffixes(next, rule.minAffixMatches()));
            });
            row += LINE - 2;
        }
        row = Math.max(row, topPos + LIST_Y + 7 * LINE + 6 * (LINE - 2) - 6);

        // Thresholds and states
        stepperLabeled(guiGraphics, x, row, "rngtech.affix_sorter.cond.quality", rule.minQuality(),
                () -> edit(rule.withThresholds(rule.minQuality() - 1, rule.minRefinementPotential(), rule.minUniqueQuality())),
                () -> edit(rule.withThresholds(rule.minQuality() + 1, rule.minRefinementPotential(), rule.minUniqueQuality())));
        stepperLabeled(guiGraphics, x + 66, row, "rngtech.affix_sorter.cond.rp", rule.minRefinementPotential(),
                () -> edit(rule.withThresholds(rule.minQuality(), rule.minRefinementPotential() - 1, rule.minUniqueQuality())),
                () -> edit(rule.withThresholds(rule.minQuality(), rule.minRefinementPotential() + 1, rule.minUniqueQuality())));
        row += LINE;
        stepperLabeled(guiGraphics, x, row, "rngtech.affix_sorter.cond.unique", rule.minUniqueQuality(),
                () -> edit(rule.withThresholds(rule.minQuality(), rule.minRefinementPotential(), rule.minUniqueQuality() - 5)),
                () -> edit(rule.withThresholds(rule.minQuality(), rule.minRefinementPotential(), rule.minUniqueQuality() + 5)));
        chip(guiGraphics, x + 66, row, 31, stateLabel("corrupted", rule.corrupted()), PANEL_DARK,
                Component.translatable("rngtech.affix_sorter.cond.corrupted_hint"), () -> edit(rule.withStates(rule.corrupted().next(), rule.mirrored())));
        chip(guiGraphics, x + 99, row, 31, stateLabel("mirrored", rule.mirrored()), PANEL_DARK,
                Component.translatable("rngtech.affix_sorter.cond.mirrored_hint"), () -> edit(rule.withStates(rule.corrupted(), rule.mirrored().next())));
    }

    private void editAffix(SorterRule rule, int index, SorterRule.AffixCondition condition) {
        List<SorterRule.AffixCondition> next = new ArrayList<>(rule.affixes());
        next.set(index, condition);
        edit(rule.withAffixes(next, rule.minAffixMatches()));
    }

    /** Adds the item waiting in the sorter's input slot to the rule. */
    private void addInputItem(SorterRule rule) {
        ItemStack input = menu.getSlot(AffixSorterBlockEntity.SLOT_INPUT).getItem();
        if (input.isEmpty() || rule.items().contains(input.getItem())) {
            return;
        }
        List<Item> next = new ArrayList<>(rule.items());
        next.add(input.getItem());
        edit(rule.withItems(next, rule.tag()));
    }

    /** Cycles through the item tags the rule's items share, then back to no tag. */
    private void cycleTag(SorterRule rule) {
        List<ResourceLocation> candidates = new ArrayList<>();
        ItemStack input = menu.getSlot(AffixSorterBlockEntity.SLOT_INPUT).getItem();
        List<Item> sources = rule.items().isEmpty() && !input.isEmpty() ? List.of(input.getItem()) : rule.items();
        for (Item item : sources) {
            BuiltInRegistries.ITEM.wrapAsHolder(item).tags().map(TagKey::location)
                    .filter(tag -> !candidates.contains(tag))
                    .sorted(Comparator.comparing(ResourceLocation::toString))
                    .forEach(candidates::add);
        }
        if (candidates.isEmpty()) {
            edit(rule.withItems(rule.items(), Optional.empty()));
            return;
        }
        tagCursor = (tagCursor + 1) % (candidates.size() + 1);
        edit(rule.withItems(rule.items(), tagCursor == candidates.size() ? Optional.empty() : Optional.of(candidates.get(tagCursor))));
    }

    private void renderPicker(GuiGraphics guiGraphics) {
        int x = leftPos + EDITOR_X;
        int y = topPos + LIST_Y + 14;
        SorterRule rule = selected();
        String query = search.getValue().toLowerCase(Locale.ROOT);
        List<ModifierDefinition> options = affixCatalog(rule).values().stream()
                .filter(definition -> rule.affixes().stream().noneMatch(condition -> condition.affixId().equals(definition.id())))
                .filter(definition -> query.isEmpty() || affixLabel(definition).getString().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(definition -> affixLabel(definition).getString()))
                .toList();
        pickerScroll = Math.min(pickerScroll, Math.max(0, options.size() - PICKER_ROWS));
        guiGraphics.fill(x - 1, y - 1, x + EDITOR_WIDTH + 1, y + PICKER_ROWS * ROW_HEIGHT + 1, WELL);
        for (int row = 0; row < PICKER_ROWS && pickerScroll + row < options.size(); row++) {
            ModifierDefinition definition = options.get(pickerScroll + row);
            int rowY = y + row * ROW_HEIGHT;
            guiGraphics.fill(x, rowY, x + EDITOR_WIDTH, rowY + ROW_HEIGHT - 1, ROW);
            guiGraphics.drawString(font, clip(affixLabel(definition).getString(), EDITOR_WIDTH - 6), x + 3, rowY + 2, TEXT, false);
            hits.add(new Hit(x, rowY, EDITOR_WIDTH, ROW_HEIGHT - 1, () -> {
                List<SorterRule.AffixCondition> next = new ArrayList<>(rule.affixes());
                next.add(new SorterRule.AffixCondition(definition.id(), 0));
                edit(rule.withAffixes(next, rule.minAffixMatches()));
                picking = false;
                search.setFocused(false);
            }, MachineModifierText.definitionStats(definition)));
        }
        button(guiGraphics, x + EDITOR_WIDTH - 30, y + PICKER_ROWS * ROW_HEIGHT + 3, 30, Component.translatable("gui.cancel").getString(), null, () -> {
            picking = false;
            search.setFocused(false);
        });
    }

    private void renderFooter(GuiGraphics guiGraphics) {
        int enabled = rules().enabledCount();
        long perItem = AffixSorterCosts.sorterFePerItem(Math.min(enabled, menu.ruleCapacity()));
        long nextRule = AffixSorterCosts.sorterFePerItem(Math.min(enabled, menu.ruleCapacity()) + 1);
        Component footer = Component.translatable("rngtech.affix_sorter.footer", enabled, menu.ruleCapacity(), CompactValueText.energyAmount(perItem));
        int x = leftPos + EDITOR_X;
        int y = topPos + imageHeight - 12;
        guiGraphics.drawString(font, footer, x, y, TEXT, false);
        hits.add(new Hit(x, y - 1, font.width(footer), 10, () -> {
        }, Component.translatable("rngtech.affix_sorter.footer.hint", CompactValueText.energyAmount(nextRule))));
    }

    /** Affixes the rule's items can roll, or every affix when the rule names no items. */
    private static Map<String, ModifierDefinition> affixCatalog(SorterRule rule) {
        List<ModifierEligibilityProfile> profiles = new ArrayList<>();
        if (rule != null && !rule.items().isEmpty()) {
            for (Item item : rule.items()) {
                ModifierEligibilityProfile profile = RefinementTargets.eligibilityProfile(new ItemStack(item));
                if (profile != null) {
                    profiles.add(profile);
                }
            }
        }
        if (profiles.isEmpty()) {
            profiles = ModifierEligibilityProfiles.allProfiles();
        }
        Map<String, ModifierDefinition> definitions = new LinkedHashMap<>();
        for (ModifierEligibilityProfile profile : profiles) {
            for (ModifierSlot slot : List.of(ModifierSlot.PREFIX, ModifierSlot.SUFFIX)) {
                for (ModifierDefinition definition : profile.rollableDefinitions(slot)) {
                    definitions.putIfAbsent(definition.id(), definition);
                }
            }
        }
        return definitions;
    }

    private static MutableComponent affixLabel(ModifierDefinition definition) {
        return MachineModifierText.displayName(definition)
                .append(" (")
                .append(Component.translatable(definition.stat().translationKey()))
                .append(")");
    }

    private static Component summary(SorterRule rule) {
        List<String> parts = new ArrayList<>();
        if (!rule.items().isEmpty()) {
            parts.add(new ItemStack(rule.items().getFirst()).getHoverName().getString() + (rule.items().size() > 1 ? " +" + (rule.items().size() - 1) : ""));
        } else if (rule.tag().isPresent()) {
            parts.add("#" + rule.tag().get().getPath());
        }
        if (!rule.rarities().isEmpty()) {
            parts.add(rule.rarities().stream().map(rarity -> Component.translatable(rarity.translationKey()).getString().substring(0, 1))
                    .reduce("", String::concat));
        }
        if (!rule.affixes().isEmpty()) {
            parts.add(rule.minAffixMatches() + "/" + rule.affixes().size() + "A");
        }
        if (rule.minQuality() > 0) {
            parts.add("Q" + rule.minQuality());
        }
        return parts.isEmpty() ? Component.translatable("rngtech.affix_sorter.rule.anything") : Component.literal(String.join(" ", parts));
    }

    private static Component describe(SorterRule rule) {
        return Component.translatable("rngtech.affix_sorter.rule.describe",
                Component.translatable("rngtech.affix_sorter.route." + rule.route().getSerializedName()), summary(rule));
    }

    private Component stateLabel(String key, SorterRule.StateMatch match) {
        return Component.translatable("rngtech.affix_sorter.cond." + key + "." + match.getSerializedName());
    }

    private void label(GuiGraphics guiGraphics, String key, int x, int y) {
        guiGraphics.drawString(font, Component.translatable(key), x, y + 1, TEXT_MUTED, false);
    }

    private void chip(GuiGraphics guiGraphics, int x, int y, int width, Component text, int color, Component tooltip, Runnable action) {
        guiGraphics.fill(x, y, x + width, y + 10, 0xFF373737);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + 9, color);
        guiGraphics.drawString(font, clip(text.getString(), width - 4), x + 2, y + 1, 0xFFFFFFFF, true);
        hits.add(new Hit(x, y, width, 10, action, tooltip));
    }

    private void button(GuiGraphics guiGraphics, int x, int y, int width, String text, Component tooltip, Runnable action) {
        guiGraphics.fill(x, y, x + width, y + 11, 0xFF373737);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + 10, PANEL_LIGHT);
        guiGraphics.drawString(font, text, x + (width - font.width(text)) / 2, y + 2, TEXT, false);
        hits.add(new Hit(x, y, width, 11, action, tooltip));
    }

    private void stepper(GuiGraphics guiGraphics, int x, int y, String value, Runnable down, Runnable up) {
        button(guiGraphics, x, y - 1, 9, "-", null, down);
        guiGraphics.drawString(font, value, x + 11, y + 1, TEXT, false);
        button(guiGraphics, x + 13 + Math.max(16, font.width(value)), y - 1, 9, "+", null, up);
    }

    private void stepperLabeled(GuiGraphics guiGraphics, int x, int y, String key, int value, Runnable down, Runnable up) {
        Component name = Component.translatable(key);
        guiGraphics.drawString(font, name, x, y + 1, TEXT_MUTED, false);
        stepper(guiGraphics, x + font.width(name) + 2, y, String.valueOf(value), down, up);
    }

    private void renderCheck(GuiGraphics guiGraphics, int x, int y, boolean on) {
        guiGraphics.fill(x, y, x + 9, y + 9, 0xFF373737);
        guiGraphics.fill(x + 1, y + 1, x + 8, y + 8, on ? STAT_ACCENT : PANEL_LIGHT);
    }

    private String clip(String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width("..."))) + "...";
    }

    private static int darker(int color) {
        int r = ((color >> 16) & 0xFF) / 2;
        int g = ((color >> 8) & 0xFF) / 2;
        int b = (color & 0xFF) / 2;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private void renderIconBox(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + ICON_SIZE, top + ICON_SIZE, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + ICON_SIZE - 1, top + ICON_SIZE - 1, 0xFF5F5F5F);
        guiGraphics.fill(left + 2, top + 2, left + ICON_SIZE - 2, top + ICON_SIZE - 2, 0xFF2F2F2F);
    }

    private void renderSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + 18, top + 18, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + 17, top + 17, 0xFFE0E0E0);
        guiGraphics.fill(left + 2, top + 2, left + 16, top + 16, 0xFF8B8B8B);
    }

    private void drawCentered(GuiGraphics guiGraphics, Component label, int centerX, int y) {
        guiGraphics.drawString(font, label, centerX - font.width(label) / 2, y, TEXT_MUTED, false);
    }
}
