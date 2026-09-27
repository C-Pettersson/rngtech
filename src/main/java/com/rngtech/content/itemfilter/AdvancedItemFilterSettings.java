package com.rngtech.content.itemfilter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record AdvancedItemFilterSettings(
        Mode mode,
        boolean exactItemsEnabled,
        boolean tagEnabled,
        boolean namespaceEnabled,
        boolean stageEnabled,
        boolean stabilityEnabled,
        boolean strictComponents,
        IdentityMode identityMode,
        RarityMode rarityMode,
        int minStage,
        int maxStage,
        int minStability,
        int maxStability,
        List<ItemStack> samples,
        List<ResourceLocation> tags,
        List<String> namespaces
) {
    public static final int SAMPLE_SLOT_COUNT = 9;
    public static final int MAX_TAGS = 4;
    public static final int MAX_NAMESPACES = 4;
    public static final int MIN_STAGE = 0;
    public static final int MAX_STAGE = 8;
    public static final int MIN_STABILITY = 0;
    public static final int MAX_STABILITY = 200;

    public static final AdvancedItemFilterSettings EMPTY = new AdvancedItemFilterSettings(
            Mode.ALLOW,
            true,
            false,
            false,
            false,
            false,
            false,
            IdentityMode.ANY,
            RarityMode.ANY,
            MIN_STAGE,
            MAX_STAGE,
            100,
            200,
            emptySamples(),
            List.of(),
            List.of()
    );

    public static final Codec<AdvancedItemFilterSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Mode.CODEC.optionalFieldOf("mode", Mode.ALLOW).forGetter(AdvancedItemFilterSettings::mode),
            Codec.BOOL.optionalFieldOf("exact_items", true).forGetter(AdvancedItemFilterSettings::exactItemsEnabled),
            Codec.BOOL.optionalFieldOf("tags_enabled", false).forGetter(AdvancedItemFilterSettings::tagEnabled),
            Codec.BOOL.optionalFieldOf("namespaces_enabled", false).forGetter(AdvancedItemFilterSettings::namespaceEnabled),
            Codec.BOOL.optionalFieldOf("stage_enabled", false).forGetter(AdvancedItemFilterSettings::stageEnabled),
            Codec.BOOL.optionalFieldOf("stability_enabled", false).forGetter(AdvancedItemFilterSettings::stabilityEnabled),
            Codec.BOOL.optionalFieldOf("strict_components", false).forGetter(AdvancedItemFilterSettings::strictComponents),
            IdentityMode.CODEC.optionalFieldOf("identity", IdentityMode.ANY).forGetter(AdvancedItemFilterSettings::identityMode),
            RarityMode.CODEC.optionalFieldOf("rarity", RarityMode.ANY).forGetter(AdvancedItemFilterSettings::rarityMode),
            Codec.INT.optionalFieldOf("min_stage", MIN_STAGE).forGetter(AdvancedItemFilterSettings::minStage),
            Codec.INT.optionalFieldOf("max_stage", MAX_STAGE).forGetter(AdvancedItemFilterSettings::maxStage),
            Codec.INT.optionalFieldOf("min_stability", 100).forGetter(AdvancedItemFilterSettings::minStability),
            Codec.INT.optionalFieldOf("max_stability", MAX_STABILITY).forGetter(AdvancedItemFilterSettings::maxStability),
            ItemStack.OPTIONAL_CODEC.listOf()
                    .optionalFieldOf("samples", emptySamples())
                    .forGetter(AdvancedItemFilterSettings::samples),
            ResourceLocation.CODEC.listOf()
                    .optionalFieldOf("tags", List.of())
                    .forGetter(AdvancedItemFilterSettings::tags),
            Codec.STRING.listOf()
                    .optionalFieldOf("namespaces", List.of())
                    .forGetter(AdvancedItemFilterSettings::namespaces)
    ).apply(instance, AdvancedItemFilterSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdvancedItemFilterSettings> STREAM_CODEC =
            StreamCodec.ofMember(AdvancedItemFilterSettings::write, AdvancedItemFilterSettings::read);

    public AdvancedItemFilterSettings {
        mode = mode == null ? Mode.ALLOW : mode;
        identityMode = identityMode == null ? IdentityMode.ANY : identityMode;
        rarityMode = rarityMode == null ? RarityMode.ANY : rarityMode;
        minStage = Mth.clamp(minStage, MIN_STAGE, MAX_STAGE);
        maxStage = Mth.clamp(maxStage, MIN_STAGE, MAX_STAGE);
        if (minStage > maxStage) {
            int swap = minStage;
            minStage = maxStage;
            maxStage = swap;
        }
        minStability = Mth.clamp(minStability, MIN_STABILITY, MAX_STABILITY);
        maxStability = Mth.clamp(maxStability, MIN_STABILITY, MAX_STABILITY);
        if (minStability > maxStability) {
            int swap = minStability;
            minStability = maxStability;
            maxStability = swap;
        }
        samples = normalizeSamples(samples);
        tags = normalizeTags(tags);
        namespaces = normalizeNamespaces(namespaces);
    }

    public boolean hasActiveCriteria() {
        return exactItemsEnabled
                || tagEnabled
                || namespaceEnabled
                || stageEnabled
                || stabilityEnabled
                || identityMode != IdentityMode.ANY
                || rarityMode != RarityMode.ANY;
    }

    public boolean hasSample() {
        return samples.stream().anyMatch(stack -> !stack.isEmpty());
    }

    public boolean hasTag() {
        return !tags.isEmpty();
    }

    public boolean hasNamespace() {
        return !namespaces.isEmpty();
    }

    public AdvancedItemFilterSettings withMode(Mode nextMode) {
        return new AdvancedItemFilterSettings(
                nextMode,
                exactItemsEnabled,
                tagEnabled,
                namespaceEnabled,
                stageEnabled,
                stabilityEnabled,
                strictComponents,
                identityMode,
                rarityMode,
                minStage,
                maxStage,
                minStability,
                maxStability,
                samples,
                tags,
                namespaces
        );
    }

    public AdvancedItemFilterSettings withExactItemsEnabled(boolean enabled) {
        return copy(enabled, tagEnabled, namespaceEnabled, stageEnabled, stabilityEnabled, strictComponents);
    }

    public AdvancedItemFilterSettings withTagEnabled(boolean enabled) {
        return copy(exactItemsEnabled, enabled, namespaceEnabled, stageEnabled, stabilityEnabled, strictComponents);
    }

    public AdvancedItemFilterSettings withNamespaceEnabled(boolean enabled) {
        return copy(exactItemsEnabled, tagEnabled, enabled, stageEnabled, stabilityEnabled, strictComponents);
    }

    public AdvancedItemFilterSettings withStageEnabled(boolean enabled) {
        return copy(exactItemsEnabled, tagEnabled, namespaceEnabled, enabled, stabilityEnabled, strictComponents);
    }

    public AdvancedItemFilterSettings withStabilityEnabled(boolean enabled) {
        return copy(exactItemsEnabled, tagEnabled, namespaceEnabled, stageEnabled, enabled, strictComponents);
    }

    public AdvancedItemFilterSettings withStrictComponents(boolean enabled) {
        return copy(exactItemsEnabled, tagEnabled, namespaceEnabled, stageEnabled, stabilityEnabled, enabled);
    }

    public AdvancedItemFilterSettings withIdentityMode(IdentityMode nextIdentityMode) {
        return new AdvancedItemFilterSettings(
                mode,
                exactItemsEnabled,
                tagEnabled,
                namespaceEnabled,
                stageEnabled,
                stabilityEnabled,
                strictComponents,
                nextIdentityMode,
                rarityMode,
                minStage,
                maxStage,
                minStability,
                maxStability,
                samples,
                tags,
                namespaces
        );
    }

    public AdvancedItemFilterSettings withRarityMode(RarityMode nextRarityMode) {
        return new AdvancedItemFilterSettings(
                mode,
                exactItemsEnabled,
                tagEnabled,
                namespaceEnabled,
                stageEnabled,
                stabilityEnabled,
                strictComponents,
                identityMode,
                nextRarityMode,
                minStage,
                maxStage,
                minStability,
                maxStability,
                samples,
                tags,
                namespaces
        );
    }

    public AdvancedItemFilterSettings withStageRange(int nextMinStage, int nextMaxStage) {
        return new AdvancedItemFilterSettings(
                mode,
                exactItemsEnabled,
                tagEnabled,
                namespaceEnabled,
                stageEnabled,
                stabilityEnabled,
                strictComponents,
                identityMode,
                rarityMode,
                nextMinStage,
                nextMaxStage,
                minStability,
                maxStability,
                samples,
                tags,
                namespaces
        );
    }

    public AdvancedItemFilterSettings withStabilityRange(int nextMinStability, int nextMaxStability) {
        return new AdvancedItemFilterSettings(
                mode,
                exactItemsEnabled,
                tagEnabled,
                namespaceEnabled,
                stageEnabled,
                stabilityEnabled,
                strictComponents,
                identityMode,
                rarityMode,
                minStage,
                maxStage,
                nextMinStability,
                nextMaxStability,
                samples,
                tags,
                namespaces
        );
    }

    public AdvancedItemFilterSettings withSample(int index, ItemStack stack) {
        if (index < 0 || index >= SAMPLE_SLOT_COUNT) {
            return this;
        }
        List<ItemStack> nextSamples = new ArrayList<>(samples);
        nextSamples.set(index, normalizeSample(stack));
        return withSamples(nextSamples);
    }

    public AdvancedItemFilterSettings withSamples(List<ItemStack> nextSamples) {
        return new AdvancedItemFilterSettings(
                mode,
                exactItemsEnabled,
                tagEnabled,
                namespaceEnabled,
                stageEnabled,
                stabilityEnabled,
                strictComponents,
                identityMode,
                rarityMode,
                minStage,
                maxStage,
                minStability,
                maxStability,
                nextSamples,
                tags,
                namespaces
        );
    }

    public AdvancedItemFilterSettings withTags(List<ResourceLocation> nextTags) {
        return new AdvancedItemFilterSettings(
                mode,
                exactItemsEnabled,
                tagEnabled,
                namespaceEnabled,
                stageEnabled,
                stabilityEnabled,
                strictComponents,
                identityMode,
                rarityMode,
                minStage,
                maxStage,
                minStability,
                maxStability,
                samples,
                nextTags,
                namespaces
        );
    }

    public AdvancedItemFilterSettings withNamespaces(List<String> nextNamespaces) {
        return new AdvancedItemFilterSettings(
                mode,
                exactItemsEnabled,
                tagEnabled,
                namespaceEnabled,
                stageEnabled,
                stabilityEnabled,
                strictComponents,
                identityMode,
                rarityMode,
                minStage,
                maxStage,
                minStability,
                maxStability,
                samples,
                tags,
                nextNamespaces
        );
    }

    public AdvancedItemFilterSettings cycleTagSlot(int slot, List<ResourceLocation> candidates) {
        if (slot < 0 || slot >= MAX_TAGS) {
            return this;
        }
        List<ResourceLocation> normalizedCandidates = normalizeTags(candidates, Integer.MAX_VALUE);
        if (normalizedCandidates.isEmpty()) {
            return this;
        }
        List<ResourceLocation> nextTags = editableTags();
        ResourceLocation current = slot < nextTags.size() ? nextTags.get(slot) : null;
        int currentIndex = current == null ? -1 : normalizedCandidates.indexOf(current);
        ResourceLocation next = normalizedCandidates.get(Math.floorMod(currentIndex + 1, normalizedCandidates.size()));
        while (nextTags.size() <= slot) {
            nextTags.add(null);
        }
        nextTags.set(slot, next);
        return withTags(nextTags);
    }

    public AdvancedItemFilterSettings clearTagSlot(int slot) {
        if (slot < 0 || slot >= MAX_TAGS || slot >= tags.size()) {
            return this;
        }
        List<ResourceLocation> nextTags = editableTags();
        nextTags.remove(slot);
        return withTags(nextTags);
    }

    public AdvancedItemFilterSettings cycleNamespaceSlot(int slot, List<String> candidates) {
        if (slot < 0 || slot >= MAX_NAMESPACES) {
            return this;
        }
        List<String> normalizedCandidates = normalizeNamespaces(candidates, Integer.MAX_VALUE);
        if (normalizedCandidates.isEmpty()) {
            return this;
        }
        List<String> nextNamespaces = editableNamespaces();
        String current = slot < nextNamespaces.size() ? nextNamespaces.get(slot) : null;
        int currentIndex = current == null ? -1 : normalizedCandidates.indexOf(current);
        String next = normalizedCandidates.get(Math.floorMod(currentIndex + 1, normalizedCandidates.size()));
        while (nextNamespaces.size() <= slot) {
            nextNamespaces.add(null);
        }
        nextNamespaces.set(slot, next);
        return withNamespaces(nextNamespaces);
    }

    public AdvancedItemFilterSettings clearNamespaceSlot(int slot) {
        if (slot < 0 || slot >= MAX_NAMESPACES || slot >= namespaces.size()) {
            return this;
        }
        List<String> nextNamespaces = editableNamespaces();
        nextNamespaces.remove(slot);
        return withNamespaces(nextNamespaces);
    }

    private AdvancedItemFilterSettings copy(
            boolean nextExact,
            boolean nextTags,
            boolean nextNamespaces,
            boolean nextStage,
            boolean nextStability,
            boolean nextStrict
    ) {
        return new AdvancedItemFilterSettings(
                mode,
                nextExact,
                nextTags,
                nextNamespaces,
                nextStage,
                nextStability,
                nextStrict,
                identityMode,
                rarityMode,
                minStage,
                maxStage,
                minStability,
                maxStability,
                samples,
                tags,
                namespaces
        );
    }

    private List<ResourceLocation> editableTags() {
        List<ResourceLocation> next = new ArrayList<>(tags);
        next.removeIf(location -> location == null);
        return next;
    }

    private List<String> editableNamespaces() {
        List<String> next = new ArrayList<>(namespaces);
        next.removeIf(namespace -> namespace == null || namespace.isBlank());
        return next;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        ByteBufCodecs.VAR_INT.encode(buffer, mode.ordinal());
        ByteBufCodecs.BOOL.encode(buffer, exactItemsEnabled);
        ByteBufCodecs.BOOL.encode(buffer, tagEnabled);
        ByteBufCodecs.BOOL.encode(buffer, namespaceEnabled);
        ByteBufCodecs.BOOL.encode(buffer, stageEnabled);
        ByteBufCodecs.BOOL.encode(buffer, stabilityEnabled);
        ByteBufCodecs.BOOL.encode(buffer, strictComponents);
        ByteBufCodecs.VAR_INT.encode(buffer, identityMode.ordinal());
        ByteBufCodecs.VAR_INT.encode(buffer, rarityMode.ordinal());
        ByteBufCodecs.VAR_INT.encode(buffer, minStage);
        ByteBufCodecs.VAR_INT.encode(buffer, maxStage);
        ByteBufCodecs.VAR_INT.encode(buffer, minStability);
        ByteBufCodecs.VAR_INT.encode(buffer, maxStability);
        writeSamples(buffer, samples);
        writeTags(buffer, tags);
        writeNamespaces(buffer, namespaces);
    }

    private static AdvancedItemFilterSettings read(RegistryFriendlyByteBuf buffer) {
        return new AdvancedItemFilterSettings(
                Mode.byId(ByteBufCodecs.VAR_INT.decode(buffer)),
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer),
                IdentityMode.byId(ByteBufCodecs.VAR_INT.decode(buffer)),
                RarityMode.byId(ByteBufCodecs.VAR_INT.decode(buffer)),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                readSamples(buffer),
                readTags(buffer),
                readNamespaces(buffer)
        );
    }

    private static void writeSamples(RegistryFriendlyByteBuf buffer, List<ItemStack> samples) {
        List<ItemStack> normalized = normalizeSamples(samples);
        ByteBufCodecs.VAR_INT.encode(buffer, normalized.size());
        for (ItemStack sample : normalized) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, sample);
        }
    }

    private static List<ItemStack> readSamples(RegistryFriendlyByteBuf buffer) {
        int count = ByteBufCodecs.VAR_INT.decode(buffer);
        List<ItemStack> decoded = new ArrayList<>(Math.min(count, SAMPLE_SLOT_COUNT));
        for (int index = 0; index < count; index++) {
            ItemStack sample = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            if (index < SAMPLE_SLOT_COUNT) {
                decoded.add(sample);
            }
        }
        return normalizeSamples(decoded);
    }

    private static void writeTags(RegistryFriendlyByteBuf buffer, List<ResourceLocation> tags) {
        List<ResourceLocation> normalized = normalizeTags(tags);
        ByteBufCodecs.VAR_INT.encode(buffer, normalized.size());
        for (ResourceLocation tag : normalized) {
            ResourceLocation.STREAM_CODEC.encode(buffer, tag);
        }
    }

    private static List<ResourceLocation> readTags(RegistryFriendlyByteBuf buffer) {
        int count = ByteBufCodecs.VAR_INT.decode(buffer);
        List<ResourceLocation> decoded = new ArrayList<>(Math.min(count, MAX_TAGS));
        for (int index = 0; index < count; index++) {
            ResourceLocation tag = ResourceLocation.STREAM_CODEC.decode(buffer);
            if (index < MAX_TAGS) {
                decoded.add(tag);
            }
        }
        return normalizeTags(decoded);
    }

    private static void writeNamespaces(RegistryFriendlyByteBuf buffer, List<String> namespaces) {
        List<String> normalized = normalizeNamespaces(namespaces);
        ByteBufCodecs.VAR_INT.encode(buffer, normalized.size());
        for (String namespace : normalized) {
            ByteBufCodecs.STRING_UTF8.encode(buffer, namespace);
        }
    }

    private static List<String> readNamespaces(RegistryFriendlyByteBuf buffer) {
        int count = ByteBufCodecs.VAR_INT.decode(buffer);
        List<String> decoded = new ArrayList<>(Math.min(count, MAX_NAMESPACES));
        for (int index = 0; index < count; index++) {
            String namespace = ByteBufCodecs.STRING_UTF8.decode(buffer);
            if (index < MAX_NAMESPACES) {
                decoded.add(namespace);
            }
        }
        return normalizeNamespaces(decoded);
    }

    public static List<ItemStack> emptySamples() {
        List<ItemStack> samples = new ArrayList<>(SAMPLE_SLOT_COUNT);
        for (int index = 0; index < SAMPLE_SLOT_COUNT; index++) {
            samples.add(ItemStack.EMPTY);
        }
        return List.copyOf(samples);
    }

    private static List<ItemStack> normalizeSamples(List<ItemStack> samples) {
        List<ItemStack> normalized = new ArrayList<>(SAMPLE_SLOT_COUNT);
        if (samples != null) {
            for (ItemStack sample : samples) {
                if (normalized.size() >= SAMPLE_SLOT_COUNT) {
                    break;
                }
                normalized.add(normalizeSample(sample));
            }
        }
        while (normalized.size() < SAMPLE_SLOT_COUNT) {
            normalized.add(ItemStack.EMPTY);
        }
        return List.copyOf(normalized);
    }

    private static ItemStack normalizeSample(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    private static List<ResourceLocation> normalizeTags(List<ResourceLocation> tags) {
        return normalizeTags(tags, MAX_TAGS);
    }

    private static List<ResourceLocation> normalizeTags(List<ResourceLocation> tags, int maxTags) {
        Set<ResourceLocation> unique = new LinkedHashSet<>();
        if (tags != null) {
            for (ResourceLocation tag : tags) {
                if (tag != null) {
                    unique.add(tag);
                }
            }
        }
        return unique.stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .limit(maxTags)
                .toList();
    }

    private static List<String> normalizeNamespaces(List<String> namespaces) {
        return normalizeNamespaces(namespaces, MAX_NAMESPACES);
    }

    private static List<String> normalizeNamespaces(List<String> namespaces, int maxNamespaces) {
        Set<String> unique = new LinkedHashSet<>();
        if (namespaces != null) {
            for (String namespace : namespaces) {
                if (namespace != null && !namespace.isBlank()) {
                    unique.add(namespace.toLowerCase(Locale.ROOT));
                }
            }
        }
        return unique.stream()
                .sorted()
                .limit(maxNamespaces)
                .toList();
    }

    public enum Mode implements StringRepresentable {
        ALLOW,
        DENY;

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);
        private static final Mode[] VALUES = values();
        private final String serializedName;

        Mode() {
            serializedName = name().toLowerCase(Locale.ROOT);
        }

        public static Mode byId(int id) {
            return VALUES[Math.floorMod(id, VALUES.length)];
        }

        public Mode next() {
            return byId(ordinal() + 1);
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    public enum IdentityMode implements StringRepresentable {
        ANY,
        IDENTIFIED,
        UNIDENTIFIED;

        public static final Codec<IdentityMode> CODEC = StringRepresentable.fromEnum(IdentityMode::values);
        private static final IdentityMode[] VALUES = values();
        private final String serializedName;

        IdentityMode() {
            serializedName = name().toLowerCase(Locale.ROOT);
        }

        public static IdentityMode byId(int id) {
            return VALUES[Math.floorMod(id, VALUES.length)];
        }

        public IdentityMode next() {
            return byId(ordinal() + 1);
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    public enum RarityMode implements StringRepresentable {
        ANY,
        NORMAL,
        MAGIC,
        RARE,
        UNIQUE;

        public static final Codec<RarityMode> CODEC = StringRepresentable.fromEnum(RarityMode::values);
        private static final RarityMode[] VALUES = values();
        private final String serializedName;

        RarityMode() {
            serializedName = name().toLowerCase(Locale.ROOT);
        }

        public static RarityMode byId(int id) {
            return VALUES[Math.floorMod(id, VALUES.length)];
        }

        public RarityMode next() {
            return byId(ordinal() + 1);
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
