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

import java.util.List;

/**
 * What an Alloy Furnace shows in the world: the crucible's temperature against the recipe target, setpoint, overheat
 * line, and the Heat Core's limit, the wobble, the item in each feed hopper, how far the current charge has melted, the
 * Nitrogen tank, and the installed Heat Core, Crucible, Servo, and Battery Cell. Temperatures are in degrees Celsius.
 * {@code charge} is negative without a Battery Cell, and the part keys are empty when the slot is empty.
 *
 * @param target     the recipe's target temperature, 0 without a recipe; {@code setpoint} and {@code overheat} likewise
 * @param maxHeat    the Max Temperature the Heat Core allows, the full scale of the heat bar
 * @param wobble     how far the crucible swings around its setpoint
 * @param hoppers    active input slots, 1 to 4
 * @param melt       the molten share of the current charge, 0 without a recipe
 * @param nitrogen   the Nitrogen tank's fill, 0 to 1
 * @param atmosphere whether the crucible used Nitrogen last tick
 */
public record AlloyFurnaceVisualState(
        int heat,
        int target,
        int setpoint,
        int overheat,
        int maxHeat,
        int wobble,
        boolean working,
        int hoppers,
        Item[] inputs,
        float melt,
        String heatCore,
        String crucible,
        String servo,
        String cell,
        float charge,
        float nitrogen,
        boolean atmosphere
) {
    public static final int HOPPERS = 4;
    public static final int CHARGE_SEGMENTS = 8;
    public static final AlloyFurnaceVisualState IDLE = new AlloyFurnaceVisualState(
            HeatControl.AMBIENT_TEMPERATURE, 0, 0, 0, 0, 0, false, 1, FurnaceVisualState.emptyInputs(), 0.0F, "", "", "", "", -1.0F,
            0.0F, false);

    /** Whether the crucible is hotter than its overheat temperature, which turns the heat bar red. */
    public boolean overheating() {
        return overheat > 0 && heat > overheat;
    }

    public Item input(int hopper) {
        return inputs[hopper];
    }

    /** Lit charge-meter segments: none for an empty or missing cell, one as soon as it holds any FE. */
    public int chargeSegments() {
        return charge <= 0.0F ? 0 : Math.max(1, Math.min(CHARGE_SEGMENTS, Math.round(charge * CHARGE_SEGMENTS)));
    }

    /** Quantized so a warming crucible or a draining cell does not send an update every tick. */
    public Key key() {
        return new Key(
                heat / 20,
                target / 20,
                setpoint / 20,
                overheating(),
                maxHeat / 20,
                wobble / 10,
                working,
                hoppers,
                List.of(inputs),
                Math.round(melt * 16.0F),
                heatCore,
                crucible,
                servo,
                cell,
                charge < 0.0F ? -1 : chargeSegments(),
                Math.round(nitrogen * 32.0F),
                atmosphere
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Heat", heat);
        tag.putInt("Target", target);
        tag.putInt("Setpoint", setpoint);
        tag.putInt("Overheat", overheat);
        tag.putInt("MaxHeat", maxHeat);
        tag.putInt("Wobble", wobble);
        tag.putBoolean("Working", working);
        tag.putInt("Hoppers", hoppers);
        ListTag items = new ListTag();
        for (Item input : inputs) {
            items.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(input).toString()));
        }
        tag.put("Inputs", items);
        tag.putFloat("Melt", melt);
        tag.putString("HeatCore", heatCore);
        tag.putString("Crucible", crucible);
        tag.putString("Servo", servo);
        tag.putString("Cell", cell);
        tag.putFloat("Charge", charge);
        tag.putFloat("Nitrogen", nitrogen);
        tag.putBoolean("Atmosphere", atmosphere);
        return tag;
    }

    public static AlloyFurnaceVisualState load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return IDLE;
        }
        Item[] inputs = FurnaceVisualState.emptyInputs();
        ListTag items = tag.getList("Inputs", Tag.TAG_STRING);
        for (int hopper = 0; hopper < Math.min(HOPPERS, items.size()); hopper++) {
            ResourceLocation id = ResourceLocation.tryParse(items.getString(hopper));
            inputs[hopper] = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
        }
        return new AlloyFurnaceVisualState(
                tag.getInt("Heat"),
                tag.getInt("Target"),
                tag.getInt("Setpoint"),
                tag.getInt("Overheat"),
                tag.getInt("MaxHeat"),
                tag.getInt("Wobble"),
                tag.getBoolean("Working"),
                Math.max(1, Math.min(HOPPERS, tag.getInt("Hoppers"))),
                inputs,
                tag.getFloat("Melt"),
                tag.getString("HeatCore"),
                tag.getString("Crucible"),
                tag.getString("Servo"),
                tag.getString("Cell"),
                tag.getFloat("Charge"),
                tag.getFloat("Nitrogen"),
                tag.getBoolean("Atmosphere")
        );
    }

    public record Key(
            int heat,
            int target,
            int setpoint,
            boolean overheating,
            int maxHeat,
            int wobble,
            boolean working,
            int hoppers,
            List<Item> inputs,
            int melt,
            String heatCore,
            String crucible,
            String servo,
            String cell,
            int charge,
            int nitrogen,
            boolean atmosphere
    ) {
    }

    /** Client-side easing between updates, so the glow, the pool, and the stirrer change smoothly. */
    public static final class Animation {
        public float heat;
        public float melt;
        public float stir;
        public float angle;
        public float nitrogen;
        public float atmosphere;
        public double lastTime = Double.NaN;
        public final ItemStack[] inputs = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    }
}
