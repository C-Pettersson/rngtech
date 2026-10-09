package com.rngtech.client.screen;

import com.rngtech.content.cable.CableConnectorMode;
import com.rngtech.content.cable.EnergyDistributionMode;
import com.rngtech.content.cable.FluidConnectorMode;
import com.rngtech.content.cable.ItemConnectorMode;
import com.rngtech.content.cable.NetworkBridgeType;
import com.rngtech.content.menu.UniversalConnectorMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class UniversalConnectorScreen extends AbstractContainerScreen<UniversalConnectorMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int BUTTON = 0xFF4F6F73;
    private static final int BUTTON_DARK = 0xFF283D40;
    private static final int BUTTON_LIGHT = 0xFF88AEB2;
    private static final int SELECTED = 0xFF6C7658;
    private static final int SELECTED_LIGHT = 0xFFA5B17E;
    private static final int DISABLED = 0xFF777777;
    private static final int TAB_X = 8;
    private static final int TAB_Y = 20;
    private static final int TAB_WIDTH = 56;
    private static final int TAB_HEIGHT = 18;
    private static final int ENERGY_MODE_X = 74;
    private static final int ENERGY_MODE_WIDTH = 44;
    private static final int ENERGY_CHANNEL_DOWN_X = 126;
    private static final int ENERGY_CHANNEL_UP_X = 168;
    private static final int ENERGY_CHANNEL_VALUE_X = 147;
    private static final int ENERGY_DISTRIBUTION_X = 196;
    private static final int ENERGY_DISTRIBUTION_WIDTH = 52;
    private static final int ENERGY_ATTACH_X = 252;
    private static final int ENERGY_ATTACH_WIDTH = 48;
    private static final int ENERGY_ROW_Y = 59;
    private static final int SMALL_BUTTON = 18;
    private static final int ITEM_SLOT_X = 29;
    private static final int ITEM_SLOT_Y = 59;
    private static final int ITEM_ROW_GAP = 32;
    private static final int ITEM_MODE_X = 74;
    private static final int ITEM_MODE_WIDTH = 34;
    private static final int ITEM_CHANNEL_DOWN_X = 122;
    private static final int ITEM_CHANNEL_UP_X = 165;
    private static final int ITEM_CHANNEL_VALUE_X = 143;
    private static final int ITEM_ATTACH_X = 192;
    private static final int ITEM_ATTACH_WIDTH = 48;
    private static final int ITEM_FILTER_X = 252;
    private static final int FILTER_SLOT_COUNT = 2;
    private static final int FILTER_SLOT_GAP = 18;
    private static final int BRIDGE_CHANNEL_DOWN_X = 126;
    private static final int BRIDGE_CHANNEL_UP_X = 168;
    private static final int BRIDGE_CHANNEL_VALUE_X = 147;
    private static final int BRIDGE_ROW_Y = 59;
    private static final int BRIDGE_STATUS_X = 196;
    private static final int BRIDGE_STATUS_Y = 52;
    private static final int BRIDGE_STATUS_WIDTH = 106;
    private static final int BRIDGE_STATUS_HEIGHT = 66;
    private static final int DEBUG_CARD_X = 16;
    private static final int DEBUG_CARD_Y = 51;
    private static final int DEBUG_CARD_WIDTH = 58;
    private static final int DEBUG_CARD_HEIGHT = 28;
    private static final int DEBUG_CARD_GAP = 8;
    private static final int DEBUG_ROW_X = 16;
    private static final int DEBUG_ROW_Y = 87;
    private static final int DEBUG_ROW_WIDTH = 286;
    private static final int DEBUG_ROW_HEIGHT = 17;
    private static final int DEBUG_ROW_GAP = 5;
    private static final int DEBUG_CACHE_X = 16;
    private static final int DEBUG_CACHE_Y = 176;
    private static final int DEBUG_CACHE_WIDTH = 138;
    private static final int DEBUG_CACHE_HEIGHT = 18;
    private static final int DEBUG_BUTTON_X = 164;
    private static final int DEBUG_BUTTON_Y = 176;
    private static final int DEBUG_BUTTON_WIDTH = 138;
    private static final int DEBUG_BUTTON_HEIGHT = 18;
    private static final int NETWORK_ENERGY_ROW_X = 16;
    private static final int NETWORK_ENERGY_ROW_Y = 199;
    private static final int NETWORK_ENERGY_ROW_WIDTH = 286;
    private static final int NETWORK_ENERGY_ROW_HEIGHT = 28;
    private static final int NETWORK_ENERGY_WINDOW_BUTTON_WIDTH = 34;
    private static final int NETWORK_ENERGY_WINDOW_BUTTON_HEIGHT = 18;
    private static final int NETWORK_ENERGY_WINDOW_BUTTON_X =
            NETWORK_ENERGY_ROW_X + NETWORK_ENERGY_ROW_WIDTH - NETWORK_ENERGY_WINDOW_BUTTON_WIDTH - 5;
    private static final int NETWORK_ENERGY_WINDOW_BUTTON_Y = NETWORK_ENERGY_ROW_Y + 5;
    private static final int COLOR_CABLE = 0xFF8A6A3F;
    private static final int COLOR_UNIVERSAL = 0xFF4F6F73;
    private static final int COLOR_ENERGY = 0xFFC49A36;
    private static final int COLOR_FLUID = 0xFF4F7EA1;
    private static final int COLOR_ITEM = 0xFF6C7658;
    private static final int COLOR_CHANNEL = 0xFF8A5E85;
    private static final int COLOR_BRIDGE = 0xFF7B5EA7;
    private static final int COLOR_NEGATIVE = 0xFFA14D4D;

    private static final String[] NETWORK_ENERGY_WINDOW_LABELS = {"1m", "5m", "15m"};

    private static final String[] TAB_KEYS = {
            "rngtech.universal_connector.tab.energy",
            "rngtech.universal_connector.tab.fluid",
            "rngtech.universal_connector.tab.item",
            "rngtech.universal_connector.tab.bridge",
            "rngtech.universal_connector.tab.network"
    };

    private int networkEnergyWindowIndex;

    public UniversalConnectorScreen(UniversalConnectorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 318;
        imageHeight = 236;
        inventoryLabelX = 35;
        inventoryLabelY = 144;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderControlTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        renderTabs(guiGraphics);
        if (isEnergyTab()) {
            renderSlotFrame(guiGraphics, ITEM_SLOT_X, ITEM_SLOT_Y, false);
            renderEnergyControls(guiGraphics);
        } else if (isModuleTab()) {
            renderModuleControls(guiGraphics);
        } else if (isBridgeTab()) {
            renderBridgeControls(guiGraphics);
        } else {
            renderNetworkControls(guiGraphics);
        }
        if (!isNetworkTab()) {
            renderPlayerInventoryFrames(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        for (int tab = UniversalConnectorMenu.TAB_ENERGY; tab <= UniversalConnectorMenu.TAB_NETWORK; tab++) {
            int x = TAB_X + tab * (TAB_WIDTH + 2);
            int color = menu.selectedTab() == tab ? TEXT : 0xFF505050;
            drawCentered(guiGraphics, Component.translatable(TAB_KEYS[tab]), x, TAB_Y + 5, TAB_WIDTH, color);
        }
        if (isEnergyTab()) {
            drawEnergyLabels(guiGraphics);
        } else if (isModuleTab()) {
            drawModuleLabels(guiGraphics);
        } else if (isBridgeTab()) {
            drawBridgeLabels(guiGraphics);
        } else {
            drawNetworkLabels(guiGraphics);
        }
        if (!isNetworkTab()) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int tab = UniversalConnectorMenu.TAB_ENERGY; tab <= UniversalConnectorMenu.TAB_NETWORK; tab++) {
                if (isOverTab(mouseX, mouseY, tab)) {
                    sendButton(UniversalConnectorMenu.BUTTON_TAB_BASE + tab);
                    return true;
                }
            }

            if (isEnergyTab()) {
                if (inBounds(mouseX, mouseY, ENERGY_CHANNEL_DOWN_X, ENERGY_ROW_Y, SMALL_BUTTON, SMALL_BUTTON)) {
                    sendButton(UniversalConnectorMenu.BUTTON_CHANNEL_DOWN);
                    return true;
                }
                if (inBounds(mouseX, mouseY, ENERGY_CHANNEL_UP_X, ENERGY_ROW_Y, SMALL_BUTTON, SMALL_BUTTON)) {
                    sendButton(UniversalConnectorMenu.BUTTON_CHANNEL_UP);
                    return true;
                }
                if (inBounds(mouseX, mouseY, ENERGY_MODE_X, ENERGY_ROW_Y, ENERGY_MODE_WIDTH, SMALL_BUTTON)) {
                    sendButton(UniversalConnectorMenu.BUTTON_MODE);
                    return true;
                }
                if (inBounds(mouseX, mouseY, ENERGY_DISTRIBUTION_X, ENERGY_ROW_Y, ENERGY_DISTRIBUTION_WIDTH, SMALL_BUTTON)) {
                    sendButton(UniversalConnectorMenu.BUTTON_DISTRIBUTION);
                    return true;
                }
                if (inBounds(mouseX, mouseY, ENERGY_ATTACH_X, ENERGY_ROW_Y, ENERGY_ATTACH_WIDTH, SMALL_BUTTON)) {
                    sendButton(UniversalConnectorMenu.BUTTON_ATTACH_AS_BASE + nextDirection(menu.attachAs()).ordinal());
                    return true;
                }
            } else if (isModuleTab()) {
                for (int moduleIndex = 0; moduleIndex < moduleCount(); moduleIndex++) {
                    int rowY = ITEM_SLOT_Y + moduleIndex * ITEM_ROW_GAP;
                    if (inBounds(mouseX, mouseY, ITEM_CHANNEL_DOWN_X, rowY, SMALL_BUTTON, SMALL_BUTTON)) {
                        sendButton(moduleChannelDownButtonBase() + moduleIndex);
                        return true;
                    }
                    if (inBounds(mouseX, mouseY, ITEM_CHANNEL_UP_X, rowY, SMALL_BUTTON, SMALL_BUTTON)) {
                        sendButton(moduleChannelUpButtonBase() + moduleIndex);
                        return true;
                    }
                    if (inBounds(mouseX, mouseY, ITEM_MODE_X, rowY, ITEM_MODE_WIDTH, SMALL_BUTTON)) {
                        sendButton(moduleModeButtonBase() + moduleIndex);
                        return true;
                    }
                    if (inBounds(mouseX, mouseY, ITEM_ATTACH_X, rowY, ITEM_ATTACH_WIDTH, SMALL_BUTTON)) {
                        sendButton(moduleAttachButtonBase() + moduleIndex);
                        return true;
                    }
                }
            } else if (isBridgeTab()) {
                if (inBounds(mouseX, mouseY, BRIDGE_CHANNEL_DOWN_X, BRIDGE_ROW_Y, SMALL_BUTTON, SMALL_BUTTON)) {
                    sendButton(UniversalConnectorMenu.BUTTON_BRIDGE_CHANNEL_DOWN);
                    return true;
                }
                if (inBounds(mouseX, mouseY, BRIDGE_CHANNEL_UP_X, BRIDGE_ROW_Y, SMALL_BUTTON, SMALL_BUTTON)) {
                    sendButton(UniversalConnectorMenu.BUTTON_BRIDGE_CHANNEL_UP);
                    return true;
                }
            } else {
                if (inBounds(mouseX, mouseY, DEBUG_BUTTON_X, DEBUG_BUTTON_Y, DEBUG_BUTTON_WIDTH, DEBUG_BUTTON_HEIGHT)) {
                    sendButton(UniversalConnectorMenu.BUTTON_CLEAR_NETWORK_CACHE);
                    return true;
                }
                if (inBounds(
                        mouseX,
                        mouseY,
                        NETWORK_ENERGY_WINDOW_BUTTON_X,
                        NETWORK_ENERGY_WINDOW_BUTTON_Y,
                        NETWORK_ENERGY_WINDOW_BUTTON_WIDTH,
                        NETWORK_ENERGY_WINDOW_BUTTON_HEIGHT
                )) {
                    cycleNetworkEnergyWindow();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
        for (int tab = UniversalConnectorMenu.TAB_ENERGY; tab <= UniversalConnectorMenu.TAB_NETWORK; tab++) {
            int x = leftPos + TAB_X + tab * (TAB_WIDTH + 2);
            int y = topPos + TAB_Y;
            boolean selected = menu.selectedTab() == tab;
            guiGraphics.fill(x, y, x + TAB_WIDTH, y + TAB_HEIGHT, selected ? PANEL_LIGHT : PANEL_DARK);
            guiGraphics.fill(x + 1, y + 1, x + TAB_WIDTH - 1, y + TAB_HEIGHT - 1, selected ? PANEL : 0xFF9A9A9A);
        }
    }

    private void renderEnergyControls(GuiGraphics guiGraphics) {
        renderButton(guiGraphics, ENERGY_MODE_X, ENERGY_ROW_Y, ENERGY_MODE_WIDTH, SMALL_BUTTON, false);
        renderButton(guiGraphics, ENERGY_CHANNEL_DOWN_X, ENERGY_ROW_Y, SMALL_BUTTON, SMALL_BUTTON, false);
        renderButton(guiGraphics, ENERGY_CHANNEL_UP_X, ENERGY_ROW_Y, SMALL_BUTTON, SMALL_BUTTON, false);
        renderButton(guiGraphics, ENERGY_DISTRIBUTION_X, ENERGY_ROW_Y, ENERGY_DISTRIBUTION_WIDTH, SMALL_BUTTON, false);
        renderButton(guiGraphics, ENERGY_ATTACH_X, ENERGY_ROW_Y, ENERGY_ATTACH_WIDTH, SMALL_BUTTON, false);
    }

    private void renderModuleControls(GuiGraphics guiGraphics) {
        for (int moduleIndex = 0; moduleIndex < moduleCount(); moduleIndex++) {
            int rowY = ITEM_SLOT_Y + moduleIndex * ITEM_ROW_GAP;
            renderSlotFrame(guiGraphics, ITEM_SLOT_X, rowY, false);
            renderButton(guiGraphics, ITEM_MODE_X, rowY, ITEM_MODE_WIDTH, SMALL_BUTTON, false);
            renderButton(guiGraphics, ITEM_CHANNEL_DOWN_X, rowY, SMALL_BUTTON, SMALL_BUTTON, false);
            renderButton(guiGraphics, ITEM_CHANNEL_UP_X, rowY, SMALL_BUTTON, SMALL_BUTTON, false);
            renderButton(guiGraphics, ITEM_ATTACH_X, rowY, ITEM_ATTACH_WIDTH, SMALL_BUTTON, false);
            for (int filterIndex = 0; filterIndex < FILTER_SLOT_COUNT; filterIndex++) {
                renderSlotFrame(guiGraphics, ITEM_FILTER_X + filterIndex * FILTER_SLOT_GAP, rowY, false);
            }
        }
    }

    private void renderBridgeControls(GuiGraphics guiGraphics) {
        renderSlotFrame(guiGraphics, ITEM_SLOT_X, ITEM_SLOT_Y, false);
        renderButton(guiGraphics, BRIDGE_CHANNEL_DOWN_X, BRIDGE_ROW_Y, SMALL_BUTTON, SMALL_BUTTON, false);
        renderButton(guiGraphics, BRIDGE_CHANNEL_UP_X, BRIDGE_ROW_Y, SMALL_BUTTON, SMALL_BUTTON, false);
        int left = leftPos + BRIDGE_STATUS_X;
        int top = topPos + BRIDGE_STATUS_Y;
        guiGraphics.fill(left, top, left + BRIDGE_STATUS_WIDTH, top + BRIDGE_STATUS_HEIGHT, 0xFF5B5B5B);
        guiGraphics.fill(left + 1, top + 1, left + BRIDGE_STATUS_WIDTH - 1, top + BRIDGE_STATUS_HEIGHT - 1, 0xFFB8B8B8);
        guiGraphics.fill(left + 3, top + 3, left + 11, top + BRIDGE_STATUS_HEIGHT - 3, bridgeStatusAccent());
    }

    private void renderNetworkControls(GuiGraphics guiGraphics) {
        renderMetricCard(guiGraphics, metricX(0), DEBUG_CARD_Y, COLOR_CABLE);
        renderMetricCard(guiGraphics, metricX(1), DEBUG_CARD_Y, COLOR_UNIVERSAL);
        renderMetricCard(guiGraphics, metricX(2), DEBUG_CARD_Y, COLOR_ENERGY);
        renderMetricCard(guiGraphics, metricX(3), DEBUG_CARD_Y, COLOR_CHANNEL);
        renderDebugRow(guiGraphics, 0, COLOR_ENERGY, menu.networkEnergyChannelsMask());
        renderDebugRow(guiGraphics, 1, COLOR_FLUID, menu.networkFluidChannelsMask());
        renderDebugRow(guiGraphics, 2, COLOR_ITEM, menu.networkItemChannelsMask());
        renderDebugRow(guiGraphics, 3, COLOR_BRIDGE, menu.networkBridgeChannelsMask());
        renderCachePanel(guiGraphics);
        renderButton(guiGraphics, DEBUG_BUTTON_X, DEBUG_BUTTON_Y, DEBUG_BUTTON_WIDTH, DEBUG_BUTTON_HEIGHT, false);
        renderNetworkEnergyRow(guiGraphics);
        renderButton(
                guiGraphics,
                NETWORK_ENERGY_WINDOW_BUTTON_X,
                NETWORK_ENERGY_WINDOW_BUTTON_Y,
                NETWORK_ENERGY_WINDOW_BUTTON_WIDTH,
                NETWORK_ENERGY_WINDOW_BUTTON_HEIGHT,
                false
        );
    }

    private void drawEnergyLabels(GuiGraphics guiGraphics) {
        String transfer = menu.hasConnector()
                ? Component.translatable("rngtech.cable_connector.transfer", menu.transferRate()).getString()
                : Component.translatable("rngtech.universal_connector.no_module").getString();
        String live = Component.translatable(
                "rngtech.universal_connector.energy_live",
                menu.lastEnergyInput(),
                menu.lastEnergyOutput()
        ).getString();
        drawSlotLabel(guiGraphics, Component.translatable("rngtech.universal_connector.module"));
        guiGraphics.drawString(font, Component.translatable("rngtech.cable_connector.mode"), ENERGY_MODE_X, 48, TEXT_MUTED, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.cable_connector.channel"), ENERGY_CHANNEL_DOWN_X, 48, TEXT_MUTED, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.cable_connector.distribution.short"), ENERGY_DISTRIBUTION_X, 48, TEXT_MUTED, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.universal_connector.attach"), ENERGY_ATTACH_X, 48, TEXT_MUTED, false);
        drawCentered(guiGraphics, modeName(), ENERGY_MODE_X, ENERGY_ROW_Y + 5, ENERGY_MODE_WIDTH, 0xFFFFFFFF);
        drawCentered(guiGraphics, Component.literal("-"), ENERGY_CHANNEL_DOWN_X, ENERGY_ROW_Y + 5, SMALL_BUTTON, 0xFFFFFFFF);
        drawCentered(guiGraphics, Component.literal("+"), ENERGY_CHANNEL_UP_X, ENERGY_ROW_Y + 5, SMALL_BUTTON, 0xFFFFFFFF);
        drawCentered(
                guiGraphics,
                Component.literal(Integer.toString(menu.channel())),
                ENERGY_CHANNEL_VALUE_X,
                ENERGY_ROW_Y + 5,
                18,
                TEXT
        );
        drawCentered(
                guiGraphics,
                distributionName(),
                ENERGY_DISTRIBUTION_X,
                ENERGY_ROW_Y + 5,
                ENERGY_DISTRIBUTION_WIDTH,
                0xFFFFFFFF
        );
        drawCentered(
                guiGraphics,
                directionName(menu.attachAs()),
                ENERGY_ATTACH_X,
                ENERGY_ROW_Y + 5,
                ENERGY_ATTACH_WIDTH,
                0xFFFFFFFF
        );
        guiGraphics.drawString(font, font.plainSubstrByWidth(transfer, 170), 76, 84, TEXT_MUTED, false);
        guiGraphics.drawString(font, font.plainSubstrByWidth(live, 190), 76, 96, TEXT_MUTED, false);
        if (menu.hasConnector() && !menu.targetHasEnergyAccess()) {
            String warning = Component.translatable("rngtech.cable_connector.no_energy_access").getString();
            guiGraphics.drawString(font, font.plainSubstrByWidth(warning, 226), 76, 110, COLOR_NEGATIVE, false);
        }
    }

    private void drawModuleLabels(GuiGraphics guiGraphics) {
        drawSlotLabel(guiGraphics, Component.translatable(moduleLabelKey()));
        guiGraphics.drawString(font, Component.translatable("rngtech.cable_connector.mode"), ITEM_MODE_X, 48, TEXT_MUTED, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.cable_connector.channel"), ITEM_CHANNEL_DOWN_X, 48, TEXT_MUTED, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.universal_connector.attach"), ITEM_ATTACH_X, 48, TEXT_MUTED, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.universal_connector.filter"), ITEM_FILTER_X, 48, TEXT_MUTED, false);

        for (int moduleIndex = 0; moduleIndex < moduleCount(); moduleIndex++) {
            int rowY = ITEM_SLOT_Y + moduleIndex * ITEM_ROW_GAP;
            drawCentered(guiGraphics, moduleModeName(moduleIndex), ITEM_MODE_X, rowY + 5, ITEM_MODE_WIDTH, 0xFFFFFFFF);
            drawCentered(guiGraphics, Component.literal("-"), ITEM_CHANNEL_DOWN_X, rowY + 5, SMALL_BUTTON, 0xFFFFFFFF);
            drawCentered(guiGraphics, Component.literal("+"), ITEM_CHANNEL_UP_X, rowY + 5, SMALL_BUTTON, 0xFFFFFFFF);
            drawCentered(
                    guiGraphics,
                    Component.literal(Integer.toString(moduleChannel(moduleIndex))),
                    ITEM_CHANNEL_VALUE_X,
                    rowY + 5,
                    18,
                    TEXT
            );
            drawCentered(
                    guiGraphics,
                    moduleAttachName(moduleAttachAs(moduleIndex)),
                    ITEM_ATTACH_X,
                    rowY + 5,
                    ITEM_ATTACH_WIDTH,
                    0xFFFFFFFF
            );
        }
    }

    private void drawBridgeLabels(GuiGraphics guiGraphics) {
        NetworkBridgeType bridgeType = menu.bridgeType();
        drawSlotLabel(guiGraphics, Component.translatable("rngtech.universal_connector.bridge_module"));
        guiGraphics.drawString(font, Component.translatable("rngtech.cable_connector.channel"), BRIDGE_CHANNEL_DOWN_X, 48, TEXT_MUTED, false);
        drawCentered(guiGraphics, Component.literal("-"), BRIDGE_CHANNEL_DOWN_X, BRIDGE_ROW_Y + 5, SMALL_BUTTON, 0xFFFFFFFF);
        drawCentered(guiGraphics, Component.literal("+"), BRIDGE_CHANNEL_UP_X, BRIDGE_ROW_Y + 5, SMALL_BUTTON, 0xFFFFFFFF);
        drawCentered(
                guiGraphics,
                Component.literal(Integer.toString(menu.bridgeChannel())),
                BRIDGE_CHANNEL_VALUE_X,
                BRIDGE_ROW_Y + 5,
                18,
                TEXT
        );

        Component module = bridgeType == NetworkBridgeType.NONE
                ? Component.translatable("rngtech.universal_connector.bridge.none")
                : Component.translatable(bridgeType.translationKey());
        Component loaded = bridgeType == NetworkBridgeType.NONE
                ? Component.translatable("rngtech.universal_connector.bridge.no_mod")
                : Component.translatable(menu.bridgeModLoaded()
                        ? "rngtech.universal_connector.bridge.mod_loaded"
                        : "rngtech.universal_connector.bridge.mod_missing");
        Component peers = Component.translatable(
                menu.bridgeEndpointCount() > 1
                        ? "rngtech.universal_connector.bridge.peers"
                        : "rngtech.universal_connector.bridge.no_peers",
                menu.bridgeEndpointCount()
        );
        drawClipped(guiGraphics, Component.translatable("rngtech.universal_connector.bridge.status"), BRIDGE_STATUS_X + 15,
                BRIDGE_STATUS_Y + 5, BRIDGE_STATUS_WIDTH - 20, TEXT);
        drawClipped(guiGraphics, module, BRIDGE_STATUS_X + 15, BRIDGE_STATUS_Y + 20, BRIDGE_STATUS_WIDTH - 20, TEXT);
        int loadedColor = bridgeType == NetworkBridgeType.NONE ? TEXT_MUTED : menu.bridgeModLoaded() ? TEXT_MUTED : COLOR_NEGATIVE;
        int peerColor = bridgeType == NetworkBridgeType.NONE ? TEXT_MUTED : menu.bridgeEndpointCount() > 1 ? TEXT_MUTED : COLOR_NEGATIVE;
        drawClipped(guiGraphics, loaded, BRIDGE_STATUS_X + 15, BRIDGE_STATUS_Y + 34, BRIDGE_STATUS_WIDTH - 20,
                loadedColor);
        drawClipped(guiGraphics, peers, BRIDGE_STATUS_X + 15, BRIDGE_STATUS_Y + 48, BRIDGE_STATUS_WIDTH - 20,
                peerColor);
    }

    private void drawNetworkLabels(GuiGraphics guiGraphics) {
        drawMetricText(
                guiGraphics,
                0,
                Component.translatable("rngtech.universal_connector.debug.cable_nodes"),
                Component.literal(formatCompact(menu.networkCableNodes()))
        );
        drawMetricText(
                guiGraphics,
                1,
                Component.translatable("rngtech.universal_connector.debug.universal_connectors.short"),
                Component.literal(formatCompact(menu.networkUniversalConnectors()))
        );
        drawMetricText(
                guiGraphics,
                2,
                Component.translatable("rngtech.universal_connector.debug.energy_outputs.short"),
                Component.literal(formatCompact(menu.networkEnergyEndpoints()))
        );
        drawMetricText(
                guiGraphics,
                3,
                Component.translatable("rngtech.universal_connector.debug.channels.short"),
                Component.literal(Integer.toString(Integer.bitCount(menu.networkActiveChannelsMask())))
        );
        drawDebugRowText(
                guiGraphics,
                0,
                Component.translatable("rngtech.universal_connector.debug.energy"),
                Component.literal(formatCompact(menu.networkEnergyTransferCap()) + " FE/t"),
                menu.networkEnergyModules(),
                menu.networkEnergyChannelsMask()
        );
        drawDebugRowText(
                guiGraphics,
                1,
                Component.translatable("rngtech.universal_connector.debug.fluid"),
                Component.literal(formatCompact(menu.networkFluidShipmentCap()) + " mB"),
                menu.networkFluidModules(),
                menu.networkFluidChannelsMask()
        );
        drawDebugRowText(
                guiGraphics,
                2,
                Component.translatable("rngtech.universal_connector.debug.item"),
                Component.literal(formatCompact(menu.networkItemShipmentCap()) + " item"),
                menu.networkItemModules(),
                menu.networkItemChannelsMask()
        );
        drawDebugRowText(
                guiGraphics,
                3,
                Component.translatable("rngtech.universal_connector.debug.bridge"),
                Component.literal("AE2 " + menu.networkAe2BridgeEndpoints() + " RS " + menu.networkRefinedStorageBridgeEndpoints()),
                menu.networkBridgeModules(),
                menu.networkBridgeChannelsMask()
        );
        drawCacheText(guiGraphics);
        drawNetworkEnergyText(guiGraphics);
        drawCenteredClipped(
                guiGraphics,
                Component.translatable("rngtech.universal_connector.debug.clear_network_cache"),
                DEBUG_BUTTON_X,
                DEBUG_BUTTON_Y + 5,
                DEBUG_BUTTON_WIDTH,
                0xFFFFFFFF
        );
        drawCenteredClipped(
                guiGraphics,
                Component.literal(networkEnergyWindowLabel()),
                NETWORK_ENERGY_WINDOW_BUTTON_X,
                NETWORK_ENERGY_WINDOW_BUTTON_Y + 5,
                NETWORK_ENERGY_WINDOW_BUTTON_WIDTH,
                0xFFFFFFFF
        );
    }

    private void renderButton(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean selected) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + width, top + height, BUTTON_DARK);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, selected ? SELECTED : BUTTON);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + 2, selected ? SELECTED_LIGHT : BUTTON_LIGHT);
    }

    private void renderMetricCard(GuiGraphics guiGraphics, int x, int y, int accent) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + DEBUG_CARD_WIDTH, top + DEBUG_CARD_HEIGHT, 0xFF5B5B5B);
        guiGraphics.fill(left + 1, top + 1, left + DEBUG_CARD_WIDTH - 1, top + DEBUG_CARD_HEIGHT - 1, 0xFFB5B5B5);
        guiGraphics.fill(left + 2, top + 2, left + DEBUG_CARD_WIDTH - 2, top + 3, PANEL_LIGHT);
        guiGraphics.fill(left + 3, top + DEBUG_CARD_HEIGHT - 6, left + DEBUG_CARD_WIDTH - 3, top + DEBUG_CARD_HEIGHT - 3, accent);
    }

    private void renderDebugRow(GuiGraphics guiGraphics, int row, int accent, int channelMask) {
        int x = DEBUG_ROW_X;
        int y = debugRowY(row);
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + DEBUG_ROW_WIDTH, top + DEBUG_ROW_HEIGHT, 0xFF5B5B5B);
        guiGraphics.fill(left + 1, top + 1, left + DEBUG_ROW_WIDTH - 1, top + DEBUG_ROW_HEIGHT - 1, 0xFFB8B8B8);
        guiGraphics.fill(left + 3, top + 3, left + 11, top + DEBUG_ROW_HEIGHT - 3, accent);

        int barWidth = Math.min(DEBUG_ROW_WIDTH - 22, Integer.bitCount(channelMask) * (DEBUG_ROW_WIDTH - 22) / 16);
        if (barWidth > 0) {
            guiGraphics.fill(left + 14, top + DEBUG_ROW_HEIGHT - 4, left + 14 + barWidth, top + DEBUG_ROW_HEIGHT - 2, accent);
        }
    }

    private void renderCachePanel(GuiGraphics guiGraphics) {
        int left = leftPos + DEBUG_CACHE_X;
        int top = topPos + DEBUG_CACHE_Y;
        guiGraphics.fill(left, top, left + DEBUG_CACHE_WIDTH, top + DEBUG_CACHE_HEIGHT, 0xFF5B5B5B);
        guiGraphics.fill(left + 1, top + 1, left + DEBUG_CACHE_WIDTH - 1, top + DEBUG_CACHE_HEIGHT - 1, 0xFFB8B8B8);
        guiGraphics.fill(left + 3, top + DEBUG_CACHE_HEIGHT - 5, left + DEBUG_CACHE_WIDTH - 3, top + DEBUG_CACHE_HEIGHT - 3,
                menu.networkCacheAgeTicks() == 0 ? COLOR_ITEM : COLOR_ENERGY);
    }

    private void renderNetworkEnergyRow(GuiGraphics guiGraphics) {
        int left = leftPos + NETWORK_ENERGY_ROW_X;
        int top = topPos + NETWORK_ENERGY_ROW_Y;
        guiGraphics.fill(left, top, left + NETWORK_ENERGY_ROW_WIDTH, top + NETWORK_ENERGY_ROW_HEIGHT, 0xFF5B5B5B);
        guiGraphics.fill(left + 1, top + 1, left + NETWORK_ENERGY_ROW_WIDTH - 1, top + NETWORK_ENERGY_ROW_HEIGHT - 1, 0xFFB8B8B8);
        guiGraphics.fill(left + 3, top + 3, left + 11, top + NETWORK_ENERGY_ROW_HEIGHT - 3, COLOR_ENERGY);
    }

    private void renderSlotFrame(GuiGraphics guiGraphics, int x, int y, boolean locked) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + 18, top + 18, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + 17, top + 17, locked ? 0xFFBEBEBE : 0xFFE0E0E0);
        guiGraphics.fill(left + 2, top + 2, left + 16, top + 16, locked ? DISABLED : 0xFF8B8B8B);
    }

    private void renderPlayerInventoryFrames(GuiGraphics guiGraphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlotFrame(guiGraphics, 34 + column * 18, 155 + row * 18, false);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderSlotFrame(guiGraphics, 34 + column * 18, 213, false);
        }
    }

    private void sendButton(int id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.gameMode != null && menu.clickMenuButton(minecraft.player, id)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private boolean inBounds(double mouseX, double mouseY, int x, int y, int width, int height) {
        int left = leftPos + x;
        int top = topPos + y;
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private boolean isOverTab(double mouseX, double mouseY, int tab) {
        return inBounds(mouseX, mouseY, TAB_X + tab * (TAB_WIDTH + 2), TAB_Y, TAB_WIDTH, TAB_HEIGHT);
    }

    private boolean isEnergyTab() {
        return menu.selectedTab() == UniversalConnectorMenu.TAB_ENERGY;
    }

    private boolean isFluidTab() {
        return menu.selectedTab() == UniversalConnectorMenu.TAB_FLUID;
    }

    private boolean isItemTab() {
        return menu.selectedTab() == UniversalConnectorMenu.TAB_ITEM;
    }

    private boolean isBridgeTab() {
        return menu.selectedTab() == UniversalConnectorMenu.TAB_BRIDGE;
    }

    private boolean isModuleTab() {
        return isFluidTab() || isItemTab();
    }

    private String moduleLabelKey() {
        return switch (menu.selectedTab()) {
            case UniversalConnectorMenu.TAB_FLUID -> "rngtech.universal_connector.fluid_module";
            case UniversalConnectorMenu.TAB_ITEM -> "rngtech.universal_connector.item_module";
            default -> "rngtech.universal_connector.energy_module";
        };
    }

    private void drawCentered(GuiGraphics guiGraphics, Component text, int x, int y, int width, int color) {
        guiGraphics.drawString(font, text, x + (width - font.width(text)) / 2, y, color, false);
    }

    private void drawSlotLabel(GuiGraphics guiGraphics, Component text) {
        guiGraphics.drawString(font, text, ITEM_SLOT_X + 9 - font.width(text) / 2, 48, TEXT_MUTED, false);
    }

    private void drawCenteredClipped(GuiGraphics guiGraphics, Component text, int x, int y, int width, int color) {
        String clipped = font.plainSubstrByWidth(text.getString(), width - 4);
        guiGraphics.drawString(font, clipped, x + (width - font.width(clipped)) / 2, y, color, false);
    }

    private void drawMetricText(GuiGraphics guiGraphics, int index, Component label, Component value) {
        int x = metricX(index);
        drawClipped(guiGraphics, label, x + 4, DEBUG_CARD_Y + 5, DEBUG_CARD_WIDTH - 8, TEXT_MUTED);
        drawCenteredClipped(guiGraphics, value, x + 3, DEBUG_CARD_Y + 16, DEBUG_CARD_WIDTH - 6, TEXT);
    }

    private void drawDebugRowText(
            GuiGraphics guiGraphics,
            int row,
            Component label,
            Component cap,
            int modules,
            int channelMask
    ) {
        int y = debugRowY(row);
        drawClipped(guiGraphics, label, DEBUG_ROW_X + 14, y + 5, 52, TEXT);
        drawClipped(
                guiGraphics,
                Component.literal(modules + "x"),
                DEBUG_ROW_X + 76,
                y + 5,
                24,
                TEXT_MUTED
        );
        drawClipped(guiGraphics, cap, DEBUG_ROW_X + 112, y + 5, 78, TEXT);
        drawClipped(
                guiGraphics,
                Component.literal(channelSummary(channelMask)),
                DEBUG_ROW_X + DEBUG_ROW_WIDTH - 66,
                y + 5,
                62,
                TEXT_MUTED
        );
    }

    private void drawCacheText(GuiGraphics guiGraphics) {
        drawClipped(
                guiGraphics,
                Component.literal(Component.translatable("rngtech.universal_connector.debug.cache").getString() + " " + cacheSummary()),
                DEBUG_CACHE_X + 5,
                DEBUG_CACHE_Y + 5,
                DEBUG_CACHE_WIDTH - 10,
                TEXT
        );
    }

    private void drawNetworkEnergyText(GuiGraphics guiGraphics) {
        int input = selectedNetworkEnergyInput();
        int output = selectedNetworkEnergyOutput();
        drawClipped(
                guiGraphics,
                Component.translatable("rngtech.universal_connector.network_energy.title", menu.channel()),
                NETWORK_ENERGY_ROW_X + 15,
                NETWORK_ENERGY_ROW_Y + 4,
                154,
                TEXT
        );
        drawClipped(
                guiGraphics,
                Component.translatable(
                        "rngtech.universal_connector.network_energy.input.short",
                        CompactValueText.energyRate(input)
                ),
                NETWORK_ENERGY_ROW_X + 15,
                NETWORK_ENERGY_ROW_Y + 16,
                74,
                TEXT_MUTED
        );
        drawClipped(
                guiGraphics,
                Component.translatable(
                        "rngtech.universal_connector.network_energy.output.short",
                        CompactValueText.energyRate(output)
                ),
                NETWORK_ENERGY_ROW_X + 91,
                NETWORK_ENERGY_ROW_Y + 16,
                78,
                TEXT_MUTED
        );
        drawClipped(
                guiGraphics,
                Component.translatable(
                        "rngtech.universal_connector.network_energy.max.short",
                        CompactValueText.energyRate(menu.networkEnergyChannelCap())
                ),
                NETWORK_ENERGY_ROW_X + 172,
                NETWORK_ENERGY_ROW_Y + 16,
                71,
                TEXT_MUTED
        );
    }

    private void drawClipped(GuiGraphics guiGraphics, Component text, int x, int y, int width, int color) {
        guiGraphics.drawString(font, font.plainSubstrByWidth(text.getString(), width), x, y, color, false);
    }

    private static int metricX(int index) {
        return DEBUG_CARD_X + index * (DEBUG_CARD_WIDTH + DEBUG_CARD_GAP);
    }

    private static int debugRowY(int row) {
        return DEBUG_ROW_Y + row * (DEBUG_ROW_HEIGHT + DEBUG_ROW_GAP);
    }

    private String cacheSummary() {
        int age = menu.networkCacheAgeTicks();
        String state = Component.translatable(age == 0
                ? "rngtech.universal_connector.debug.cache.fresh"
                : "rngtech.universal_connector.debug.cache.stale").getString();
        return state + " " + age + "t";
    }

    private static String formatCompact(int value) {
        if (value >= 1_000_000) {
            return value / 1_000_000 + "M";
        }
        if (value >= 10_000) {
            return value / 1_000 + "k";
        }
        return Integer.toString(value);
    }

    private void cycleNetworkEnergyWindow() {
        networkEnergyWindowIndex = (networkEnergyWindowIndex + 1) % NETWORK_ENERGY_WINDOW_LABELS.length;
    }

    private String networkEnergyWindowLabel() {
        return NETWORK_ENERGY_WINDOW_LABELS[Math.floorMod(networkEnergyWindowIndex, NETWORK_ENERGY_WINDOW_LABELS.length)];
    }

    private int selectedNetworkEnergyInput() {
        return switch (networkEnergyWindowIndex) {
            case 1 -> menu.networkEnergyInput5m();
            case 2 -> menu.networkEnergyInput15m();
            default -> menu.networkEnergyInput1m();
        };
    }

    private int selectedNetworkEnergyOutput() {
        return switch (networkEnergyWindowIndex) {
            case 1 -> menu.networkEnergyOutput5m();
            case 2 -> menu.networkEnergyOutput15m();
            default -> menu.networkEnergyOutput1m();
        };
    }


    private static String channelSummary(int channelMask) {
        if (channelMask == 0) {
            return "-";
        }
        int count = Integer.bitCount(channelMask);
        if (count > 4) {
            return count + " ch";
        }
        StringBuilder builder = new StringBuilder();
        for (int channel = 0; channel <= 15; channel++) {
            if ((channelMask & (1 << channel)) == 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(channel);
        }
        return builder.toString();
    }

    private int bridgeStatusAccent() {
        if (menu.bridgeType() == NetworkBridgeType.NONE) {
            return DISABLED;
        }
        if (!menu.bridgeModLoaded() || menu.bridgeEndpointCount() <= 1) {
            return COLOR_NEGATIVE;
        }
        return COLOR_BRIDGE;
    }

    private Component modeName() {
        return Component.translatable(connectorMode().translationKey());
    }

    private CableConnectorMode connectorMode() {
        CableConnectorMode[] values = CableConnectorMode.values();
        int ordinal = menu.modeOrdinal();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : CableConnectorMode.BOTH;
    }

    private EnergyDistributionMode distributionMode() {
        EnergyDistributionMode[] values = EnergyDistributionMode.values();
        int ordinal = menu.distributionOrdinal();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : EnergyDistributionMode.ROUND_ROBIN;
    }

    private Component distributionName() {
        return Component.translatable(distributionMode().translationKey());
    }

    private void renderControlTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isNetworkTab()) {
            List<Component> tooltip = networkTooltip(mouseX, mouseY);
            if (!tooltip.isEmpty()) {
                guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
            return;
        }
        if (isBridgeTab()) {
            List<Component> tooltip = bridgeTooltip(mouseX, mouseY);
            if (!tooltip.isEmpty()) {
                guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
            return;
        }
        if (isEnergyTab() && inBounds(mouseX, mouseY, ENERGY_MODE_X, ENERGY_ROW_Y, ENERGY_MODE_WIDTH, SMALL_BUTTON)) {
            CableConnectorMode connectorMode = connectorMode();
            guiGraphics.renderComponentTooltip(
                    font,
                    List.of(
                            Component.translatable(connectorMode.translationKey()),
                            Component.translatable(connectorMode.descriptionKey())
                    ),
                    mouseX,
                    mouseY
            );
            return;
        }
        if (!isEnergyTab()
                || !inBounds(mouseX, mouseY, ENERGY_DISTRIBUTION_X, ENERGY_ROW_Y, ENERGY_DISTRIBUTION_WIDTH, SMALL_BUTTON)) {
            return;
        }
        EnergyDistributionMode mode = distributionMode();
        guiGraphics.renderComponentTooltip(
                font,
                List.of(Component.translatable(mode.translationKey()), Component.translatable(mode.descriptionKey())),
                mouseX,
                mouseY
        );
    }

    private List<Component> bridgeTooltip(int mouseX, int mouseY) {
        NetworkBridgeType bridgeType = menu.bridgeType();
        if (inBounds(mouseX, mouseY, ITEM_SLOT_X, ITEM_SLOT_Y, 18, 18)) {
            return List.of(
                    Component.translatable("rngtech.universal_connector.bridge.slot.tooltip"),
                    Component.translatable("rngtech.universal_connector.bridge.semantics.tooltip")
            );
        }
        if (inBounds(mouseX, mouseY, BRIDGE_CHANNEL_DOWN_X, BRIDGE_ROW_Y, SMALL_BUTTON, SMALL_BUTTON)
                || inBounds(mouseX, mouseY, BRIDGE_CHANNEL_UP_X, BRIDGE_ROW_Y, SMALL_BUTTON, SMALL_BUTTON)
                || inBounds(mouseX, mouseY, BRIDGE_CHANNEL_VALUE_X, BRIDGE_ROW_Y, 18, SMALL_BUTTON)) {
            return List.of(Component.translatable(
                    "rngtech.universal_connector.bridge.channel.tooltip",
                    menu.bridgeChannel()
            ));
        }
        if (inBounds(mouseX, mouseY, BRIDGE_STATUS_X, BRIDGE_STATUS_Y, BRIDGE_STATUS_WIDTH, BRIDGE_STATUS_HEIGHT)) {
            if (bridgeType == NetworkBridgeType.NONE) {
                return List.of(
                        Component.translatable("rngtech.universal_connector.bridge.status"),
                        Component.translatable("rngtech.universal_connector.bridge.status.empty.tooltip")
                );
            }
            Component loaded = Component.translatable(menu.bridgeModLoaded()
                    ? "rngtech.universal_connector.bridge.status.loaded.tooltip"
                    : "rngtech.universal_connector.bridge.status.missing.tooltip", bridgeType.modId());
            Component peers = Component.translatable(
                    menu.bridgeEndpointCount() > 1
                            ? "rngtech.universal_connector.bridge.status.peers.tooltip"
                            : "rngtech.universal_connector.bridge.status.no_peer.tooltip",
                    menu.bridgeEndpointCount(),
                    menu.bridgeChannel()
            );
            return List.of(
                    Component.translatable("rngtech.universal_connector.bridge.status"),
                    Component.translatable(bridgeType.translationKey()),
                    loaded,
                    peers,
                    Component.translatable("rngtech.universal_connector.bridge.semantics.tooltip")
            );
        }
        return List.of();
    }

    private List<Component> networkTooltip(int mouseX, int mouseY) {
        if (inBounds(mouseX, mouseY, DEBUG_BUTTON_X, DEBUG_BUTTON_Y, DEBUG_BUTTON_WIDTH, DEBUG_BUTTON_HEIGHT)) {
            return List.of(Component.translatable("rngtech.universal_connector.debug.clear_network_cache.tooltip"));
        }
        if (inBounds(
                mouseX,
                mouseY,
                NETWORK_ENERGY_WINDOW_BUTTON_X,
                NETWORK_ENERGY_WINDOW_BUTTON_Y,
                NETWORK_ENERGY_WINDOW_BUTTON_WIDTH,
                NETWORK_ENERGY_WINDOW_BUTTON_HEIGHT
        )) {
            return List.of(Component.translatable(
                    "rngtech.universal_connector.network_energy.window.tooltip",
                    networkEnergyWindowLabel()
            ));
        }
        if (inBounds(
                mouseX,
                mouseY,
                NETWORK_ENERGY_ROW_X,
                NETWORK_ENERGY_ROW_Y,
                NETWORK_ENERGY_ROW_WIDTH,
                NETWORK_ENERGY_ROW_HEIGHT
        )) {
            return networkEnergyTooltip();
        }
        int metric = hoveredMetricCard(mouseX, mouseY);
        if (metric >= 0) {
            return metricTooltip(metric);
        }
        int row = hoveredDebugRow(mouseX, mouseY);
        if (row >= 0) {
            return moduleRowTooltip(row);
        }
        if (inBounds(mouseX, mouseY, DEBUG_CACHE_X, DEBUG_CACHE_Y, DEBUG_CACHE_WIDTH, DEBUG_CACHE_HEIGHT)) {
            return List.of(
                    Component.translatable("rngtech.universal_connector.debug.cache"),
                    Component.translatable(
                            "rngtech.universal_connector.debug.cache.tooltip",
                            cacheSummary()
                    )
            );
        }
        return List.of();
    }

    private List<Component> metricTooltip(int metric) {
        return switch (metric) {
            case 0 -> List.of(
                    Component.translatable("rngtech.universal_connector.debug.cable_nodes"),
                    Component.translatable(
                            "rngtech.universal_connector.debug.cable_nodes.tooltip",
                            menu.networkCableNodes()
                    )
            );
            case 1 -> List.of(
                    Component.translatable("rngtech.universal_connector.debug.universal_connectors"),
                    Component.translatable(
                            "rngtech.universal_connector.debug.universal_connectors.tooltip",
                            menu.networkUniversalConnectors()
                    )
            );
            case 2 -> List.of(
                    Component.translatable("rngtech.universal_connector.debug.energy_outputs"),
                    Component.translatable(
                            "rngtech.universal_connector.debug.energy_outputs.tooltip",
                            menu.networkEnergyEndpoints(),
                            CompactValueText.exactEnergyRate(menu.networkEnergyOutputCap())
                    )
            );
            case 3 -> List.of(
                    Component.translatable("rngtech.universal_connector.debug.channels"),
                    Component.translatable(
                            "rngtech.universal_connector.debug.channels.tooltip",
                            Integer.bitCount(menu.networkActiveChannelsMask()),
                            channelSummary(menu.networkActiveChannelsMask())
                    )
            );
            default -> List.of();
        };
    }

    private List<Component> moduleRowTooltip(int row) {
        return switch (row) {
            case 0 -> List.of(
                    Component.translatable("rngtech.universal_connector.debug.energy"),
                    Component.translatable(
                            "rngtech.universal_connector.debug.energy.tooltip",
                            menu.networkEnergyModules(),
                            channelSummary(menu.networkEnergyChannelsMask()),
                            CompactValueText.exactEnergyRate(menu.networkEnergyTransferCap()),
                            CompactValueText.exactEnergyRate(menu.networkEnergyInputCap()),
                            CompactValueText.exactEnergyRate(menu.networkEnergyOutputCap())
                    )
            );
            case 1 -> List.of(
                    Component.translatable("rngtech.universal_connector.debug.fluid"),
                    Component.translatable(
                            "rngtech.universal_connector.debug.module.tooltip",
                            menu.networkFluidModules(),
                            channelSummary(menu.networkFluidChannelsMask()),
                            menu.networkFluidShipmentCap() + " mB"
                    )
            );
            case 2 -> List.of(
                    Component.translatable("rngtech.universal_connector.debug.item"),
                    Component.translatable(
                            "rngtech.universal_connector.debug.module.tooltip",
                            menu.networkItemModules(),
                            channelSummary(menu.networkItemChannelsMask()),
                            menu.networkItemShipmentCap() + " items"
                    )
            );
            case 3 -> List.of(
                    Component.translatable("rngtech.universal_connector.debug.bridge"),
                    Component.translatable(
                            "rngtech.universal_connector.debug.bridge.tooltip",
                            menu.networkBridgeModules(),
                            menu.networkAe2BridgeEndpoints(),
                            menu.networkRefinedStorageBridgeEndpoints(),
                            channelSummary(menu.networkBridgeChannelsMask())
                    )
            );
            default -> List.of();
        };
    }

    private List<Component> networkEnergyTooltip() {
        int input = selectedNetworkEnergyInput();
        int output = selectedNetworkEnergyOutput();
        int liveInput = menu.networkEnergyLiveInput();
        int liveOutput = menu.networkEnergyLiveOutput();
        return List.of(
                Component.translatable(
                        "rngtech.universal_connector.network_energy.tooltip",
                        menu.channel(),
                        networkEnergyWindowLabel()
                ),
                Component.translatable(
                        "rngtech.universal_connector.network_energy.input.tooltip",
                        CompactValueText.exactEnergyRate(input),
                        CompactValueText.exactEnergyRate(liveInput)
                ),
                Component.translatable(
                        "rngtech.universal_connector.network_energy.output.tooltip",
                        CompactValueText.exactEnergyRate(output),
                        CompactValueText.exactEnergyRate(liveOutput)
                ),
                Component.translatable(
                        "rngtech.universal_connector.network_energy.max.tooltip",
                        CompactValueText.exactEnergyRate(menu.networkEnergyChannelCap())
                )
        );
    }

    private int hoveredMetricCard(int mouseX, int mouseY) {
        for (int index = 0; index < 4; index++) {
            if (inBounds(mouseX, mouseY, metricX(index), DEBUG_CARD_Y, DEBUG_CARD_WIDTH, DEBUG_CARD_HEIGHT)) {
                return index;
            }
        }
        return -1;
    }

    private int hoveredDebugRow(int mouseX, int mouseY) {
        for (int row = 0; row < 4; row++) {
            if (inBounds(mouseX, mouseY, DEBUG_ROW_X, debugRowY(row), DEBUG_ROW_WIDTH, DEBUG_ROW_HEIGHT)) {
                return row;
            }
        }
        return -1;
    }

    private Component itemModeName(int moduleIndex) {
        ItemConnectorMode[] values = ItemConnectorMode.values();
        int ordinal = menu.itemModeOrdinal(moduleIndex);
        ItemConnectorMode mode = ordinal >= 0 && ordinal < values.length ? values[ordinal] : ItemConnectorMode.INPUT;
        return Component.translatable(mode.translationKey());
    }

    private Component fluidModeName(int moduleIndex) {
        FluidConnectorMode[] values = FluidConnectorMode.values();
        int ordinal = menu.fluidModeOrdinal(moduleIndex);
        FluidConnectorMode mode = ordinal >= 0 && ordinal < values.length ? values[ordinal] : FluidConnectorMode.INPUT;
        return Component.translatable(mode.translationKey());
    }

    private Component moduleModeName(int moduleIndex) {
        return isFluidTab() ? fluidModeName(moduleIndex) : itemModeName(moduleIndex);
    }

    private int moduleCount() {
        return isFluidTab() ? UniversalConnectorMenu.FLUID_MODULE_COUNT : UniversalConnectorMenu.ITEM_MODULE_COUNT;
    }

    private int moduleChannel(int moduleIndex) {
        return isFluidTab() ? menu.fluidChannel(moduleIndex) : menu.itemChannel(moduleIndex);
    }

    private Direction moduleAttachAs(int moduleIndex) {
        return isFluidTab() ? menu.fluidAttachAs(moduleIndex) : menu.itemAttachAs(moduleIndex);
    }

    private int moduleChannelDownButtonBase() {
        return isFluidTab()
                ? UniversalConnectorMenu.BUTTON_FLUID_CHANNEL_DOWN_BASE
                : UniversalConnectorMenu.BUTTON_ITEM_CHANNEL_DOWN_BASE;
    }

    private int moduleChannelUpButtonBase() {
        return isFluidTab()
                ? UniversalConnectorMenu.BUTTON_FLUID_CHANNEL_UP_BASE
                : UniversalConnectorMenu.BUTTON_ITEM_CHANNEL_UP_BASE;
    }

    private int moduleAttachButtonBase() {
        return isFluidTab()
                ? UniversalConnectorMenu.BUTTON_FLUID_ATTACH_BASE
                : UniversalConnectorMenu.BUTTON_ITEM_ATTACH_BASE;
    }

    private int moduleModeButtonBase() {
        return isFluidTab()
                ? UniversalConnectorMenu.BUTTON_FLUID_MODE_BASE
                : UniversalConnectorMenu.BUTTON_ITEM_MODE_BASE;
    }

    private static Component directionName(Direction direction) {
        return Component.translatable("rngtech.direction." + direction.getSerializedName());
    }

    private static Direction nextDirection(Direction direction) {
        Direction[] values = Direction.values();
        int ordinal = direction == null ? 0 : direction.ordinal() + 1;
        return values[Math.floorMod(ordinal, values.length)];
    }

    private static Component moduleAttachName(Direction direction) {
        return direction == null
                ? Component.translatable("rngtech.universal_connector.attach.none")
                : directionName(direction);
    }

    private boolean isNetworkTab() {
        return menu.selectedTab() == UniversalConnectorMenu.TAB_NETWORK;
    }
}
