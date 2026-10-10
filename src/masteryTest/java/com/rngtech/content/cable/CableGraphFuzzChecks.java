package com.rngtech.content.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Randomised checks for cable connectivity. A model world places, breaks, dyes, and wrenches cables and loads and
 * unloads chunks. After every step, the traversal the network snapshot uses must find exactly the networks a
 * brute-force union of linked pairs finds.
 *
 * <p>When every edit happens with its neighbours loaded, as a player's edits do, the networks must follow the link
 * rule itself. Edits next to an unloaded chunk leave that chunk's arms stale, as block states are; then the networks
 * must follow the arms both cables show.</p>
 *
 * <p>The model also keeps a {@link NetworkIndex} of snapshots that never expire and reports to it the events the
 * cable block and block entity report: a cable placed, broken, loaded, unloaded, or changing its arms, and a dye or
 * Wrench switch on a cable. Every cached snapshot must still match the brute-force network, so a change the
 * invalidation rules miss fails here instead of leaving routing stale.</p>
 */
public final class CableGraphFuzzChecks {
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final int SIZE_X = 8;
    private static final int SIZE_Y = 3;
    private static final int SIZE_Z = 8;
    private static final int CHUNK = 4;
    private static final int COLORS = 3;
    private static final int STEPS = 6_000;
    private static int checks;

    private CableGraphFuzzChecks() {
    }

    public static void run() {
        linkRuleMatchesDyes();
        for (long seed = 1; seed <= 4; seed++) {
            fuzz(seed, seed > 2);
        }
        System.out.println("Cable graph fuzz: " + checks + " checks passed");
    }

    private static void linkRuleMatchesDyes() {
        require(CableGraph.canLink(null, null, false, false), "undyed cables link");
        require(CableGraph.canLink(1, null, false, false), "a dyed cable links to an undyed one");
        require(CableGraph.canLink(1, 1, false, false), "cables of one colour link");
        require(!CableGraph.canLink(1, 2, false, false), "different colours never link");
        require(!CableGraph.canLink(null, null, true, false), "a link switched off on one side stays off");
        require(!CableGraph.canLink(null, null, false, true), "a link switched off on the other side stays off");
    }

    private static void fuzz(long seed, boolean borderEdits) {
        Random random = new Random(seed);
        ModelWorld world = new ModelWorld(borderEdits);
        for (int step = 0; step < STEPS; step++) {
            String operation = world.randomStep(random);
            compare(world, random, seed, step, operation);
        }
    }

