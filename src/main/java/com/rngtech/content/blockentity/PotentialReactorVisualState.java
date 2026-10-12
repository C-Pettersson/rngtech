package com.rngtech.content.blockentity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/**
 * What a Potential Reactor shows in the world: the installed Gear materials (empty when a slot is empty), the core
 * state, what is burning, reaction intensity, stability, and the FE buffer and residue levels. Fractions run 0 to 1.
 */
public record PotentialReactorVisualState(
        String chamber,
        String filter,
        String lining,
        int core,
        int burn,
        float intensity,
        float stability,
        float charge,
        float residue
) {
    public static final int CORE_OFF = 0;
    /** A run is loaded but paused, for example by a full FE buffer or residue slot. */
    public static final int CORE_HELD = 1;
    public static final int CORE_REACTING = 2;
    /** Salvage fuel; recycled gear burns as its {@link com.rngtech.rpg.Rarity} ordinal. */
    public static final int BURN_SALVAGE = -1;
    public static final PotentialReactorVisualState IDLE = new PotentialReactorVisualState("", "", "", CORE_OFF, BURN_SALVAGE, 0.0F, 1.0F, 0.0F, 0.0F);

    private static final double INTENSITY_FLOOR_FE = 10.0;
    private static final double INTENSITY_DECADES = 2.0;

    /** Reaction intensity on a log scale: 10 FE/t or less reads as a faint core, 1,000 FE/t as full. */
    public static float intensity(double energyPerTick) {
        if (energyPerTick <= 0.0) {
            return 0.0F;
        }
        double decades = Math.log10(Math.max(energyPerTick, INTENSITY_FLOOR_FE) / INTENSITY_FLOOR_FE) / INTENSITY_DECADES;
        return Mth.clamp((float) decades, 0.1F, 1.0F);
    }

    /** How unsettled the core looks, 0 to 1: steady from 1.20 stability, fully unsettled at 0.70 or below. */
    public float instability() {
        return Mth.clamp((1.2F - stability) / 0.5F, 0.0F, 1.0F);
    }

    /** Quantized so slow drifts, such as the FE buffer filling, do not send an update every tick. */
    public Key key() {
        return new Key(
                chamber,
                filter,
                lining,
                core,
                burn,
                Math.round(intensity * 20.0F),
                Math.round(stability * 20.0F),
                Math.round(charge * 32.0F),
                Math.round(residue * 32.0F)
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Chamber", chamber);
        tag.putString("Filter", filter);
        tag.putString("Lining", lining);
        tag.putInt("Core", core);
        tag.putInt("Burn", burn);
        tag.putFloat("Intensity", intensity);
        tag.putFloat("Stability", stability);
        tag.putFloat("Charge", charge);
        tag.putFloat("Residue", residue);
        return tag;
    }

    public static PotentialReactorVisualState load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return IDLE;
        }
        return new PotentialReactorVisualState(
                tag.getString("Chamber"),
                tag.getString("Filter"),
                tag.getString("Lining"),
                tag.getInt("Core"),
                tag.getInt("Burn"),
                tag.getFloat("Intensity"),
                tag.getFloat("Stability"),
                tag.getFloat("Charge"),
                tag.getFloat("Residue")
        );
    }

    public record Key(
            String chamber,
            String filter,
            String lining,
            int core,
            int burn,
            int intensity,
            int stability,
            int charge,
            int residue
    ) {
    }

    /** Client-side easing between updates, so the core and the dial needle change smoothly. */
    public static final class Animation {
        public float glow;
        public float intensity;
        public float needle = -1.0F;
        public double lastTime = Double.NaN;
    }
}
