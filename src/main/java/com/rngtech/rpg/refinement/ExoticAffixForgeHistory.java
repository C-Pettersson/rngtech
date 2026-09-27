package com.rngtech.rpg.refinement;

import com.rngtech.content.registry.ModDataComponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

public record ExoticAffixForgeHistory(int totalUses, Map<ExoticAffixForgeAction, Integer> actionUses) {
    public static final ExoticAffixForgeHistory EMPTY = new ExoticAffixForgeHistory(0, Map.of());

    public static final Codec<ExoticAffixForgeHistory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("total_uses", 0).forGetter(ExoticAffixForgeHistory::totalUses),
            Codec.unboundedMap(ExoticAffixForgeAction.CODEC, Codec.INT)
                    .optionalFieldOf("action_uses", Map.of())
                    .forGetter(ExoticAffixForgeHistory::actionUses)
    ).apply(instance, ExoticAffixForgeHistory::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExoticAffixForgeHistory> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public ExoticAffixForgeHistory decode(RegistryFriendlyByteBuf buffer) {
                    int totalUses = buffer.readVarInt();
                    EnumMap<ExoticAffixForgeAction, Integer> actionUses = new EnumMap<>(ExoticAffixForgeAction.class);
                    for (ExoticAffixForgeAction action : ExoticAffixForgeAction.values()) {
                        int uses = buffer.readVarInt();
                        if (uses > 0) {
                            actionUses.put(action, uses);
                        }
                    }
                    return new ExoticAffixForgeHistory(totalUses, actionUses);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, ExoticAffixForgeHistory history) {
                    buffer.writeVarInt(history.totalUses());
                    for (ExoticAffixForgeAction action : ExoticAffixForgeAction.values()) {
                        buffer.writeVarInt(history.uses(action));
                    }
                }
            };

    public ExoticAffixForgeHistory {
        totalUses = Math.max(0, totalUses);
        EnumMap<ExoticAffixForgeAction, Integer> normalized = new EnumMap<>(ExoticAffixForgeAction.class);
        if (actionUses != null) {
            actionUses.forEach((action, uses) -> {
                if (action != null && uses != null && uses > 0) {
                    normalized.put(action, uses);
                }
            });
        }
        actionUses = Map.copyOf(normalized);
    }

    public static ExoticAffixForgeHistory get(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.EXOTIC_AFFIX_FORGE_HISTORY.get(), EMPTY);
    }

    public int uses(ExoticAffixForgeAction action) {
        return actionUses.getOrDefault(action, 0);
    }

    public ExoticAffixForgeHistory increment(ExoticAffixForgeAction action) {
        EnumMap<ExoticAffixForgeAction, Integer> updated = new EnumMap<>(ExoticAffixForgeAction.class);
        updated.putAll(actionUses);
        updated.put(action, uses(action) + 1);
        return new ExoticAffixForgeHistory(totalUses + 1, updated);
    }
}