    /** Every loaded cable's network from the traversal must equal its brute-force network. */
    private static void compare(ModelWorld world, Random random, long seed, int step, String operation) {
        Map<BlockPos, Set<BlockPos>> expected = world.bruteForceNetworks();
        Set<Set<BlockPos>> seen = new HashSet<>();
        for (Map.Entry<BlockPos, Set<BlockPos>> entry : expected.entrySet()) {
            Set<BlockPos> network = entry.getValue();
            if (!seen.add(network) && random.nextInt(4) != 0) {
                continue;
            }
            List<BlockPos> found = CableGraph.component(entry.getKey(), world);
            Set<BlockPos> foundSet = new HashSet<>(found);
            if (found.size() != foundSet.size() || !foundSet.equals(network)) {
                throw new IllegalStateException("seed " + seed + " step " + step + " after " + operation
                        + ": traversal from " + entry.getKey() + " found " + foundSet.size()
                        + " cables, brute force " + network.size());
            }
            checks++;
        }
        for (Map.Entry<BlockPos, Set<BlockPos>> entry : expected.entrySet()) {
            Set<BlockPos> cached = world.cachedNetwork(entry.getKey(), step);
            if (!cached.equals(entry.getValue())) {
                throw new IllegalStateException("seed " + seed + " step " + step + " after " + operation
                        + ": cached network at " + entry.getKey() + " has " + cached.size()
                        + " cables, brute force " + entry.getValue().size());
            }
            checks++;
        }
        BlockPos probe = world.randomPos(random);
        if (!world.isLoaded(probe) || !world.isCable(probe)) {
            require(CableGraph.component(probe, world).isEmpty(), "no network starts from an unloaded or empty space");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
        checks++;
    }

    /**
     * Cables on a small grid. Link arms are kept the way the cable block keeps them: recomputed from the link rule for
     * a cable and its neighbours whenever one is placed, broken, dyed or wrenched, and left alone while unloaded.
     */
    private static final class ModelWorld implements CableGraph.View {
        private final Map<BlockPos, Cable> cables = new HashMap<>();
        private final Set<Long> unloadedChunks = new HashSet<>();
        private final NetworkIndex<Set<BlockPos>> index = new NetworkIndex<>();
        private final boolean borderEdits;

        private ModelWorld(boolean borderEdits) {
            this.borderEdits = borderEdits;
        }

        @Override
        public boolean isLoaded(BlockPos pos) {
            return inBounds(pos) && !unloadedChunks.contains(chunkKey(pos));
        }

        @Override
        public boolean isCable(BlockPos pos) {
            requireLoaded(pos);
            return cables.containsKey(pos);
        }

        @Override
        public boolean hasLink(BlockPos pos, Direction direction) {
            requireLoaded(pos);
            Cable cable = cables.get(pos);
            return cable != null && cable.arms.contains(direction);
        }

        private void requireLoaded(BlockPos pos) {
            if (!isLoaded(pos)) {
                throw new IllegalStateException("the traversal read " + pos + ", which is not loaded");
            }
        }

        private String randomStep(Random random) {
            int roll = random.nextInt(100);
            if (roll < 4) {
                return toggleChunk(random);
            }
            BlockPos pos = randomLoadedPos(random);
            if (pos == null) {
                return "nothing (all unloaded)";
            }
            Cable cable = cables.get(pos);
            if (cable == null) {
                place(pos, random.nextInt(COLORS + 1));
                return "place " + pos;
            }
            if (roll < 30) {
                breakCable(pos);
                return "break " + pos;
            }
            if (roll < 55) {
                dye(pos, random.nextInt(COLORS + 1));
                return "dye " + pos;
            }
            Direction direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
            wrench(pos, direction);
            return "wrench " + pos + " " + direction;
        }

        /** The snapshot the index holds for a cable, built and stored the way the network cache does on a miss. */
        private Set<BlockPos> cachedNetwork(BlockPos pos, long tick) {
            Set<BlockPos> cached = index.get(pos, tick, Long.MAX_VALUE);
            if (cached == null) {
                List<BlockPos> component = CableGraph.component(pos, this);
                cached = new HashSet<>(component);
                index.put(cached, component, tick);
            }
            return cached;
        }

        /** The block placed (onPlace) and its block entity loaded (onLoad). */
        private void place(BlockPos pos, int color) {
            cables.put(pos, new Cable(color == 0 ? null : color));
            index.cableChanged(pos);
            refreshAround(pos);
        }

        /** The block removed (onRemove) and its block entity removed (setRemoved). */
        private void breakCable(BlockPos pos) {
            cables.remove(pos);
            index.cableChanged(pos);
            refreshAround(pos);
        }

        /** Dyeing invalidates the cable (setColor), then refreshes its arms and its neighbours'. */
        private void dye(BlockPos pos, int color) {
            cables.get(pos).color = color == 0 ? null : color;
            index.connectorChanged(pos);
            refreshAround(pos);
        }

        /** Like the Wrench on a cable arm: switch a live link off, or switch an off link back on if dyes allow. */
        private void wrench(BlockPos pos, Direction direction) {
            BlockPos neighborPos = pos.relative(direction);
            Cable cable = cables.get(pos);
            Cable neighbor = isLoaded(neighborPos) ? cables.get(neighborPos) : null;
            if (neighbor == null) {
                return;
            }
            boolean disable = cable.arms.contains(direction);
            if (!disable && !CableGraph.colorsLink(cable.color, neighbor.color)) {
                return;
            }
            setDisabled(cable, direction, disable);
            setDisabled(neighbor, direction.getOpposite(), disable);
            index.connectorChanged(pos);
            index.connectorChanged(neighborPos);
            refresh(pos);
            refresh(neighborPos);
        }

        /** Every cable block entity in the chunk is removed on unload (setRemoved) and loaded again (onLoad). */
        private String toggleChunk(Random random) {
            long key = chunkKey(randomPos(random));
            boolean load = unloadedChunks.remove(key);
            if (!load) {
                unloadedChunks.add(key);
            }
            for (BlockPos pos : cables.keySet()) {
                if (chunkKey(pos) == key) {
                    index.cableChanged(pos);
                }
            }
            return (load ? "load chunk " : "unload chunk ") + key;
        }

        private void refreshAround(BlockPos pos) {
            refresh(pos);
            for (Direction direction : DIRECTIONS) {
                refresh(pos.relative(direction));
            }
        }

        /** Recomputes a loaded cable's arms; a changed block state reports itself (onRemove and onPlace). */
        private void refresh(BlockPos pos) {
            Cable cable = cables.get(pos);
            if (cable == null || !isLoaded(pos)) {
                return;
            }
            EnumSet<Direction> before = EnumSet.copyOf(cable.arms);
            cable.arms.clear();
            for (Direction direction : DIRECTIONS) {
                if (linksByRule(pos, direction)) {
                    cable.arms.add(direction);
                }
            }
            if (!cable.arms.equals(before)) {
                index.cableChanged(pos);
            }
        }

        private boolean linksByRule(BlockPos pos, Direction direction) {
            BlockPos neighborPos = pos.relative(direction);
            Cable cable = cables.get(pos);
            Cable neighbor = cables.get(neighborPos);
            return cable != null
                    && neighbor != null
                    && CableGraph.canLink(
                            cable.color,
                            neighbor.color,
                            cable.disabled.contains(direction),
                            neighbor.disabled.contains(direction.getOpposite())
                    );
        }

        /** Rule-only worlds join pairs the link rule allows; border-edit worlds join pairs that both show the arm. */
        private boolean linked(BlockPos pos, Direction direction) {
            if (!borderEdits) {
                return linksByRule(pos, direction);
            }
            Cable neighbor = cables.get(pos.relative(direction));
            return cables.get(pos).arms.contains(direction)
                    && neighbor != null
                    && neighbor.arms.contains(direction.getOpposite());
        }

        /** Loaded cables joined by every linked pair, found by union rather than by walking the network. */
        private Map<BlockPos, Set<BlockPos>> bruteForceNetworks() {
            Map<BlockPos, BlockPos> parent = new HashMap<>();
            for (BlockPos pos : cables.keySet()) {
                if (isLoaded(pos)) {
                    parent.put(pos, pos);
                }
            }
            for (BlockPos pos : parent.keySet()) {
                for (Direction direction : DIRECTIONS) {
                    BlockPos neighbor = pos.relative(direction);
                    if (parent.containsKey(neighbor) && linked(pos, direction)) {
                        union(parent, pos, neighbor);
                    }
                }
            }
            Map<BlockPos, Set<BlockPos>> byRoot = new HashMap<>();
            for (BlockPos pos : parent.keySet()) {
                byRoot.computeIfAbsent(find(parent, pos), ignored -> new HashSet<>()).add(pos);
            }
            Map<BlockPos, Set<BlockPos>> networks = new HashMap<>();
            for (BlockPos pos : parent.keySet()) {
                networks.put(pos, byRoot.get(find(parent, pos)));
            }
            return networks;
        }

        private static BlockPos find(Map<BlockPos, BlockPos> parent, BlockPos pos) {
            BlockPos root = pos;
            while (!parent.get(root).equals(root)) {
                root = parent.get(root);
            }
            parent.put(pos, root);
            return root;
        }

        private static void union(Map<BlockPos, BlockPos> parent, BlockPos first, BlockPos second) {
            BlockPos firstRoot = find(parent, first);
            BlockPos secondRoot = find(parent, second);
            if (!firstRoot.equals(secondRoot)) {
                parent.put(firstRoot, secondRoot);
            }
        }

        private BlockPos randomPos(Random random) {
            return new BlockPos(random.nextInt(SIZE_X), random.nextInt(SIZE_Y), random.nextInt(SIZE_Z));
        }

        /** A position a player could build at: it and every neighbour inside the grid are loaded. */
        private BlockPos randomLoadedPos(Random random) {
            for (int attempt = 0; attempt < 20; attempt++) {
                BlockPos pos = randomPos(random);
                if (isLoaded(pos) && (borderEdits || neighborsLoaded(pos))) {
                    return pos;
                }
            }
            return null;
        }

        private boolean neighborsLoaded(BlockPos pos) {
            for (Direction direction : DIRECTIONS) {
                BlockPos neighbor = pos.relative(direction);
                if (inBounds(neighbor) && !isLoaded(neighbor)) {
                    return false;
                }
            }
            return true;
        }

        private static void setDisabled(Cable cable, Direction direction, boolean disabled) {
            if (disabled) {
                cable.disabled.add(direction);
            } else {
                cable.disabled.remove(direction);
            }
        }

        private static boolean inBounds(BlockPos pos) {
            return pos.getX() >= 0 && pos.getX() < SIZE_X
                    && pos.getY() >= 0 && pos.getY() < SIZE_Y
                    && pos.getZ() >= 0 && pos.getZ() < SIZE_Z;
        }

        private static long chunkKey(BlockPos pos) {
            return Math.floorDiv(pos.getX(), CHUNK) * 31L + Math.floorDiv(pos.getZ(), CHUNK);
        }
    }

    private static final class Cable {
        private final EnumSet<Direction> arms = EnumSet.noneOf(Direction.class);
        private final EnumSet<Direction> disabled = EnumSet.noneOf(Direction.class);
        private Integer color;

        private Cable(Integer color) {
            this.color = color;
        }
    }
}
