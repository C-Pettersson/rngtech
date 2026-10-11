package com.rngtech.content.blockentity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * What a Syngas Combustor shows in the world: whether it fires, how fast its shaft turns, both tank levels, and the
 * installed Servo and Battery Cell. Fractions run 0 to 1; {@code speed} is the processing speed multiplier. Gear is a
 * material name, {@code "unique"} for a Unique, or empty when the slot is empty.
 */
public record CombustorVisualState(
        boolean firing,
        float speed,
        float fuel,
        Fluid fuelFluid,
        float exhaust,
        Fluid exhaustFluid,
        String servo,
        String cell,
        float charge
) {
    public static final CombustorVisualState IDLE = new CombustorVisualState(false, 1.0F, 0.0F, Fluids.EMPTY, 0.0F, Fluids.EMPTY, "", "", 0.0F);

    /** Quantized so slow drifts, such as a tank draining, do not send an update every tick. */
    public Key key() {
        return new Key(
                firing,
                Math.round(speed * 20.0F),
                Math.round(fuel * 32.0F),
                fuelFluid,
                Math.round(exhaust * 32.0F),
                exhaustFluid,
                servo,
                cell,
                Math.round(charge * 16.0F)
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Firing", firing);
        tag.putFloat("Speed", speed);
        tag.putFloat("Fuel", fuel);
        tag.putString("FuelFluid", BuiltInRegistries.FLUID.getKey(fuelFluid).toString());
        tag.putFloat("Exhaust", exhaust);
        tag.putString("ExhaustFluid", BuiltInRegistries.FLUID.getKey(exhaustFluid).toString());
        tag.putString("Servo", servo);
        tag.putString("Cell", cell);
        tag.putFloat("Charge", charge);
        return tag;
    }

    public static CombustorVisualState load(CompoundTag tag) {
        if (tag == null || !tag.contains("Firing")) {
            return IDLE;
        }
        return new CombustorVisualState(
                tag.getBoolean("Firing"),
                tag.getFloat("Speed"),
                tag.getFloat("Fuel"),
                fluid(tag.getString("FuelFluid")),
                tag.getFloat("Exhaust"),
                fluid(tag.getString("ExhaustFluid")),
                tag.getString("Servo"),
                tag.getString("Cell"),
                tag.getFloat("Charge")
        );
    }

    private static Fluid fluid(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(key);
    }

    public record Key(
            boolean firing,
            int speed,
            int fuel,
            Fluid fuelFluid,
            int exhaust,
            Fluid exhaustFluid,
            String servo,
            String cell,
            int charge
    ) {
    }

    /** Client-side easing between updates, so the flame fades and the shaft spins up and coasts down. */
    public static final class Animation {
        public float flame;
        public float spin;
        public float angle;
        public double lastTime = Double.NaN;
    }
}
