package com.rngtech.content.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Splits a connected mass of logs between the trunks standing in it, so tightly planted or merged trees are
 * harvested one tree at a time instead of as one oversized snapshot.
 */
public final class ForestryTreeScan {
    public static final int MAX_CLUSTER_LOGS = 2048;
    private static final int OWN = 0;
    private static final int[][] NEIGHBOURS = neighbourOffsets();

    private ForestryTreeScan() {
    }

    public static OwnedLogs ownedLogs(TreeBlocks blocks, Bounds bounds, BlockPos start, int logLimit) {
        start = start.immutable();
        if (logLimit <= 0 || !bounds.contains(start) || !blocks.isLog(start)) {
            return OwnedLogs.EMPTY;
        }
        Set<BlockPos> cluster = connectedLogs(blocks, bounds, start);
        if (cluster == null) {
            return OwnedLogs.TOO_LARGE;
        }

        Set<BlockPos> bases = new HashSet<>();
        bases.add(start);
        for (BlockPos log : cluster) {
            if (isTrunkBase(blocks, bounds, log)) {
                bases.add(log);
            }
        }
        List<BlockPos> ownBases = ownTrunkBases(bases, start);
        List<BlockPos> foreignBases = new ArrayList<>();
        for (BlockPos log : cluster) {
            if (bases.contains(log) && !ownBases.contains(log)) {
                foreignBases.add(log);
            }
        }

        Map<BlockPos, Integer> owners = new HashMap<>();
        for (BlockPos log : cluster) {
            owners.put(log, nearestOwner(log, ownBases, foreignBases));
        }
        Set<BlockPos> regions = new HashSet<>();
        Set<BlockPos> owned = new LinkedHashSet<>(growRegion(ownBases, OWN, owners, regions));
        for (int i = 0; i < foreignBases.size(); i++) {
            growRegion(List.of(foreignBases.get(i)), i + 1, owners, regions);
        }
        attachUnclaimed(owned, cluster, regions);
        if (owned.size() > logLimit) {
            return OwnedLogs.TOO_LARGE;
        }
        return new OwnedLogs(List.copyOf(owned), false);
    }

