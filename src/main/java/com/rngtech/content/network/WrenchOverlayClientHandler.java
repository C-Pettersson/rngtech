package com.rngtech.content.network;

import com.rngtech.client.wrench.WrenchOverlayClient;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class WrenchOverlayClientHandler {
    private WrenchOverlayClientHandler() {
    }

    public static void handle(WrenchOverlayDataPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        WrenchOverlayClient.acceptData(payload);
    }
}
