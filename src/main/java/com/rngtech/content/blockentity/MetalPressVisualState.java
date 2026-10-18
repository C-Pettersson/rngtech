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
 * What a Metal Press shows in the world: its heat against the recipe's target, setpoint, and overheat line, the cycle's
 * progress and finished presses for the ram, the input on the die and the parts in the tray, the selected Mold and the
 * Molds stored in the rack, the installed Heat Core, Servo, and Battery Cell, the atmosphere tank, and the failure strain.
 * Temperatures are in degrees Celsius; {@code target}, {@code setpoint}, and {@code overheat} are 0 without a recipe.
 * {@code charge} is negative without a Battery Cell, {@code strain} is negative on the Crude press, which never fails,
 * and the part and Mold keys are empty when the slot is empty.
 *
 * @param maxHeat    the Max Temperature the Heat Core allows, the full scale of the heat bar
 * @param wobble     how far the heat swings around the setpoint while pressing
 * @param progress   the share of the current cycle done, 0 to 1
 * @param presses    finished cycles, counted modulo 256, so the client can strike the ram once for each
 * @param batch      input sets in the current cycle, 1 unless the press batches
 * @param output     the parts in the output slot, shown in the tray
 * @param outputFill the output stack's share of its maximum size
 * @param die        the Mold on the bed, by its kind such as {@code plate}
 * @param rack       every Mold slot's kind, with the selected slot left empty because its Mold is on the bed
 * @param gas        the atmosphere tank's fill, 0 to 1
 * @param hydrogen   whether the tank holds Hydrogen instead of Nitrogen
 * @param atmosphere whether the press used Nitrogen last tick
 */
public record MetalPressVisualState(
        int heat,
        int target,
        int setpoint,
        int overheat,
        int maxHeat,
        int wobble,
        boolean working,
        float progress,
        int presses,
        int batch,
        Item input,
        Item output,
        float outputFill,
        String die,
        String[] rack,
        String heatCore,
        String servo,
        String cell,
        float charge,
        float gas,
        boolean hydrogen,
        boolean atmosphere,
        float strain
) {
    public static final int RACK_SLOTS = 5;
    public static final int CHARGE_SEGMENTS = 8;
    public static final int STRAIN_SEGMENTS = 8;
    public static final MetalPressVisualState IDLE = new MetalPressVisualState(
            HeatControl.AMBIENT_TEMPERATURE, 0, 0, 0, 0, 0, false, 0.0F, 0, 1, Items.AIR, Items.AIR, 0.0F,
            "", emptyRack(), "", "", "", -1.0F, 0.0F, false, false, -1.0F
    );

    /** Whether the press is hotter than its overheat temperature, which turns its heat bar red. */
    public boolean overheating() {
        return overheat > 0 && heat > overheat;
    }

    public String rack(int slot) {
        return rack[slot];
    }

    /** Lit charge-meter segments: none for an empty or missing cell, one as soon as it holds any FE. */
    public int chargeSegments() {
        return charge <= 0.0F ? 0 : Math.max(1, Math.min(CHARGE_SEGMENTS, Math.round(charge * CHARGE_SEGMENTS)));
    }

    /** Quantized so a warming press, a running cycle, or a draining cell does not send an update every tick. */
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
                presses,
                batch,
                input,
                output,
                Math.round(outputFill * 8.0F),
                die,
                List.of(rack),
                heatCore,
                servo,
                cell,
                charge < 0.0F ? -1 : chargeSegments(),
                Math.round(gas * 32.0F),
                hydrogen,
                atmosphere,
                strain < 0.0F ? -1 : Math.round(strain * STRAIN_SEGMENTS)
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
        tag.putInt("Presses", presses);
        tag.putInt("Batch", batch);
        tag.putString("Input", BuiltInRegistries.ITEM.getKey(input).toString());
        tag.putString("Output", BuiltInRegistries.ITEM.getKey(output).toString());
        tag.putFloat("OutputFill", outputFill);
        tag.putString("Die", die);
        ListTag molds = new ListTag();
        for (String mold : rack) {
            molds.add(StringTag.valueOf(mold));
        }
        tag.put("Rack", molds);
        tag.putString("HeatCore", heatCore);
        tag.putString("Servo", servo);
        tag.putString("Cell", cell);
        tag.putFloat("Charge", charge);
        tag.putFloat("Gas", gas);
        tag.putBoolean("Hydrogen", hydrogen);
        tag.putBoolean("Atmosphere", atmosphere);
        tag.putFloat("Strain", strain);
        return tag;
    }

    public static MetalPressVisualState load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return IDLE;
        }
        String[] rack = emptyRack();
        ListTag molds = tag.getList("Rack", Tag.TAG_STRING);
        for (int slot = 0; slot < Math.min(RACK_SLOTS, molds.size()); slot++) {
            rack[slot] = molds.getString(slot);
        }
        return new MetalPressVisualState(
                tag.getInt("Heat"),
                tag.getInt("Target"),
                tag.getInt("Setpoint"),
                tag.getInt("Overheat"),
                tag.getInt("MaxHeat"),
                tag.getInt("Wobble"),
                tag.getBoolean("Working"),
                tag.getFloat("Progress"),
                tag.getInt("Presses"),
                Math.max(1, tag.getInt("Batch")),
                item(tag.getString("Input")),
                item(tag.getString("Output")),
                tag.getFloat("OutputFill"),
                tag.getString("Die"),
                rack,
                tag.getString("HeatCore"),
                tag.getString("Servo"),
                tag.getString("Cell"),
                tag.getFloat("Charge"),
                tag.getFloat("Gas"),
                tag.getBoolean("Hydrogen"),
                tag.getBoolean("Atmosphere"),
                tag.getFloat("Strain")
        );
    }

    static String[] emptyRack() {
        String[] rack = new String[RACK_SLOTS];
        Arrays.fill(rack, "");
        return rack;
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
            int presses,
            int batch,
            Item input,
            Item output,
            int outputFill,
            String die,
            List<String> rack,
            String heatCore,
            String servo,
            String cell,
            int charge,
            int gas,
            boolean hydrogen,
            boolean atmosphere,
            int strain
    ) {
    }

    /** Client-side easing between updates, so the glow, the ram, and the gauges move smoothly. */
    public static final class Animation {
        public float heat;
        public float ram;
        public float strike;
        public float screw;
        public float gas;
        public float atmosphere;
        public float strain;
        public int presses = -1;
        public double lastTime = Double.NaN;
        public ItemStack input = ItemStack.EMPTY;
        public ItemStack output = ItemStack.EMPTY;
    }
}
