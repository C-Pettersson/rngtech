package com.rngtech.rpg.workorder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.Optional;

/**
 * A Work Order as its item shows it: the machine family it is for, its tier and job, what a machine needs to load it,
 * its Bounty and Prestige, and the exact items it pays. Rarity and conditions live in the item's traits beside it.
 * {@code minMasteryLevel} 0 means no Mastery level is needed. Bounty and Prestige set how big a reward the board rolls
 * when it rolls the order; {@code rewards} is that roll, or null for an order without a list, which rolls the reward
 * table when it is opened. The rewards codec is lazy, so orders without a list never touch item classes.
 */
public record WorkOrder(String family, int tier, WorkObjective objective, int minStage, int minMasteryLevel, int bounty, int prestige,
        ItemContainerContents rewards) {
    public static final int MIN_TIER = 1;
    public static final int MAX_TIER = 5;
    public static final int BASE_BOUNTY = 100;
    private static final int[] TIER_STAGE = {4, 5, 6, 7, 8};

    public static final Codec<WorkOrder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("family").forGetter(WorkOrder::family),
            Codec.INT.fieldOf("tier").forGetter(WorkOrder::tier),
            WorkObjective.CODEC.fieldOf("objective").forGetter(WorkOrder::objective),
            Codec.INT.optionalFieldOf("min_stage", 0).forGetter(WorkOrder::minStage),
            Codec.INT.optionalFieldOf("min_mastery_level", 0).forGetter(WorkOrder::minMasteryLevel),
            Codec.INT.optionalFieldOf("bounty", BASE_BOUNTY).forGetter(WorkOrder::bounty),
            Codec.INT.optionalFieldOf("prestige", 0).forGetter(WorkOrder::prestige),
            Codec.lazyInitialized(() -> ItemContainerContents.CODEC).optionalFieldOf("rewards").forGetter(order -> Optional.ofNullable(order.rewards))
    ).apply(instance, (family, tier, objective, minStage, minMasteryLevel, bounty, prestige, rewards) ->
            new WorkOrder(family, tier, objective, minStage, minMasteryLevel, bounty, prestige, rewards.orElse(null))));

    public static final StreamCodec<RegistryFriendlyByteBuf, WorkOrder> STREAM_CODEC = new StreamCodec<>() {
        @Override public WorkOrder decode(RegistryFriendlyByteBuf buffer) {
            return new WorkOrder(buffer.readUtf(64), buffer.readVarInt(), WorkObjective.STREAM_CODEC.decode(buffer),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readBoolean() ? ItemContainerContents.STREAM_CODEC.decode(buffer) : null);
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, WorkOrder order) {
            buffer.writeUtf(order.family, 64); buffer.writeVarInt(order.tier); WorkObjective.STREAM_CODEC.encode(buffer, order.objective);
            buffer.writeVarInt(order.minStage); buffer.writeVarInt(order.minMasteryLevel);
            buffer.writeVarInt(order.bounty); buffer.writeVarInt(order.prestige);
            buffer.writeBoolean(order.rewards != null);
            if (order.rewards != null) { ItemContainerContents.STREAM_CODEC.encode(buffer, order.rewards); }
        }
    };

    public WorkOrder {
        family = family == null ? "" : family;
        tier = Mth.clamp(tier, MIN_TIER, MAX_TIER);
        minStage = Math.max(0, minStage);
        minMasteryLevel = Math.max(0, minMasteryLevel);
        bounty = Math.max(0, bounty);
        prestige = Math.max(0, prestige);
    }

    public WorkOrder(String family, int tier, WorkObjective objective, int minStage, int minMasteryLevel, int bounty, int prestige) {
        this(family, tier, objective, minStage, minMasteryLevel, bounty, prestige, null);
    }

    public WorkOrder withRewards(ItemContainerContents value) {
        return new WorkOrder(family, tier, objective, minStage, minMasteryLevel, bounty, prestige, value);
    }

    /** The tier's chassis stage before any family's top stage caps it. */
    public static int tierStage(int tier) {
        return TIER_STAGE[Mth.clamp(tier, MIN_TIER, MAX_TIER) - 1];
    }
}
