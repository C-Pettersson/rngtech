package com.rngtech.content.network;

import com.rngtech.RNGTech;
import com.rngtech.content.menu.StatBreakdownMenu;
import com.rngtech.rpg.MachineStatAccumulator;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Asks the server to explain the stat panel of the open menu; answered with a {@link StatBreakdownPayload}. */
public record StatBreakdownRequestPayload(int containerId) implements CustomPacketPayload {
    public static final Type<StatBreakdownRequestPayload> TYPE = new Type<>(RNGTech.id("stat_breakdown_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StatBreakdownRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    StatBreakdownRequestPayload::containerId,
                    StatBreakdownRequestPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StatBreakdownRequestPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.containerId != payload.containerId()
                || !(menu instanceof StatBreakdownMenu breakdownMenu)
                || !menu.stillValid(player)) {
            return;
        }
        MachineStatAccumulator stats = MachineStatAccumulator.recording(breakdownMenu::breakdownStats);
        context.reply(new StatBreakdownPayload(payload.containerId(), stats.breakdowns()));
    }
}
