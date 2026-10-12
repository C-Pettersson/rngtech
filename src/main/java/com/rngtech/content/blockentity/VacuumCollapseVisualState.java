package com.rngtech.content.blockentity;

import net.minecraft.nbt.CompoundTag;

/**
 * What a Vacuum Collapse Generator shows in the world: the installed Void Chamber, Collapse Nozzle, and Dimensional
 * Stabilizer materials (empty when the slot is empty), whether a collapse is running, its progress from 0 to 1, and its
 * instability pressure.
 */
public record VacuumCollapseVisualState(
        String chamber,
        String nozzle,
        String stabilizer,
        boolean collapsing,
        float progress,
        float instability
) {
    public static final VacuumCollapseVisualState IDLE = new VacuumCollapseVisualState("", "", "", false, 0.0F, 0.0F);

    /** Quantized so a running collapse sends an update about every 1/24 of its cycle rather than every tick. */
    public Key key() {
        return new Key(chamber, nozzle, stabilizer, collapsing, Math.round(progress * 24.0F), Math.round(Math.min(instability, 2.0F) * 10.0F));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Chamber", chamber);
        tag.putString("Nozzle", nozzle);
        tag.putString("Stabilizer", stabilizer);
        tag.putBoolean("Collapsing", collapsing);
        tag.putFloat("Progress", progress);
        tag.putFloat("Instability", instability);
        return tag;
    }

    public static VacuumCollapseVisualState load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return IDLE;
        }
        return new VacuumCollapseVisualState(
                tag.getString("Chamber"),
                tag.getString("Nozzle"),
                tag.getString("Stabilizer"),
                tag.getBoolean("Collapsing"),
                tag.getFloat("Progress"),
                tag.getFloat("Instability")
        );
    }

    public record Key(String chamber, String nozzle, String stabilizer, boolean collapsing, int progress, int instability) {
    }

    /** Client-side easing between updates, so the core and rim light change smoothly. */
    public static final class Animation {
        public float progress;
        public float intensity;
        public float pulse;
        public float spin;
        public double lastTime = Double.NaN;
    }
}
