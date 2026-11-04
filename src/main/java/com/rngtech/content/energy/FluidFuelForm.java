package com.rngtech.content.energy;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** Which Fuel Tank material burns a fluid fuel: a tank takes liquids, a Gas Jet takes gases. */
public enum FluidFuelForm implements StringRepresentable {
    LIQUID,
    GAS;

    public static final Codec<FluidFuelForm> CODEC = StringRepresentable.fromEnum(FluidFuelForm::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
