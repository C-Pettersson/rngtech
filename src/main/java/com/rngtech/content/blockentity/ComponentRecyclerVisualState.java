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
 * What a Component Recycler shows in the world: the installed Disassembly Head as its claw, the Recovery Filter
 * cartridge, the Battery Cell in its cradle with its charge, the item on the turntable, the returns in the three trays,
 * and whether it works. Keys are empty when the slot is empty; {@code charge} is negative without a Battery Cell.
 *
 * @param head       the Disassembly Head material
 * @param filter     the Recovery Filter material
 * @param input      one of the item being recycled, drawn on the turntable
 * @param outputs    one entry per output slot, west to east; each stack's count is how many copies to draw, 0 to
 *                   {@value #PILE}
 * @param craftTicks the length of the current craft, which sets how fast the claw turns; 0 while idle
 * @param cranks     Hand Crank turns since the Crude Recycler loaded, each of which turns its runner stone a step
 */
public record ComponentRecyclerVisualState(
        String head,
        String filter,
        String cell,
        float charge,
        ItemStack input,
        List<ItemStack> outputs,
        boolean running,
        int craftTicks,
        int cranks
) {
    public static final int OUTPUTS = 3;
    public static final int PILE = 3;
    public static final int CHARGE_SEGMENTS = 4;
    public static final ComponentRecyclerVisualState IDLE = new ComponentRecyclerVisualState(
            "", "", "", -1.0F, ItemStack.EMPTY, List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY), false, 0, 0);

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

    /** Quantized so a draining cell or a slowly filling tray does not send an update every tick. */
    public Key key() {
        List<String> returns = new ArrayList<>(outputs.size());
        for (ItemStack output : outputs) {
            returns.add(output.isEmpty() ? "" : itemKey(output) + "*" + output.getCount());
        }
        return new Key(head, filter, cell, charge < 0.0F ? -1 : chargeSegments(), itemKey(input), returns, running, craftTicks, cranks);
    }

    private static String itemKey(ItemStack stack) {
        return stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Head", head);
        tag.putString("Filter", filter);
        tag.putString("Cell", cell);
        tag.putFloat("Charge", charge);
        tag.put("Input", input.saveOptional(registries));
        ListTag returns = new ListTag();
        for (ItemStack output : outputs) {
            returns.add(output.saveOptional(registries));
        }
        tag.put("Outputs", returns);
        tag.putBoolean("Running", running);
        tag.putInt("CraftTicks", craftTicks);
        tag.putInt("Cranks", cranks);
        return tag;
    }

    public static ComponentRecyclerVisualState load(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag == null || tag.isEmpty()) {
            return IDLE;
        }
        ListTag returns = tag.getList("Outputs", Tag.TAG_COMPOUND);
        List<ItemStack> outputs = new ArrayList<>(OUTPUTS);
        for (int i = 0; i < OUTPUTS; i++) {
            outputs.add(i < returns.size() ? ItemStack.parseOptional(registries, returns.getCompound(i)) : ItemStack.EMPTY);
        }
        return new ComponentRecyclerVisualState(
                tag.getString("Head"),
                tag.getString("Filter"),
                tag.getString("Cell"),
                tag.getFloat("Charge"),
                ItemStack.parseOptional(registries, tag.getCompound("Input")),
                List.copyOf(outputs),
                tag.getBoolean("Running"),
                tag.getInt("CraftTicks"),
                tag.getInt("Cranks")
        );
    }

    public record Key(
            String head,
            String filter,
            String cell,
            int charge,
            String input,
            List<String> outputs,
            boolean running,
            int craftTicks,
            int cranks
    ) {
    }

    /**
     * Client-side motion: how far the claw (or the Crude Recycler's runner stone) has turned and its eased speed in
     * turns per tick, and the eased dip of the claw toward the turntable.
     */
    public static final class Animation {
        public float angle;
        public float speed;
        public float dip;
        public double lastTime = Double.NaN;
    }
}
