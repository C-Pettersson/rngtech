package com.rngtech.client;

import com.rngtech.content.item.ItemLinks;
import com.rngtech.content.network.LinkItemPayload;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Ctrl+Alt+click an item with a rarity in any inventory screen to link it in chat. */
public final class ItemLinkClient {
    private ItemLinkClient() {
    }

    public static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT
                || !Screen.hasControlDown()
                || !Screen.hasAltDown()
                || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        Slot slot = screen.getSlotUnderMouse();
        if (slot == null || !ItemLinks.linkable(slot.getItem())) {
            return;
        }
        LinkItemPayload payload = payload(screen, slot);
        if (payload == null) {
            return;
        }
        PacketDistributor.sendToServer(payload);
        event.setCanceled(true);
    }

    private static LinkItemPayload payload(AbstractContainerScreen<?> screen, Slot slot) {
        if (slot.container instanceof Inventory) {
            return new LinkItemPayload(LinkItemPayload.PLAYER_INVENTORY, slot.getContainerSlot());
        }
        // Creative tab slots are client-only, so their indices mean nothing to the server's menu.
        if (screen instanceof CreativeModeInventoryScreen) {
            return null;
        }
        return new LinkItemPayload(screen.getMenu().containerId, slot.index);
    }
}
