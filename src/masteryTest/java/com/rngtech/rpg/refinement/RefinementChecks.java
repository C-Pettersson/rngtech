package com.rngtech.rpg.refinement;

import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineModifierText;
import com.rngtech.rpg.MachineTraitRoller;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.ModifierDefinition;
import com.rngtech.rpg.ModifierEligibilityProfile;
import com.rngtech.rpg.ModifierEligibilityProfiles;
import com.rngtech.rpg.ModifierLensTag;
import com.rngtech.rpg.ModifierLensTargets;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.Rarity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.RandomSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Affix Forge currencies keep their documented compatibility, caps and failure safety. */
public final class RefinementChecks {
    private static final int SEEDS = 200;
    private static final int STAGE = 3;
    private static int checks;

    private RefinementChecks() {
    }

    public static int run() {
        checks = 0;
        conservationWorksWithAffixUpgrader();
        conservationWorksWithEveryCatalystButVolatile();
        frugalityWorksWithEveryRolledCost();
        upgradeCrystalsWorkWithAffixUpgrader();
        lensBiasesRemoval();
        lensBiasesRerollAndAscension();
        ascensionMatrixRespectsUpgradeCap();
        failuresLeaveTargetAndItemsAlone();
        everyRollableAffixHasAName();
        mirrorStaysOutOfTheEngine();
        return checks;
    }

    /**
     * The Mirror Catalyst copies item stacks in the Affix Forge, outside the engine: through the engine, as a placed
     * machine's Refinement tab calls it, it fails and spends nothing, and no lens or crystal works with it.
     */
    private static void mirrorStaysOutOfTheEngine() {
        ModifierEligibilityProfile profile = profile();
        MachineTraits traits = magicWith(tieredPrefix(profile, 2).roll(1, RandomSource.create(1L)));
        RefinementResult placed = apply(profile, traits, RefinementOperation.MIRROR, RefinementModifier.NONE, 0);
        require(!placed.success() && "rngtech.refinement.failure.mirror_forge_only".equals(placed.messageKey())
                        && !placed.consumeCatalyst() && placed.traits().equals(traits),
                "a Mirror Catalyst in a Refinement tab fails, keeps the traits, and is not spent");
        for (RefinementModifier modifier : RefinementModifier.values()) {
            require(modifier == RefinementModifier.NONE
                            ? modifier.failureKey(RefinementOperation.MIRROR) == null
                            : "rngtech.refinement.failure.focus_rejects_mirror".equals(modifier.failureKey(RefinementOperation.MIRROR)),
                    modifier + " is rejected with a Mirror Catalyst");
        }
        require(!RefinementEngine.operationSupportsLens(RefinementOperation.MIRROR), "a lens does not work with a Mirror Catalyst");
    }

    private static void conservationWorksWithAffixUpgrader() {
        ModifierEligibilityProfile profile = profile();
        MachineTraits traits = magicWith(tieredPrefix(profile, 2).roll(1, RandomSource.create(1L)));
        int preserved = 0;
        for (int seed = 0; seed < SEEDS; seed++) {
            RefinementResult result = apply(profile, traits, RefinementOperation.UPGRADE_RANDOM_MODIFIER,
                    RefinementModifier.CONSERVE_CATALYST, seed);
            require(result.success(), "Conservation Crystal + Affix Upgrader succeeds: " + result.messageKey());
            require(result.consumeModifier(), "Conservation Crystal is consumed on success");
            if (!result.consumeCatalyst()) {
                preserved++;
            }
        }
        require(preserved > 0 && preserved < SEEDS, "Conservation Crystal sometimes preserves the Affix Upgrader");

        RefinementResult ward = apply(profile, traits, RefinementOperation.UPGRADE_RANDOM_MODIFIER,
                RefinementModifier.CORRUPTION_WARD, 0);
        require(!ward.success() && !ward.consumeModifier() && !ward.consumeCatalyst(),
                "Stabilization Crystal is rejected with Affix Upgrader and consumes nothing");
    }

