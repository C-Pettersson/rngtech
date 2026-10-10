package com.rngtech.rpg;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public record MachineTraits(
        Rarity rarity,
        int refinementPotential,
        List<MachineModifier> modifiers,
        List<MachineBehavior> behaviors
) {
    private static final int MAX_NETWORK_MODIFIERS = 32;
    private static final int MAX_NETWORK_BEHAVIORS = 16;

    public static final MachineTraits EMPTY = new MachineTraits(Rarity.NORMAL, 0, List.of(), List.of());

    public static final Codec<MachineTraits> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Rarity.CODEC.fieldOf("rarity").forGetter(MachineTraits::rarity),
            Codec.INT.fieldOf("refinement_potential").forGetter(MachineTraits::refinementPotential),
            MachineModifier.CODEC.listOf().fieldOf("modifiers").forGetter(MachineTraits::modifiers),
            MachineBehavior.CODEC.listOf().optionalFieldOf("behaviors", List.of()).forGetter(MachineTraits::behaviors)
    ).apply(instance, MachineTraits::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineTraits> STREAM_CODEC = StreamCodec.composite(
            Rarity.STREAM_CODEC,
            MachineTraits::rarity,
            ByteBufCodecs.VAR_INT,
            MachineTraits::refinementPotential,
            MachineModifier.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_NETWORK_MODIFIERS)),
            MachineTraits::modifiers,
            MachineBehavior.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_NETWORK_BEHAVIORS)),
            MachineTraits::behaviors,
            MachineTraits::new
    );

    public MachineTraits(Rarity rarity, int refinementPotential, List<MachineModifier> modifiers) {
        this(rarity, refinementPotential, modifiers, List.of());
    }

    public MachineTraits {
        refinementPotential = Math.max(0, refinementPotential);
        modifiers = List.copyOf(modifiers);
        behaviors = List.copyOf(new LinkedHashSet<>(behaviors));
    }

    public ModifierSet modifierSet() {
        return new ModifierSet(rarity, refinementPotential, modifiers);
    }

    public boolean hasBehavior(MachineBehavior behavior) {
        return behaviors.contains(behavior)
                || modifiers.stream().anyMatch(modifier -> ModifierEligibilityProfiles.enablesBehavior(modifier, behavior));
    }

    public boolean isEmpty() {
        return refinementPotential == 0 && modifiers.isEmpty() && behaviors.isEmpty() && rarity == Rarity.NORMAL;
    }

    public static MachineTraits withIdentity(MachineTraits stored, MachineTraits identity) {
        if (identity.isEmpty()) {
            return stored;
        }

        boolean unique = identity.rarity() == Rarity.UNIQUE;
        Rarity rarity = unique ? Rarity.UNIQUE : stored.rarity();
        int refinementPotential = unique ? 0 : stored.refinementPotential();
        List<MachineModifier> mergedModifiers = new ArrayList<>(identity.modifiers());
        mergedModifiers.addAll(stored.modifiers().stream()
                .filter(modifier -> modifier.slot().isAffix() || unique && modifier.slot() == ModifierSlot.UNIQUE)
                .toList());
        List<MachineBehavior> mergedBehaviors = new ArrayList<>(identity.behaviors());
        mergedBehaviors.addAll(stored.behaviors());
        return new MachineTraits(rarity, refinementPotential, mergedModifiers, mergedBehaviors);
    }

    /** Stored traits keep affixes, and a Unique's rolls only while the stored rarity is Unique. */
    public static MachineTraits normalizedStored(MachineTraits traits) {
        if (traits.modifiers().stream().allMatch(modifier -> keepsStored(traits, modifier))) {
            return traits;
        }
        return new MachineTraits(
                traits.rarity(),
                traits.refinementPotential(),
                traits.modifiers().stream()
                        .filter(modifier -> keepsStored(traits, modifier))
                        .toList(),
                traits.behaviors()
        );
    }

    private static boolean keepsStored(MachineTraits traits, MachineModifier modifier) {
        return modifier.slot().isAffix() || traits.rarity() == Rarity.UNIQUE && modifier.slot() == ModifierSlot.UNIQUE;
    }
}
