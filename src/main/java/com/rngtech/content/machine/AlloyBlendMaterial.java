package com.rngtech.content.machine;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum AlloyBlendMaterial implements StringRepresentable {
    BRONZE("Bronze"),
    STEEL("Steel"),
    INVAR("Invar"),
    SPARKSTEEL("Sparksteel");

    private final String displayName;
    private final String serializedName;

    AlloyBlendMaterial(String displayName) {
        this.displayName = displayName;
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public String itemId() {
        return serializedName + "_blend";
    }

    public String ingotItemId() {
        return serializedName + "_ingot";
    }

    public String materialId() {
        return serializedName;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
