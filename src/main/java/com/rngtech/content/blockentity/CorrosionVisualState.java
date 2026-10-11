package com.rngtech.content.blockentity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * What a Corrosion Cell shows in the world: the dissolving fuel, the cell current, the Cathode, the electrolyte
 * reservoir, and the sludge trap. Fractions run 0 to 1; {@code fuel} is the item id of the fuel in the bath and
 * {@code cathode} the installed Cathode's material, each empty when there is none.
 */
public record CorrosionVisualState(
        String fuel,
        float fuelLeft,
        float current,
        String cathode,
        boolean bath,
        float reservoir,
        Fluid reservoirFluid,
        float sludge
) {
    public static final CorrosionVisualState IDLE = new CorrosionVisualState("", 0.0F, 0.0F, "", false, 0.0F, Fluids.EMPTY, 0.0F);

    /** Quantized so a slowly dissolving fuel does not send an update every tick. */
    public Key key() {
        return new Key(
                fuel,
                Math.round(fuelLeft * 40.0F),
                Math.round(current * 20.0F),
                cathode,
                bath,
                Math.round(reservoir * 32.0F),
                reservoirFluid,
                Math.round(sludge * 16.0F)
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Fuel", fuel);
        tag.putFloat("FuelLeft", fuelLeft);
        tag.putFloat("Current", current);
        tag.putString("Cathode", cathode);
        tag.putBoolean("Bath", bath);
        tag.putFloat("Reservoir", reservoir);
        tag.putString("ReservoirFluid", BuiltInRegistries.FLUID.getKey(reservoirFluid).toString());
        tag.putFloat("Sludge", sludge);
        return tag;
    }

    public static CorrosionVisualState load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return IDLE;
        }
        return new CorrosionVisualState(
                tag.getString("Fuel"),
                tag.getFloat("FuelLeft"),
                tag.getFloat("Current"),
                tag.getString("Cathode"),
                tag.getBoolean("Bath"),
                tag.getFloat("Reservoir"),
                fluid(tag.getString("ReservoirFluid")),
                tag.getFloat("Sludge")
        );
    }

    private static Fluid fluid(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(key);
    }

    public record Key(
            String fuel,
            int fuelLeft,
            int current,
            String cathode,
            boolean bath,
            int reservoir,
            Fluid reservoirFluid,
            int sludge
    ) {
    }

    /** Client-side animation, eased toward the synced state between updates. */
    public static final class Animation {
        public float fanAngle;
        public float current;
        public float fuelLeft;
        public double lastTime = Double.NaN;
    }
}
