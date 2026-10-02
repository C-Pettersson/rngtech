package com.rngtech.content.entity;

import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Executable Forestry Companion tree-ownership checks against a synthetic block grid. */
public final class ForestryTreeScanChecks {
    private static final int HORIZONTAL_BOUND = 8;
    private static final int MAX_HEIGHT = 64;
    private static final int LOG_LIMIT = 512;
    private static int checks;

    private ForestryTreeScanChecks() {
    }

    public static void run() {
        tightRowHarvestsOneTrunk();
        rowEdgesStayInsideTheirOwnTree();
        mergedBranchesSplitBetweenTrunks();
        orphanedBranchFollowsReachableTrunk();
        twoByTwoTrunkIsOneTree();
        overhangingBranchIsClippedNotRejected();
        downhillTrunkIsItsOwnTree();
        oversizedTreeAndStructureAreRejected();
        leavesTouchingNeighbourTrunksAreLeft();
        System.out.println("Forestry tree scan: " + checks + " checks passed");
    }

    private static void tightRowHarvestsOneTrunk() {
        Grid grid = new Grid();
        for (int x = 0; x <= 20; x++) {
            grid.trunk(x, 0, 5);
        }
        ForestryTreeScan.OwnedLogs owned = grid.owned(new BlockPos(10, 0, 0));
        require(!owned.tooLarge(), "tight row is not too large");
        require(Set.copyOf(owned.logs()).equals(column(10, 0, 5)), "tight row snapshot takes only the root trunk");
    }

    private static void rowEdgesStayInsideTheirOwnTree() {
        Grid grid = new Grid();
        for (int x = 0; x <= 20; x++) {
            grid.trunk(x, 0, 5);
        }
        require(Set.copyOf(grid.owned(new BlockPos(0, 0, 0)).logs()).equals(column(0, 0, 5)), "row start takes only its trunk");
        require(Set.copyOf(grid.owned(new BlockPos(20, 0, 0)).logs()).equals(column(20, 0, 5)), "row end takes only its trunk");
    }

    private static void mergedBranchesSplitBetweenTrunks() {
        Grid grid = new Grid();
        grid.trunk(0, 0, 6);
        grid.trunk(4, 0, 6);
        for (int x = 1; x <= 3; x++) {
            grid.log(x, 5, 0);
        }
        Set<BlockPos> left = Set.copyOf(grid.owned(new BlockPos(0, 0, 0)).logs());
        Set<BlockPos> right = Set.copyOf(grid.owned(new BlockPos(4, 0, 0)).logs());
        require(left.containsAll(column(0, 0, 6)) && !left.contains(new BlockPos(4, 0, 0)), "left tree keeps its trunk only");
        require(right.containsAll(column(4, 0, 6)) && !right.contains(new BlockPos(0, 0, 0)), "right tree keeps its trunk only");
        require(left.contains(new BlockPos(1, 5, 0)) && !left.contains(new BlockPos(3, 5, 0)), "near branch goes to the left tree");
        require(right.contains(new BlockPos(3, 5, 0)) && !right.contains(new BlockPos(1, 5, 0)), "near branch goes to the right tree");
        require(left.contains(new BlockPos(2, 5, 0)) && right.contains(new BlockPos(2, 5, 0)), "midpoint branch is claimed from both sides");
        Set<BlockPos> union = new HashSet<>(left);
        union.addAll(right);
        require(union.equals(grid.logs), "merged trees leave no unclaimed logs");
    }

    private static void orphanedBranchFollowsReachableTrunk() {
        Grid grid = new Grid();
        grid.trunk(0, 0, 3);
        grid.trunk(6, 0, 6);
        for (int x = 1; x <= 5; x++) {
            grid.log(x, 3, 0);
        }
        for (int x = 2; x <= 5; x++) {
            grid.log(x, 5, 0);
        }
        Set<BlockPos> shortTree = Set.copyOf(grid.owned(new BlockPos(0, 0, 0)).logs());
        Set<BlockPos> tallTree = Set.copyOf(grid.owned(new BlockPos(6, 0, 0)).logs());
        require(!shortTree.contains(new BlockPos(2, 5, 0)) && !shortTree.contains(new BlockPos(3, 5, 0)), "unreachable branch logs are not taken by the nearer short tree");
        require(tallTree.containsAll(List.of(new BlockPos(2, 5, 0), new BlockPos(3, 5, 0))), "branch tip stays with the tree it hangs from");
        Set<BlockPos> union = new HashSet<>(shortTree);
        union.addAll(tallTree);
        require(union.equals(grid.logs), "orphaned branch logs are still claimed");
    }

