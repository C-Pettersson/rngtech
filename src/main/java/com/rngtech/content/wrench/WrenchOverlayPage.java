package com.rngtech.content.wrench;

public enum WrenchOverlayPage {
    SUMMARY("summary"),
    ENERGY("energy"),
    ITEM("item"),
    FLUID("fluid");

    private final String serializedName;

    WrenchOverlayPage(String serializedName) {
        this.serializedName = serializedName;
    }

    public WrenchOverlayPage next(int amount) {
        WrenchOverlayPage[] values = values();
        return values[Math.floorMod(ordinal() + amount, values.length)];
    }

    public String translationKey() {
        return "rngtech.wrench_overlay.page." + serializedName;
    }
}
