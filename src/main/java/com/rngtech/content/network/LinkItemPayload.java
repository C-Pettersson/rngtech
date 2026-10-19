package com.rngtech.content.network;

import com.rngtech.RNGTech;
import com.rngtech.content.item.ItemLinks;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Links an item with a rarity in chat. The client names a slot, not the item, so the server posts what the player really
 * holds. {@code containerId} is {@link #PLAYER_INVENTORY} for a player inventory slot, which also covers the creative
 * inventory, or the open menu's id for a menu slot.
 */
public record LinkItemPayload(int containerId, int slot) implements CustomPacketPayload {
    public static final int PLAYER_INVENTORY = -1;
    public static final Type<LinkItemPayload> TYPE = new Type<>(RNGTech.id("link_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LinkItemPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            LinkItemPayload::containerId,
            ByteBufCodecs.VAR_INT,
            LinkItemPayload::slot,
            LinkItemPayload::new
    );

    private static final int COOLDOWN_TICKS = 20;
    private static final Map<ServerPlayer, Integer> LAST_LINK_TICK = new WeakHashMap<>();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LinkItemPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = linkedStack(player, payload);
        if (!ItemLinks.linkable(stack)) {
            return;
        }
        int tick = player.server.getTickCount();
        Integer last = LAST_LINK_TICK.get(player);
        if (last != null && tick - last < COOLDOWN_TICKS) {
            return;
        }
        LAST_LINK_TICK.put(player, tick);
        player.server.getPlayerList().broadcastSystemMessage(
                Component.translatable("rngtech.chat.linked_item", player.getDisplayName(), ItemLinks.link(stack)),
                false
        );
    }

    private static ItemStack linkedStack(ServerPlayer player, LinkItemPayload payload) {
        if (payload.containerId() == PLAYER_INVENTORY) {
            return payload.slot() >= 0 && payload.slot() < player.getInventory().getContainerSize()
                    ? player.getInventory().getItem(payload.slot())
                    : ItemStack.EMPTY;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.containerId != payload.containerId()
                || payload.slot() < 0
                || payload.slot() >= menu.slots.size()
                || !menu.stillValid(player)) {
            return ItemStack.EMPTY;
        }
        return menu.getSlot(payload.slot()).getItem();
    }
}
