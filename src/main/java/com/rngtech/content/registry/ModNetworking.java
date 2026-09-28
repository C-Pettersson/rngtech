package com.rngtech.content.registry;

import com.rngtech.content.network.OpenConfiguratorAdvancedPayload;
import com.rngtech.content.network.OpenConfiguratorAdvancedServerHandler;
import com.rngtech.content.network.WrenchOverlayClientHandler;
import com.rngtech.content.network.WrenchOverlayDataPayload;
import com.rngtech.content.network.WrenchOverlayRequestPayload;
import com.rngtech.content.network.WrenchOverlayServerHandler;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private static final String VERSION = "2";

    private ModNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(com.rngtech.content.network.MasteryActionPayload.TYPE,
                com.rngtech.content.network.MasteryActionPayload.STREAM_CODEC,
                com.rngtech.content.network.MasteryActionPayload::handle);
        registrar.playToServer(
                WrenchOverlayRequestPayload.TYPE,
                WrenchOverlayRequestPayload.STREAM_CODEC,
                WrenchOverlayServerHandler::handle
        );
        registrar.playToServer(
                OpenConfiguratorAdvancedPayload.TYPE,
                OpenConfiguratorAdvancedPayload.STREAM_CODEC,
                OpenConfiguratorAdvancedServerHandler::handle
        );
        registrar.playToClient(
                WrenchOverlayDataPayload.TYPE,
                WrenchOverlayDataPayload.STREAM_CODEC,
                WrenchOverlayClientHandler::handle
        );
    }
}
