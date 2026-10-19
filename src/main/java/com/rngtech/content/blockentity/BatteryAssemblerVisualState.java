package com.rngtech.content.blockentity;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * What a Component Assembler shows in the world: the item in each input slot lying in its jig pocket, the part the
 * current recipe makes, the finished items in the output rack, the assembly fluid in the reservoir, the Battery Cell with
 * its charge, and the cycle the gantry head follows. {@code charge} is negative without a Battery Cell, and {@code fluid}
 * is empty for an empty reservoir.
 *
 * @param inputs     one entry per input slot, empty when the slot is empty; the count is 2 when the slot still holds items
 *                   after the current job takes its share, so its pocket stays filled once the head picks from it, else 1
 * @param outputs    one entry per output slot; each stack's count is how many copies to draw, 0 to
 *                   {@value #PILE}
 * @param product    the current recipe's result, which replaces the delivered parts on the nest as the job ends
 * @param doses      whether the current recipe takes fluid, which ends the head's cycle with a dose on the nest
 * @param progress   the share of the current cycle done, 0 to 1
 * @param craftTicks the length of the current cycle, which the client uses to run the head on between updates
 */
public record BatteryAssemblerVisualState(
        List<ItemStack> inputs,
        List<ItemStack> outputs,
        ItemStack product,
        String fluid,
        float fill,
        boolean doses,
        boolean working,
        float progress,
        int craftTicks,
        String cell,
        float charge
) {
    public static final int SLOTS = 4;
    public static final int PILE = 3;
    public static final int CHARGE_SEGMENTS = 4;
    public static final BatteryAssemblerVisualState IDLE = new BatteryAssemblerVisualState(
            empty(), empty(), ItemStack.EMPTY, "", 0.0F, false, false, 0.0F, 0, "", -1.0F);

    /** One copy of a stack, counted as the copies to draw: one for any items, up to {@value #PILE} for a full stack. */
    public static ItemStack pile(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int copies = (int) Math.ceil(stack.getCount() * (double) PILE / stack.getMaxStackSize());
        return stack.copyWithCount(Math.max(1, Math.min(PILE, copies)));
    }

    /** Lit charge-meter segments: none for an empty or missing cell, one as soon as it holds any FE. */
    public int chargeSegments() {
        return charge <= 0.0F ? 0 : Math.max(1, Math.min(CHARGE_SEGMENTS, Math.round(charge * CHARGE_SEGMENTS)));
    }

    /** Quantized so a running cycle, a filling rack, or a draining cell does not send an update every tick. */
    public Key key() {
        List<String> slots = new ArrayList<>(SLOTS);
        for (ItemStack input : inputs) {
            slots.add(input.isEmpty() ? "" : itemKey(input) + "*" + input.getCount());
        }
        List<String> rack = new ArrayList<>(SLOTS);
        for (ItemStack output : outputs) {
            rack.add(output.isEmpty() ? "" : itemKey(output) + "*" + output.getCount());
        }
        return new Key(slots, rack, itemKey(product), fluid, Math.round(fill * 32.0F), doses, working, Math.round(progress * 8.0F),
                craftTicks, cell, charge < 0.0F ? -1 : chargeSegments());
    }

    private static String itemKey(ItemStack stack) {
        return stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Inputs", stacks(inputs, registries));
        tag.put("Outputs", stacks(outputs, registries));
        tag.put("Product", product.saveOptional(registries));
        tag.putString("Fluid", fluid);
        tag.putFloat("Fill", fill);
        tag.putBoolean("Doses", doses);
        tag.putBoolean("Working", working);
        tag.putFloat("Progress", progress);
        tag.putInt("CraftTicks", craftTicks);
        tag.putString("Cell", cell);
        tag.putFloat("Charge", charge);
        return tag;
    }

    public static BatteryAssemblerVisualState load(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag == null || tag.isEmpty()) {
            return IDLE;
        }
        return new BatteryAssemblerVisualState(
                stacks(tag.getList("Inputs", Tag.TAG_COMPOUND), registries),
                stacks(tag.getList("Outputs", Tag.TAG_COMPOUND), registries),
                ItemStack.parseOptional(registries, tag.getCompound("Product")),
                tag.getString("Fluid"),
                tag.getFloat("Fill"),
                tag.getBoolean("Doses"),
                tag.getBoolean("Working"),
                tag.getFloat("Progress"),
                tag.getInt("CraftTicks"),
                tag.getString("Cell"),
                tag.getFloat("Charge")
        );
    }

    private static ListTag stacks(List<ItemStack> stacks, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ItemStack stack : stacks) {
            list.add(stack.saveOptional(registries));
        }
        return list;
    }

    private static List<ItemStack> stacks(ListTag list, HolderLookup.Provider registries) {
        List<ItemStack> stacks = new ArrayList<>(SLOTS);
        for (int i = 0; i < SLOTS; i++) {
            stacks.add(i < list.size() ? ItemStack.parseOptional(registries, list.getCompound(i)) : ItemStack.EMPTY);
        }
        return List.copyOf(stacks);
    }

    private static List<ItemStack> empty() {
        return List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
    }

    public record Key(
            List<String> inputs,
            List<String> outputs,
            String product,
            String fluid,
            int fill,
            boolean doses,
            boolean working,
            int progress,
            int craftTicks,
            String cell,
            int charge
    ) {
    }

    /**
     * Client-side motion: the cycle's progress run on between updates, the eased reservoir level, and where the head
     * is, in model pixels, with how far the quill has dipped.
     */
    public static final class Animation {
        public float progress;
        public float syncedProgress = -1.0F;
        public float fill;
        public float x = 8.0F;
        public float z = 7.5F;
        public float dip;
        public double lastTime = Double.NaN;
    }
}
