package com.rngtech.rpg;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * A Volatile Catalyst result. Its presence on {@link MachineTraits} marks the target Corrupted; the modifiers and
 * behaviors are the corruption implicit, empty for Untouched and Reforged. Modifiers are stored in the
 * {@link ModifierSlot#CORRUPTION} slot with one fixed value each.
 */
public record MachineCorruption(
        CorruptionOutcome outcome,
        List<MachineModifier> modifiers,
        List<MachineBehavior> behaviors
) {
    private static final int MAX_NETWORK_MODIFIERS = 8;
    private static final int MAX_NETWORK_BEHAVIORS = 8;

    public static final Codec<MachineCorruption> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CorruptionOutcome.CODEC.fieldOf("outcome").forGetter(MachineCorruption::outcome),
            MachineModifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(MachineCorruption::modifiers),
            MachineBehavior.CODEC.listOf().optionalFieldOf("behaviors", List.of()).forGetter(MachineCorruption::behaviors)
    ).apply(instance, MachineCorruption::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineCorruption> STREAM_CODEC = StreamCodec.composite(
            CorruptionOutcome.STREAM_CODEC,
            MachineCorruption::outcome,
            MachineModifier.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_NETWORK_MODIFIERS)),
            MachineCorruption::modifiers,
            MachineBehavior.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_NETWORK_BEHAVIORS)),
            MachineCorruption::behaviors,
            MachineCorruption::new
    );

    public MachineCorruption {
        Objects.requireNonNull(outcome, "outcome");
        modifiers = modifiers.stream().map(MachineCorruption::inCorruptionSlot).toList();
        behaviors = List.copyOf(new LinkedHashSet<>(behaviors));
    }

    public static MachineCorruption of(CorruptionOutcome outcome) {
        return new MachineCorruption(outcome, List.of(), List.of());
    }

    public boolean hasImplicit() {
        return !modifiers.isEmpty() || !behaviors.isEmpty();
    }

    private static MachineModifier inCorruptionSlot(MachineModifier modifier) {
        if (modifier.slot() == ModifierSlot.CORRUPTION) {
            return modifier;
        }
        return new MachineModifier(
                modifier.affixId(),
                modifier.modGroup(),
                ModifierSlot.CORRUPTION,
                modifier.stat(),
                modifier.operation(),
                0,
                ModifierValueRange.fixed(modifier.value()),
                modifier.value(),
                modifier.effects()
        );
    }
}
