package com.rngtech.content.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Executable Forestry Companion Work Range geometry checks. */
public final class ForestryCartRulesChecks {
    private static final BlockPos RAIL = new BlockPos(10, 64, -4);
    private static int checks;

    private ForestryCartRulesChecks() {
    }

    public static void run() {
        rangeOneMatchesTheOriginalSideCells();
        rootsCoverEveryRowOnBothSides();
        pruneRootsStartPastTheRange();
        outwardDirectionWorksOnOuterRows();
        workRangeIsClamped();
        breakTimeFollowsHardnessToolAndSpeed();
        slopeFootGetsTheSlopeClearance();
        System.out.println("Forestry cart rules: " + checks + " checks passed");
    }

    private static void rangeOneMatchesTheOriginalSideCells() {
        for (Direction route : Direction.Plane.HORIZONTAL) {
            List<BlockPos> roots = ForestryCartRules.scanRoots(RAIL, route, 1);
            require(roots.equals(List.of(RAIL.relative(route.getCounterClockWise()), RAIL.relative(route.getClockWise()))),
                    "range 1 works the two blocks beside the rail heading " + route);
        }
        require(ForestryCartRules.scanRoots(RAIL, Direction.UP, 1).equals(ForestryCartRules.scanRoots(RAIL, Direction.NORTH, 1)),
                "a vertical route falls back to north");
    }

    private static void rootsCoverEveryRowOnBothSides() {
        for (Direction route : Direction.Plane.HORIZONTAL) {
            for (int range = 1; range <= ForestryCartRules.MAX_WORK_RANGE; range++) {
                List<BlockPos> roots = ForestryCartRules.scanRoots(RAIL, route, range);
                Set<BlockPos> distinct = new HashSet<>(roots);
                require(roots.size() == 2 * range && distinct.size() == roots.size(), "range " + range + " gives " + 2 * range + " distinct cells");
                for (int index = 0; index < roots.size(); index++) {
                    BlockPos root = roots.get(index);
                    int along = route.getAxis() == Direction.Axis.X ? root.getX() - RAIL.getX() : root.getZ() - RAIL.getZ();
                    require(along == 0 && root.getY() == RAIL.getY(), "cells sit perpendicular to the route at rail height");
                    require(ForestryCartRules.rowDistance(RAIL, root) == index / 2 + 1, "cells are ordered nearest row first");
                }
            }
        }
    }

    private static void pruneRootsStartPastTheRange() {
        for (int range = 1; range <= ForestryCartRules.MAX_WORK_RANGE; range++) {
            List<BlockPos> prune = ForestryCartRules.pruneRoots(RAIL, Direction.EAST, range);
            require(prune.size() == 2 * (ForestryCartRules.MAX_WORK_RANGE - range), "range " + range + " prunes every row out to the maximum");
            int worked = range;
            require(prune.stream().allMatch(pos -> ForestryCartRules.rowDistance(RAIL, pos) > worked), "pruning never touches a worked row");
            Set<BlockPos> overlap = new HashSet<>(prune);
            overlap.retainAll(ForestryCartRules.scanRoots(RAIL, Direction.EAST, range));
            require(overlap.isEmpty(), "pruned and worked cells never overlap");
        }
    }

    private static void outwardDirectionWorksOnOuterRows() {
        for (Direction route : Direction.Plane.HORIZONTAL) {
            for (int distance = 1; distance <= ForestryCartRules.MAX_WORK_RANGE; distance++) {
                Direction left = route.getCounterClockWise();
                require(ForestryCartRules.outwardDirection(RAIL, RAIL.relative(left, distance)) == left,
                        "row " + distance + " points away from the rail");
            }
        }
        require(ForestryCartRules.outwardDirection(RAIL, RAIL) == null, "the rail itself has no outward direction");
        require(ForestryCartRules.outwardDirection(RAIL, RAIL.offset(2, 0, 3)) == null, "a diagonal cell has no outward direction");
    }

    private static void workRangeIsClamped() {
        require(ForestryCartRules.workRange(0.0) == 1, "Work Range never drops below one row");
        require(ForestryCartRules.workRange(3.9) == 3, "partial rows round down");
        require(ForestryCartRules.workRange(12.0) == ForestryCartRules.MAX_WORK_RANGE, "Work Range caps at seven rows");
    }

    private static void breakTimeFollowsHardnessToolAndSpeed() {
        int ironAxeLog = ForestryCartRules.breakTicks(2.0F, 5.0F, 1.0);
        require(ironAxeLog == 24, "an iron-speed axe cracks a log in 24 ticks");
        require(ForestryCartRules.breakTicks(3.0F, 5.0F, 1.0) > ironAxeLog, "harder blocks take longer");
        require(ForestryCartRules.breakTicks(2.0F, 9.0F, 1.0) < ironAxeLog, "faster tools break sooner");
        require(ForestryCartRules.breakTicks(2.0F, 5.0F, 1.5) < ironAxeLog, "Processing Speed shortens the break");
        require(ForestryCartRules.breakTicks(0.2F, 5.0F, 1.0) == ForestryCartRules.MIN_BREAK_TICKS, "soft blocks still take the minimum");
        require(ForestryCartRules.breakTicks(2.0F, 0.0F, 1.0) == ForestryCartRules.breakTicks(2.0F, 1.0F, 1.0), "tool speed never drops below one");
        int previous = 0;
        for (int count = 1; count <= 64; count *= 2) {
            int batch = ForestryCartRules.batchBreakTicks(30, count);
            require(batch > previous || count == 1, "bigger batches take longer");
            require(batch < 30 * count || count == 1, "a batch is faster than cutting each log in turn");
            previous = batch;
        }
        require(ForestryCartRules.batchBreakTicks(30, 1) == 30, "a one-log batch takes one log's time");
        require(ForestryCartRules.plantIntervalTicks(1.0) == ForestryCartRules.PLANT_INTERVAL_TICKS, "planting takes 30 ticks at base speed");
        require(ForestryCartRules.crackStage(0, 20) == 0 && ForestryCartRules.crackStage(19, 20) == 9 && ForestryCartRules.crackStage(40, 20) == 9,
                "crack stages run 0 to 9");
    }

    /** A leaf over the flat rail at a slope's foot blocks a cart still riding the slope, so it must count as in the path. */
    private static void slopeFootGetsTheSlopeClearance() {
        double cartHeight = 0.7;
        int low = 64;
        double leafBottom = low + 1;
        require(ForestryCartRules.clearanceTop(low, false, cartHeight) < leafBottom, "a flat rail alone clears a block one above");
        double descending = ForestryCartRules.transitionClearanceTop(low, true, low, false, cartHeight);
        double climbing = ForestryCartRules.transitionClearanceTop(low, false, low, true, cartHeight);
        require(descending > leafBottom, "leaving a slope onto its foot reaches the block over the foot");
        require(climbing > leafBottom, "climbing from the foot onto a slope reaches the block over the foot");
        double top = ForestryCartRules.transitionClearanceTop(low + 1, false, low, true, cartHeight);
        require(top == ForestryCartRules.clearanceTop(low + 1, false, cartHeight), "the rail at a slope's top keeps its own clearance");
        require(top < low + 2, "a one-high tunnel over the top rail stays passable");
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
