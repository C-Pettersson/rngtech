package com.rngtech.client.configurator;

import com.rngtech.content.network.OpenConfiguratorAdvancedPayload;
import com.rngtech.content.registry.ModItems;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ConfiguratorClient {
    private static final String KEY_CATEGORY = "key.categories.rngtech";
    private static final KeyMapping OPEN_ADVANCED = new KeyMapping(
            "key.rngtech.open_configurator_advanced",
            InputConstants.UNKNOWN.getType(),
            InputConstants.UNKNOWN.getValue(),
            KEY_CATEGORY
    );

    private ConfiguratorClient() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_ADVANCED);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_ADVANCED.consumeClick()) {
            if (minecraft.player == null) {
                continue;
            }
            InteractionHand hand = null;
            if (minecraft.player.getMainHandItem().is(ModItems.CONFIGURATOR.get())) {
                hand = InteractionHand.MAIN_HAND;
            } else if (minecraft.player.getOffhandItem().is(ModItems.CONFIGURATOR.get())) {
                hand = InteractionHand.OFF_HAND;
            }
            if (hand != null) {
                PacketDistributor.sendToServer(new OpenConfiguratorAdvancedPayload(hand == InteractionHand.OFF_HAND));
            }
        }
    }
}
