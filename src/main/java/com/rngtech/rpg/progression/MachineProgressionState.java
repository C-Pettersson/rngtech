package com.rngtech.rpg.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public record MachineProgressionState(
        long xp, int xpRemainder, int level, List<String> allocatedNodes,
        String startNodeId, List<String> targetNodes, boolean following
) implements PassiveProgressionView {
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 80;
    public static final int XP_REMAINDER_SCALE = 4;
    private static final long[] LEVEL_XP = createLevelXp();
    public static final MachineProgressionState EMPTY = new MachineProgressionState(0, 0, 1, List.of(), "", List.of(), false);
    private static final Codec<List<String>> NODES_CODEC = Codec.STRING.sizeLimitedListOf(MegaPassiveTree.MAX_ALLOCATIONS);
    public static final Codec<MachineProgressionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("tree_version", 0).forGetter(state -> MegaPassiveTree.VERSION),
            Codec.LONG.optionalFieldOf("xp", 0L).forGetter(MachineProgressionState::xp),
            Codec.INT.optionalFieldOf("xp_remainder", 0).forGetter(MachineProgressionState::xpRemainder),
            Codec.INT.optionalFieldOf("level", MIN_LEVEL).forGetter(MachineProgressionState::level),
            NODES_CODEC.optionalFieldOf("allocated_nodes", List.of()).forGetter(MachineProgressionState::allocatedNodes),
            Codec.STRING.optionalFieldOf("start", "").forGetter(MachineProgressionState::startNodeId),
            NODES_CODEC.optionalFieldOf("target_nodes", List.of()).forGetter(MachineProgressionState::targetNodes),
            Codec.BOOL.optionalFieldOf("following", false).forGetter(MachineProgressionState::following)
    ).apply(instance, (version, xp, remainder, level, nodes, start, target, following) -> new MachineProgressionState(
            xp, remainder, level, version == MegaPassiveTree.VERSION ? nodes : List.of(), start,
            version == MegaPassiveTree.VERSION ? target : List.of(), version == MegaPassiveTree.VERSION && following)));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineProgressionState> STREAM_CODEC = new StreamCodec<>() {
        @Override public MachineProgressionState decode(RegistryFriendlyByteBuf buffer) {
            return new MachineProgressionState(buffer.readVarLong(), buffer.readVarInt(), buffer.readVarInt(),
                    readNodes(buffer), buffer.readUtf(80), readNodes(buffer), buffer.readBoolean());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, MachineProgressionState state) {
            buffer.writeVarLong(state.xp); buffer.writeVarInt(state.xpRemainder); buffer.writeVarInt(state.level);
            writeNodes(buffer, state.allocatedNodes); buffer.writeUtf(state.startNodeId, 80);
            writeNodes(buffer, state.targetNodes); buffer.writeBoolean(state.following);
        }
    };

    public MachineProgressionState {
        xp = Math.max(0, xp);
        xpRemainder = Mth.clamp(xpRemainder, 0, XP_REMAINDER_SCALE - 1);
        level = Mth.clamp(Math.max(level, levelForXp(xp)), MIN_LEVEL, MAX_LEVEL);
        startNodeId = startNodeId == null ? "" : startNodeId;
        allocatedNodes = List.copyOf(allocatedNodes);
        targetNodes = List.copyOf(targetNodes);
        if (!allocatedNodes.isEmpty() && (allocatedNodes.size() > level - 1 || !MegaPassiveTree.validBuild(startNodeId, allocatedNodes))) {
            allocatedNodes = List.of();
        }
        if (!targetNodes.isEmpty() && !MegaPassiveTree.validBuild(startNodeId, targetNodes)) { targetNodes = List.of(); }
        following = following && !targetNodes.isEmpty() && targetNodes.containsAll(allocatedNodes);
    }

    public MachineProgressionState forFamily(MachineMasteryFamily family) {
        if (startNodeId.equals(family.startNodeId())) { return this; }
        return new MachineProgressionState(xp, xpRemainder, level, List.of(), family.startNodeId(), List.of(), false);
    }

    public MachineProgressionState withAddedXp(long amount) { return withAddedScaledXp(amount, XP_REMAINDER_SCALE); }
    public MachineProgressionState withAddedScaledXp(long amount, int quarters) {
        quarters = Mth.clamp(quarters, 0, XP_REMAINDER_SCALE);
        if (amount <= 0 || quarters == 0 || level >= MAX_LEVEL) { return this; }
        long scaled = amount > (Long.MAX_VALUE - xpRemainder) / quarters ? Long.MAX_VALUE : amount * quarters + xpRemainder;
        long whole = scaled / XP_REMAINDER_SCALE;
        long next = Long.MAX_VALUE - xp < whole ? Long.MAX_VALUE : xp + whole;
        return new MachineProgressionState(next, (int) (scaled % XP_REMAINDER_SCALE), level,
                allocatedNodes, startNodeId, targetNodes, following);
    }

    public MachineProgressionState withUnlockedNode(int index) {
        MegaPassiveNode node = MegaPassiveTree.byIndex(index);
        if (node == null || !MegaPassiveTree.TREE.canUnlock(node, this)) { return this; }
        List<String> nodes = new ArrayList<>(allocatedNodes); nodes.add(node.id());
        return new MachineProgressionState(xp, xpRemainder, level, nodes, startNodeId, targetNodes, following);
    }
    public MachineProgressionState withTarget(List<String> target) {
        return new MachineProgressionState(xp, xpRemainder, level, allocatedNodes, startNodeId, target, true);
    }
    public MachineProgressionState withFollowing(boolean enabled) {
        return new MachineProgressionState(xp, xpRemainder, level, allocatedNodes, startNodeId, targetNodes, enabled);
    }
    public MachineProgressionState withoutNode(String id) {
        List<String> nodes = new ArrayList<>(allocatedNodes); nodes.remove(id);
        if (!MegaPassiveTree.connected(startNodeId, nodes)) { return this; }
        return new MachineProgressionState(xp, xpRemainder, level,
                MegaPassiveTree.allocationOrder(startNodeId, nodes), startNodeId, targetNodes, false);
    }
    public MachineProgressionState cleared() {
        return new MachineProgressionState(xp, xpRemainder, level, List.of(), startNodeId, List.of(), false);
    }
    @Override public boolean hasNode(int index) {
        MegaPassiveNode node = MegaPassiveTree.byIndex(index);
        return node != null && allocatedNodes.contains(node.id());
    }
    public int spentPoints() { return allocatedNodes.size(); }
    public int totalPoints() { return level - 1; }
    @Override public int unspentPoints() { return Math.max(0, totalPoints() - spentPoints()); }
    public long unlockedNodeMask() { return legacyMask(0); }
    public long unlockedNodeMaskHigh() { return legacyMask(64); }
    private long legacyMask(int start) {
        long mask = 0; for (int i = 0; i < 64; i++) { if (hasNode(start + i)) { mask |= 1L << i; } } return mask;
    }
    public long xpForCurrentLevel() { return xpForLevel(level); }
    public long xpForNextLevel() { return xpForLevel(level + 1); }
    public long xpInCurrentLevel() { return Math.max(0, xp - xpForCurrentLevel()); }
    public long xpNeededForNextLevel() { return level >= MAX_LEVEL ? 0 : xpForNextLevel() - xpForCurrentLevel(); }
    public static int levelForXp(long xp) {
        int result = 1;
        while (result < MAX_LEVEL && xp >= LEVEL_XP[result]) { result++; }
        return result;
    }
    public static long xpForLevel(int level) { return LEVEL_XP[Mth.clamp(level, 1, MAX_LEVEL) - 1]; }
    public static int xpQuarters(int level, int band) {
        int delta = level - band;
        return delta <= 0 ? 4 : delta == 1 ? 2 : delta == 2 ? 1 : 0;
    }
    public static long workXp(long base, int band) {
        long multiplier = 1L + (long) Math.max(1, band) * Math.max(1, band) / 20;
        return base > Long.MAX_VALUE / multiplier ? Long.MAX_VALUE : base * multiplier;
    }
    public static int progressionBand(int legacyBand) { return Math.min(78, 1 + Math.max(0, legacyBand - 1) * 77 / 24); }
    private static long[] createLevelXp() {
        long[] old = {0, 100, 243, 448, 741, 1160, 1760, 2619, 3848, 5600, 8100, 11650, 16650, 23650,
                33500, 47300, 66600, 93600, 131300, 184000, 257800, 361100, 505700, 708100, 991500,
                1388300, 1943800, 2721500, 3810300, 5334600};
        long[] result = java.util.Arrays.copyOf(old, MAX_LEVEL);
        for (int i = old.length; i < result.length; i++) { result[i] = Math.round(result[i - 1] * 1.10); }
        return result;
    }
    private static List<String> readNodes(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MegaPassiveTree.MAX_ALLOCATIONS) { throw new IllegalArgumentException("Invalid mastery allocation count"); }
        List<String> nodes = new ArrayList<>(); for (int i = 0; i < count; i++) { nodes.add(buffer.readUtf(80)); } return nodes;
    }
    private static void writeNodes(RegistryFriendlyByteBuf buffer, List<String> nodes) {
        buffer.writeVarInt(nodes.size()); nodes.forEach(id -> buffer.writeUtf(id, 80));
    }
}
