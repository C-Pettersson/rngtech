package com.rngtech.content.network;

import com.rngtech.RNGTech;
import com.rngtech.content.wrench.ConnectorOverlaySnapshot;
import com.rngtech.content.wrench.WrenchOverlayTarget;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;

public record WrenchOverlayDataPayload(
        WrenchOverlayTarget target,
        Optional<ConnectorOverlaySnapshot> snapshot
) implements CustomPacketPayload {
    public static final Type<WrenchOverlayDataPayload> TYPE = new Type<>(RNGTech.id("wrench_overlay_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WrenchOverlayDataPayload> STREAM_CODEC =
            StreamCodec.ofMember(WrenchOverlayDataPayload::write, WrenchOverlayDataPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        WrenchOverlayTarget.STREAM_CODEC.encode(buffer, target);
        buffer.writeBoolean(snapshot.isPresent());
        snapshot.ifPresent(value -> ConnectorOverlaySnapshot.STREAM_CODEC.encode(buffer, value));
    }

    private static WrenchOverlayDataPayload read(RegistryFriendlyByteBuf buffer) {
        WrenchOverlayTarget target = WrenchOverlayTarget.STREAM_CODEC.decode(buffer);
        Optional<ConnectorOverlaySnapshot> snapshot = buffer.readBoolean()
                ? Optional.of(ConnectorOverlaySnapshot.STREAM_CODEC.decode(buffer))
                : Optional.empty();
        return new WrenchOverlayDataPayload(target, snapshot);
    }
}
