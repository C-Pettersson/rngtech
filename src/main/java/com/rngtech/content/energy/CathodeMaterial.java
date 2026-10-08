package com.rngtech.content.energy;

import com.rngtech.rpg.ModifierSet;
import com.rngtech.rpg.Rarity;

import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Locale;

/** Corrosion Cell cathodes: the installed cathode's stage sets the highest anode tier the cell can burn. */
public enum CathodeMaterial implements StringRepresentable {
    ALUMINUM(5, 16),
    TITANIUM(6, 18),
    TUNGSTENSTEEL(7, 20),
    NAQUADAH(8, 22);

    private final int stage;
    private final int refinementPotential;
    private final String serializedName;

    CathodeMaterial(int stage, int refinementPotential) {
        this.stage = stage;
        this.refinementPotential = refinementPotential;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public int stage() {
        return stage;
    }

    public int refinementPotential() {
        return refinementPotential;
    }

    /** A small efficiency step per tier above Aluminum; the stage gate is the cathode's main identity. */
    public double efficiency() {
        return 1.0 + (stage - 5) * 0.02;
    }

    public ModifierSet modifierSet() {
        return new ModifierSet(Rarity.NORMAL, refinementPotential, List.of());
    }

    public String itemId() {
        return serializedName + "_cathode";
    }

    public String anodeItemId() {
        return serializedName + "_anode";
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
