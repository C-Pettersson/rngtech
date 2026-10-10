package com.rngtech.content.item;

import com.rngtech.rpg.CorruptionOutcome;
import com.rngtech.rpg.MachineCorruption;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.Rarity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.ArrayList;
import java.util.List;

/** A Corrupted tooltip header shows the Corrupted line and hides Refinement Potential. */
public final class CorruptionTooltipChecks {
    private CorruptionTooltipChecks() {
    }

    public static int run() {
        List<Component> corrupted = new ArrayList<>();
        MachineTraitTooltip.appendTraitHeader(new MachineTraits(Rarity.RARE, 9, List.of(), List.of(),
                MachineCorruption.of(CorruptionOutcome.UNTOUCHED)), corrupted, true);
        require(keys(corrupted).contains("rngtech.tooltip.corrupted_outcome"), "a Corrupted header shows the Corrupted line");
        require(!keys(corrupted).contains("rngtech.tooltip.refinement_potential"), "a Corrupted header hides Refinement Potential");

        List<Component> plain = new ArrayList<>();
        MachineTraitTooltip.appendTraitHeader(new MachineTraits(Rarity.RARE, 9, List.of()), plain, true);
        require(keys(plain).contains("rngtech.tooltip.refinement_potential") && !keys(plain).contains("rngtech.tooltip.corrupted_outcome"),
                "an uncorrupted header keeps Refinement Potential");
        return 3;
    }

    private static List<String> keys(List<Component> lines) {
        return lines.stream()
                .map(Component::getContents)
                .filter(TranslatableContents.class::isInstance)
                .map(contents -> ((TranslatableContents) contents).getKey())
                .toList();
    }

    private static void require(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
