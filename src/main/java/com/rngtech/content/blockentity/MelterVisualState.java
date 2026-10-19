package com.rngtech.content.blockentity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What a Melter shows in the world: its heat against the recipe's melting point, setpoint, and overheat line, the melt in
 * progress, the two ingredients, both tanks, and the installed Crush Head, Heat Core, Fluid Pump, Servo, and Battery Cell.
 * Temperatures are in degrees Celsius; {@code target}, {@code setpoint}, and {@code overheat} are 0 without a recipe.
 * {@code charge} is negative without a Battery Cell, fluid ids are empty for an empty tank, and the part keys are empty
 * when the slot is empty.
 *
 * @param maxHeat    the Max Temperature the Heat Core allows, the full scale of the heat bar
 * @param wobble     how far the heat swings around the setpoint while melting
 * @param progress   the share of the current melt done, 0 to 1
 * @param cycleTicks the length of the current melt, which sets how fast the rolls turn; 0 without a recipe
 * @param primary    the item in the first input slot, drawn on the rolls and melting in the crucible
 * @param secondary  the item in the second input slot
 * @param inputFill  the input tank's share of its capacity
 * @param outputFill the output tank's share of its capacity
 * @param product    the fluid the current recipe makes, which fills the crucible as the melt runs
 */
public record MelterVisualState(
        int heat,
        int target,
        int setpoint,
        int overheat,
        int maxHeat,
        int wobble,
        boolean working,
        float progress,
        int cycleTicks,
        Item primary,
        Item secondary,
        String inputFluid,
        float inputFill,
        String outputFluid,
        float outputFill,
        String product,
        String crushHead,
        String heatCore,
        String pump,
        String servo,
        String cell,
        float charge
) {
    public static final int CHARGE_SEGMENTS = 4;
    public static final MelterVisualState IDLE = new MelterVisualState(
            HeatControl.AMBIENT_TEMPERATURE, 0, 0, 0, 0, 0, false, 0.0F, 0, Items.AIR, Items.AIR,
            "", 0.0F, "", 0.0F, "", "", "", "", "", "", -1.0F);

    /** Whether the crucible is hotter than its overheat temperature, which turns the heat bar red. */
    public boolean overheating() {
        return overheat > 0 && heat > overheat;
    }

    /** Lit charge-meter segments: none for an empty or missing cell, one as soon as it holds any FE. */
    public int chargeSegments() {
        return charge <= 0.0F ? 0 : Math.max(1, Math.min(CHARGE_SEGMENTS, Math.round(charge * CHARGE_SEGMENTS)));
    }

    /** Quantized so a warming crucible, a running melt, or a filling tank does not send an update every tick. */
    public Key key() {
        return new Key(
                heat / 20,
                target / 20,
                setpoint / 20,
                overheating(),
                maxHeat / 20,
                wobble / 10,
                working,
                Math.round(progress * 16.0F),
                cycleTicks,
                primary,
                secondary,
                inputFluid,
                Math.round(inputFill * 32.0F),
                outputFluid,
                Math.round(outputFill * 32.0F),
                product,
                crushHead,
                heatCore,
                pump,
                servo,
                cell,
                charge < 0.0F ? -1 : chargeSegments()
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
        tag.putFloat("Progress", progress);
        tag.putInt("CycleTicks", cycleTicks);
        tag.putString("Primary", BuiltInRegistries.ITEM.getKey(primary).toString());
        tag.putString("Secondary", BuiltInRegistries.ITEM.getKey(secondary).toString());
        tag.putString("InputFluid", inputFluid);
        tag.putFloat("InputFill", inputFill);
        tag.putString("OutputFluid", outputFluid);
        tag.putFloat("OutputFill", outputFill);
        tag.putString("Product", product);
        tag.putString("CrushHead", crushHead);
        tag.putString("HeatCore", heatCore);
        tag.putString("Pump", pump);
        tag.putString("Servo", servo);
        tag.putString("Cell", cell);
        tag.putFloat("Charge", charge);
        return tag;
    }

    public static MelterVisualState load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return IDLE;
        }
        return new MelterVisualState(
                tag.getInt("Heat"),
                tag.getInt("Target"),
                tag.getInt("Setpoint"),
                tag.getInt("Overheat"),
                tag.getInt("MaxHeat"),
                tag.getInt("Wobble"),
                tag.getBoolean("Working"),
                tag.getFloat("Progress"),
                tag.getInt("CycleTicks"),
                item(tag.getString("Primary")),
                item(tag.getString("Secondary")),
                tag.getString("InputFluid"),
                tag.getFloat("InputFill"),
                tag.getString("OutputFluid"),
                tag.getFloat("OutputFill"),
                tag.getString("Product"),
                tag.getString("CrushHead"),
                tag.getString("HeatCore"),
                tag.getString("Pump"),
                tag.getString("Servo"),
                tag.getString("Cell"),
                tag.getFloat("Charge")
        );
    }

    private static Item item(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location == null ? Items.AIR : BuiltInRegistries.ITEM.get(location);
    }

    public record Key(
            int heat,
            int target,
            int setpoint,
            boolean overheating,
            int maxHeat,
            int wobble,
            boolean working,
            int progress,
            int cycleTicks,
            Item primary,
            Item secondary,
            String inputFluid,
            int inputFill,
            String outputFluid,
            int outputFill,
            String product,
            String crushHead,
            String heatCore,
            String pump,
            String servo,
            String cell,
            int charge
    ) {
    }

    /**
     * Client-side motion: the eased heat and tank levels, the melt's progress run on between updates, how far the rolls
     * have turned, and their eased speed in turns per tick.
     */
    public static final class Animation {
        public float heat;
        public float progress;
        public float syncedProgress = -1.0F;
        public float inputFill;
        public float outputFill;
        public float angle;
        public float speed;
        public double lastTime = Double.NaN;
        public ItemStack primary = ItemStack.EMPTY;
        public ItemStack secondary = ItemStack.EMPTY;
    }
}
