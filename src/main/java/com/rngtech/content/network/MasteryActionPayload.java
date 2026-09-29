package com.rngtech.content.network;

import com.rngtech.RNGTech;
import com.rngtech.content.menu.MasteryMenuView;
import com.rngtech.rpg.progression.MasteryOperations;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MasteryActionPayload(int containerId, String action, String value) implements CustomPacketPayload {
    public static final Type<MasteryActionPayload> TYPE = new Type<>(RNGTech.id("mastery_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MasteryActionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public MasteryActionPayload decode(RegistryFriendlyByteBuf buffer) {
            return new MasteryActionPayload(buffer.readVarInt(), buffer.readUtf(24), buffer.readUtf(10000));
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, MasteryActionPayload payload) {
            buffer.writeVarInt(payload.containerId); buffer.writeUtf(payload.action, 24); buffer.writeUtf(payload.value, 10000);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(MasteryActionPayload payload, IPayloadContext context) {
        var player = context.player();
        var menu = player.containerMenu;
        if (player.isSpectator() || menu.containerId != payload.containerId || !menu.stillValid(player)
                || !(menu instanceof MasteryMenuView<?> view)) { return; }
        MasteryOperations.perform(player, view.masteryHost(), payload.action, payload.value);
        menu.broadcastChanges();
    }
}
