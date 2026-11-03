package com.rngtech.content.blockentity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * What an Affix Sorter shows in the world: the item in the input slot, which hangs in the reader bay, and how long a
 * reading takes, which paces the scan.
 *
 * @param input      the item waiting to be read, or air
 * @param cycleTicks the length of a reading while the sorter works; 0 while idle
 */
public record AffixSorterVisualState(Item input, int cycleTicks) {
    public static final AffixSorterVisualState IDLE = new AffixSorterVisualState(Items.AIR, 0);

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Input", BuiltInRegistries.ITEM.getKey(input).toString());
        tag.putInt("CycleTicks", cycleTicks);
        return tag;
    }

    public static AffixSorterVisualState load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return IDLE;
        }
        ResourceLocation input = ResourceLocation.tryParse(tag.getString("Input"));
        return new AffixSorterVisualState(input == null ? Items.AIR : BuiltInRegistries.ITEM.get(input), tag.getInt("CycleTicks"));
    }

    /** Client-side motion: the scan's fade and sweep phase, and the flap's swing from -1 (Reject) to 1 (Pass). */
    public static final class Animation {
        public float scan;
        public float sweep;
        public float flap;
        public double lastTime = Double.NaN;
    }
}
