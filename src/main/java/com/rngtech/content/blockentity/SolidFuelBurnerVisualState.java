package com.rngtech.content.blockentity;

import net.minecraft.nbt.CompoundTag;

/**
 * What a Solid Fuel Burner shows in the world: the fire, the installed Heat Core and Fuel Box, the fuel in each hopper
 * bin, and the Battery Cell charge. Fractions run 0 to 1; {@code charge} is negative without a Battery Cell, and the
 * part keys are empty when the slot is empty.
 *
 * @param burning   whether fuel burned this tick; a paused burner keeps its {@code burn} as a banked fire
 * @param burn      the share of the current fuel item left to burn
 * @param rate      FE/t against a Steel Heat Core, so a stronger core burns visibly hotter
 * @param fuelTints colours for the fuel in each bin, as 0xRRGGBB
 */
public record SolidFuelBurnerVisualState(
        boolean burning,
        float burn,
        float rate,
        String heatCore,
        String fuelBox,
        int bins,
        float fuel0,
        float fuel1,
        int fuelTint0,
        int fuelTint1,
        float charge
) {
    public static final SolidFuelBurnerVisualState IDLE = new SolidFuelBurnerVisualState(false, 0.0F, 0.0F, "", "", 0, 0.0F, 0.0F, 0, 0, -1.0F);
    public static final int CHARGE_SEGMENTS = 8;

    public float fuel(int bin) {
        return bin == 0 ? fuel0 : fuel1;
    }

    public int fuelTint(int bin) {
        return bin == 0 ? fuelTint0 : fuelTint1;
    }

    /** Lit charge-meter segments: none for an empty or missing cell, one as soon as it holds any FE. */
    public int chargeSegments() {
        return charge <= 0.0F ? 0 : Math.max(1, Math.min(CHARGE_SEGMENTS, Math.round(charge * CHARGE_SEGMENTS)));
    }

    /** Quantized so a burning fuel item or a draining cell does not send an update every tick. */
    public Key key() {
        return new Key(
                burning,
                Math.round(burn * 20.0F),
                Math.round(rate * 20.0F),
                heatCore,
                fuelBox,
                bins,
                Math.round(fuel0 * 16.0F),
                Math.round(fuel1 * 16.0F),
                fuelTint0,
                fuelTint1,
                charge < 0.0F ? -1 : chargeSegments()
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Burning", burning);
        tag.putFloat("Burn", burn);
        tag.putFloat("Rate", rate);
        tag.putString("HeatCore", heatCore);
        tag.putString("FuelBox", fuelBox);
        tag.putInt("Bins", bins);
        tag.putFloat("Fuel0", fuel0);
        tag.putFloat("Fuel1", fuel1);
        tag.putInt("FuelTint0", fuelTint0);
        tag.putInt("FuelTint1", fuelTint1);
        tag.putFloat("Charge", charge);
        return tag;
    }

    public static SolidFuelBurnerVisualState load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return IDLE;
        }
        return new SolidFuelBurnerVisualState(
                tag.getBoolean("Burning"),
                tag.getFloat("Burn"),
                tag.getFloat("Rate"),
                tag.getString("HeatCore"),
                tag.getString("FuelBox"),
                tag.getInt("Bins"),
                tag.getFloat("Fuel0"),
                tag.getFloat("Fuel1"),
                tag.getInt("FuelTint0"),
                tag.getInt("FuelTint1"),
                tag.getFloat("Charge")
        );
    }

    public record Key(
            boolean burning,
            int burn,
            int rate,
            String heatCore,
            String fuelBox,
            int bins,
            int fuel0,
            int fuel1,
            int fuelTint0,
            int fuelTint1,
            int charge
    ) {
    }

    /** Client-side easing between updates, so the fire and the fin heat rise and fall smoothly. */
    public static final class Animation {
        public float fire;
        public float heat;
        public double lastTime = Double.NaN;
    }
}
