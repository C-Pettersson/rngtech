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
        String startNodeId, List<String> targetNodes, boolean following,
        String ascendancy, List<String> ascendancyNodes, int sealTiers
) implements PassiveProgressionView {
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 100;
    public static final int XP_REMAINDER_SCALE = 4;
    private static final long[] LEVEL_XP = createLevelXp();
    public static final MachineProgressionState EMPTY = new MachineProgressionState(0, 0, 1, List.of(), "", List.of(), false);
    private static final Codec<List<String>> NODES_CODEC = Codec.STRING.sizeLimitedListOf(MegaPassiveTree.MAX_ALLOCATIONS);
    private static final Codec<List<String>> ASCENDANCY_NODES_CODEC = Codec.STRING.sizeLimitedListOf(AscendancyCatalog.MAX_NODES);
    public static final Codec<MachineProgressionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("tree_version", 0).forGetter(state -> MegaPassiveTree.VERSION),
            Codec.LONG.optionalFieldOf("xp", 0L).forGetter(MachineProgressionState::xp),
            Codec.INT.optionalFieldOf("xp_remainder", 0).forGetter(MachineProgressionState::xpRemainder),
            Codec.INT.optionalFieldOf("level", MIN_LEVEL).forGetter(MachineProgressionState::level),
            NODES_CODEC.optionalFieldOf("allocated_nodes", List.of()).forGetter(MachineProgressionState::allocatedNodes),
            Codec.STRING.optionalFieldOf("start", "").forGetter(MachineProgressionState::startNodeId),
            NODES_CODEC.optionalFieldOf("target_nodes", List.of()).forGetter(MachineProgressionState::targetNodes),
            Codec.BOOL.optionalFieldOf("following", false).forGetter(MachineProgressionState::following),
            Codec.STRING.optionalFieldOf("ascendancy", "").forGetter(MachineProgressionState::ascendancy),
            ASCENDANCY_NODES_CODEC.optionalFieldOf("ascendancy_nodes", List.of()).forGetter(MachineProgressionState::ascendancyNodes),
            Codec.INT.optionalFieldOf("seal_tiers", 0).forGetter(MachineProgressionState::sealTiers)
    ).apply(instance, (version, xp, remainder, level, nodes, start, target, following, ascendancy, ascendancyNodes, sealTiers) -> new MachineProgressionState(
            xp, remainder, level, version == MegaPassiveTree.VERSION ? nodes : List.of(), start,
            version == MegaPassiveTree.VERSION ? target : List.of(), version == MegaPassiveTree.VERSION && following,
            ascendancy, ascendancyNodes, sealTiers)));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineProgressionState> STREAM_CODEC = new StreamCodec<>() {
        @Override public MachineProgressionState decode(RegistryFriendlyByteBuf buffer) {
            return new MachineProgressionState(buffer.readVarLong(), buffer.readVarInt(), buffer.readVarInt(),
                    readNodes(buffer, MegaPassiveTree.MAX_ALLOCATIONS), buffer.readUtf(80), readNodes(buffer, MegaPassiveTree.MAX_ALLOCATIONS), buffer.readBoolean(),
                    buffer.readUtf(80), readNodes(buffer, AscendancyCatalog.MAX_NODES), buffer.readVarInt());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, MachineProgressionState state) {
            buffer.writeVarLong(state.xp); buffer.writeVarInt(state.xpRemainder); buffer.writeVarInt(state.level);
            writeNodes(buffer, state.allocatedNodes); buffer.writeUtf(state.startNodeId, 80);
            writeNodes(buffer, state.targetNodes); buffer.writeBoolean(state.following);
            buffer.writeUtf(state.ascendancy, 80); writeNodes(buffer, state.ascendancyNodes); buffer.writeVarInt(state.sealTiers);
        }
    };

    public MachineProgressionState {
        xp = Math.max(0, xp);
        xpRemainder = Mth.clamp(xpRemainder, 0, XP_REMAINDER_SCALE - 1);
        level = Mth.clamp(Math.max(level, levelForXp(xp)), MIN_LEVEL, MAX_LEVEL);
        startNodeId = startNodeId == null ? "" : startNodeId;
        allocatedNodes = List.copyOf(allocatedNodes);
        targetNodes = List.copyOf(targetNodes);
        if (!allocatedNodes.isEmpty() && !MegaPassiveTree.validBuild(startNodeId, allocatedNodes)) {
            allocatedNodes = MegaPassiveTree.salvage(startNodeId, allocatedNodes);
        }
        if (allocatedNodes.size() > level - 1) { allocatedNodes = List.of(); }
        if (!targetNodes.isEmpty() && !MegaPassiveTree.validBuild(startNodeId, targetNodes)) {
            targetNodes = MegaPassiveTree.salvage(startNodeId, targetNodes);
        }
        following = following && !targetNodes.isEmpty() && targetNodes.containsAll(allocatedNodes);
        sealTiers = Mth.clamp(sealTiers, 0, AscendancyCatalog.MAX_TIERS);
        Ascendancy chosen = sealTiers > 0 ? AscendancyCatalog.get(ascendancy) : null;
        ascendancy = chosen == null ? "" : chosen.id();
        ascendancyNodes = chosen == null || ascendancyNodes == null ? List.of()
                : AscendancyCatalog.sanitize(chosen, ascendancyNodes, sealTiers * AscendancyCatalog.POINTS_PER_TIER);
    }

    public MachineProgressionState(long xp, int xpRemainder, int level, List<String> allocatedNodes,
            String startNodeId, List<String> targetNodes, boolean following) {
        this(xp, xpRemainder, level, allocatedNodes, startNodeId, targetNodes, following, "", List.of(), 0);
    }

    /** Resets the shared tree for a different start, and drops an ascendancy that belongs to another family. Earned Seal tiers stay. */
    public MachineProgressionState forFamily(MachineMasteryFamily family) {
        Ascendancy chosen = AscendancyCatalog.get(ascendancy);
        boolean sameStart = startNodeId.equals(family.startNodeId());
        boolean foreign = chosen != null && chosen.family() != family;
        if (sameStart && !foreign) { return this; }
        return new MachineProgressionState(xp, xpRemainder, level, sameStart ? allocatedNodes : List.of(), family.startNodeId(),
                sameStart ? targetNodes : List.of(), sameStart && following, foreign ? "" : ascendancy, foreign ? List.of() : ascendancyNodes, sealTiers);
    }

    public MachineProgressionState withAddedXp(long amount) { return withAddedScaledXp(amount, XP_REMAINDER_SCALE); }
    public MachineProgressionState withAddedScaledXp(long amount, int quarters) {
        quarters = Mth.clamp(quarters, 0, XP_REMAINDER_SCALE);
        if (amount <= 0 || quarters == 0 || level >= MAX_LEVEL) { return this; }
        long scaled = amount > (Long.MAX_VALUE - xpRemainder) / quarters ? Long.MAX_VALUE : amount * quarters + xpRemainder;
        long whole = scaled / XP_REMAINDER_SCALE;
        long next = Long.MAX_VALUE - xp < whole ? Long.MAX_VALUE : xp + whole;
        return shared(next, (int) (scaled % XP_REMAINDER_SCALE), allocatedNodes, targetNodes, following);
    }

    public MachineProgressionState withUnlockedNode(int index) {
        MegaPassiveNode node = MegaPassiveTree.byIndex(index);
        if (node == null || !MegaPassiveTree.TREE.canUnlock(node, this)) { return this; }
        List<String> nodes = new ArrayList<>(allocatedNodes); nodes.add(node.id());
        return shared(xp, xpRemainder, nodes, targetNodes, following);
    }
    public MachineProgressionState withTarget(List<String> target) { return shared(xp, xpRemainder, allocatedNodes, target, true); }
    public MachineProgressionState withFollowing(boolean enabled) { return shared(xp, xpRemainder, allocatedNodes, targetNodes, enabled); }
    public MachineProgressionState withoutNode(String id) {
        List<String> nodes = new ArrayList<>(allocatedNodes); nodes.remove(id);
        if (!MegaPassiveTree.connected(startNodeId, nodes)) { return this; }
        return shared(xp, xpRemainder, MegaPassiveTree.allocationOrder(startNodeId, nodes), targetNodes, false);
    }
    /** Clears the shared tree; the ascendancy is untouched. */
    public MachineProgressionState cleared() { return shared(xp, xpRemainder, List.of(), List.of(), false); }

    private MachineProgressionState shared(long xp, int xpRemainder, List<String> allocated, List<String> target, boolean following) {
        return new MachineProgressionState(xp, xpRemainder, level, allocated, startNodeId, target, following, ascendancy, ascendancyNodes, sealTiers);
    }

    private MachineProgressionState ascendant(String ascendancy, List<String> nodes, int tiers) {
        return new MachineProgressionState(xp, xpRemainder, level, allocatedNodes, startNodeId, targetNodes, following, ascendancy, nodes, tiers);
    }

    public int ascendancyPoints() { return sealTiers * AscendancyCatalog.POINTS_PER_TIER; }
    public int ascendancyUnspent() { return Math.max(0, ascendancyPoints() - ascendancyNodes.size()); }
    /** An earned tier without a valid choice, for example after an ascendancy is retired, allows a free choice. */
    public boolean awaitingAscendancyChoice() { return sealTiers > 0 && ascendancy.isEmpty(); }

    /**
     * Applies the next Seal tier. The first tier needs an entry stage of {@value AscendancyCatalog#ENTRY_STAGE} or more and
     * chooses one of the family's ascendancies; later tiers only add points. Returns this state when the Seal cannot be used.
     */
    public MachineProgressionState withSealTier(int tier, MachineMasteryFamily family, int entryStage, String choice) {
        if (tier != sealTiers + 1 || tier > AscendancyCatalog.MAX_TIERS) { return this; }
        if (tier == 1) {
            Ascendancy picked = AscendancyCatalog.get(choice);
            if (entryStage < AscendancyCatalog.ENTRY_STAGE || picked == null || picked.family() != family) { return this; }
            return ascendant(picked.id(), List.of(), tier);
        }
        return ascendancy.isEmpty() ? this : ascendant(ascendancy, ascendancyNodes, tier);
    }

    /** Chooses an ascendancy without a Seal when an earned tier has no valid choice. */
    public MachineProgressionState withAscendancyChoice(MachineMasteryFamily family, String choice) {
        Ascendancy picked = AscendancyCatalog.get(choice);
        if (!awaitingAscendancyChoice() || picked == null || picked.family() != family) { return this; }
        return ascendant(picked.id(), List.of(), sealTiers);
    }

    /** Switches to another ascendancy of the same family after every non-root node is refunded. Earned tiers stay. */
    public MachineProgressionState withSwitchedAscendancy(MachineMasteryFamily family, String choice) {
        Ascendancy picked = AscendancyCatalog.get(choice);
        if (ascendancy.isEmpty() || !ascendancyNodes.isEmpty() || picked == null || picked.family() != family || picked.id().equals(ascendancy)) {
            return this;
        }
        return ascendant(picked.id(), List.of(), sealTiers);
    }

    public MachineProgressionState withAscendancyNode(String nodeId) {
        Ascendancy chosen = AscendancyCatalog.get(ascendancy);
        AscendancyNode node = chosen == null ? null : chosen.node(nodeId);
        if (node == null || ascendancyUnspent() <= 0 || ascendancyNodes.contains(nodeId)
                || !(node.parent().equals(chosen.root().id()) || ascendancyNodes.contains(node.parent()))) {
            return this;
        }
        List<String> nodes = new ArrayList<>(ascendancyNodes); nodes.add(nodeId);
        return ascendant(ascendancy, nodes, sealTiers);
    }

    /** Refunds a node when no allocated node depends on it. The root is never refunded. */
    public MachineProgressionState withoutAscendancyNode(String nodeId) {
        Ascendancy chosen = AscendancyCatalog.get(ascendancy);
        if (chosen == null || !ascendancyNodes.contains(nodeId)
                || chosen.children(nodeId).stream().anyMatch(child -> ascendancyNodes.contains(child.id()))) {
            return this;
        }
        List<String> nodes = new ArrayList<>(ascendancyNodes); nodes.remove(nodeId);
        return ascendant(ascendancy, nodes, sealTiers);
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
    public static int progressionBand(int legacyBand) { return Math.min(MAX_LEVEL - 2, 1 + Math.max(0, legacyBand - 1) * (MAX_LEVEL - 3) / 24); }
    private static long[] createLevelXp() {
        long[] old = {0, 100, 243, 448, 741, 1160, 1760, 2619, 3848, 5600, 8100, 11650, 16650, 23650,
                33500, 47300, 66600, 93600, 131300, 184000, 257800, 361100, 505700, 708100, 991500,
                1388300, 1943800, 2721500, 3810300, 5334600};
        long[] result = java.util.Arrays.copyOf(old, MAX_LEVEL);
        for (int i = old.length; i < result.length; i++) { result[i] = Math.round(result[i - 1] * 1.10); }
        return result;
    }
    private static List<String> readNodes(RegistryFriendlyByteBuf buffer, int limit) {
        int count = buffer.readVarInt();
        if (count < 0 || count > limit) { throw new IllegalArgumentException("Invalid mastery allocation count"); }
        List<String> nodes = new ArrayList<>(); for (int i = 0; i < count; i++) { nodes.add(buffer.readUtf(80)); } return nodes;
    }
    private static void writeNodes(RegistryFriendlyByteBuf buffer, List<String> nodes) {
        buffer.writeVarInt(nodes.size()); nodes.forEach(id -> buffer.writeUtf(id, 80));
    }
}
