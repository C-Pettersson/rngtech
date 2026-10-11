package com.rngtech.content.blockentity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * What an Algae Photobioreactor shows in the world: culture density, aeration, light, tank levels, and the harvested
 * Algae Biomass in the settling tank. Fractions run 0 to 1.
 */
public record AlgaeVisualState(
        float density,
        int aeration,
        boolean growing,
        boolean lit,
        float water,
        float carbon,
        Fluid carbonFluid,
        float harvest
) {
    public static final AlgaeVisualState IDLE = new AlgaeVisualState(0.0F, AlgaeCultureDynamics.RATED_AERATION, false, false, 0.0F, 0.0F, Fluids.EMPTY, 0.0F);

    /** Quantized so slow drifts, such as die-back, do not send an update every tick. */
    public Key key() {
        return new Key(
                Math.round(density * 40.0F),
                aeration,
                growing,
                lit,
                Math.round(water * 32.0F),
                Math.round(carbon * 32.0F),
                carbonFluid,
                Math.round(harvest * 32.0F)
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Density", density);
        tag.putInt("Aeration", aeration);
        tag.putBoolean("Growing", growing);
        tag.putBoolean("Lit", lit);
        tag.putFloat("Water", water);
        tag.putFloat("Carbon", carbon);
        tag.putString("CarbonFluid", BuiltInRegistries.FLUID.getKey(carbonFluid).toString());
        tag.putFloat("Harvest", harvest);
        return tag;
    }

    public static AlgaeVisualState load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return IDLE;
        }
        return new AlgaeVisualState(
                tag.getFloat("Density"),
                tag.getInt("Aeration"),
                tag.getBoolean("Growing"),
                tag.getBoolean("Lit"),
                tag.getFloat("Water"),
                tag.getFloat("Carbon"),
                fluid(tag.getString("CarbonFluid")),
                tag.getFloat("Harvest")
        );
    }

    private static Fluid fluid(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(key);
    }

    public record Key(int density, int aeration, boolean growing, boolean lit, int water, int carbon, Fluid carbonFluid, int harvest) {
    }

    /** Client-side easing between updates, so colour and bubble flow change smoothly. */
    public static final class Animation {
        public float density = -1.0F;
        public float flow;
        public float bubbleClock;
        public double lastTime = Double.NaN;
    }
}
