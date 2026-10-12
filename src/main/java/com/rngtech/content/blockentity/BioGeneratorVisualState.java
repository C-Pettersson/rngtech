package com.rngtech.content.blockentity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What a Bio Generator shows in the world. {@code gas} is the gas holder fill and {@code feed} the undigested share of the
 * current item, both 0 to 1. {@code digestion} and {@code engine} are gas made and burned per tick against the rated
 * generation, so 1 is the rated pace and 0 is idle.
 */
public record BioGeneratorVisualState(
        float gas,
        float feed,
        float digestion,
        float engine,
        boolean chamber,
        Item hopper
) {
    public static final BioGeneratorVisualState IDLE = new BioGeneratorVisualState(0.0F, 0.0F, 0.0F, 0.0F, false, Items.AIR);

    /** Quantized so the slow gas and feed drifts do not send an update every tick. */
    public Key key() {
        return new Key(
                Math.round(gas * 40.0F),
                Math.round(feed * 16.0F),
                Math.round(digestion * 10.0F),
                Math.round(engine * 10.0F),
                chamber,
                hopper
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Gas", gas);
        tag.putFloat("Feed", feed);
        tag.putFloat("Digestion", digestion);
        tag.putFloat("Engine", engine);
        tag.putBoolean("Chamber", chamber);
        tag.putString("Hopper", BuiltInRegistries.ITEM.getKey(hopper).toString());
        return tag;
    }

    public static BioGeneratorVisualState load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return IDLE;
        }
        ResourceLocation hopper = ResourceLocation.tryParse(tag.getString("Hopper"));
        return new BioGeneratorVisualState(
                tag.getFloat("Gas"),
                tag.getFloat("Feed"),
                tag.getFloat("Digestion"),
                tag.getFloat("Engine"),
                tag.getBoolean("Chamber"),
                hopper == null ? Items.AIR : BuiltInRegistries.ITEM.get(hopper)
        );
    }

    public record Key(int gas, int feed, int digestion, int engine, boolean chamber, Item hopper) {
    }

    /** Client-side animation, eased toward the synced state between updates. */
    public static final class Animation {
        public float dome;
        public float flywheelAngle;
        public float flywheelSpeed;
        public float bubbles;
        public float slurry;
        public ItemStack hopper = ItemStack.EMPTY;
        /** Client ticks this block entity has run: a steady animation clock, unlike the server-corrected game time. */
        public long clientTicks;
        public double lastTime = Double.NaN;
    }
}
