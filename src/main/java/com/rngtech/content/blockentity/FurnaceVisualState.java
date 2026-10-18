package com.rngtech.content.blockentity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;

/**
 * What a Furnace shows in the world: each lane's temperature, target, setpoint, overheat line, and input item, the
 * Heat Core's limit and the wobble, the installed Heat Core and Battery Cell, the Nitrogen tank, and on the Stage 0
 * Furnace the fire. Temperatures are in degrees Celsius; {@code target}, {@code setpoint}, and {@code overheat} are 0 for
 * a lane with no recipe. {@code charge} is negative without a Battery Cell, {@code nitrogen} is negative on the Stage 0
 * Furnace, which has no tank, and the part keys are empty when the slot is empty.
 *
 * @param lanes      active smelting lanes, 1 to 4
 * @param maxHeat    the Max Temperature the Heat Core allows, the full scale of the heat bar
 * @param wobble     how far the hottest working lane swings around its setpoint
 * @param burn       the share of the Stage 0 Furnace's current fuel item left to burn, 0 when unlit
 * @param nitrogen   the Nitrogen tank's fill, 0 to 1
 * @param atmosphere whether a lane used Nitrogen last tick
 */
public record FurnaceVisualState(
        int lanes,
        int[] heat,
        int[] target,
        int[] setpoint,
        int[] overheat,
        Item[] inputs,
        int maxHeat,
        int wobble,
        boolean working,
        float burn,
        String heatCore,
        String cell,
        float charge,
        float nitrogen,
        boolean atmosphere
) {
    public static final int LANES = 4;
    public static final int CHARGE_SEGMENTS = 8;
    public static final FurnaceVisualState IDLE = new FurnaceVisualState(
            1,
            filled(HeatControl.AMBIENT_TEMPERATURE),
            new int[LANES],
            new int[LANES],
            new int[LANES],
            emptyInputs(),
            0,
            0,
            false,
            0.0F,
            "",
            "",
            -1.0F,
            -1.0F,
            false
    );

    public int heat(int lane) {
        return heat[lane];
    }

    public int target(int lane) {
        return target[lane];
    }

    public int setpoint(int lane) {
        return setpoint[lane];
    }

    public int overheat(int lane) {
        return overheat[lane];
    }

    /** Whether the lane is hotter than its overheat temperature, which turns its heat bar red. */
    public boolean overheating(int lane) {
        return overheat[lane] > 0 && heat[lane] > overheat[lane];
    }

    public Item input(int lane) {
        return inputs[lane];
    }

    /** The lane the heat bar reads on a single chamber: the hottest one. */
    public int hottestLane() {
        int hottest = 0;
        for (int lane = 1; lane < lanes; lane++) {
            if (heat[lane] > heat[hottest]) {
                hottest = lane;
            }
        }
        return hottest;
    }

    /** Lit charge-meter segments: none for an empty or missing cell, one as soon as it holds any FE. */
    public int chargeSegments() {
        return charge <= 0.0F ? 0 : Math.max(1, Math.min(CHARGE_SEGMENTS, Math.round(charge * CHARGE_SEGMENTS)));
    }

    /** Quantized so a warming lane or a draining cell does not send an update every tick. */
    public Key key() {
        Integer[] heatSteps = new Integer[LANES];
        Integer[] targetSteps = new Integer[LANES];
        Integer[] setpointSteps = new Integer[LANES];
        Boolean[] overheated = new Boolean[LANES];
        for (int lane = 0; lane < LANES; lane++) {
            heatSteps[lane] = heat[lane] / 20;
            targetSteps[lane] = target[lane] / 20;
            setpointSteps[lane] = setpoint[lane] / 20;
            overheated[lane] = overheating(lane);
        }
        return new Key(
                lanes,
                List.of(heatSteps),
                List.of(targetSteps),
                List.of(setpointSteps),
                List.of(overheated),
                List.of(inputs),
                maxHeat / 20,
                wobble / 10,
                working,
                Math.round(burn * 20.0F),
                heatCore,
                cell,
                charge < 0.0F ? -1 : chargeSegments(),
                nitrogen < 0.0F ? -1 : Math.round(nitrogen * 32.0F),
                atmosphere
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Lanes", lanes);
        tag.putIntArray("Heat", heat);
        tag.putIntArray("Target", target);
        tag.putIntArray("Setpoint", setpoint);
        tag.putIntArray("Overheat", overheat);
        ListTag items = new ListTag();
        for (Item input : inputs) {
            items.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(input).toString()));
        }
        tag.put("Inputs", items);
        tag.putInt("MaxHeat", maxHeat);
        tag.putInt("Wobble", wobble);
        tag.putBoolean("Working", working);
        tag.putFloat("Burn", burn);
        tag.putString("HeatCore", heatCore);
        tag.putString("Cell", cell);
        tag.putFloat("Charge", charge);
        tag.putFloat("Nitrogen", nitrogen);
        tag.putBoolean("Atmosphere", atmosphere);
        return tag;
    }

    public static FurnaceVisualState load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return IDLE;
        }
        Item[] inputs = emptyInputs();
        ListTag items = tag.getList("Inputs", Tag.TAG_STRING);
        for (int lane = 0; lane < Math.min(LANES, items.size()); lane++) {
            ResourceLocation id = ResourceLocation.tryParse(items.getString(lane));
            inputs[lane] = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
        }
        return new FurnaceVisualState(
                Math.max(1, Math.min(LANES, tag.getInt("Lanes"))),
                lanes(tag.getIntArray("Heat"), HeatControl.AMBIENT_TEMPERATURE),
                lanes(tag.getIntArray("Target"), 0),
                lanes(tag.getIntArray("Setpoint"), 0),
                lanes(tag.getIntArray("Overheat"), 0),
                inputs,
                tag.getInt("MaxHeat"),
                tag.getInt("Wobble"),
                tag.getBoolean("Working"),
                tag.getFloat("Burn"),
                tag.getString("HeatCore"),
                tag.getString("Cell"),
                tag.getFloat("Charge"),
                tag.contains("Nitrogen") ? tag.getFloat("Nitrogen") : -1.0F,
                tag.getBoolean("Atmosphere")
        );
    }

    static Item[] emptyInputs() {
        Item[] inputs = new Item[LANES];
        Arrays.fill(inputs, Items.AIR);
        return inputs;
    }

    private static int[] filled(int value) {
        int[] values = new int[LANES];
        Arrays.fill(values, value);
        return values;
    }

    private static int[] lanes(int[] saved, int fallback) {
        int[] values = filled(fallback);
        System.arraycopy(saved, 0, values, 0, Math.min(LANES, saved.length));
        return values;
    }

    public record Key(
            int lanes,
            List<Integer> heat,
            List<Integer> target,
            List<Integer> setpoint,
            List<Boolean> overheating,
            List<Item> inputs,
            int maxHeat,
            int wobble,
            boolean working,
            int burn,
            String heatCore,
            String cell,
            int charge,
            int nitrogen,
            boolean atmosphere
    ) {
    }

    /** Client-side easing between updates, so the glow rises and falls smoothly. */
    public static final class Animation {
        public final float[] heat = new float[LANES];
        public float fire;
        public float nitrogen;
        public float atmosphere;
        public double lastTime = Double.NaN;
        public final ItemStack[] inputs = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    }
}
