package com.rngtech.content.network;

import com.rngtech.RNGTech;
import com.rngtech.rpg.corruption.CorruptionCatalog;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

/** The server's Volatile Catalyst data, so client outcome previews match the rolls. */
public record CorruptionCatalogPayload(String outcomes, Map<String, String> pools) implements CustomPacketPayload {
    public static final Type<CorruptionCatalogPayload> TYPE = new Type<>(RNGTech.id("corruption_catalog"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CorruptionCatalogPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(Integer.MAX_VALUE / 4),
                    CorruptionCatalogPayload::outcomes,
                    ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.stringUtf8(Integer.MAX_VALUE / 4)),
                    CorruptionCatalogPayload::pools,
                    CorruptionCatalogPayload::new
            );

    public static CorruptionCatalogPayload of(CorruptionCatalog.Source source) {
        return new CorruptionCatalogPayload(source.outcomes(), source.pools());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CorruptionCatalogPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                CorruptionCatalog.setActive(CorruptionCatalog.parse(new CorruptionCatalog.Source(payload.outcomes(), payload.pools())));
            } catch (IllegalArgumentException exception) {
                RNGTech.LOGGER.error("Server sent invalid corruption data; keeping the previous catalog", exception);
            }
        });
    }
}
