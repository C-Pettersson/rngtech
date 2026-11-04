package com.rngtech.client.screen;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.IdentifierBlockEntity;
import com.rngtech.content.menu.IdentifierMenu;
import com.rngtech.content.menu.WorkOrderMenuSupport;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdentifierScreen extends AbstractContainerScreen<IdentifierMenu> implements MachineTabs.Host {
    private static final int BASE_IMAGE_WIDTH = 240;
    private static final int BASE_IMAGE_HEIGHT = 200;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFFB43B28;
    private static final int FLUID = 0xFF8FD8CC;
    private static final int PROGRESS = 0xFF3F8C86;
    private static final int STAT_ACCENT = 0xFF3F8C86;
    private static final int START_NODE = 0xFF38D857;
    private static final int MASTERY_RING = 0xFF2F5A58;
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 18;
    private static final int STAT_PANEL_WIDTH = 224;
    private static final int BAR_Y = 22;
    private static final int BAR_HEIGHT = 60;
    private static final int FLUID_BAR_X = 23;
    private static final int FLUID_BAR_WIDTH = 14;
    private static final int ENERGY_BAR_X = 44;
    private static final int ENERGY_BAR_WIDTH = 12;
    private static final int INPUT_SLOT_X = 71;
    private static final int OUTPUT_SLOT_X = 149;
    private static final int SLOT_Y = 51;
    private static final int CONTAINER_SLOT_X = 21;
    private static final int CONTAINER_SLOT_Y = 85;
    private static final int PROGRESS_BAR_X = 94;
    private static final int PROGRESS_BAR_Y = 57;
    private static final int PROGRESS_BAR_WIDTH = 50;
    private static final int PROGRESS_BAR_HEIGHT = 7;
    private static final int STATUS_ICON_X = 94;
    private static final int ENERGY_ICON_X = 110;
    private static final int ICON_Y = 40;
    private static final int ICON_SIZE = 12;
    private static final String[] STAT_LABEL_KEYS = {
            "rngtech.stat.processing_speed",
            "rngtech.stat.energy_usage",
            "rngtech.stat.fluid_usage",
            "rngtech.stat.energy_capacity",
            "rngtech.stat.fluid_transfer"
    };
    private static final int[] STAT_DATA_INDICES = {
            IdentifierBlockEntity.DATA_PROCESSING_SPEED,
            IdentifierBlockEntity.DATA_ENERGY_USAGE,
            IdentifierBlockEntity.DATA_FLUID_USAGE,
            IdentifierBlockEntity.DATA_ENERGY_CAPACITY_STAT,
            IdentifierBlockEntity.DATA_FLUID_TRANSFER
    };
    private static final MachineStat[] STAT_TYPES = {
            MachineStat.PROCESSING_SPEED,
            MachineStat.ENERGY_USAGE,
            MachineStat.FLUID_USAGE,
            MachineStat.ENERGY_CAPACITY,
            MachineStat.FLUID_TRANSFER
    };
    private static final Map<MegaPassiveNode, ResourceLocation> MASTERY_ICON_TEXTURES = createMasteryIconTextures();

    private final MasteryScreenSupport<MegaPassiveNode> masterySupport;
    private final MachineTabs tabs;
    private final WorkOrdersTab workOrdersTab;

    private static Map<MegaPassiveNode, ResourceLocation> createMasteryIconTextures() {
        Map<MegaPassiveNode, ResourceLocation> textures = new HashMap<>();
        for (MegaPassiveNode node : MegaPassiveTree.TREE.nodes()) {
            textures.put(node, RNGTech.id("textures/gui/mastery/" + node.masteryIconKey() + ".png"));
        }
        return Map.copyOf(textures);
    }

    public IdentifierScreen(IdentifierMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = BASE_IMAGE_WIDTH;
        imageHeight = BASE_IMAGE_HEIGHT;
        inventoryLabelX = 39;
        inventoryLabelY = 104;
        tabs = MachineTabs.builder(this)
                .processing(IdentifierMenu.TAB_PROCESSING, ModBlocks.IDENTIFIER.get())
                .gear(IdentifierMenu.TAB_GEAR)
                .stats(IdentifierMenu.TAB_STATS)
                .refinement(IdentifierMenu.TAB_REFINEMENT)
                .mastery(IdentifierMenu.TAB_MASTERY)
                .jewels(IdentifierMenu.TAB_JEWELS, menu::jewelsShown)
                .orders(IdentifierMenu.TAB_ORDERS, menu::workOrdersShown)
                .build();
        workOrdersTab = new WorkOrdersTab(menu, STAT_ACCENT);
        masterySupport = new MasteryScreenSupport<>(
                MegaPassiveTree.TREE,
                MegaPassiveTree.TREE.nodes(),
                MegaPassiveTree.node(MachineMasteryFamily.IDENTIFIER.startNodeId()),
                MASTERY_ICON_TEXTURES,
                menu,
                new MasteryScreenSupport.Callbacks<>() {
                    @Override
                    public boolean gearAllowsUnlock(MegaPassiveNode node) {
                        return true;
                    }

                    @Override
                    public void geometryChanged() {
                        updateImageSizeForSelectedTab();
                    }

                    @Override
                    public ItemStack socketedJewel(int index) {
                        return menu.jewel(index);
                    }
                },
                new MasteryScreenSupport.Palette(PANEL_DARK, PANEL_LIGHT, TEXT, STAT_ACCENT, FLUID, START_NODE, MASTERY_RING)
        );
    }

    @Override
    public MachineTabs machineTabs() {
        return tabs;
    }

    @Override
    protected void init() {
        updateImageSizeForSelectedTab();
        super.init();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        tabs.renderTooltip(guiGraphics, mouseX, mouseY);
        renderValueTooltips(guiGraphics, mouseX, mouseY);
        renderStatTooltips(guiGraphics, mouseX, mouseY);
        if (menu.selectedTab() == IdentifierMenu.TAB_MASTERY) {
            masterySupport.renderTooltips(guiGraphics, font, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        }
        if (menu.selectedTab() == IdentifierMenu.TAB_ORDERS) {
            List<Component> orderTooltip = workOrdersTab.tooltip(mouseX, mouseY, leftPos, topPos, imageWidth, imageHeight);
            if (!orderTooltip.isEmpty()) {
                guiGraphics.renderComponentTooltip(font, orderTooltip, mouseX, mouseY);
            }
        }
        if (menu.selectedTab() == IdentifierMenu.TAB_JEWELS) {
            List<Component> jewelTooltip = JewelsTab.tooltip(this);
            if (!jewelTooltip.isEmpty()) {
                guiGraphics.renderComponentTooltip(font, jewelTooltip, mouseX, mouseY);
            }
        }
        RefinementScreenStyle.renderTooltips(guiGraphics, font, leftPos, topPos, mouseX, mouseY,
                menu.selectedTab() == RefinementScreenStyle.REFINEMENT_TAB_INDEX, menu.machineTraits(), menu);
        GearSlotTooltips.render(this, guiGraphics, font, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        tabs.render(guiGraphics, mouseX, mouseY, menu.selectedTab());
        renderSlotFrames(guiGraphics);
        if (menu.selectedTab() == IdentifierMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == IdentifierMenu.TAB_STATS) {
            MachineScreenStyle.renderStatPanel(guiGraphics, leftPos, topPos, STAT_PANEL_X, STAT_PANEL_Y, STAT_PANEL_WIDTH,
                    statLines().length, STAT_ACCENT);
        } else if (menu.selectedTab() == IdentifierMenu.TAB_REFINEMENT) {
            RefinementScreenStyle.render(guiGraphics, leftPos, topPos, menu.machineTraits(), STAT_ACCENT);
        } else if (menu.selectedTab() == IdentifierMenu.TAB_MASTERY) {
            masterySupport.render(guiGraphics, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        } else if (menu.selectedTab() == IdentifierMenu.TAB_ORDERS) {
            workOrdersTab.render(guiGraphics, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        } else if (menu.selectedTab() == IdentifierMenu.TAB_JEWELS) {
            JewelsTab.render(guiGraphics, font, menu, leftPos, topPos, TEXT, TEXT_MUTED, (x, y) -> renderSlotFrame(guiGraphics, x, y));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        tabs.renderSelectedLabel(guiGraphics, menu.selectedTab(), titleLabelX, titleLabelY, TEXT_MUTED);
        if (menu.selectedTab() != IdentifierMenu.TAB_STATS && menu.selectedTab() != IdentifierMenu.TAB_MASTERY) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }
        if (menu.selectedTab() == IdentifierMenu.TAB_PROCESSING) {
            drawCentered(guiGraphics, Component.translatable("rngtech.processing.input.tiny"), INPUT_SLOT_X + 9, 39);
            drawCentered(guiGraphics, Component.translatable("rngtech.processing.output.tiny"), OUTPUT_SLOT_X + 9, 39);
        } else if (menu.selectedTab() == IdentifierMenu.TAB_GEAR) {
            drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.battery_cell.short"), 64);
            drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.fluid_pump.short"), 100);
            drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.servo.short"), 136);
        } else if (menu.selectedTab() == IdentifierMenu.TAB_STATS) {
            MachineScreenStyle.drawStatPanelLabels(guiGraphics, font, Component.translatable("rngtech.tab.stats"), statLines(),
                    STAT_PANEL_X, STAT_PANEL_Y, STAT_PANEL_WIDTH, STAT_ACCENT);
        } else if (menu.selectedTab() == IdentifierMenu.TAB_REFINEMENT) {
            RefinementScreenStyle.drawLabels(guiGraphics, font, menu.machineTraits(), STAT_ACCENT);
        } else if (menu.selectedTab() == IdentifierMenu.TAB_MASTERY) {
            masterySupport.drawLabels(guiGraphics, font, imageWidth);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int tab = tabs.tabAt(mouseX, mouseY);
            if (tab >= 0) {
                selectTab(tab);
                return true;
            }
            if (menu.selectedTab() == IdentifierMenu.TAB_ORDERS
                    && workOrdersTab.mouseClicked(mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight)) {
                return true;
            }
            if (menu.selectedTab() == IdentifierMenu.TAB_REFINEMENT && RefinementScreenStyle.handleApplyClick(this, menu, mouseX, mouseY)) {
                return true;
            }
            if (menu.selectedTab() == IdentifierMenu.TAB_PROCESSING && FluidPurgeButton.handleClick(menu, leftPos, topPos, mouseX, mouseY,
                    FLUID_BAR_X + 3, BAR_Y + 2, IdentifierBlockEntity.PURGE_FLUID_TANK)) {
                return true;
            }
        }
        if (menu.selectedTab() == IdentifierMenu.TAB_MASTERY
                && masterySupport.mouseClicked(mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (menu.selectedTab() == IdentifierMenu.TAB_MASTERY && masterySupport.mouseDragged(mouseX, mouseY, button, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (masterySupport.mouseReleased(mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (menu.selectedTab() == IdentifierMenu.TAB_MASTERY
                && masterySupport.mouseScrolled(mouseX, mouseY, scrollX, scrollY, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (menu.selectedTab() == IdentifierMenu.TAB_MASTERY && masterySupport.keyPressed(key, scan, modifiers, imageWidth, imageHeight)) {
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (menu.selectedTab() == IdentifierMenu.TAB_MASTERY && masterySupport.charTyped(character)) {
            return true;
        }
        return super.charTyped(character, modifiers);
    }

    private void selectTab(int tab) {
        masterySupport.resetDragging();
        menu.selectTab(tab);
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, WorkOrderMenuSupport.BUTTON_SELECT_TAB + menu.selectedTab());
        }
        updateImageSizeForSelectedTab();
        if (menu.selectedTab() == IdentifierMenu.TAB_MASTERY) {
            masterySupport.clampAfterGeometryChange(imageWidth, imageHeight);
        }
    }

    private void updateImageSizeForSelectedTab() {
        if (menu.selectedTab() == IdentifierMenu.TAB_MASTERY && masterySupport != null && masterySupport.expanded()) {
            imageWidth = masterySupport.imageWidth(BASE_IMAGE_WIDTH, width);
            imageHeight = masterySupport.imageHeight(BASE_IMAGE_HEIGHT, height);
        } else {
            imageWidth = BASE_IMAGE_WIDTH;
            imageHeight = BASE_IMAGE_HEIGHT;
        }
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
        if (masterySupport != null) {
            masterySupport.clampAfterGeometryChange(imageWidth, imageHeight);
        }
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

    private void renderSlotFrames(GuiGraphics guiGraphics) {
        int tab = menu.selectedTab();
        if (tab == IdentifierMenu.TAB_PROCESSING) {
            renderSlotFrame(guiGraphics, INPUT_SLOT_X, SLOT_Y);
            renderSlotFrame(guiGraphics, OUTPUT_SLOT_X, SLOT_Y);
            renderSlotFrame(guiGraphics, CONTAINER_SLOT_X, CONTAINER_SLOT_Y);
        } else if (tab == IdentifierMenu.TAB_GEAR) {
            renderSlotFrame(guiGraphics, 55, 47);
            renderSlotFrame(guiGraphics, 91, 47);
            renderSlotFrame(guiGraphics, 127, 47);
        } else if (tab == IdentifierMenu.TAB_REFINEMENT) {
            renderSlotFrame(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            renderSlotFrame(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
        }
        if (tab == IdentifierMenu.TAB_STATS || tab == IdentifierMenu.TAB_MASTERY) {
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
        guiGraphics.fill(x + FLUID_BAR_X, y + BAR_Y, x + FLUID_BAR_X + FLUID_BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, 0xFF5F5F5F);
        int fluidHeight = Math.round((BAR_HEIGHT - 2) * menu.fluidProgress());
        FluidBarRenderer.fill(
                guiGraphics,
                ModFluids.IDENTIFYING_FLUID_SOURCE.get(),
                x + FLUID_BAR_X + 1,
                y + BAR_Y + BAR_HEIGHT - 1 - fluidHeight,
                x + FLUID_BAR_X + FLUID_BAR_WIDTH - 1,
                y + BAR_Y + BAR_HEIGHT - 1,
                FLUID
        );
        FluidPurgeButton.render(guiGraphics, leftPos, topPos, FLUID_BAR_X + 3, BAR_Y + 2, menu.fluidAmount() > 0);

        guiGraphics.fill(x + ENERGY_BAR_X, y + BAR_Y, x + ENERGY_BAR_X + ENERGY_BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, 0xFF5F5F5F);
        int energyHeight = Math.round((BAR_HEIGHT - 4) * menu.energyProgress());
        int energyBottom = y + BAR_Y + BAR_HEIGHT - 2;
        guiGraphics.fill(x + ENERGY_BAR_X + 2, energyBottom - energyHeight, x + ENERGY_BAR_X + ENERGY_BAR_WIDTH - 2, energyBottom, ENERGY);

        guiGraphics.fill(x + PROGRESS_BAR_X, y + PROGRESS_BAR_Y, x + PROGRESS_BAR_X + PROGRESS_BAR_WIDTH, y + PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT, 0xFF5F5F5F);
        guiGraphics.fill(
                x + PROGRESS_BAR_X + 1,
                y + PROGRESS_BAR_Y + 1,
                x + PROGRESS_BAR_X + 1 + Math.round((PROGRESS_BAR_WIDTH - 2) * menu.processingProgress()),
                y + PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT - 1,
                menu.instantFlash() ? MachineScreenStyle.INSTANT_FLASH : PROGRESS
        );

        renderIconBox(guiGraphics, STATUS_ICON_X, ICON_Y);
        guiGraphics.fill(x + STATUS_ICON_X + 3, y + ICON_Y + 3, x + STATUS_ICON_X + 9, y + ICON_Y + 9, statusColor());
        renderIconBox(guiGraphics, ENERGY_ICON_X, ICON_Y);
        int boltX = x + ENERGY_ICON_X;
        int boltY = y + ICON_Y;
        guiGraphics.fill(boltX + 6, boltY + 2, boltX + 9, boltY + 3, ENERGY);
        guiGraphics.fill(boltX + 5, boltY + 3, boltX + 8, boltY + 6, ENERGY);
        guiGraphics.fill(boltX + 4, boltY + 6, boltX + 7, boltY + 7, ENERGY);
        guiGraphics.fill(boltX + 3, boltY + 7, boltX + 6, boltY + 10, ENERGY);
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != IdentifierMenu.TAB_PROCESSING) {
            return;
        }
        if (FluidPurgeButton.renderTooltip(guiGraphics, font, leftPos, topPos, mouseX, mouseY, FLUID_BAR_X + 3, BAR_Y + 2,
                Component.translatable("rngtech.purge.target.identifying_fluid_tank"),
                menu.fluidAmount() > 0 ? Component.translatable("fluid.rngtech.identifying_fluid") : Component.translatable("rngtech.purge.empty_fluid"),
                menu.fluidAmount(), menu.fluidCapacity())) {
            return;
        }
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY,
                FLUID_BAR_X, BAR_Y, FLUID_BAR_WIDTH, BAR_HEIGHT,
                Component.translatable("rngtech.identifier.tooltip.fluid", menu.fluidAmount(), menu.fluidCapacity(), menu.fluidPerItem()));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY,
                ENERGY_BAR_X, BAR_Y, ENERGY_BAR_WIDTH, BAR_HEIGHT,
                CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity()));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY,
                PROGRESS_BAR_X, PROGRESS_BAR_Y, PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT,
                menu.processingTicks() <= 0
                        ? Component.translatable("rngtech.recycler.tooltip.progress.empty")
                        : Component.translatable("rngtech.recycler.tooltip.progress", menu.progress(), menu.processingTicks()));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY,
                STATUS_ICON_X, ICON_Y, ICON_SIZE, ICON_SIZE,
                Component.translatable("rngtech.recycler.tooltip.status", statusText()));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY,
                ENERGY_ICON_X, ICON_Y, ICON_SIZE, ICON_SIZE,
                Component.translatable("rngtech.identifier.tooltip.energy",
                        CompactValueText.energyRate(menu.energyPerTick()), CompactValueText.energyAmount(menu.energyPerItem())));
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() == IdentifierMenu.TAB_STATS) {
            MachineScreenStyle.renderStatPanelTooltip(guiGraphics, font, leftPos, topPos, mouseX, mouseY,
                    STAT_PANEL_X, STAT_PANEL_Y, STAT_PANEL_WIDTH, statLines());
        }
    }

    private Component statusText() {
        String key = IdentifierBlockEntity.statusKey(menu.statusCode());
        return Component.translatable(key.isEmpty() ? "rngtech.identifier.status.ready" : key);
    }

    private int statusColor() {
        return switch (menu.statusCode()) {
            case IdentifierBlockEntity.STATUS_READY -> STAT_ACCENT;
            case IdentifierBlockEntity.STATUS_NO_INPUT, IdentifierBlockEntity.STATUS_MISSING_GEAR -> PANEL_DARK;
            case IdentifierBlockEntity.STATUS_NO_POWER, IdentifierBlockEntity.STATUS_NO_FLUID -> 0xFFD3A33A;
            default -> 0xFFB45B4A;
        };
    }

    private MachineScreenStyle.StatLine[] statLines() {
        MachineScreenStyle.StatLine[] lines = new MachineScreenStyle.StatLine[STAT_LABEL_KEYS.length];
        for (int index = 0; index < lines.length; index++) {
            double value = menu.statValue(STAT_DATA_INDICES[index]);
            boolean integral = STAT_DATA_INDICES[index] == IdentifierBlockEntity.DATA_ENERGY_CAPACITY_STAT
                    || STAT_DATA_INDICES[index] == IdentifierBlockEntity.DATA_FLUID_TRANSFER;
            boolean lowerIsBetter = STAT_TYPES[index] == MachineStat.ENERGY_USAGE || STAT_TYPES[index] == MachineStat.FLUID_USAGE;
            boolean enhanced = !integral && (lowerIsBetter ? value < 0.999 : value > 1.001);
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

    private void drawGearLabel(GuiGraphics guiGraphics, Component label, int centerX) {
        MachineScreenStyle.drawClippedCentered(guiGraphics, font, label, centerX, 35, 34, TEXT_MUTED);
    }
}
