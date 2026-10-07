package com.rngtech.content.energy;

import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSet;
import com.rngtech.rpg.ModifierSlot;

import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Locale;

public enum SolarArrayExtenderMaterial implements StringRepresentable {
    SPARKSTEEL("sparksteel", 5, MachineStat.CLEAR_SKY_AMPLIFICATION),
    AETHERGOLD("aethergold", 5, MachineStat.MOONLIGHT_CONVERSION);

    private final String id;
    private final int stage;
    private final MachineStat implicitBonusStat;

    SolarArrayExtenderMaterial(String id, int stage, MachineStat implicitBonusStat) {
        this.id = id;
        this.stage = stage;
        this.implicitBonusStat = implicitBonusStat;
    }

    public String itemId() {
        return id + "_solar_array_extender";
    }

    public String translationKey() {
        return "rngtech.solar_array_extender_material." + id;
    }

    public int stage() {
        return stage;
    }

    public int rangeBonus() {
        return 1;
    }

    public double generationMultiplier() {
        return 1.0;
    }

    public double implicitBonusPercent() {
        return 20.0;
    }

    public MachineStat implicitBonusStat() {
        return implicitBonusStat;
    }

    public ModifierSet modifierSet() {
        return new ModifierSet(
                com.rngtech.rpg.Rarity.NORMAL,
                0,
                List.of(new com.rngtech.rpg.MachineModifier(
                        ModifierSlot.IMPLICIT,
                        implicitBonusStat,
                        ModifierOperation.ADD,
                        implicitBonusPercent()
                ))
        );
    }

    @Override
    public String getSerializedName() {
        return id.toLowerCase(Locale.ROOT);
    }
}
