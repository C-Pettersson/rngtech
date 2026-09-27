package com.rngtech.content.network;

import com.rngtech.content.menu.ConfiguratorAdvancedMenu;
import com.rngtech.content.registry.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class OpenConfiguratorAdvancedServerHandler {
    private OpenConfiguratorAdvancedServerHandler() {
    }

    public static void handle(OpenConfiguratorAdvancedPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModItems.CONFIGURATOR.get())) {
            return;
        }
        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.rngtech.configurator_advanced");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return new ConfiguratorAdvancedMenu(containerId, inventory, hand);
            }
        };
        player.openMenu(provider, buffer -> buffer.writeBoolean(hand == InteractionHand.OFF_HAND));
    }
}
