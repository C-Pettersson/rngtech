package com.rngtech.content.blockentity;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * What a Battery Chassis shows in the world: one entry per active cell slot, the total charge, whether energy is flowing
 * in or out, and the plate on each side. Charges run 0 to 1; {@code charge} is negative while the chassis holds no
 * capacity. The side masks hold a bit per {@link Direction#get3DDataValue()}.
 *
 * @param cells      one entry per active slot, in slot order; an empty slot has an empty kind
 * @param connectors sides a cable or Universal Connector faces, which show a connector port
 * @param contacts   other sides with an FE neighbour, such as a machine or another chassis, which show a smooth plate
 */
public record BatteryChassisVisualState(List<Cell> cells, float charge, Flow flow, int connectors, int contacts) {
    public static final BatteryChassisVisualState EMPTY = new BatteryChassisVisualState(List.of(), -1.0F, Flow.IDLE, 0, 0);
    public static final int METER_SEGMENTS = 8;
    private static final int CELL_STEPS = 16;

    public BatteryChassisVisualState {
        cells = List.copyOf(cells);
    }

    public int slots() {
        return cells.size();
    }

    public Plate plate(Direction side) {
        int bit = 1 << side.get3DDataValue();
        if ((connectors & bit) != 0) {
            return Plate.CONNECTOR;
        }
        return (contacts & bit) != 0 ? Plate.CONTACT : Plate.NONE;
    }

    /** Lit meter segments: none without stored FE, one as soon as the chassis holds any. */
    public int meterSegments() {
        return charge <= 0.0F ? 0 : Math.max(1, Math.min(METER_SEGMENTS, Math.round(charge * METER_SEGMENTS)));
    }

    /** Quantized so a slowly charging bank does not send an update every tick. */
    public Key key() {
        List<String> kinds = new ArrayList<>(cells.size());
        List<Integer> charges = new ArrayList<>(cells.size());
        for (Cell cell : cells) {
            kinds.add(cell.kind());
            charges.add(Math.round(cell.charge() * CELL_STEPS));
        }
        return new Key(kinds, charges, charge < 0.0F ? -1 : meterSegments(), flow, connectors, contacts);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Cell cell : cells) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Kind", cell.kind());
            entry.putFloat("Charge", cell.charge());
            list.add(entry);
        }
        tag.put("Cells", list);
        tag.putFloat("Charge", charge);
        tag.putByte("Flow", (byte) flow.ordinal());
        tag.putByte("Connectors", (byte) connectors);
        tag.putByte("Contacts", (byte) contacts);
        return tag;
    }

    public static BatteryChassisVisualState load(CompoundTag tag) {
        if (tag == null || !tag.contains("Cells")) {
            return EMPTY;
        }
        List<Cell> cells = new ArrayList<>();
        for (Tag entry : tag.getList("Cells", Tag.TAG_COMPOUND)) {
            CompoundTag cell = (CompoundTag) entry;
            cells.add(new Cell(cell.getString("Kind"), cell.getFloat("Charge")));
        }
        Flow[] flows = Flow.values();
        int flow = tag.getByte("Flow");
        return new BatteryChassisVisualState(
                cells,
                tag.getFloat("Charge"),
                flow >= 0 && flow < flows.length ? flows[flow] : Flow.IDLE,
                tag.getByte("Connectors") & 0x3F,
                tag.getByte("Contacts") & 0x3F
        );
    }

    /**
     * One cell slot.
     *
     * @param kind   the cell's {@code BatteryCellMaterial} name, or empty for an empty slot
     * @param charge the cell's fill in this chassis
     */
    public record Cell(String kind, float charge) {
        public boolean empty() {
            return kind.isEmpty();
        }
    }

    /** Which way FE is moving, judged over the last second so a bursty source does not flicker. */
    public enum Flow {
        IDLE,
        CHARGING,
        DISCHARGING,
        /** FE passes through in about equal amounts. */
        BALANCED
    }

    /** What covers one side of the chassis. */
    public enum Plate {
        NONE,
        /** A port for the cable or Universal Connector facing this side. */
        CONNECTOR,
        /** A smooth contact plate against a machine or another chassis. */
        CONTACT
    }

    public record Key(List<String> kinds, List<Integer> charges, int charge, Flow flow, int connectors, int contacts) {
    }
}
