package com.rngtech.content.blockentity;

import net.minecraft.nbt.CompoundTag;

/**
 * What a Fuel Cell shows in the world: whether a Fuel Cell Membrane sits in the stack, the fuel tank level, the Corrosion
 * Residue collected in the trap, and whether the tank holds Hydrogen instead of Ammonia. Fractions run 0 to 1.
 */
public record AmmoniaFuelCellVisualState(boolean membrane, float ammonia, float residue, boolean hydrogen) {
    public static final AmmoniaFuelCellVisualState IDLE = new AmmoniaFuelCellVisualState(false, 0.0F, 0.0F, false);

    /** Quantized so a tank draining by a few mB does not send an update. */
    public Key key() {
        return new Key(membrane, Math.round(ammonia * 32.0F), Math.round(residue * 32.0F), hydrogen);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Membrane", membrane);
        tag.putFloat("Ammonia", ammonia);
        tag.putFloat("Residue", residue);
        tag.putBoolean("Hydrogen", hydrogen);
        return tag;
    }

    public static AmmoniaFuelCellVisualState load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return IDLE;
        }
        return new AmmoniaFuelCellVisualState(tag.getBoolean("Membrane"), tag.getFloat("Ammonia"), tag.getFloat("Residue"), tag.getBoolean("Hydrogen"));
    }

    public record Key(boolean membrane, int ammonia, int residue, boolean hydrogen) {
    }
}
