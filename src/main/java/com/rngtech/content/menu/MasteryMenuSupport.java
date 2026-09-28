package com.rngtech.content.menu;

import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveTree;
import com.rngtech.rpg.progression.PassiveProgressionView;

import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ContainerData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class MasteryMenuSupport {
    public static final int FIELD_COUNT = 168;
    private static final int ATTRIBUTE_START = 165;

    public static int get(MachineProgressionState state, int field, Supplier<MachineStatAccumulator> stats) {
        if (field >= ATTRIBUTE_START && field < FIELD_COUNT) {
            return (int) Math.round(stats.get().value(attribute(field - ATTRIBUTE_START)) * 100);
        }
        return get(state, field);
    }

    private static MachineStat attribute(int index) {
        return switch (index) { case 0 -> MachineStat.CONTROL; case 1 -> MachineStat.DRIVE; default -> MachineStat.RESERVE; };
    }

    public static double attribute(ContainerData data, int base, MachineStat stat) {
        int index = switch (stat) { case CONTROL -> 0; case DRIVE -> 1; case RESERVE -> 2; default -> throw new IllegalArgumentException("Not an attribute"); };
        return data.get(base + ATTRIBUTE_START + index) / 100.0;
    }
    private static final int ALLOCATED_START = 6;
    private static final int TARGET_START = ALLOCATED_START + MegaPassiveTree.MAX_ALLOCATIONS;
    private static final int FOLLOWING = TARGET_START + MegaPassiveTree.MAX_ALLOCATIONS;

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
    public static MachineProgressionState snapshot(ContainerData data, int base, MachineMasteryFamily family) {
        return new MachineProgressionState(machineXp(data, base), 0, machineLevel(data, base),
                readNodes(data, base + ALLOCATED_START), family.startNodeId(), readNodes(data, base + TARGET_START), data.get(base + FOLLOWING) != 0);
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
}