    private static void conservationWorksWithEveryCatalystButVolatile() {
        ModifierEligibilityProfile profile = profile();
        MachineTraits traits = magicWith(tieredPrefix(profile, 2).roll(1, RandomSource.create(1L)));
        for (RefinementOperation operation : List.of(
                RefinementOperation.ADD_MODIFIER,
                RefinementOperation.UPGRADE_SELECTED_MODIFIER,
                RefinementOperation.ASCEND_RARITY,
                RefinementOperation.ASCENSION_CATALYST,
                RefinementOperation.REMOVE_MODIFIER,
                RefinementOperation.CHAOS_CRYSTAL,
                RefinementOperation.EXPANSION_CRYSTAL,
                RefinementOperation.NULL_CRYSTAL
        )) {
            int preserved = 0;
            for (int seed = 0; seed < SEEDS; seed++) {
                RefinementResult result = apply(profile, traits, operation, RefinementModifier.CONSERVE_CATALYST, seed);
                require(result.success(), "Conservation Crystal + " + operation + " succeeds: " + result.messageKey());
                require(result.consumeModifier(), "Conservation Crystal is consumed with " + operation);
                if (!result.consumeCatalyst()) {
                    preserved++;
                }
            }
            require(preserved > 0 && preserved < SEEDS, "Conservation Crystal sometimes preserves " + operation);
        }
        RefinementResult corrupt = apply(profile, traits, RefinementOperation.CORRUPT, RefinementModifier.CONSERVE_CATALYST, 0);
        require(!corrupt.success() && !corrupt.consumeCatalyst() && !corrupt.consumeModifier(),
                "Conservation Crystal is rejected with a Volatile Catalyst and consumes nothing");
    }

    private static void frugalityWorksWithEveryRolledCost() {
        ModifierEligibilityProfile profile = profile();
        MachineTraits traits = magicWith(tieredPrefix(profile, 2).roll(1, RandomSource.create(1L)));
        for (RefinementOperation operation : List.of(
                RefinementOperation.ADD_MODIFIER,
                RefinementOperation.UPGRADE_SELECTED_MODIFIER,
                RefinementOperation.ASCENSION_CATALYST,
                RefinementOperation.REMOVE_MODIFIER,
                RefinementOperation.CHAOS_CRYSTAL,
                RefinementOperation.EXPANSION_CRYSTAL,
                RefinementOperation.NULL_CRYSTAL
        )) {
            int reduced = 0;
            int full = 0;
            for (int seed = 0; seed < SEEDS; seed++) {
                RefinementResult result = apply(profile, traits, operation, RefinementModifier.REDUCE_POTENTIAL_COST, seed);
                require(result.success(), "Frugality Crystal + " + operation + " succeeds: " + result.messageKey());
                require(result.consumeModifier() && result.consumeCatalyst(), "Frugality Crystal and " + operation + " are consumed");
                if (result.consumedPotential() == 1) {
                    reduced++;
                } else {
                    full++;
                }
            }
            require(reduced > 0 && full > 0, "Frugality Crystal sometimes makes " + operation + " cost 1 RP");
        }
        RefinementResult ascend = apply(profile, traits, RefinementOperation.ASCEND_RARITY, RefinementModifier.REDUCE_POTENTIAL_COST, 0);
        require(!ascend.success() && "rngtech.refinement.failure.modifier_requires_rp_cost".equals(ascend.messageKey()),
                "Frugality Crystal is rejected with an Ascension Matrix");
    }

