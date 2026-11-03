package com.rngtech.content.blockentity;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * What an Identifier shows in the world: the item on the scanning bed, whether it is being read and how long a reading
 * takes, and how full the sight vial of Identifying Fluid stands.
 *
 * @param input      one of the item on the bed: the one being read, or the next one waiting
 * @param cycleTicks the length of a reading, which sets the pace of the scan; 0 while idle
 * @param fluidFill  the vial's share of the tank's capacity, 0 to 1
 */
public record IdentifierVisualState(ItemStack input, boolean working, int cycleTicks, float fluidFill) {
    public static final IdentifierVisualState IDLE = new IdentifierVisualState(ItemStack.EMPTY, false, 0, 0.0F);
    private static final int FILL_STEPS = 32;

    /** Quantized so a slowly draining vial does not send an update every tick. */
    public Key key() {
        String item = input.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(input.getItem()).toString();
        return new Key(item, working, cycleTicks, Math.round(fluidFill * FILL_STEPS));
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Input", input.saveOptional(registries));
        tag.putBoolean("Working", working);
        tag.putInt("CycleTicks", cycleTicks);
        tag.putFloat("Fill", fluidFill);
        return tag;
    }

    public static IdentifierVisualState load(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag == null || tag.isEmpty()) {
            return IDLE;
        }
        return new IdentifierVisualState(
                ItemStack.parseOptional(registries, tag.getCompound("Input")),
                tag.getBoolean("Working"),
                tag.getInt("CycleTicks"),
                tag.getFloat("Fill")
        );
    }

    public record Key(String input, boolean working, int cycleTicks, int fill) {
    }

    /** Client-side motion: the eased lift of the item off the bed, its turn, and the scan sweep's phase. */
    public static final class Animation {
        public float lift;
        public float turn;
        public float sweep;
        public double lastTime = Double.NaN;
    }
}
