package com.rngtech.content.cable;

import net.minecraft.core.Direction;

/** Finds which cable arm a click landed on, from the hit position inside the cable's block space. */
public final class CableArmTarget {
    private static final double CORE_MIN = 6.0D / 16.0D;
    private static final double CORE_MAX = 10.0D / 16.0D;
    private static final double EPSILON = 1.0E-4D;

    private CableArmTarget() {
    }

    /**
     * Returns the direction of the arm containing the local hit point (0–1 on each axis), or null when the point is on
     * the core.
     */
    public static Direction armAt(double x, double y, double z) {
        Direction best = null;
        double bestDepth = EPSILON;
        for (Direction direction : Direction.values()) {
            double depth = depthOutsideCore(direction, x, y, z);
            if (depth > bestDepth) {
                bestDepth = depth;
                best = direction;
            }
        }
        return best;
    }

    private static double depthOutsideCore(Direction direction, double x, double y, double z) {
        return switch (direction) {
            case DOWN -> CORE_MIN - y;
            case UP -> y - CORE_MAX;
            case NORTH -> CORE_MIN - z;
            case SOUTH -> z - CORE_MAX;
            case WEST -> CORE_MIN - x;
            case EAST -> x - CORE_MAX;
        };
    }
}
