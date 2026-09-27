package com.rngtech.content.network;

import com.rngtech.RNGTech;
import com.rngtech.content.wrench.WrenchOverlayTarget;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record WrenchOverlayRequestPayload(WrenchOverlayTarget target) implements CustomPacketPayload {
    public static final Type<WrenchOverlayRequestPayload> TYPE = new Type<>(RNGTech.id("wrench_overlay_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WrenchOverlayRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    WrenchOverlayTarget.STREAM_CODEC,
                    WrenchOverlayRequestPayload::target,
                    WrenchOverlayRequestPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
