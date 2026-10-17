package com.rngtech.content.blockentity;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * What a Crusher shows in the world: the installed Crush Head as its rolls, the Battery Cell in its cradle with its
 * charge, the feed in the hopper and the product in the output bin, and whether the rolls turn or are jammed. Keys are
 * empty when the slot is empty; {@code charge} is negative without a Battery Cell.
 *
 * @param head       the Crush Head material, {@code unique} for a Unique head
 * @param input      one of the input item, drawn in the hopper
 * @param inputPile  how many copies of the input to draw, 0 to {@value #PILE}
 * @param output     one of the output item, drawn in the bin
 * @param outputPile how many copies of the output to draw, 0 to {@value #PILE}
 * @param craftTicks the length of the current craft, which sets how fast the rolls turn; 0 while idle
 */
public record CrusherVisualState(
        String head,
        String cell,
        float charge,
        ItemStack input,
        int inputPile,
        ItemStack output,
        int outputPile,
        boolean running,
        boolean jammed,
        int craftTicks
) {
    public static final CrusherVisualState IDLE =
            new CrusherVisualState("", "", -1.0F, ItemStack.EMPTY, 0, ItemStack.EMPTY, 0, false, false, 0);
    public static final int PILE = 3;
    public static final int CHARGE_SEGMENTS = 4;

    /** Copies of a stack to draw: one for any items, up to {@value #PILE} for a full stack. */
    public static int pile(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        return Math.max(1, Math.min(PILE, (int) Math.ceil(stack.getCount() * (double) PILE / stack.getMaxStackSize())));
    }

    /** Lit charge-meter segments: none for an empty or missing cell, one as soon as it holds any FE. */
    public int chargeSegments() {
        return charge <= 0.0F ? 0 : Math.max(1, Math.min(CHARGE_SEGMENTS, Math.round(charge * CHARGE_SEGMENTS)));
    }

    /** Quantized so a draining cell or a slowly filling bin does not send an update every tick. */
    public Key key() {
        return new Key(
                head,
                cell,
                charge < 0.0F ? -1 : chargeSegments(),
                itemKey(input),
                inputPile,
                itemKey(output),
                outputPile,
                running,
                jammed,
                craftTicks
        );
    }

    private static String itemKey(ItemStack stack) {
        return stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Head", head);
        tag.putString("Cell", cell);
        tag.putFloat("Charge", charge);
        tag.put("Input", input.saveOptional(registries));
        tag.putInt("InputPile", inputPile);
        tag.put("Output", output.saveOptional(registries));
        tag.putInt("OutputPile", outputPile);
        tag.putBoolean("Running", running);
        tag.putBoolean("Jammed", jammed);
        tag.putInt("CraftTicks", craftTicks);
        return tag;
    }

    public static CrusherVisualState load(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag == null || tag.isEmpty()) {
            return IDLE;
        }
        return new CrusherVisualState(
                tag.getString("Head"),
                tag.getString("Cell"),
                tag.getFloat("Charge"),
                ItemStack.parseOptional(registries, tag.getCompound("Input")),
                tag.getInt("InputPile"),
                ItemStack.parseOptional(registries, tag.getCompound("Output")),
                tag.getInt("OutputPile"),
                tag.getBoolean("Running"),
                tag.getBoolean("Jammed"),
                tag.getInt("CraftTicks")
        );
    }

    public record Key(
            String head,
            String cell,
            int charge,
            String input,
            int inputPile,
            String output,
            int outputPile,
            boolean running,
            boolean jammed,
            int craftTicks
    ) {
    }

    /**
     * Client-side roll motion, the angle the rolls have turned and their eased speed in turns per tick, and where this
     * Crusher scatters the items in its hopper and bin, worked out once from its position.
     */
    public static final class Animation {
        public float angle;
        public float speed;
        public double lastTime = Double.NaN;
        public float[][] hopperSpots;
        public float[][] binSpots;
    }
}
