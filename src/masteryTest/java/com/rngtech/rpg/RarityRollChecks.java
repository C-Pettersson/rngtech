package com.rngtech.rpg;

import net.minecraft.util.RandomSource;

/** Crafted rolls: Stage 1-4 rarity odds, and PoE-style affix counts that leave room for refinement. */
public final class RarityRollChecks {
    private static final int SAMPLES = 20000;
    private static int checks;

    private RarityRollChecks() {
    }

    public static int run() {
        checks = 0;
        earlyStagesFavourRare();
        affixCountsStayInRarityRange();
        return checks;
    }

    private static void earlyStagesFavourRare() {
        ModifierEligibilityProfile profile = ModifierEligibilityProfiles.forMachine(MachineType.CRUSHER);
        RandomSource random = RandomSource.create(7L);
        int magic = 0;
        int rare = 0;
        for (int sample = 0; sample < SAMPLES; sample++) {
            Rarity rarity = MachineTraitRoller.roll(profile, 1 + sample % 4, random).rarity();
            require(rarity == Rarity.MAGIC || rarity == Rarity.RARE, "Stage 1-4 never rolls " + rarity);
            if (rarity == Rarity.MAGIC) {
                magic++;
            } else {
                rare++;
            }
        }
        require(Math.abs(rare / (double) SAMPLES - 0.65) < 0.02, "Stage 1-4 rolls about 65% Rare, was " + rare);
        require(Math.abs(magic / (double) SAMPLES - 0.35) < 0.02, "Stage 1-4 rolls about 35% Magic, was " + magic);
    }

    private static void affixCountsStayInRarityRange() {
        ModifierEligibilityProfile profile = ModifierEligibilityProfiles.forMachine(MachineType.CRUSHER);
        RandomSource random = RandomSource.create(11L);
        boolean[] magicCounts = new boolean[3];
        boolean[] rareCounts = new boolean[7];
        int rareTotal = 0;
        int rareItems = 0;
        for (int sample = 0; sample < SAMPLES; sample++) {
            MachineTraits traits = MachineTraitRoller.roll(profile, 2, random);
            int prefixes = count(traits, ModifierSlot.PREFIX);
            int suffixes = count(traits, ModifierSlot.SUFFIX) + traits.behaviors().size();
            int total = prefixes + suffixes;
            requirePrefixesFirst(traits);
            if (traits.rarity() == Rarity.MAGIC) {
                require(total >= 1 && total <= 2 && prefixes <= 1 && suffixes <= 1,
                        "Magic rolls one or two affixes, at most one per side, was " + prefixes + "+" + suffixes);
                magicCounts[total] = true;
            } else {
                require(total >= 3 && total <= 6 && prefixes <= 3 && suffixes <= 3,
                        "Rare rolls three to six affixes, at most three per side, was " + prefixes + "+" + suffixes);
                rareCounts[total] = true;
                rareTotal += total;
                rareItems++;
            }
        }
        require(magicCounts[1] && magicCounts[2], "Magic rolls both one and two affixes");
        require(rareCounts[3] && rareCounts[4] && rareCounts[5] && rareCounts[6], "Rare rolls every count from three to six");
        double rareMean = rareTotal / (double) rareItems;
        require(Math.abs(rareMean - 4.05) < 0.1, "a Crusher Rare averages about 4 affixes, was " + rareMean);
    }

    private static void requirePrefixesFirst(MachineTraits traits) {
        boolean suffixSeen = false;
        for (MachineModifier modifier : traits.modifiers()) {
            if (modifier.slot() == ModifierSlot.SUFFIX) {
                suffixSeen = true;
            }
            require(!(suffixSeen && modifier.slot() == ModifierSlot.PREFIX), "rolled prefixes list before suffixes");
        }
    }

    private static int count(MachineTraits traits, ModifierSlot slot) {
        return (int) traits.modifiers().stream().filter(modifier -> modifier.slot() == slot).count();
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
