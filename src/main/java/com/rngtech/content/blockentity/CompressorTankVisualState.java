package com.rngtech.content.blockentity;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * What a Fluid or Compressor Tank shows in the world: its fluid, how full the loose and compressed stores are, the
 * installed Servos and Battery Cell, and which sides a cable or Universal Connector faces. Fills run 0 to 1;
 * {@code compressed} is negative on a plain Fluid Tank, and part keys are empty for an empty slot.
 *
 * @param fluid      the fluid's registry id, or empty while the tank is empty
 * @param servos     one material key per Servo slot, such as {@code steel} or {@code unique}
 * @param cell       the installed Battery Cell's material key
 * @param connectors a bit per {@link Direction#get3DDataValue()} for each side that shows a connector port
 */
public record CompressorTankVisualState(
        String fluid,
        float loose,
        float compressed,
        List<String> servos,
        String cell,
        int connectors
) {
    public static final int SERVO_SLOTS = 4;
    public static final CompressorTankVisualState EMPTY =
            new CompressorTankVisualState("", 0.0F, -1.0F, List.of("", "", "", ""), "", 0);

    public CompressorTankVisualState {
        servos = List.copyOf(servos);
    }

    public boolean hasCompressedStore() {
        return compressed >= 0.0F;
    }

    public boolean connector(Direction side) {
        return (connectors & 1 << side.get3DDataValue()) != 0;
    }

    /** Quantized so a tank filling or draining by a few mB does not send an update. */
    public Key key() {
        return new Key(fluid, Math.round(loose * 48.0F), compressed < 0.0F ? -1 : Math.round(compressed * 48.0F), servos, cell, connectors);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Fluid", fluid);
        tag.putFloat("Loose", loose);
        tag.putFloat("Compressed", compressed);
        ListTag list = new ListTag();
        servos.forEach(servo -> list.add(StringTag.valueOf(servo)));
        tag.put("Servos", list);
        tag.putString("Cell", cell);
        tag.putByte("Connectors", (byte) connectors);
        return tag;
    }

    public static CompressorTankVisualState load(CompoundTag tag) {
        if (tag == null || !tag.contains("Fluid")) {
            return EMPTY;
        }
        ListTag list = tag.getList("Servos", Tag.TAG_STRING);
        List<String> servos = new ArrayList<>(SERVO_SLOTS);
        for (int slot = 0; slot < SERVO_SLOTS; slot++) {
            servos.add(slot < list.size() ? list.getString(slot) : "");
        }
        return new CompressorTankVisualState(
                tag.getString("Fluid"),
                tag.getFloat("Loose"),
                tag.getFloat("Compressed"),
                servos,
                tag.getString("Cell"),
                tag.getByte("Connectors") & 0x3F
        );
    }

    public record Key(String fluid, int loose, int compressed, List<String> servos, String cell, int connectors) {
    }

    /** Client-side easing between updates, so levels and the gauge glide and the pumps run up and coast down. */
    public static final class Animation {
        public float loose = Float.NaN;
        public float compressed = Float.NaN;
        public float spin;
        public float angle;
        public double lastTime = Double.NaN;
    }
}
