package com.rngtech.rpg.workorder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.Optional;

/**
 * What a Fulfilled Work Order keeps for its payout: the tier, the machine's family and ascendancy, Bounty, Prestige, and
 * the exact items the order listed, or null with no list, when opening it rolls the reward table instead.
 */
public record FulfilledWorkOrder(int tier, String family, String ascendancy, int bounty, int prestige, ItemContainerContents rewards) {
    public static final Codec<FulfilledWorkOrder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("tier", WorkOrder.MIN_TIER).forGetter(FulfilledWorkOrder::tier),
            Codec.STRING.optionalFieldOf("family", "").forGetter(FulfilledWorkOrder::family),
            Codec.STRING.optionalFieldOf("ascendancy", "").forGetter(FulfilledWorkOrder::ascendancy),
            Codec.INT.optionalFieldOf("bounty", WorkOrder.BASE_BOUNTY).forGetter(FulfilledWorkOrder::bounty),
            Codec.INT.optionalFieldOf("prestige", 0).forGetter(FulfilledWorkOrder::prestige),
            Codec.lazyInitialized(() -> ItemContainerContents.CODEC).optionalFieldOf("rewards").forGetter(order -> Optional.ofNullable(order.rewards))
    ).apply(instance, (tier, family, ascendancy, bounty, prestige, rewards) ->
            new FulfilledWorkOrder(tier, family, ascendancy, bounty, prestige, rewards.orElse(null))));

    public static final StreamCodec<RegistryFriendlyByteBuf, FulfilledWorkOrder> STREAM_CODEC = new StreamCodec<>() {
        @Override public FulfilledWorkOrder decode(RegistryFriendlyByteBuf buffer) {
            return new FulfilledWorkOrder(buffer.readVarInt(), buffer.readUtf(64), buffer.readUtf(80), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readBoolean() ? ItemContainerContents.STREAM_CODEC.decode(buffer) : null);
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, FulfilledWorkOrder order) {
            buffer.writeVarInt(order.tier); buffer.writeUtf(order.family, 64); buffer.writeUtf(order.ascendancy, 80);
            buffer.writeVarInt(order.bounty); buffer.writeVarInt(order.prestige);
            buffer.writeBoolean(order.rewards != null);
            if (order.rewards != null) { ItemContainerContents.STREAM_CODEC.encode(buffer, order.rewards); }
        }
    };

    /** A plain stack from {@code /give} or a quest reads as a Tier I order at base Bounty with no listed items. */
    public static final FulfilledWorkOrder BASE = new FulfilledWorkOrder(WorkOrder.MIN_TIER, "", "", WorkOrder.BASE_BOUNTY, 0, null);

    public FulfilledWorkOrder {
        tier = Math.clamp(tier, WorkOrder.MIN_TIER, WorkOrder.MAX_TIER);
        family = family == null ? "" : family;
        ascendancy = ascendancy == null ? "" : ascendancy;
        bounty = Math.max(0, bounty);
        prestige = Math.max(0, prestige);
    }
}
