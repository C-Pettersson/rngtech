package com.rngtech.content.network;

import com.rngtech.RNGTech;
import com.rngtech.client.screen.StatBreakdownCache;
import com.rngtech.rpg.StatBreakdown;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Every stat breakdown behind an open menu's stat panel, recorded on the server. */
public record StatBreakdownPayload(int containerId, List<StatBreakdown> breakdowns) implements CustomPacketPayload {
    public static final Type<StatBreakdownPayload> TYPE = new Type<>(RNGTech.id("stat_breakdown"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StatBreakdownPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    StatBreakdownPayload::containerId,
                    StatBreakdown.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    StatBreakdownPayload::breakdowns,
                    StatBreakdownPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StatBreakdownPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        StatBreakdownCache.accept(payload.containerId(), payload.breakdowns());
    }
}
