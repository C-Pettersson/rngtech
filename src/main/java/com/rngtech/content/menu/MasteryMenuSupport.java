package com.rngtech.content.menu;

import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.progression.Ascendancy;
import com.rngtech.rpg.progression.AscendancyCatalog;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MasteryDeclarations;
import com.rngtech.rpg.progression.MegaPassiveTree;
import com.rngtech.rpg.progression.PassiveProgressionView;
import com.rngtech.rpg.unique.UniqueCatalog;
import com.rngtech.rpg.unique.UniqueStatLine;

import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ContainerData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class MasteryMenuSupport {
    private static final int ALLOCATED_START = 6;
    private static final int TARGET_START = ALLOCATED_START + MegaPassiveTree.MAX_ALLOCATIONS;
    private static final int FOLLOWING = TARGET_START + MegaPassiveTree.MAX_ALLOCATIONS;
    private static final int ATTRIBUTE_START = FOLLOWING + 1;
    private static final int ASCENDANCY = ATTRIBUTE_START + 3;
    private static final int SEAL_TIERS = ASCENDANCY + 1;
    private static final int ENTRY_STAGE = SEAL_TIERS + 1;
    private static final int ASCENDANCY_NODES_START = ENTRY_STAGE + 1;
    /** Declared stats granted by the ascendancy, as a stat id plus one and its value times 100. */
    private static final int GRANTED_STATS_START = ASCENDANCY_NODES_START + AscendancyCatalog.MAX_POINTS;
    public static final int GRANTED_STAT_SLOTS = 8;
    public static final int FIELD_COUNT = GRANTED_STATS_START + GRANTED_STAT_SLOTS * 2;

    private static final Map<MachineMasteryHost, List<MachineStat>> SHOWN = Collections.synchronizedMap(new WeakHashMap<>());

    /** A declared stat the chosen ascendancy grants, with its current value. */
    public record GrantedStat(MachineStat stat, double value) {
    }

    public static int get(MachineMasteryHost host, int field, Supplier<MachineStatAccumulator> stats) {
        if (field >= ATTRIBUTE_START && field < ASCENDANCY) {
            return (int) Math.round(stats.get().value(attribute(field - ATTRIBUTE_START)) * 100);
        }
        if (field == ENTRY_STAGE) { return host.ascendancyEntryStage(); }
        if (field >= GRANTED_STATS_START) {
            int slot = (field - GRANTED_STATS_START) / 2;
            // Sync reads fields in order, so the list is built once per pass, at the first granted-stat field.
            List<MachineStat> granted = field == GRANTED_STATS_START || !SHOWN.containsKey(host)
                    ? shownStats(host, stats) : SHOWN.get(host);
            SHOWN.put(host, granted);
            if (slot >= granted.size()) { return 0; }
            MachineStat stat = granted.get(slot);
            return (field - GRANTED_STATS_START) % 2 == 0 ? stat.ordinal() + 1 : (int) Math.round(stats.get().value(stat) * 100);
        }
        return field >= ASCENDANCY ? ascendancyField(host.masteryState(), field) : get(host.machineProgression(), field);
    }

    /** Ascendancy stats the chosen ascendancy grants, then Unique Gear stats the machine currently has. */
    private static List<MachineStat> shownStats(MachineMasteryHost host, Supplier<MachineStatAccumulator> stats) {
        List<MachineStat> shown = new ArrayList<>(AscendancyCatalog.grantedStats(host.masteryState(), host.masteryFamily()));
        MachineStatAccumulator values = stats.get();
        for (MachineStat stat : UniqueGearStats.STATS) {
            if (!shown.contains(stat) && Math.abs(values.value(stat)) > 1.0E-9) {
                shown.add(stat);
            }
        }
        return shown.size() > GRANTED_STAT_SLOTS ? shown.subList(0, GRANTED_STAT_SLOTS) : shown;
    }

    public static List<GrantedStat> grantedStats(ContainerData data, int base) {
        List<GrantedStat> granted = new ArrayList<>();
        for (int slot = 0; slot < GRANTED_STAT_SLOTS; slot++) {
            int id = data.get(base + GRANTED_STATS_START + slot * 2) - 1;
            if (id >= 0 && id < MachineStat.values().length
                    && (MasteryDeclarations.declared(MachineStat.byId(id)) || UniqueGearStats.STATS.contains(MachineStat.byId(id)))) {
                granted.add(new GrantedStat(MachineStat.byId(id), data.get(base + GRANTED_STATS_START + slot * 2 + 1) / 100.0));
            }
        }
        return granted;
    }

    /** The chosen ascendancy as its catalog index plus one, and its nodes as their index in that ascendancy plus one. */
    private static int ascendancyField(MachineProgressionState state, int field) {
        Ascendancy chosen = AscendancyCatalog.get(state.ascendancy());
        if (field == ASCENDANCY) { return chosen == null ? 0 : AscendancyCatalog.index(chosen) + 1; }
        if (field == SEAL_TIERS) { return state.sealTiers(); }
        int slot = field - ASCENDANCY_NODES_START;
        return chosen == null || slot >= state.ascendancyNodes().size() ? 0 : chosen.nodeIndex(state.ascendancyNodes().get(slot)) + 1;
    }

    private static MachineStat attribute(int index) {
        return switch (index) { case 0 -> MachineStat.CONTROL; case 1 -> MachineStat.DRIVE; default -> MachineStat.RESERVE; };
    }

    public static double attribute(ContainerData data, int base, MachineStat stat) {
        int index = switch (stat) { case CONTROL -> 0; case DRIVE -> 1; case RESERVE -> 2; default -> throw new IllegalArgumentException("Not an attribute"); };
        return data.get(base + ATTRIBUTE_START + index) / 100.0;
    }

    public static int get(MachineProgressionState state, int field) {
        if (field >= ALLOCATED_START && field < TARGET_START) { return encodedNode(state.allocatedNodes(), field - ALLOCATED_START); }
        if (field >= TARGET_START && field < FOLLOWING) { return encodedNode(state.targetNodes(), field - TARGET_START); }
        return switch (field) {
            case 0 -> (int) state.xp();
            case 1 -> (int) (state.xp() >>> 32);
            case 2 -> state.level();
            case 3 -> (int) Math.min(Integer.MAX_VALUE, state.xpInCurrentLevel());
            case 4 -> (int) Math.min(Integer.MAX_VALUE, state.xpNeededForNextLevel());
            case 5 -> state.unspentPoints();
            case FOLLOWING -> state.following() ? 1 : 0;
            default -> 0;
        };
    }
    private static int encodedNode(List<String> nodes, int index) {
        if (index >= nodes.size()) { return 0; }
        var node = MegaPassiveTree.node(nodes.get(index));
        return node == null ? 0 : node.index() + 1;
    }
    public static long machineXp(ContainerData data, int base) {
        return Integer.toUnsignedLong(data.get(base)) | Integer.toUnsignedLong(data.get(base + 1)) << 32;
    }
    public static int machineLevel(ContainerData data, int base) { return data.get(base + 2); }
    public static int machineXpInLevel(ContainerData data, int base) { return data.get(base + 3); }
    public static int machineXpToNextLevel(ContainerData data, int base) { return data.get(base + 4); }
    public static float machineXpProgress(ContainerData data, int base) {
        int needed = machineXpToNextLevel(data, base);
        return needed <= 0 ? 1 : Mth.clamp(machineXpInLevel(data, base) / (float) needed, 0, 1);
    }
    public static int unspentPassivePoints(ContainerData data, int base) { return data.get(base + 5); }
    public static boolean hasPassiveNodeIndex(ContainerData data, int base, int index) {
        for (int i = 0; i < MegaPassiveTree.MAX_ALLOCATIONS; i++) {
            if (data.get(base + ALLOCATED_START + i) == index + 1) { return index >= 0; }
        }
        return false;
    }
    public static long unlockedPassiveNodeMask(ContainerData data, int base) { return mask(data, base, 0); }
    public static long unlockedPassiveNodeMaskHigh(ContainerData data, int base) { return mask(data, base, 64); }
    private static long mask(ContainerData data, int base, int offset) {
        long result = 0; for (int i = 0; i < 64; i++) { if (hasPassiveNodeIndex(data, base, offset + i)) { result |= 1L << i; } } return result;
    }
    public static int ascendancyEntryStage(ContainerData data, int base) { return data.get(base + ENTRY_STAGE); }
    public static MachineProgressionState snapshot(ContainerData data, int base, MachineMasteryFamily family) {
        Ascendancy chosen = AscendancyCatalog.byIndex(data.get(base + ASCENDANCY) - 1);
        List<String> ascendancyNodes = new ArrayList<>();
        for (int i = 0; chosen != null && i < AscendancyCatalog.MAX_POINTS; i++) {
            var node = chosen.nodeAt(data.get(base + ASCENDANCY_NODES_START + i) - 1);
            if (node != null) { ascendancyNodes.add(node.id()); }
        }
        return new MachineProgressionState(machineXp(data, base), 0, machineLevel(data, base),
                readNodes(data, base + ALLOCATED_START), family.startNodeId(), readNodes(data, base + TARGET_START), data.get(base + FOLLOWING) != 0,
                chosen == null ? "" : chosen.id(), ascendancyNodes, data.get(base + SEAL_TIERS));
    }
    private static List<String> readNodes(ContainerData data, int base) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < MegaPassiveTree.MAX_ALLOCATIONS; i++) {
            var node = MegaPassiveTree.byIndex(data.get(base + i) - 1);
            if (node != null) { result.add(node.id()); }
        }
        return result;
    }
    public static PassiveProgressionView progressionView(IntPredicate hasNode, IntSupplier level, IntSupplier points, String start) {
        return new PassiveProgressionView() {
            @Override public boolean hasNode(int index) { return hasNode.test(index); }
            @Override public int level() { return level.getAsInt(); }
            @Override public int unspentPoints() { return points.getAsInt(); }
            @Override public String startNodeId() { return start; }
        };
    }
    private MasteryMenuSupport() { }

    /** Stats only Unique Gear grants outside an ascendancy, shown on the Stats tab while a machine has them. */
    private static final class UniqueGearStats {
        private static final Set<MachineStat> STATS = UniqueCatalog.all().stream()
                .flatMap(unique -> unique.lines().stream())
                .filter(line -> line.role() != UniqueStatLine.Role.HOOK)
                .map(UniqueStatLine::stat)
                .filter(stat -> MasteryDeclarations.declared(stat) || stat == MachineStat.CYCLE_JAM_CHANCE || stat == MachineStat.ESCAPEMENT_SPEED)
                .collect(java.util.stream.Collectors.toCollection(() -> java.util.EnumSet.noneOf(MachineStat.class)));
    }
}
