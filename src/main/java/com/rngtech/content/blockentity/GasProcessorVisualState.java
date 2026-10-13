package com.rngtech.content.blockentity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * What a Coal Gasifier or Steam Methane Reformer shows in the world: whether it runs, its processing speed and cycle
 * progress, its tank levels, the Coal Dust and residue it holds, and its installed Gear. Fractions run 0 to 1. The water,
 * output, and secondary tanks each hold one fixed fluid; {@code inputFluid} is the Reformer's feed gas (Methane or Syngas).
 * Heat Core, Servo, and Battery Cell are a material name, {@code "unique"} for a Unique, or empty when the slot is empty.
 */
public record GasProcessorVisualState(
        boolean active,
        float speed,
        float progress,
        float water,
        float input,
        Fluid inputFluid,
        float output,
        float secondary,
        float feed,
        float residue,
        String heatCore,
        String servo,
        String cell,
        float charge,
        boolean catalyst
) {
    public static final GasProcessorVisualState IDLE =
            new GasProcessorVisualState(false, 1.0F, 0.0F, 0.0F, 0.0F, Fluids.EMPTY, 0.0F, 0.0F, 0.0F, 0.0F, "", "", "", 0.0F, false);

    /** Quantized so slow drifts, such as a tank draining, do not send an update every tick. */
    public Key key() {
        return new Key(
                active,
                Math.round(speed * 20.0F),
                Math.round(progress * 8.0F),
                Math.round(water * 32.0F),
                Math.round(input * 32.0F),
                inputFluid,
                Math.round(output * 32.0F),
                Math.round(secondary * 32.0F),
                Math.round(feed * 16.0F),
                Math.round(residue * 16.0F),
                heatCore,
                servo,
                cell,
                Math.round(charge * 16.0F),
                catalyst
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Active", active);
        tag.putFloat("Speed", speed);
        tag.putFloat("Progress", progress);
        tag.putFloat("Water", water);
        tag.putFloat("Input", input);
        tag.putString("InputFluid", BuiltInRegistries.FLUID.getKey(inputFluid).toString());
        tag.putFloat("Output", output);
        tag.putFloat("Secondary", secondary);
        tag.putFloat("Feed", feed);
        tag.putFloat("Residue", residue);
        tag.putString("HeatCore", heatCore);
        tag.putString("Servo", servo);
        tag.putString("Cell", cell);
        tag.putFloat("Charge", charge);
        tag.putBoolean("Catalyst", catalyst);
        return tag;
    }

    public static GasProcessorVisualState load(CompoundTag tag) {
        if (tag == null || !tag.contains("Active")) {
            return IDLE;
        }
        ResourceLocation inputFluid = ResourceLocation.tryParse(tag.getString("InputFluid"));
        return new GasProcessorVisualState(
                tag.getBoolean("Active"),
                tag.getFloat("Speed"),
                tag.getFloat("Progress"),
                tag.getFloat("Water"),
                tag.getFloat("Input"),
                inputFluid == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(inputFluid),
                tag.getFloat("Output"),
                tag.getFloat("Secondary"),
                tag.getFloat("Feed"),
                tag.getFloat("Residue"),
                tag.getString("HeatCore"),
                tag.getString("Servo"),
                tag.getString("Cell"),
                tag.getFloat("Charge"),
                tag.getBoolean("Catalyst")
        );
    }

    public record Key(
            boolean active,
            int speed,
            int progress,
            int water,
            int input,
            Fluid inputFluid,
            int output,
            int secondary,
            int feed,
            int residue,
            String heatCore,
            String servo,
            String cell,
            int charge,
            boolean catalyst
    ) {
    }

    /** Client-side easing between updates, so heat glows up and fades and moving parts spin up and coast down. */
    public static final class Animation {
        public float heat;
        public float spin;
        public float angle;
        public double lastTime = Double.NaN;
    }
}