    public static List<BlockPos> leavesAround(TreeBlocks blocks, Bounds bounds, List<BlockPos> logs, int leafReach, int maxLeaves) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> leaves = new ArrayList<>();
        Set<BlockPos> logSet = new HashSet<>(logs);
        Set<BlockPos> seen = new HashSet<>(logs);
        for (BlockPos log : logs) {
            enqueueAdjacentLeaves(blocks, bounds, log, logs, leafReach, seen, queue);
        }
        while (!queue.isEmpty() && leaves.size() < maxLeaves) {
            BlockPos current = queue.remove();
            if (touchesForeignLog(blocks, current, logSet)) {
                continue;
            }
            leaves.add(current);
            enqueueAdjacentLeaves(blocks, bounds, current, logs, leafReach, seen, queue);
        }
        return List.copyOf(leaves);
    }

    public static boolean touchesForeignLog(TreeBlocks blocks, BlockPos leaf, Set<BlockPos> ownLogs) {
        for (int[] offset : NEIGHBOURS) {
            BlockPos neighbour = leaf.offset(offset[0], offset[1], offset[2]);
            if (!ownLogs.contains(neighbour) && blocks.isLog(neighbour)) {
                return true;
            }
        }
        return false;
    }

    public static boolean withinLeafReach(Collection<BlockPos> logs, BlockPos candidate, int leafReach) {
        for (BlockPos log : logs) {
            if (Math.abs(candidate.getX() - log.getX()) <= leafReach
                    && Math.abs(candidate.getY() - log.getY()) <= leafReach
                    && Math.abs(candidate.getZ() - log.getZ()) <= leafReach) {
                return true;
            }
        }
        return false;
    }

    private static Set<BlockPos> connectedLogs(TreeBlocks blocks, Bounds bounds, BlockPos start) {
        Set<BlockPos> cluster = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        cluster.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos current = queue.remove();
            for (int[] offset : NEIGHBOURS) {
                BlockPos next = current.offset(offset[0], offset[1], offset[2]);
                if (cluster.contains(next) || !bounds.contains(next) || !blocks.isLog(next)) {
                    continue;
                }
                if (cluster.size() >= MAX_CLUSTER_LOGS) {
                    return null;
                }
                cluster.add(next);
                queue.add(next);
            }
        }
        return cluster;
    }

    private static boolean isTrunkBase(TreeBlocks blocks, Bounds bounds, BlockPos log) {
        BlockPos below = log.below();
        return blocks.isSoil(below) || log.getY() == bounds.root().getY() && blocks.isLog(below);
    }

    private static List<BlockPos> ownTrunkBases(Set<BlockPos> bases, BlockPos start) {
        List<BlockPos> ownBases = new ArrayList<>(List.of(start));
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                BlockPos corner = start.offset(dx, 0, dz);
                List<BlockPos> square = List.of(corner, corner.east(), corner.south(), corner.east().south());
                if (!bases.containsAll(square)) {
                    continue;
                }
                for (BlockPos base : square) {
                    if (!ownBases.contains(base)) {
                        ownBases.add(base);
                    }
                }
            }
        }
        return ownBases;
    }

    private static int nearestOwner(BlockPos log, List<BlockPos> ownBases, List<BlockPos> foreignBases) {
        int owner = OWN;
        int nearest = Integer.MAX_VALUE;
        for (BlockPos base : ownBases) {
            nearest = Math.min(nearest, horizontalDistanceSquared(log, base));
        }
        for (int i = 0; i < foreignBases.size(); i++) {
            int distance = horizontalDistanceSquared(log, foreignBases.get(i));
            if (distance < nearest) {
                owner = i + 1;
                nearest = distance;
            }
        }
        return owner;
    }

    private static Set<BlockPos> growRegion(List<BlockPos> seeds, int owner, Map<BlockPos, Integer> owners, Set<BlockPos> regions) {
        Set<BlockPos> region = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos seed : seeds) {
            if (owners.get(seed) == owner && regions.add(seed)) {
                region.add(seed);
                queue.add(seed);
            }
        }
        while (!queue.isEmpty()) {
            BlockPos current = queue.remove();
            for (int[] offset : NEIGHBOURS) {
                BlockPos next = current.offset(offset[0], offset[1], offset[2]);
                Integer nextOwner = owners.get(next);
                if (nextOwner == null || nextOwner != owner || !regions.add(next)) {
                    continue;
                }
                region.add(next);
                queue.add(next);
            }
        }
        return region;
    }

    private static void attachUnclaimed(Set<BlockPos> owned, Set<BlockPos> cluster, Set<BlockPos> regions) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(owned);
        while (!queue.isEmpty()) {
            BlockPos current = queue.remove();
            for (int[] offset : NEIGHBOURS) {
                BlockPos next = current.offset(offset[0], offset[1], offset[2]);
                if (!cluster.contains(next) || regions.contains(next) || !owned.add(next)) {
                    continue;
                }
                queue.add(next);
            }
        }
    }

    private static void enqueueAdjacentLeaves(
            TreeBlocks blocks,
            Bounds bounds,
            BlockPos current,
            List<BlockPos> logs,
            int leafReach,
            Set<BlockPos> seen,
            ArrayDeque<BlockPos> queue
    ) {
        for (int[] offset : NEIGHBOURS) {
            BlockPos next = current.offset(offset[0], offset[1], offset[2]);
            if (!seen.add(next)
                    || !bounds.contains(next)
                    || !withinLeafReach(logs, next, leafReach)
                    || !blocks.isLeaves(next)) {
                continue;
            }
            queue.add(next);
        }
    }

    private static int horizontalDistanceSquared(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        return dx * dx + dz * dz;
    }

    private static int[][] neighbourOffsets() {
        int[][] offsets = new int[26][];
        int index = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dy != 0 || dz != 0) {
                        offsets[index++] = new int[] {dx, dy, dz};
                    }
                }
            }
        }
        return offsets;
    }

    public interface TreeBlocks {
        boolean isLog(BlockPos pos);

        boolean isLeaves(BlockPos pos);

        /** A non-tree block a trunk can stand on; logs resting on it are treated as trunk bases. */
        boolean isSoil(BlockPos pos);

        static TreeBlocks of(BlockGetter level) {
            return new TreeBlocks() {
                @Override
                public boolean isLog(BlockPos pos) {
                    return level.getBlockState(pos).is(BlockTags.LOGS);
                }

                @Override
                public boolean isLeaves(BlockPos pos) {
                    return level.getBlockState(pos).is(BlockTags.LEAVES);
                }

                @Override
                public boolean isSoil(BlockPos pos) {
                    BlockState state = level.getBlockState(pos);
                    if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                        return false;
                    }
                    return state.is(BlockTags.DIRT) || state.isFaceSturdy(level, pos, Direction.UP);
                }
            };
        }
    }

    public record Bounds(BlockPos root, int maxHeight, int horizontal) {
        public boolean contains(BlockPos pos) {
            return pos.getY() >= root.getY()
                    && pos.getY() <= root.getY() + maxHeight
                    && Math.abs(pos.getX() - root.getX()) <= horizontal
                    && Math.abs(pos.getZ() - root.getZ()) <= horizontal;
        }
    }

    public record OwnedLogs(List<BlockPos> logs, boolean tooLarge) {
        private static final OwnedLogs EMPTY = new OwnedLogs(List.of(), false);
        private static final OwnedLogs TOO_LARGE = new OwnedLogs(List.of(), true);
    }
}
