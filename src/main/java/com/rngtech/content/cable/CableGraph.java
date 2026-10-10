package com.rngtech.content.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Cable connectivity without the world: which cables link, and which cables one network reaches. The world-backed
 * network snapshot and the randomised graph checks both run on it.
 */
public final class CableGraph {
    private static final Direction[] DIRECTIONS = Direction.values();

    private CableGraph() {
    }

    /** What the traversal may read about one position. It never reads a position that is not loaded. */
    public interface View {
        boolean isLoaded(BlockPos pos);

        /** True when the loaded position holds a cable. */
        boolean isCable(BlockPos pos);

        /** True when the cable at the position shows an arm toward {@code direction}. */
        boolean hasLink(BlockPos pos, Direction direction);
    }

    /**
     * Whether two touching cables link: neither side switched the link off with a Wrench, and their dyes match or at
     * least one is undyed. A {@code null} colour means undyed.
     */
    public static boolean canLink(Object color, Object neighborColor, boolean disabledHere, boolean disabledThere) {
        return !disabledHere && !disabledThere && colorsLink(color, neighborColor);
    }

    public static boolean colorsLink(Object first, Object second) {
        return first == null || second == null || Objects.equals(first, second);
    }

    /**
     * Every cable the network at {@code start} reaches, in breadth-first order from {@code start}. Two cables are
     * joined only when both show an arm toward each other and both are loaded; an unloaded position is the edge.
     */
    public static List<BlockPos> component(BlockPos start, View view) {
        if (start == null || !view.isLoaded(start) || !view.isCable(start)) {
            return Collections.emptyList();
        }
        List<BlockPos> component = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos first = start.immutable();
        visited.add(first);
        queue.addLast(first);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            component.add(pos);
            for (Direction direction : DIRECTIONS) {
                if (!view.hasLink(pos, direction)) {
                    continue;
                }
                BlockPos neighbor = pos.relative(direction);
                if (view.isLoaded(neighbor)
                        && view.isCable(neighbor)
                        && view.hasLink(neighbor, direction.getOpposite())
                        && visited.add(neighbor)) {
                    queue.addLast(neighbor);
                }
            }
        }
        return component;
    }
}
