package com.rngtech.content.network;

import com.rngtech.RNGTech;
import com.rngtech.content.affixsorter.SorterRules;
import com.rngtech.content.affixsorter.SorterRulesHost;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The Affix Sorter rule editor sends the whole edited list; the server clamps it to Rule Capacity. */
public record SorterRulesPayload(int containerId, SorterRules rules) implements CustomPacketPayload {
    public static final Type<SorterRulesPayload> TYPE = new Type<>(RNGTech.id("sorter_rules"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SorterRulesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SorterRulesPayload::containerId,
            SorterRules.STREAM_CODEC, SorterRulesPayload::rules,
            SorterRulesPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SorterRulesPayload payload, IPayloadContext context) {
        var player = context.player();
        var menu = player.containerMenu;
        if (player.isSpectator() || menu.containerId != payload.containerId || !menu.stillValid(player)
                || !(menu instanceof SorterRulesHost host)) {
            return;
        }
        host.applySorterRules(payload.rules);
        menu.broadcastChanges();
    }
}
