package com.rngtech.rpg;

import java.util.List;

public record ModifierSet(Rarity rarity, int refinementPotential, List<MachineModifier> modifiers) {
    public ModifierSet {
        modifiers = List.copyOf(modifiers);
    }

    public static ModifierSet empty() {
        return new ModifierSet(Rarity.NORMAL, 0, List.of());
    }
}
