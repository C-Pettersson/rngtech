package com.rngtech.content.network;

import com.rngtech.RNGTech;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenConfiguratorAdvancedPayload(boolean offhand) implements CustomPacketPayload {
    public static final Type<OpenConfiguratorAdvancedPayload> TYPE = new Type<>(RNGTech.id("open_configurator_advanced"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenConfiguratorAdvancedPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    OpenConfiguratorAdvancedPayload::offhand,
                    OpenConfiguratorAdvancedPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
