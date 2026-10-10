package com.rngtech.rpg;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Rarity, Refinement Potential, modifiers and behaviors of a part, cell or machine. {@code corruption} is null unless a
 * Volatile Catalyst was applied; stored traits keep the corruption implicit only there, and effective traits from
 * {@link #withIdentity} also list it after the affixes so stat aggregation and behavior checks read it.
 */
public record MachineTraits(
        Rarity rarity,
        int refinementPotential,
        List<MachineModifier> modifiers,
        List<MachineBehavior> behaviors,
        MachineCorruption corruption
) {
    private static final int MAX_NETWORK_MODIFIERS = 32;
    private static final int MAX_NETWORK_BEHAVIORS = 16;

    public static final MachineTraits EMPTY = new MachineTraits(Rarity.NORMAL, 0, List.of(), List.of());

    public static final Codec<MachineTraits> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Rarity.CODEC.fieldOf("rarity").forGetter(MachineTraits::rarity),
            Codec.INT.fieldOf("refinement_potential").forGetter(MachineTraits::refinementPotential),
            MachineModifier.CODEC.listOf().fieldOf("modifiers").forGetter(MachineTraits::modifiers),
            MachineBehavior.CODEC.listOf().optionalFieldOf("behaviors", List.of()).forGetter(MachineTraits::behaviors),
            MachineCorruption.CODEC.optionalFieldOf("corruption").forGetter(MachineTraits::corruptionOptional)
    ).apply(instance, MachineTraits::fromCodec));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineTraits> STREAM_CODEC = StreamCodec.composite(
            Rarity.STREAM_CODEC,
            MachineTraits::rarity,
            ByteBufCodecs.VAR_INT,
            MachineTraits::refinementPotential,
            MachineModifier.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_NETWORK_MODIFIERS)),
            MachineTraits::modifiers,
            MachineBehavior.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_NETWORK_BEHAVIORS)),
            MachineTraits::behaviors,
            ByteBufCodecs.optional(MachineCorruption.STREAM_CODEC),
            MachineTraits::corruptionOptional,
            MachineTraits::fromCodec
    );

    public MachineTraits(Rarity rarity, int refinementPotential, List<MachineModifier> modifiers) {
        this(rarity, refinementPotential, modifiers, List.of());
    }

    public MachineTraits(Rarity rarity, int refinementPotential, List<MachineModifier> modifiers, List<MachineBehavior> behaviors) {
        this(rarity, refinementPotential, modifiers, behaviors, null);
    }

    public MachineTraits {
        refinementPotential = Math.max(0, refinementPotential);
        modifiers = List.copyOf(modifiers);
        behaviors = List.copyOf(new LinkedHashSet<>(behaviors));
    }

    public ModifierSet modifierSet() {
        return new ModifierSet(rarity, refinementPotential, modifiers);
    }

    public boolean isCorrupted() {
        return corruption != null;
    }

    public Optional<MachineCorruption> corruptionOptional() {
        return Optional.ofNullable(corruption);
    }

    public MachineTraits withCorruption(MachineCorruption nextCorruption) {
        return new MachineTraits(rarity, refinementPotential, modifiers, behaviors, nextCorruption);
    }

    public boolean hasBehavior(MachineBehavior behavior) {
        return behaviors.contains(behavior)
                || modifiers.stream().anyMatch(modifier -> ModifierEligibilityProfiles.enablesBehavior(modifier, behavior));
    }

    public boolean isEmpty() {
        return refinementPotential == 0
                && modifiers.isEmpty()
                && behaviors.isEmpty()
                && rarity == Rarity.NORMAL
                && corruption == null;
    }

    /**
     * Modifiers that apply: the stored list without any stale corruption entries, then the corruption implicit. Use it
     * where stored traits feed stats directly instead of through {@link #withIdentity}.
     */
    public List<MachineModifier> activeModifiers() {
        return withCorruptionEffects(this).modifiers();
    }

    public static MachineTraits withIdentity(MachineTraits stored, MachineTraits identity) {
        if (identity.isEmpty()) {
            return withCorruptionEffects(stored);
        }

        Rarity rarity = identity.rarity() == Rarity.UNIQUE ? Rarity.UNIQUE : stored.rarity();
        int refinementPotential = identity.rarity() == Rarity.UNIQUE ? 0 : stored.refinementPotential();
        List<MachineModifier> mergedModifiers = new ArrayList<>(identity.modifiers());
        mergedModifiers.addAll(stored.modifiers().stream()
                .filter(modifier -> modifier.slot().isAffix())
                .toList());
        List<MachineBehavior> mergedBehaviors = new ArrayList<>(identity.behaviors());
        mergedBehaviors.addAll(stored.behaviors());
        return withCorruptionEffects(new MachineTraits(
                rarity,
                refinementPotential,
                mergedModifiers,
                mergedBehaviors,
                stored.corruption()
        ));
    }

    public static MachineTraits normalizedStored(MachineTraits traits) {
        if (traits.modifiers().stream().allMatch(modifier -> modifier.slot().isAffix())) {
            return traits;
        }
        return new MachineTraits(
                traits.rarity(),
                traits.refinementPotential(),
                traits.modifiers().stream()
                        .filter(modifier -> modifier.slot().isAffix())
                        .toList(),
                traits.behaviors(),
                traits.corruption()
        );
    }

    /** Lists the corruption implicit after the other modifiers and behaviors, replacing any earlier copy. */
    private static MachineTraits withCorruptionEffects(MachineTraits traits) {
        boolean hasStaleEntries = traits.modifiers().stream().anyMatch(modifier -> modifier.slot().isCorruption());
        if (traits.corruption() == null && !hasStaleEntries) {
            return traits;
        }
        List<MachineModifier> modifiers = new ArrayList<>(traits.modifiers().stream()
                .filter(modifier -> !modifier.slot().isCorruption())
                .toList());
        List<MachineBehavior> behaviors = new ArrayList<>(traits.behaviors());
        if (traits.corruption() != null) {
            modifiers.addAll(traits.corruption().modifiers());
            behaviors.addAll(traits.corruption().behaviors());
        }
        if (modifiers.equals(traits.modifiers()) && new LinkedHashSet<>(behaviors).equals(new LinkedHashSet<>(traits.behaviors()))) {
            return traits;
        }
        return new MachineTraits(traits.rarity(), traits.refinementPotential(), modifiers, behaviors, traits.corruption());
    }

    private static MachineTraits fromCodec(
            Rarity rarity,
            int refinementPotential,
            List<MachineModifier> modifiers,
            List<MachineBehavior> behaviors,
            Optional<MachineCorruption> corruption
    ) {
        return new MachineTraits(rarity, refinementPotential, modifiers, behaviors, corruption.orElse(null));
    }
}
