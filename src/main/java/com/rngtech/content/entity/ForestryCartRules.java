package com.rngtech.content.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/** Forestry Companion geometry and timing rules, kept free of entity state so they can be checked without a world. */
public final class ForestryCartRules {
    public static final int MIN_WORK_RANGE = 1;
    public static final int MAX_WORK_RANGE = 7;
    public static final int MIN_BREAK_TICKS = 6;
    public static final int PLANT_INTERVAL_TICKS = 30;
    /** Vanilla breaks a correctly tooled block in hardness * 30 / speed ticks; the cart takes this many times longer. */
    public static final double BREAK_TIME_SCALE = 2.0;
    /** Each doubling of a Treefeller batch adds this share of one log's break time. */
    public static final double BATCH_BREAK_GROWTH = 0.35;

    private ForestryCartRules() {
    }

    /** Clamps a Work Range stat value to the whole rows the cart works. */
    public static int workRange(double statValue) {
        return Math.max(MIN_WORK_RANGE, Math.min(MAX_WORK_RANGE, (int) Math.floor(statValue)));
    }

    /** Cells at distances 1..range on both sides of the rail, nearest rows first, left before right. */
    public static List<BlockPos> scanRoots(BlockPos railPos, Direction routeDirection, int range) {
        return sideCells(railPos, routeDirection, 1, Math.max(MIN_WORK_RANGE, range));
    }

    /** Cross-section cells beyond the current Work Range, up to the largest range a cart can reach. */
    public static List<BlockPos> pruneRoots(BlockPos railPos, Direction routeDirection, int range) {
        return sideCells(railPos, routeDirection, Math.max(MIN_WORK_RANGE, range) + 1, MAX_WORK_RANGE);
    }

    /** How many rows a cell sits from its rail block, measured across the route. */
    public static int rowDistance(BlockPos railPos, BlockPos root) {
        return Math.max(Math.abs(root.getX() - railPos.getX()), Math.abs(root.getZ() - railPos.getZ()));
    }

    /** The horizontal direction pointing from the rail toward a side cell in any row, or null on the rail itself. */
    public static Direction outwardDirection(BlockPos railPos, BlockPos root) {
        int dx = Integer.signum(root.getX() - railPos.getX());
        int dz = Integer.signum(root.getZ() - railPos.getZ());
        if (dx != 0 && dz != 0) {
            return null;
        }
        return Direction.fromDelta(dx, 0, dz);
    }

    /** Ticks to break one block from its hardness, the tool's speed against it, and Processing Speed. */
    public static int breakTicks(float hardness, float toolSpeed, double processingSpeed) {
        double ticks = Math.max(0.0F, hardness) * 30.0 * BREAK_TIME_SCALE / (Math.max(1.0F, toolSpeed) * Math.max(0.1, processingSpeed));
        return Math.max(MIN_BREAK_TICKS, (int) Math.ceil(ticks - 1.0E-9));
    }

    /** A Treefeller cracks its whole batch at once: slower than one log, far faster than cutting each in turn. */
    public static int batchBreakTicks(int singleTicks, int count) {
        double growth = 1.0 + BATCH_BREAK_GROWTH * Math.log(Math.max(1, count)) / Math.log(2.0);
        return Math.max(MIN_BREAK_TICKS, (int) Math.ceil(singleTicks * growth - 1.0E-9));
    }

    /** Ticks between plantings, independent of the tool. */
    public static int plantIntervalTicks(double processingSpeed) {
        return Math.max(MIN_BREAK_TICKS, (int) Math.ceil(PLANT_INTERVAL_TICKS / Math.max(0.1, processingSpeed) - 1.0E-9));
    }

    /** The crack stage, 0 to 9, shown after {@code elapsed} of {@code total} ticks. */
    public static int crackStage(int elapsed, int total) {
        return Math.max(0, Math.min(9, elapsed * 10 / Math.max(1, total)));
    }

    private static List<BlockPos> sideCells(BlockPos railPos, Direction routeDirection, int from, int to) {
        Direction forward = routeDirection.getAxis().isHorizontal() ? routeDirection : Direction.NORTH;
        Direction left = forward.getCounterClockWise();
        Direction right = forward.getClockWise();
        List<BlockPos> cells = new ArrayList<>();
        for (int distance = from; distance <= to; distance++) {
            cells.add(railPos.relative(left, distance));
            cells.add(railPos.relative(right, distance));
        }
        return List.copyOf(cells);
    }
}
