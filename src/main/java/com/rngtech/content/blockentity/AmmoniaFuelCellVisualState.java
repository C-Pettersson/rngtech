package com.rngtech.content.blockentity;

import net.minecraft.nbt.CompoundTag;

/**
 * What an Ammonia Fuel Cell shows in the world: whether a Fuel Cell Membrane sits in the stack, the Ammonia tank level,
 * and the Corrosion Residue collected in the trap. Fractions run 0 to 1.
 */
public record AmmoniaFuelCellVisualState(boolean membrane, float ammonia, float residue) {
    public static final AmmoniaFuelCellVisualState IDLE = new AmmoniaFuelCellVisualState(false, 0.0F, 0.0F);

    /** Quantized so a tank draining by a few mB does not send an update. */
    public Key key() {
        return new Key(membrane, Math.round(ammonia * 32.0F), Math.round(residue * 32.0F));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Membrane", membrane);
        tag.putFloat("Ammonia", ammonia);
        tag.putFloat("Residue", residue);
        return tag;
    }

    public static AmmoniaFuelCellVisualState load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return IDLE;
        }
        return new AmmoniaFuelCellVisualState(tag.getBoolean("Membrane"), tag.getFloat("Ammonia"), tag.getFloat("Residue"));
    }

    public record Key(boolean membrane, int ammonia, int residue) {
    }
}
