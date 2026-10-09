package com.rngtech.rpg.refinement;

import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineModifierText;
import com.rngtech.rpg.MachineTraitRoller;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.ModifierDefinition;
import com.rngtech.rpg.ModifierEligibilityProfile;
import com.rngtech.rpg.ModifierEligibilityProfiles;
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
        ascensionMatrixRespectsUpgradeCap();
        failuresLeaveTargetAndItemsAlone();
        everyRollableAffixHasAName();
        return checks;
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
        require(ward.success() && !ward.consumeModifier(), "Stabilization Crystal is accepted and kept with Affix Upgrader");
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
        return RefinementEngine.apply(profile, traits, operation, STAGE, RefinementSelection.none(), modifier, Set.of(),
                RandomSource.create(seed));
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