    private static void upgradeCrystalsWorkWithAffixUpgrader() {
        ModifierEligibilityProfile profile = profile();
        ModifierDefinition definition = tieredPrefix(profile, 2);
        MachineModifier rolled = definition.roll(1, RandomSource.create(1L));
        MachineTraits traits = magicWith(rolled);
        for (int seed = 0; seed < SEEDS; seed++) {
            RefinementResult preserved = apply(profile, traits, RefinementOperation.UPGRADE_RANDOM_MODIFIER,
                    RefinementModifier.PRESERVE_ROLL, seed);
            require(preserved.success() && preserved.consumeModifier(), "Resonance Crystal + Affix Upgrader succeeds");
            MachineModifier upgraded = preserved.traits().modifiers().get(0);
            require(upgraded.tier() == 2, "Resonance Crystal upgrades to tier 2");
            require(Math.abs(percentile(upgraded) - percentile(rolled)) < 0.15, "Resonance Crystal keeps the roll quality");

            RefinementResult transmuted = apply(profile, traits, RefinementOperation.UPGRADE_RANDOM_MODIFIER,
                    RefinementModifier.TRANSMUTE_UPGRADE, seed);
            require(transmuted.success() && transmuted.consumeModifier(), "Transmutation Crystal + Affix Upgrader succeeds");
            require(!definition.matches(transmuted.traits().modifiers().get(0)), "Transmutation Crystal swaps the upgraded affix");

            RefinementResult destabilized = apply(profile, traits, RefinementOperation.UPGRADE_RANDOM_MODIFIER,
                    RefinementModifier.DESTABILIZE_OTHERS, seed);
            require(destabilized.success() && destabilized.consumeModifier(), "Destabilization Crystal + Affix Upgrader succeeds");
        }
        RefinementResult rejected = apply(profile, traits, RefinementOperation.ADD_MODIFIER, RefinementModifier.PRESERVE_ROLL, 0);
        require(!rejected.success() && "rngtech.refinement.failure.modifier_requires_upgrade".equals(rejected.messageKey()),
                "Resonance Crystal is rejected with an Affix Injector");
    }

    private static void lensBiasesRemoval() {
        ModifierEligibilityProfile profile = profile();
        LensPair pair = lensPair(profile);
        MachineTraits traits = new MachineTraits(Rarity.RARE, 30, List.of(pair.matching(), pair.other()));
        RandomSource random = RandomSource.create(42L);
        int matchingRemoved = 0;
        for (int seed = 0; seed < SEEDS; seed++) {
            RefinementResult result = apply(profile, traits, RefinementOperation.REMOVE_MODIFIER, RefinementModifier.NONE, pair.tags(), random);
            require(result.success(), "Lens + Nullifier Coil succeeds: " + result.messageKey());
            require(result.consumeModifier() && result.consumeCatalyst(), "Lens and Nullifier Coil are consumed");
            if (!result.traits().modifiers().contains(pair.matching())) {
                matchingRemoved++;
            }
        }
        require(matchingRemoved > SEEDS / 2 && matchingRemoved < SEEDS, "Lens makes the matching affix the usual removal, not the only one");

        MachineTraits unmatched = new MachineTraits(Rarity.RARE, 30, List.of(pair.other()));
        RefinementResult blocked = apply(profile, unmatched, RefinementOperation.NULL_CRYSTAL, RefinementModifier.NONE, pair.tags(), 0);
        require(!blocked.success() && "rngtech.refinement.failure.no_matching_lens_affix".equals(blocked.messageKey()),
                "Lens blocks removal when no removable affix matches");
        require(!blocked.consumeCatalyst() && !blocked.consumeModifier(), "blocked removal consumes nothing");
    }

