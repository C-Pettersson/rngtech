package com.rngtech.content.affixsorter;

import com.rngtech.rpg.Rarity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * One Affix Sorter rule. An item matches when it meets every condition the rule sets; an unset condition matches
 * anything. The first enabled rule that matches decides the item's route.
 */
public record SorterRule(
        boolean enabled,
        Route route,
        boolean pulse,
        List<Item> items,
        Optional<ResourceLocation> tag,
        int minStage,
        int maxStage,
        List<Rarity> rarities,
        List<AffixCondition> affixes,
        int minAffixMatches,
        int minQuality,
        int minRefinementPotential,
        int minUniqueQuality,
        StateMatch corrupted,
        StateMatch mirrored
) {
    public static final int MAX_ITEMS = 6;
    public static final int MAX_AFFIXES = 6;
    public static final int MIN_STAGE = 0;
    public static final int MAX_STAGE = 8;
    public static final int MAX_QUALITY = 60;
    public static final int MAX_REFINEMENT_POTENTIAL = 100;

    public static final SorterRule DEFAULT = new SorterRule(
            true, Route.PASS, false, List.of(), Optional.empty(), MIN_STAGE, MAX_STAGE, List.of(), List.of(), 1, 0, 0, 0,
            StateMatch.ANY, StateMatch.ANY
    );

    /** Lazy, so domain checks can build rules without bootstrapping the registries. */
    private static final Codec<Item> ITEM_CODEC = Codec.lazyInitialized(() -> BuiltInRegistries.ITEM.byNameCodec());

    public static final Codec<SorterRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(SorterRule::enabled),
            Route.CODEC.optionalFieldOf("route", Route.PASS).forGetter(SorterRule::route),
            Codec.BOOL.optionalFieldOf("pulse", false).forGetter(SorterRule::pulse),
            ITEM_CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(SorterRule::items),
            ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(SorterRule::tag),
            Codec.INT.optionalFieldOf("min_stage", MIN_STAGE).forGetter(SorterRule::minStage),
            Codec.INT.optionalFieldOf("max_stage", MAX_STAGE).forGetter(SorterRule::maxStage),
            Rarity.CODEC.listOf().optionalFieldOf("rarities", List.of()).forGetter(SorterRule::rarities),
            AffixCondition.CODEC.listOf().optionalFieldOf("affixes", List.of()).forGetter(SorterRule::affixes),
            Codec.INT.optionalFieldOf("min_affix_matches", 1).forGetter(SorterRule::minAffixMatches),
            Codec.INT.optionalFieldOf("min_quality", 0).forGetter(SorterRule::minQuality),
            Codec.INT.optionalFieldOf("min_refinement_potential", 0).forGetter(SorterRule::minRefinementPotential),
            Codec.INT.optionalFieldOf("min_unique_quality", 0).forGetter(SorterRule::minUniqueQuality),
            StateMatch.CODEC.optionalFieldOf("corrupted", StateMatch.ANY).forGetter(SorterRule::corrupted),
            StateMatch.CODEC.optionalFieldOf("mirrored", StateMatch.ANY).forGetter(SorterRule::mirrored)
    ).apply(instance, SorterRule::new));

    public SorterRule {
        route = route == null ? Route.PASS : route;
        items = distinctLimited(items, MAX_ITEMS);
        tag = tag == null ? Optional.empty() : tag;
        minStage = Mth.clamp(minStage, MIN_STAGE, MAX_STAGE);
        maxStage = Mth.clamp(maxStage, MIN_STAGE, MAX_STAGE);
        if (minStage > maxStage) {
            int swap = minStage;
            minStage = maxStage;
            maxStage = swap;
        }
        rarities = distinctLimited(rarities, Rarity.values().length);
        affixes = affixes == null ? List.of() : List.copyOf(affixes.subList(0, Math.min(affixes.size(), MAX_AFFIXES)));
        minAffixMatches = affixes.isEmpty() ? 1 : Mth.clamp(minAffixMatches, 1, affixes.size());
        minQuality = Mth.clamp(minQuality, 0, MAX_QUALITY);
        minRefinementPotential = Mth.clamp(minRefinementPotential, 0, MAX_REFINEMENT_POTENTIAL);
        minUniqueQuality = Mth.clamp(minUniqueQuality, 0, 100);
        corrupted = corrupted == null ? StateMatch.ANY : corrupted;
        mirrored = mirrored == null ? StateMatch.ANY : mirrored;
    }

    public SorterRule withEnabled(boolean value) {
        return new SorterRule(value, route, pulse, items, tag, minStage, maxStage, rarities, affixes, minAffixMatches,
                minQuality, minRefinementPotential, minUniqueQuality, corrupted, mirrored);
    }

    public SorterRule withRoute(Route value) {
        return new SorterRule(enabled, value, pulse, items, tag, minStage, maxStage, rarities, affixes, minAffixMatches,
                minQuality, minRefinementPotential, minUniqueQuality, corrupted, mirrored);
    }

    public SorterRule withPulse(boolean value) {
        return new SorterRule(enabled, route, value, items, tag, minStage, maxStage, rarities, affixes, minAffixMatches,
                minQuality, minRefinementPotential, minUniqueQuality, corrupted, mirrored);
    }

    public SorterRule withItems(List<Item> value, Optional<ResourceLocation> nextTag) {
        return new SorterRule(enabled, route, pulse, value, nextTag, minStage, maxStage, rarities, affixes,
                minAffixMatches, minQuality, minRefinementPotential, minUniqueQuality, corrupted, mirrored);
    }

    public SorterRule withStages(int min, int max) {
        return new SorterRule(enabled, route, pulse, items, tag, min, max, rarities, affixes, minAffixMatches,
                minQuality, minRefinementPotential, minUniqueQuality, corrupted, mirrored);
    }

    public SorterRule withRarities(List<Rarity> value) {
        return new SorterRule(enabled, route, pulse, items, tag, minStage, maxStage, value, affixes, minAffixMatches,
                minQuality, minRefinementPotential, minUniqueQuality, corrupted, mirrored);
    }

    public SorterRule withAffixes(List<AffixCondition> value, int matches) {
        return new SorterRule(enabled, route, pulse, items, tag, minStage, maxStage, rarities, value, matches,
                minQuality, minRefinementPotential, minUniqueQuality, corrupted, mirrored);
    }

    public SorterRule withThresholds(int quality, int refinementPotential, int uniqueQuality) {
        return new SorterRule(enabled, route, pulse, items, tag, minStage, maxStage, rarities, affixes, minAffixMatches,
                quality, refinementPotential, uniqueQuality, corrupted, mirrored);
    }

    public SorterRule withStates(StateMatch nextCorrupted, StateMatch nextMirrored) {
        return new SorterRule(enabled, route, pulse, items, tag, minStage, maxStage, rarities, affixes, minAffixMatches,
                minQuality, minRefinementPotential, minUniqueQuality, nextCorrupted, nextMirrored);
    }

    private static <T> List<T> distinctLimited(List<T> values, int limit) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<T> distinct = new ArrayList<>(new LinkedHashSet<>(values));
        distinct.removeIf(value -> value == null);
        return List.copyOf(distinct.subList(0, Math.min(distinct.size(), limit)));
    }

    /** One affix the rule asks for, at or above a tier. Tier 0 accepts any tier, including untiered rolls. */
    public record AffixCondition(String affixId, int minTier) {
        public static final int MAX_TIER = 7;

        public static final Codec<AffixCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("affix").forGetter(AffixCondition::affixId),
                Codec.INT.optionalFieldOf("min_tier", 0).forGetter(AffixCondition::minTier)
        ).apply(instance, AffixCondition::new));

        public AffixCondition {
            affixId = affixId == null ? "" : affixId;
            minTier = Mth.clamp(minTier, 0, MAX_TIER);
        }
    }

    public enum Route implements StringRepresentable {
        PASS,
        REJECT;

        public static final Codec<Route> CODEC = StringRepresentable.fromEnum(Route::values);

        public Route toggled() {
            return this == PASS ? REJECT : PASS;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** A yes/no state condition: either state, only items in the state, or only items outside it. */
    public enum StateMatch implements StringRepresentable {
        ANY,
        YES,
        NO;

        public static final Codec<StateMatch> CODEC = StringRepresentable.fromEnum(StateMatch::values);

        public StateMatch next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public boolean matches(boolean state) {
            return this == ANY || (this == YES) == state;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