    private static void twoByTwoTrunkIsOneTree() {
        Grid grid = new Grid();
        grid.trunk(0, 0, 10);
        grid.trunk(1, 0, 10);
        grid.trunk(0, 1, 10);
        grid.trunk(1, 1, 10);
        grid.trunk(2, 0, 4);
        ForestryTreeScan.OwnedLogs owned = grid.owned(new BlockPos(1, 0, 1));
        require(owned.logs().size() == 40, "2x2 trunk is harvested as one tree");
        require(!owned.logs().contains(new BlockPos(2, 0, 0)), "neighbour beside a 2x2 trunk stays separate");
    }

    private static void overhangingBranchIsClippedNotRejected() {
        Grid grid = new Grid();
        grid.trunk(0, 0, 6);
        for (int x = 1; x <= 12; x++) {
            grid.log(x, 5, 0);
        }
        ForestryTreeScan.OwnedLogs owned = grid.owned(new BlockPos(0, 0, 0));
        require(!owned.tooLarge(), "branch leaving the scan bounds does not reject the tree");
        require(owned.logs().contains(new BlockPos(8, 5, 0)) && !owned.logs().contains(new BlockPos(9, 5, 0)), "branch is clipped at the scan bounds");
    }

    private static void downhillTrunkIsItsOwnTree() {
        Grid grid = new Grid();
        grid.trunk(0, 0, 6);
        for (int y = -3; y <= 5; y++) {
            grid.log(3, y, 0);
        }
        grid.log(1, 5, 0);
        grid.log(2, 5, 0);
        Set<BlockPos> owned = Set.copyOf(grid.owned(BlockPos.ZERO).logs());
        require(owned.equals(union(column(0, 0, 6), Set.of(new BlockPos(1, 5, 0)))), "trunk rising from below the scan floor stays a separate tree");
    }

    private static void oversizedTreeAndStructureAreRejected() {
        Grid tall = new Grid();
        tall.trunk(0, 0, 12);
        require(ForestryTreeScan.ownedLogs(tall, bounds(BlockPos.ZERO), BlockPos.ZERO, 8).tooLarge(), "tree over the log limit is too large");

        Grid block = new Grid();
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                block.trunk(x, z, 8);
            }
        }
        require(block.owned(BlockPos.ZERO).tooLarge(), "log mass over the cluster cap is too large");
        require(!new Grid().owned(BlockPos.ZERO).tooLarge() && new Grid().owned(BlockPos.ZERO).logs().isEmpty(), "missing tree is empty");
    }

    private static void leavesTouchingNeighbourTrunksAreLeft() {
        Grid grid = new Grid();
        grid.trunk(0, 0, 5);
        grid.trunk(3, 0, 5);
        grid.leaves(1, 4, 0);
        grid.leaves(2, 4, 0);
        grid.leaves(0, 5, 0);
        ForestryTreeScan.Bounds bounds = bounds(BlockPos.ZERO);
        List<BlockPos> logs = grid.owned(BlockPos.ZERO).logs();
        Set<BlockPos> leaves = Set.copyOf(ForestryTreeScan.leavesAround(grid, bounds, logs, 4, 192));
        require(leaves.contains(new BlockPos(1, 4, 0)) && leaves.contains(new BlockPos(0, 5, 0)), "own canopy is collected");
        require(!leaves.contains(new BlockPos(2, 4, 0)), "leaves touching a neighbour trunk wait for that tree");
    }

    private static Set<BlockPos> column(int x, int z, int height) {
        Set<BlockPos> logs = new HashSet<>();
        for (int y = 0; y < height; y++) {
            logs.add(new BlockPos(x, y, z));
        }
        return logs;
    }

    private static Set<BlockPos> union(Set<BlockPos> first, Set<BlockPos> second) {
        Set<BlockPos> union = new HashSet<>(first);
        union.addAll(second);
        return union;
    }

    private static ForestryTreeScan.Bounds bounds(BlockPos root) {
        return new ForestryTreeScan.Bounds(root, MAX_HEIGHT, HORIZONTAL_BOUND);
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    private static final class Grid implements ForestryTreeScan.TreeBlocks {
        private final Set<BlockPos> logs = new HashSet<>();
        private final Set<BlockPos> leaves = new HashSet<>();

        private void trunk(int x, int z, int height) {
            for (int y = 0; y < height; y++) {
                log(x, y, z);
            }
        }

        private void log(int x, int y, int z) {
            logs.add(new BlockPos(x, y, z));
        }

        private void leaves(int x, int y, int z) {
            leaves.add(new BlockPos(x, y, z));
        }

        private ForestryTreeScan.OwnedLogs owned(BlockPos root) {
            return ForestryTreeScan.ownedLogs(this, bounds(root), root, LOG_LIMIT);
        }

        @Override
        public boolean isLog(BlockPos pos) {
            return logs.contains(pos);
        }

        @Override
        public boolean isLeaves(BlockPos pos) {
            return leaves.contains(pos);
        }

        @Override
        public boolean isSoil(BlockPos pos) {
            return pos.getY() == -1 && !logs.contains(pos);
        }
    }
}