    private static void lensBiasesRerollAndAscension() {
        ModifierEligibilityProfile profile = profile();
        LensPair pair = lensPair(profile);
        MachineTraits rare = new MachineTraits(Rarity.RARE, 30, List.of(pair.matching(), pair.other()));
        RandomSource random = RandomSource.create(42L);
        int rerolledMatching = 0;
        for (int seed = 0; seed < SEEDS; seed++) {
            RefinementResult result = apply(profile, rare, RefinementOperation.CHAOS_CRYSTAL, RefinementModifier.NONE, pair.tags(), random);
            require(result.success() && result.consumeModifier(), "Lens + Chaos Crystal succeeds and uses the lens: " + result.messageKey());
            if (result.traits().modifiers().stream().anyMatch(modifier -> matchesLens(profile, modifier, pair.tags()))) {
                rerolledMatching++;
            }
        }
        require(rerolledMatching > 0, "Lens steers Chaos Crystal rerolls toward its family");

        MachineTraits suffixOnly = new MachineTraits(Rarity.RARE, 30, List.of(pair.other()));
        RefinementResult blocked = apply(profile, suffixOnly, RefinementOperation.CHAOS_CRYSTAL, RefinementModifier.NONE, pair.tags(), 0);
        require(!blocked.success() && "rngtech.refinement.failure.no_matching_lens_affix".equals(blocked.messageKey()),
                "Lens blocks a Chaos Crystal when no rerolled slot can match it");

        MachineTraits magic = magicWith(pair.other());
        for (RefinementOperation operation : List.of(RefinementOperation.ASCENSION_CATALYST, RefinementOperation.ASCEND_RARITY)) {
            int addedMatching = 0;
            for (int seed = 0; seed < SEEDS; seed++) {
                RefinementResult result = apply(profile, magic, operation, RefinementModifier.NONE, pair.tags(), random);
                require(result.success() && result.consumeModifier(), "Lens + " + operation + " succeeds and uses the lens: " + result.messageKey());
                if (result.traits().modifiers().stream().skip(1).anyMatch(modifier -> matchesLens(profile, modifier, pair.tags()))) {
                    addedMatching++;
                }
            }
            require(addedMatching > 0, "Lens steers " + operation + " adds toward its family");
        }

        RefinementResult corrupt = apply(profile, magic, RefinementOperation.CORRUPT, RefinementModifier.NONE, pair.tags(), 0);
        require(!corrupt.success() && "rngtech.refinement.failure.lens_rejects_corrupt".equals(corrupt.messageKey()),
                "Lens is rejected with a Volatile Catalyst");
    }

    private static boolean matchesLens(ModifierEligibilityProfile profile, MachineModifier modifier, Set<ModifierLensTag> tags) {
        ModifierDefinition definition = ModifierLensTargets.definitionFor(profile, modifier);
        return definition != null && definition.matchesLensTags(tags);
    }

    private static double percentile(MachineModifier modifier) {
        double span = modifier.range().max() - modifier.range().min();
        return span == 0 ? 1.0 : (modifier.value() - modifier.range().min()) / span;
    }

    /** A prefix the lens favors and a suffix it does not, so a biased pick is observable. */
    private static LensPair lensPair(ModifierEligibilityProfile profile) {
        for (ModifierLensTag tag : ModifierLensTag.values()) {
            Set<ModifierLensTag> tags = Set.of(tag);
            ModifierDefinition matching = profile.rollableDefinitions(ModifierSlot.PREFIX).stream()
                    .filter(definition -> definition.matchesLensTags(tags))
                    .findFirst()
                    .orElse(null);
            ModifierDefinition other = profile.rollableDefinitions(ModifierSlot.SUFFIX).stream()
                    .filter(definition -> !definition.matchesLensTags(tags))
                    .findFirst()
                    .orElse(null);
            if (matching != null && other != null) {
                return new LensPair(tags, matching.roll(1, RandomSource.create(1L)), other.roll(1, RandomSource.create(2L)));
            }
        }
        throw new AssertionError("Crusher has a prefix a lens favors and a suffix it does not");
    }

    private record LensPair(Set<ModifierLensTag> tags, MachineModifier matching, MachineModifier other) {
    }

    private static void ascensionMatrixRespectsUpgradeCap() {
        ModifierEligibilityProfile profile = profile();
        int cap = MachineTraitRoller.maxUpgradeTier();
        ModifierDefinition definition = tieredPrefix(profile, cap + 1);
        MachineTraits traits = magicWith(definition.roll(cap, RandomSource.create(1L)));
        int successes = 0;
        for (int seed = 0; seed < SEEDS; seed++) {
            RefinementResult result = apply(profile, traits, RefinementOperation.ASCEND_RARITY, RefinementModifier.NONE, seed);
            if (!result.success()) {
                continue;
            }
            successes++;
            require(result.traits().modifiers().get(0).tier() <= cap, "Ascension Matrix never upgrades past tier " + cap);
        }
        require(successes > 0, "Ascension Matrix succeeds on a Magic target");
    }

