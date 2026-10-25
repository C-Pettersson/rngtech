package com.rngtech.content.menu;

import com.rngtech.RNGTech;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.workorder.LoadedWorkOrder;
import com.rngtech.rpg.workorder.MachineWorkOrders;
import com.rngtech.rpg.workorder.WorkFilter;
import com.rngtech.rpg.workorder.WorkObjective;
import com.rngtech.rpg.workorder.WorkObjectiveType;
import com.rngtech.rpg.workorder.WorkOrderHost;
import com.rngtech.rpg.workorder.WorkPart;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ContainerData;

import java.util.ArrayList;
import java.util.List;

/**
 * Syncs a machine's chips to its Orders tab as menu data: the active slot plus one, then for each slot its tier (0 for
 * an empty slot), Bounty, Prestige, part count, and Rush seconds left (0 without Rush, at least 1 with it), and for each
 * part its verb and filter plus one, minimum, item as its registry id plus one, target, and progress.
 */
public final class WorkOrderMenuSupport {
    private static final int PART_FIELDS = 6;
    private static final int HEADER_FIELDS = 5;
    private static final int SLOT_FIELDS = HEADER_FIELDS + WorkObjective.MAX_PARTS * PART_FIELDS;
    public static final int FIELD_COUNT = 1 + MachineWorkOrders.SLOTS * SLOT_FIELDS;
    private static final ResourceLocation SYNCED = RNGTech.id("synced");
    /**
     * Menu button ids from here up select a tab on the server, so Shift-click from the inventory fills a drive bay while
     * the Orders tab is open. Tab selection is otherwise client-only. Far above the Mastery tree's node buttons.
     */
    public static final int BUTTON_SELECT_TAB = 1_000_000;

    /** One synced chip: its tier, Bounty, Prestige, job, each part's progress, and Rush seconds left, 0 without Rush. */
    public record Slot(int tier, int bounty, int prestige, WorkObjective objective, List<Long> progress, int rushSeconds) {
        public boolean rushed() {
            return rushSeconds > 0;
        }

        public long totalTarget() {
            return objective.totalTarget();
        }

        public long totalProgress() {
            return progress.stream().mapToLong(Long::longValue).sum();
        }

        /** Every part has reached its target, so the bay holds a Completed Job Chip. */
        public boolean done() {
            for (int part = 0; part < objective.parts().size(); part++) {
                if (progress.get(part) < objective.parts().get(part).target()) {
                    return false;
                }
            }
            return true;
        }
    }

    /** The synced chips, in slot order, and the active slot or {@link MachineWorkOrders#NONE}. */
    public record View(List<Slot> slots, int active) {
        public static final View EMPTY = new View(List.of(), MachineWorkOrders.NONE);

        public Slot activeSlot() {
            return active >= 0 && active < slots.size() ? slots.get(active) : null;
        }
    }

    private WorkOrderMenuSupport() {
    }

    public static int get(MachineWorkOrders orders, int field, long gameTime) {
        if (field == 0) { return orders.active() + 1; }
        int slot = (field - 1) / SLOT_FIELDS;
        if (slot >= orders.orders().size()) { return 0; }
        LoadedWorkOrder loaded = orders.orders().get(slot);
        List<WorkPart> parts = loaded.order().objective().parts();
        int offset = (field - 1) % SLOT_FIELDS;
        switch (offset) {
            case 0: return loaded.order().tier();
            case 1: return loaded.order().bounty();
            case 2: return loaded.order().prestige();
            case 3: return parts.size();
            case 4: return loaded.rushed() ? (int) Math.max(1, Math.min(Integer.MAX_VALUE, (loaded.rushDeadline() - gameTime) / 20)) : 0;
            default: break;
        }
        int part = (offset - HEADER_FIELDS) / PART_FIELDS;
        if (part >= parts.size()) { return 0; }
        WorkPart value = parts.get(part);
        return switch ((offset - HEADER_FIELDS) % PART_FIELDS) {
            case 0 -> { WorkObjectiveType type = WorkObjectiveType.byId(value.type()); yield type == null ? 0 : type.ordinal() + 1; }
            case 1 -> { WorkFilter filter = WorkFilter.byId(value.filter()); yield filter == null ? 0 : filter.ordinal() + 1; }
            case 2 -> value.minimum();
            case 3 -> value.statGoal() ? statId(value.item()) : itemId(value.item());
            case 4 -> clamp(value.target());
            default -> clamp(loaded.progress().get(part));
        };
    }

    /** The server's live menu data for a machine without Mastery fields to embed the chips in. */
    public static ContainerData data(WorkOrderHost host) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return WorkOrderMenuSupport.get(host.workOrders(), index, host.workOrderGameTime());
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return FIELD_COUNT;
            }
        };
    }

    public static View view(ContainerData data, int base) {
        List<Slot> slots = new ArrayList<>();
        for (int slot = 0; slot < MachineWorkOrders.SLOTS; slot++) {
            int start = base + 1 + slot * SLOT_FIELDS;
            int tier = data.get(start);
            if (tier <= 0) { break; }
            int count = Math.clamp(data.get(start + 3), 1, WorkObjective.MAX_PARTS);
            List<WorkPart> parts = new ArrayList<>();
            List<Long> progress = new ArrayList<>();
            for (int part = 0; part < count; part++) {
                int at = start + HEADER_FIELDS + part * PART_FIELDS;
                int type = data.get(at) - 1;
                int filter = data.get(at + 1) - 1;
                parts.add(new WorkPart(
                        type >= 0 && type < WorkObjectiveType.values().length ? WorkObjectiveType.values()[type].id() : "",
                        filter >= 0 && filter < WorkFilter.values().length ? WorkFilter.values()[filter].id() : "",
                        data.get(at + 2), filter == WorkFilter.STAT.ordinal() ? statKey(data.get(at + 3)) : itemKey(data.get(at + 3)), data.get(at + 4)));
                progress.add((long) data.get(at + 5));
            }
            slots.add(new Slot(tier, data.get(start + 1), data.get(start + 2), new WorkObjective(SYNCED, parts, 0), progress, data.get(start + 4)));
        }
        int active = data.get(base) - 1;
        return new View(List.copyOf(slots), active >= 0 && active < slots.size() ? active : MachineWorkOrders.NONE);
    }

    private static int itemId(String item) {
        ResourceLocation key = item.isEmpty() ? null : ResourceLocation.tryParse(item);
        return key == null || !BuiltInRegistries.ITEM.containsKey(key) ? 0 : BuiltInRegistries.ITEM.getId(BuiltInRegistries.ITEM.get(key)) + 1;
    }

    private static String itemKey(int id) {
        return id <= 0 ? "" : BuiltInRegistries.ITEM.getKey(BuiltInRegistries.ITEM.byId(id - 1)).toString();
    }

    /** A stat goal's stat as its ordinal plus one, synced in the part's item field. */
    private static int statId(String stat) {
        MachineStat value = MachineStat.bySerializedName(stat);
        return value == null ? 0 : value.ordinal() + 1;
    }

    private static String statKey(int id) {
        return id <= 0 || id > MachineStat.values().length ? "" : MachineStat.byId(id - 1).getSerializedName();
    }

    private static int clamp(long value) {
        return (int) Math.min(Integer.MAX_VALUE, value);
    }
}
