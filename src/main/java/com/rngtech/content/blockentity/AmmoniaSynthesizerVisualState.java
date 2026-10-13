package com.rngtech.content.blockentity;

import net.minecraft.nbt.CompoundTag;

/**
 * What an Ammonia Synthesizer shows in the world: whether it runs, its processing speed and cycle progress, its Nitrogen,
 * Hydrogen, and Ammonia levels, and whether a Catalyst Bed is installed. Fractions run 0 to 1.
 */
public record AmmoniaSynthesizerVisualState(
        boolean active,
        float speed,
        float progress,
        float nitrogen,
        float hydrogen,
        float ammonia,
        boolean catalyst
) {
    public static final AmmoniaSynthesizerVisualState IDLE = new AmmoniaSynthesizerVisualState(false, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, false);

    /** Quantized so a tank draining by a few mB does not send an update. */
    public Key key() {
        return new Key(
                active,
                Math.round(speed * 20.0F),
                Math.round(progress * 8.0F),
                Math.round(nitrogen * 32.0F),
                Math.round(hydrogen * 32.0F),
                Math.round(ammonia * 32.0F),
                catalyst
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Active", active);
        tag.putFloat("Speed", speed);
        tag.putFloat("Progress", progress);
        tag.putFloat("Nitrogen", nitrogen);
        tag.putFloat("Hydrogen", hydrogen);
        tag.putFloat("Ammonia", ammonia);
        tag.putBoolean("Catalyst", catalyst);
        return tag;
    }

    public static AmmoniaSynthesizerVisualState load(CompoundTag tag) {
        if (tag == null || !tag.contains("Active")) {
            return IDLE;
        }
        return new AmmoniaSynthesizerVisualState(
                tag.getBoolean("Active"),
                tag.getFloat("Speed"),
                tag.getFloat("Progress"),
                tag.getFloat("Nitrogen"),
                tag.getFloat("Hydrogen"),
                tag.getFloat("Ammonia"),
                tag.getBoolean("Catalyst")
        );
    }

    public record Key(boolean active, int speed, int progress, int nitrogen, int hydrogen, int ammonia, boolean catalyst) {
    }

    /** Client-side easing between updates, so the converter glow fades and the compressor spins up and coasts down. */
    public static final class Animation {
        public float heat;
        public float spin;
        public float angle;
        public double lastTime = Double.NaN;
    }
}