    private static void failuresLeaveTargetAndItemsAlone() {
        ModifierEligibilityProfile profile = profile();
        MachineTraits empty = new MachineTraits(Rarity.NORMAL, 0, List.of());
        for (RefinementOperation operation : List.of(
                RefinementOperation.ADD_MODIFIER,
                RefinementOperation.UPGRADE_RANDOM_MODIFIER,
                RefinementOperation.UPGRADE_SELECTED_MODIFIER,
                RefinementOperation.ASCEND_RARITY,
                RefinementOperation.ASCENSION_CATALYST,
                RefinementOperation.REMOVE_MODIFIER,
                RefinementOperation.CHAOS_CRYSTAL,
                RefinementOperation.EXPANSION_CRYSTAL,
                RefinementOperation.NULL_CRYSTAL
        )) {
            RefinementResult result = apply(profile, empty, operation, RefinementModifier.CONSERVE_CATALYST, 0);
            require(!result.success(), operation + " fails on a Normal target with no RP");
            require(result.traits().equals(empty), operation + " failure leaves traits unchanged");
            require(!result.consumeCatalyst() && !result.consumeModifier(), operation + " failure consumes nothing");
        }
    }

    private static void everyRollableAffixHasAName() {
        JsonObject lang = lang();
        Set<String> missing = new TreeSet<>();
        for (ModifierEligibilityProfile profile : ModifierEligibilityProfiles.allProfiles()) {
            for (ModifierSlot slot : List.of(ModifierSlot.PREFIX, ModifierSlot.SUFFIX)) {
                for (ModifierDefinition definition : profile.rollableDefinitions(slot)) {
                    for (int tier = 1; tier <= Math.max(1, definition.maxTier()); tier++) {
                        MachineModifier modifier = definition.roll(tier, RandomSource.create(tier));
                        if (MachineModifierText.displayName(modifier).getContents() instanceof TranslatableContents contents
                                && !lang.has(contents.getKey())) {
                            missing.add(contents.getKey());
                        }
                    }
                }
            }
        }
        require(missing.isEmpty(), "every rollable affix has an en_us name, missing: " + missing);
    }

    private static JsonObject lang() {
        try (InputStream stream = RefinementChecks.class.getResourceAsStream("/assets/rngtech/lang/en_us.json")) {
            if (stream == null) {
                throw new AssertionError("en_us.json is on the check classpath");
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static RefinementResult apply(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            RefinementOperation operation,
            RefinementModifier modifier,
            long seed
    ) {
        return apply(profile, traits, operation, modifier, Set.of(), seed);
    }

    private static RefinementResult apply(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            RefinementOperation operation,
            RefinementModifier modifier,
            Set<ModifierLensTag> lensTags,
            long seed
    ) {
        return apply(profile, traits, operation, modifier, lensTags, RandomSource.create(seed));
    }

    private static RefinementResult apply(
            ModifierEligibilityProfile profile,
            MachineTraits traits,
            RefinementOperation operation,
            RefinementModifier modifier,
            Set<ModifierLensTag> lensTags,
            RandomSource random
    ) {
        return RefinementEngine.apply(profile, traits, operation, STAGE, RefinementSelection.none(), modifier, lensTags, random);
    }

    private static ModifierEligibilityProfile profile() {
        return ModifierEligibilityProfiles.forMachine(MachineType.CRUSHER);
    }

    private static ModifierDefinition tieredPrefix(ModifierEligibilityProfile profile, int minimumMaxTier) {
        return profile.rollableDefinitions(ModifierSlot.PREFIX).stream()
                .filter(ModifierDefinition::isTiered)
                .filter(definition -> definition.maxTier() >= minimumMaxTier)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Crusher has a prefix with tier " + minimumMaxTier));
    }

    private static MachineTraits magicWith(MachineModifier modifier) {
        return new MachineTraits(Rarity.MAGIC, 30, List.of(modifier));
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
