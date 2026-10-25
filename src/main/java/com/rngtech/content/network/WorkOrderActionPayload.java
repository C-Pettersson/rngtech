package com.rngtech.content.network;

import com.rngtech.RNGTech;
import com.rngtech.content.menu.WorkOrderMenuView;
import com.rngtech.content.workorder.WorkOrderOperations;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** An Orders tab action from the client: load, activate, deactivate, unload, or abandon, with a slot as its value. */
public record WorkOrderActionPayload(int containerId, String action, String value) implements CustomPacketPayload {
    public static final Type<WorkOrderActionPayload> TYPE = new Type<>(RNGTech.id("work_order_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WorkOrderActionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public WorkOrderActionPayload decode(RegistryFriendlyByteBuf buffer) {
            return new WorkOrderActionPayload(buffer.readVarInt(), buffer.readUtf(16), buffer.readUtf(16));
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, WorkOrderActionPayload payload) {
            buffer.writeVarInt(payload.containerId); buffer.writeUtf(payload.action, 16); buffer.writeUtf(payload.value, 16);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(WorkOrderActionPayload payload, IPayloadContext context) {
        var player = context.player();
        var menu = player.containerMenu;
        if (player.isSpectator() || menu.containerId != payload.containerId || !menu.stillValid(player)
                || !(menu instanceof WorkOrderMenuView view)) { return; }
        WorkOrderOperations.perform(player, view.workOrderHost(), payload.action, payload.value);
        menu.broadcastChanges();
    }
}
