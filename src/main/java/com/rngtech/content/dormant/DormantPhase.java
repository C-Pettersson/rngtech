package com.rngtech.content.dormant;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** A Dormant Machine sleeps until its wake charge fills, stays awake until its soul is taken, then is a spent husk. */
public enum DormantPhase implements StringRepresentable {
    ASLEEP, AWAKE, SPENT;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
