package com.rngtech.client.screen;

import com.rngtech.content.itemfilter.AdvancedItemFilterSettings;
import com.rngtech.content.menu.AdvancedItemFilterMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class AdvancedItemFilterScreen extends AbstractContainerScreen<AdvancedItemFilterMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int BUTTON = 0xFF3F6470;
    private static final int BUTTON_DARK = 0xFF243942;
    private static final int BUTTON_LIGHT = 0xFF79A1AD;
    private static final int SELECTED = 0xFF627646;
    private static final int SELECTED_LIGHT = 0xFFA7B96F;

    public AdvancedItemFilterScreen(AdvancedItemFilterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 256;
        imageHeight = 262;
        titleLabelX = 8;
        titleLabelY = 8;
        inventoryLabelY = 178;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        for (int index = 0; index < AdvancedItemFilterSettings.SAMPLE_SLOT_COUNT; index++) {
            int row = index / 3;
            int column = index % 3;
            renderSlotFrame(guiGraphics, 20 + column * 18, 36 + row * 18);
        }

        AdvancedItemFilterSettings settings = menu.settings();
        renderButton(guiGraphics, 92, 28, 48, 18, settings.mode() == AdvancedItemFilterSettings.Mode.DENY);
        renderButton(guiGraphics, 146, 28, 44, 18, settings.exactItemsEnabled());
        renderButton(guiGraphics, 196, 28, 44, 18, settings.strictComponents());
        renderButton(guiGraphics, 92, 52, 44, 18, settings.tagEnabled());
        renderButton(guiGraphics, 142, 52, 44, 18, settings.namespaceEnabled());
        renderButton(guiGraphics, 192, 52, 44, 18, settings.stageEnabled());
        renderButton(guiGraphics, 92, 76, 44, 18, settings.stabilityEnabled());
        renderButton(guiGraphics, 142, 76, 44, 18, settings.identityMode() != AdvancedItemFilterSettings.IdentityMode.ANY);
        renderButton(guiGraphics, 192, 76, 44, 18, settings.rarityMode() != AdvancedItemFilterSettings.RarityMode.ANY);

        renderButton(guiGraphics, 92, 108, 16, 14, false);
        renderButton(guiGraphics, 144, 108, 16, 14, false);
        renderButton(guiGraphics, 166, 108, 16, 14, false);
        renderButton(guiGraphics, 218, 108, 16, 14, false);
        renderButton(guiGraphics, 92, 133, 16, 14, false);
        renderButton(guiGraphics, 144, 133, 16, 14, false);
        renderButton(guiGraphics, 166, 133, 16, 14, false);
        renderButton(guiGraphics, 218, 133, 16, 14, false);

        for (int index = 0; index < AdvancedItemFilterSettings.MAX_TAGS; index++) {
            renderButton(guiGraphics, 92 + index * 37, 154, 34, 12, index < settings.tags().size());
        }
        for (int index = 0; index < AdvancedItemFilterSettings.MAX_NAMESPACES; index++) {
            renderButton(guiGraphics, 92 + index * 37, 169, 34, 12, index < settings.namespaces().size());
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlotFrame(
                        guiGraphics,
                        AdvancedItemFilterMenu.PLAYER_INVENTORY_X + column * 18,
                        AdvancedItemFilterMenu.PLAYER_INVENTORY_Y + row * 18
                );
            }
        }
        for (int column = 0; column < 9; column++) {
            renderSlotFrame(
                    guiGraphics,
                    AdvancedItemFilterMenu.PLAYER_INVENTORY_X + column * 18,
                    AdvancedItemFilterMenu.HOTBAR_Y
            );
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        AdvancedItemFilterSettings settings = menu.settings();
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.advanced_filter.samples"), 20, 24, TEXT_MUTED, false);

        drawCentered(guiGraphics, Component.translatable("rngtech.advanced_filter.mode." + settings.mode().getSerializedName()), 92, 33, 48, 0xFFFFFFFF);
        drawCentered(guiGraphics, toggleText("rngtech.advanced_filter.exact", settings.exactItemsEnabled()), 146, 33, 44, 0xFFFFFFFF);
        drawCentered(guiGraphics, toggleText("rngtech.advanced_filter.strict", settings.strictComponents()), 196, 33, 44, 0xFFFFFFFF);
        drawCentered(guiGraphics, toggleText("rngtech.advanced_filter.tags", settings.tagEnabled()), 92, 57, 44, 0xFFFFFFFF);
        drawCentered(guiGraphics, toggleText("rngtech.advanced_filter.namespaces", settings.namespaceEnabled()), 142, 57, 44, 0xFFFFFFFF);
        drawCentered(guiGraphics, toggleText("rngtech.advanced_filter.stage", settings.stageEnabled()), 192, 57, 44, 0xFFFFFFFF);
        drawCentered(guiGraphics, toggleText("rngtech.advanced_filter.stability", settings.stabilityEnabled()), 92, 81, 44, 0xFFFFFFFF);
        drawCentered(guiGraphics, Component.translatable("rngtech.advanced_filter.identity." + settings.identityMode().getSerializedName()), 142, 81, 44, 0xFFFFFFFF);
        drawCentered(guiGraphics, Component.translatable("rngtech.advanced_filter.rarity." + settings.rarityMode().getSerializedName()), 192, 81, 44, 0xFFFFFFFF);

        guiGraphics.drawString(font, Component.translatable("rngtech.advanced_filter.stage_range"), 92, 98, TEXT_MUTED, false);
        drawCentered(guiGraphics, Component.literal("-"), 92, 111, 16, 0xFFFFFFFF);
        drawCentered(guiGraphics, Component.literal(Integer.toString(settings.minStage())), 110, 111, 32, TEXT);
        drawCentered(guiGraphics, Component.literal("+"), 144, 111, 16, 0xFFFFFFFF);
        drawCentered(guiGraphics, Component.literal("-"), 166, 111, 16, 0xFFFFFFFF);
        drawCentered(guiGraphics, Component.literal(Integer.toString(settings.maxStage())), 184, 111, 32, TEXT);
        drawCentered(guiGraphics, Component.literal("+"), 218, 111, 16, 0xFFFFFFFF);

        guiGraphics.drawString(font, Component.translatable("rngtech.advanced_filter.stability_range"), 92, 123, TEXT_MUTED, false);
        drawCentered(guiGraphics, Component.literal("-"), 92, 136, 16, 0xFFFFFFFF);
        drawCentered(guiGraphics, Component.literal(settings.minStability() + "%"), 110, 136, 32, TEXT);
        drawCentered(guiGraphics, Component.literal("+"), 144, 136, 16, 0xFFFFFFFF);
        drawCentered(guiGraphics, Component.literal("-"), 166, 136, 16, 0xFFFFFFFF);
        drawCentered(guiGraphics, Component.literal(settings.maxStability() + "%"), 184, 136, 32, TEXT);
        drawCentered(guiGraphics, Component.literal("+"), 218, 136, 16, 0xFFFFFFFF);

        for (int index = 0; index < AdvancedItemFilterSettings.MAX_TAGS; index++) {
            drawClippedCentered(guiGraphics, tagName(settings, index), 92 + index * 37, 156, 34, 0xFFFFFFFF);
        }
        for (int index = 0; index < AdvancedItemFilterSettings.MAX_NAMESPACES; index++) {
            drawClippedCentered(guiGraphics, namespaceName(settings, index), 92 + index * 37, 171, 34, 0xFFFFFFFF);
        }
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inBounds(mouseX, mouseY, 92, 28, 48, 18)) {
                sendButton(AdvancedItemFilterMenu.BUTTON_MODE);
                return true;
            }
            if (inBounds(mouseX, mouseY, 146, 28, 44, 18)) {
                sendButton(AdvancedItemFilterMenu.BUTTON_EXACT);
                return true;
            }
            if (inBounds(mouseX, mouseY, 196, 28, 44, 18)) {
                sendButton(AdvancedItemFilterMenu.BUTTON_STRICT);
                return true;
            }
            if (inBounds(mouseX, mouseY, 92, 52, 44, 18)) {
                sendButton(AdvancedItemFilterMenu.BUTTON_TAGS);
                return true;
            }
            if (inBounds(mouseX, mouseY, 142, 52, 44, 18)) {
                sendButton(AdvancedItemFilterMenu.BUTTON_NAMESPACES);
                return true;
            }
            if (inBounds(mouseX, mouseY, 192, 52, 44, 18)) {
                sendButton(AdvancedItemFilterMenu.BUTTON_STAGE);
                return true;
            }
            if (inBounds(mouseX, mouseY, 92, 76, 44, 18)) {
                sendButton(AdvancedItemFilterMenu.BUTTON_STABILITY);
                return true;
            }
            if (inBounds(mouseX, mouseY, 142, 76, 44, 18)) {
                sendButton(AdvancedItemFilterMenu.BUTTON_IDENTITY);
                return true;
            }
            if (inBounds(mouseX, mouseY, 192, 76, 44, 18)) {
                sendButton(AdvancedItemFilterMenu.BUTTON_RARITY);
                return true;
            }
            if (rangeButton(mouseX, mouseY)) {
                return true;
            }
        }
        for (int index = 0; index < AdvancedItemFilterSettings.MAX_TAGS; index++) {
            if (inBounds(mouseX, mouseY, 92 + index * 37, 154, 34, 12)) {
                sendButton((button == 1 ? AdvancedItemFilterMenu.BUTTON_TAG_CLEAR_BASE : AdvancedItemFilterMenu.BUTTON_TAG_CYCLE_BASE) + index);
                return true;
            }
        }
        for (int index = 0; index < AdvancedItemFilterSettings.MAX_NAMESPACES; index++) {
            if (inBounds(mouseX, mouseY, 92 + index * 37, 169, 34, 12)) {
                sendButton((button == 1 ? AdvancedItemFilterMenu.BUTTON_NAMESPACE_CLEAR_BASE : AdvancedItemFilterMenu.BUTTON_NAMESPACE_CYCLE_BASE) + index);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean rangeButton(double mouseX, double mouseY) {
        if (inBounds(mouseX, mouseY, 92, 108, 16, 14)) {
            sendButton(AdvancedItemFilterMenu.BUTTON_MIN_STAGE_DOWN);
            return true;
        }
        if (inBounds(mouseX, mouseY, 144, 108, 16, 14)) {
            sendButton(AdvancedItemFilterMenu.BUTTON_MIN_STAGE_UP);
            return true;
        }
        if (inBounds(mouseX, mouseY, 166, 108, 16, 14)) {
            sendButton(AdvancedItemFilterMenu.BUTTON_MAX_STAGE_DOWN);
            return true;
        }
        if (inBounds(mouseX, mouseY, 218, 108, 16, 14)) {
            sendButton(AdvancedItemFilterMenu.BUTTON_MAX_STAGE_UP);
            return true;
        }
        if (inBounds(mouseX, mouseY, 92, 133, 16, 14)) {
            sendButton(AdvancedItemFilterMenu.BUTTON_MIN_STABILITY_DOWN);
            return true;
        }
        if (inBounds(mouseX, mouseY, 144, 133, 16, 14)) {
            sendButton(AdvancedItemFilterMenu.BUTTON_MIN_STABILITY_UP);
            return true;
        }
        if (inBounds(mouseX, mouseY, 166, 133, 16, 14)) {
            sendButton(AdvancedItemFilterMenu.BUTTON_MAX_STABILITY_DOWN);
            return true;
        }
        if (inBounds(mouseX, mouseY, 218, 133, 16, 14)) {
            sendButton(AdvancedItemFilterMenu.BUTTON_MAX_STABILITY_UP);
            return true;
        }
        return false;
    }

    private void sendButton(int id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.gameMode != null && menu.clickMenuButton(minecraft.player, id)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private Component tagName(AdvancedItemFilterSettings settings, int index) {
        if (index >= settings.tags().size()) {
            return Component.translatable("rngtech.advanced_filter.empty");
        }
        ResourceLocation tag = settings.tags().get(index);
        return Component.literal(tag.getNamespace() + ":" + tag.getPath());
    }

    private Component namespaceName(AdvancedItemFilterSettings settings, int index) {
        if (index >= settings.namespaces().size()) {
            return Component.translatable("rngtech.advanced_filter.empty");
        }
        return Component.literal(settings.namespaces().get(index));
    }

    private Component toggleText(String key, boolean enabled) {
        return Component.translatable(key + (enabled ? ".on" : ".off"));
    }

    private boolean inBounds(double mouseX, double mouseY, int x, int y, int width, int height) {
        int left = leftPos + x;
        int top = topPos + y;
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
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

    private void renderSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left - 1, top - 1, left + 17, top + 17, PANEL_DARK);
        guiGraphics.fill(left, top, left + 16, top + 16, 0xFFE0E0E0);
    }

    private void renderButton(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean selected) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + width, top + height, BUTTON_DARK);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, selected ? SELECTED : BUTTON);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + 2, selected ? SELECTED_LIGHT : BUTTON_LIGHT);
    }

    private void drawCentered(GuiGraphics guiGraphics, Component text, int x, int y, int width, int color) {
        guiGraphics.drawString(font, text, x + (width - font.width(text)) / 2, y, color, false);
    }

    private void drawClippedCentered(GuiGraphics guiGraphics, Component text, int x, int y, int width, int color) {
        String value = text.getString();
        if (font.width(value) <= width) {
            guiGraphics.drawString(font, value, x + (width - font.width(value)) / 2, y, color, false);
            return;
        }
        while (font.width(value + "...") > width && value.length() > 1) {
            value = value.substring(0, value.length() - 1);
        }
        guiGraphics.drawString(font, value + "...", x + 2, y, color, false);
    }
}
