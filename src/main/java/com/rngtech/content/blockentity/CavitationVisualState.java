package com.rngtech.content.blockentity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * What a Cavitation Generator shows in the world: rotor, wear, heat, and tank levels. Fractions run 0 to 1; {@code calm}
 * is how close the rotor runs to its resonance, which steadies the chamber glow.
 */
public record CavitationVisualState(
        float speed,
        String rotor,
        float wear,
        float strain,
        float input,
        Fluid inputFluid,
        float output,
        Fluid outputFluid,
        boolean cavitating,
        float calm
) {
    public static final CavitationVisualState IDLE = new CavitationVisualState(0.0F, "", 0.0F, 0.0F, 0.0F, Fluids.EMPTY, 0.0F, Fluids.EMPTY, false, 0.0F);

    /** Quantized so slow drifts, such as cooling, do not send an update every tick. */
    public Key key() {
        return new Key(
                Math.round(speed * 50.0F),
                rotor,
                Math.round(wear * 20.0F),
                Math.round(strain * 40.0F),
                Math.round(input * 32.0F),
                inputFluid,
                Math.round(output * 32.0F),
                outputFluid,
                cavitating,
                Math.round(calm * 10.0F)
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Speed", speed);
        tag.putString("Rotor", rotor);
        tag.putFloat("Wear", wear);
        tag.putFloat("Strain", strain);
        tag.putFloat("Input", input);
        tag.putString("InputFluid", BuiltInRegistries.FLUID.getKey(inputFluid).toString());
        tag.putFloat("Output", output);
        tag.putString("OutputFluid", BuiltInRegistries.FLUID.getKey(outputFluid).toString());
        tag.putBoolean("Cavitating", cavitating);
        tag.putFloat("Calm", calm);
        return tag;
    }

    public static CavitationVisualState load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return IDLE;
        }
        return new CavitationVisualState(
                tag.getFloat("Speed"),
                tag.getString("Rotor"),
                tag.getFloat("Wear"),
                tag.getFloat("Strain"),
                tag.getFloat("Input"),
                fluid(tag.getString("InputFluid")),
                tag.getFloat("Output"),
                fluid(tag.getString("OutputFluid")),
                tag.getBoolean("Cavitating"),
                tag.getFloat("Calm")
        );
    }

    private static Fluid fluid(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(key);
    }

    public record Key(
            int speed,
            String rotor,
            int wear,
            int strain,
            int input,
            Fluid inputFluid,
            int output,
            Fluid outputFluid,
            boolean cavitating,
            int calm
    ) {
    }

    /** Client-side rotor animation, eased toward the synced speed between updates. */
    public static final class Animation {
        public float angle;
        public float speed;
        public float glow;
        public double lastTime = Double.NaN;
    }
}
