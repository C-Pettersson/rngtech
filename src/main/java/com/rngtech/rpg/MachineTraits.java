package com.rngtech.rpg;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Rarity, Refinement Potential, modifiers and behaviors of a part, cell or machine. {@code corruption} is null unless a
 * Volatile Catalyst was applied; stored traits keep the corruption implicit only there, and effective traits from
 * {@link #withIdentity} also list it after the affixes so stat aggregation and behavior checks read it.
 *
 * <p>{@code retired} holds saved entries whose stat, slot, operation, rarity or behavior names no longer exist. They
 * never affect stats, are written back unchanged, and come back to life if the name returns. Saves carry no data
 * version. The first migration that cannot be inferred from the data adds a {@code version} int to this codec and to
 * {@link com.rngtech.rpg.progression.MachineProgressionState}.</p>
 */
public record MachineTraits(
        Rarity rarity,
        int refinementPotential,
        List<MachineModifier> modifiers,
        List<MachineBehavior> behaviors,
        MachineCorruption corruption,
        Retired retired
) {
    private static final int MAX_NETWORK_MODIFIERS = 32;
    private static final int MAX_NETWORK_BEHAVIORS = 16;

    public static final MachineTraits EMPTY = new MachineTraits(Rarity.NORMAL, 0, List.of(), List.of());

    /** Reads every entry it can and keeps the rest verbatim, so one unknown name never costs the other rolls. */
    public static final Codec<MachineTraits> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<MachineTraits, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getMap(input).flatMap(map -> {
                T rarityValue = map.get("rarity");
                T potentialValue = map.get("refinement_potential");
                T modifiersValue = map.get("modifiers");
                T behaviorsValue = map.get("behaviors");
                T corruptionValue = map.get("corruption");
                if (rarityValue == null || potentialValue == null || modifiersValue == null) {
                    return DataResult.error(() -> "Machine traits need rarity, refinement_potential and modifiers");
                }
                DataResult<Integer> potential = Codec.INT.parse(ops, potentialValue);
                DataResult<Stream<T>> modifierValues = ops.getStream(modifiersValue);
                DataResult<Stream<T>> behaviorValues = behaviorsValue == null
                        ? DataResult.success(Stream.empty())
                        : ops.getStream(behaviorsValue);
                return potential.flatMap(points -> modifierValues.flatMap(modifierStream ->
                        behaviorValues.map(behaviorStream -> Pair.of(
                                decoded(ops, rarityValue, points, modifierStream.toList(), behaviorStream.toList(), corruptionValue),
                                input
                        ))));
            });
        }

        private <T> MachineTraits decoded(
                DynamicOps<T> ops,
                T rarityValue,
                int potential,
                List<T> modifierValues,
                List<T> behaviorValues,
                T corruptionValue
        ) {
            Optional<Rarity> rarity = parseOrEmpty(Rarity.CODEC, ops, rarityValue);
            List<MachineModifier> modifiers = new ArrayList<>();
            List<Dynamic<?>> retiredModifiers = new ArrayList<>();
            for (T value : modifierValues) {
                Optional<MachineModifier> modifier = parseOrEmpty(MachineModifier.CODEC, ops, value);
                modifier.ifPresentOrElse(modifiers::add, () -> retiredModifiers.add(new Dynamic<>(ops, value)));
            }
            List<MachineBehavior> behaviors = new ArrayList<>();
            List<Dynamic<?>> retiredBehaviors = new ArrayList<>();
            for (T value : behaviorValues) {
                Optional<MachineBehavior> behavior = parseOrEmpty(MachineBehavior.CODEC, ops, value);
                behavior.ifPresentOrElse(behaviors::add, () -> retiredBehaviors.add(new Dynamic<>(ops, value)));
            }
            Optional<MachineCorruption> corruption = corruptionValue == null
                    ? Optional.empty()
                    : parseOrEmpty(MachineCorruption.CODEC, ops, corruptionValue);
            Retired retired = new Retired(
                    rarity.isPresent() ? Optional.empty() : Optional.of(new Dynamic<>(ops, rarityValue)),
                    retiredModifiers,
                    retiredBehaviors,
                    corruptionValue == null || corruption.isPresent()
                            ? Optional.empty()
                            : Optional.of(new Dynamic<>(ops, corruptionValue))
            );
            return new MachineTraits(rarity.orElse(Rarity.NORMAL), potential, modifiers, behaviors, corruption.orElse(null), retired);
        }

        @Override
        public <T> DataResult<T> encode(MachineTraits traits, DynamicOps<T> ops, T prefix) {
            RecordBuilder<T> builder = ops.mapBuilder();
            Retired retired = traits.retired();
            if (retired.rarity().isPresent() && traits.rarity() == Rarity.NORMAL) {
                builder.add("rarity", retired.rarity().get().convert(ops).getValue());
            } else {
                builder.add("rarity", Rarity.CODEC.encodeStart(ops, traits.rarity()));
            }
            builder.add("refinement_potential", Codec.INT.encodeStart(ops, traits.refinementPotential()));
            builder.add("modifiers", encodeList(ops, MachineModifier.CODEC, traits.modifiers(), retired.modifiers()));
            if (!traits.behaviors().isEmpty() || !retired.behaviors().isEmpty()) {
                builder.add("behaviors", encodeList(ops, MachineBehavior.CODEC, traits.behaviors(), retired.behaviors()));
            }
            if (traits.corruption() != null) {
                builder.add("corruption", MachineCorruption.CODEC.encodeStart(ops, traits.corruption()));
            } else if (retired.corruption().isPresent()) {
                builder.add("corruption", retired.corruption().get().convert(ops).getValue());
            }
            return builder.build(prefix);
        }

        @Override
        public String toString() {
            return "MachineTraits";
        }
    };

    private static <A, T> Optional<A> parseOrEmpty(Codec<A> codec, DynamicOps<T> ops, T value) {
        try {
            return codec.parse(ops, value).result();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static <A, T> DataResult<T> encodeList(DynamicOps<T> ops, Codec<A> codec, List<A> known, List<Dynamic<?>> retired) {
        ListBuilder<T> list = ops.listBuilder();
        known.forEach(value -> list.add(codec.encodeStart(ops, value)));
        retired.forEach(value -> list.add(value.convert(ops).getValue()));
        return list.build(ops.empty());
    }

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

    public MachineTraits(
            Rarity rarity,
            int refinementPotential,
            List<MachineModifier> modifiers,
            List<MachineBehavior> behaviors,
            MachineCorruption corruption
    ) {
        this(rarity, refinementPotential, modifiers, behaviors, corruption, Retired.NONE);
    }

    public MachineTraits {
        refinementPotential = Math.max(0, refinementPotential);
        modifiers = List.copyOf(modifiers);
        behaviors = List.copyOf(new LinkedHashSet<>(behaviors));
        retired = retired == null ? Retired.NONE : retired;
    }

    /** Saved entries that no current stat, slot, operation, rarity, behavior or corruption name matches. */
    public record Retired(
            Optional<Dynamic<?>> rarity,
            List<Dynamic<?>> modifiers,
            List<Dynamic<?>> behaviors,
            Optional<Dynamic<?>> corruption
    ) {
        public static final Retired NONE = new Retired(Optional.empty(), List.of(), List.of(), Optional.empty());

        public Retired {
            modifiers = List.copyOf(modifiers);
            behaviors = List.copyOf(behaviors);
        }

        public boolean isEmpty() {
            return rarity.isEmpty() && modifiers.isEmpty() && behaviors.isEmpty() && corruption.isEmpty();
        }
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
        return new MachineTraits(rarity, refinementPotential, modifiers, behaviors, nextCorruption, retired);
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
                && corruption == null
                && retired.isEmpty();
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

        boolean unique = identity.rarity() == Rarity.UNIQUE;
        Rarity rarity = unique ? Rarity.UNIQUE : stored.rarity();
        int refinementPotential = unique ? 0 : stored.refinementPotential();
        List<MachineModifier> mergedModifiers = new ArrayList<>(identity.modifiers());
        mergedModifiers.addAll(stored.modifiers().stream()
                .filter(modifier -> modifier.slot().isAffix() || unique && modifier.slot() == ModifierSlot.UNIQUE)
                .toList());
        List<MachineBehavior> mergedBehaviors = new ArrayList<>(identity.behaviors());
        mergedBehaviors.addAll(stored.behaviors());
        return withCorruptionEffects(new MachineTraits(
                rarity,
                refinementPotential,
                mergedModifiers,
                mergedBehaviors,
                stored.corruption(),
                stored.retired()
        ));
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
                traits.behaviors(),
                traits.corruption(),
                traits.retired()
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
        return new MachineTraits(
                traits.rarity(),
                traits.refinementPotential(),
                modifiers,
                behaviors,
                traits.corruption(),
                traits.retired()
        );
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

    private static boolean keepsStored(MachineTraits traits, MachineModifier modifier) {
        return modifier.slot().isAffix() || traits.rarity() == Rarity.UNIQUE && modifier.slot() == ModifierSlot.UNIQUE;
    }
}
