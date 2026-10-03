package com.rngtech.client.screen;

import com.rngtech.rpg.progression.Ascendancy;
import com.rngtech.rpg.progression.AscendancyNode;

import java.util.ArrayList;
import java.util.List;

/** Fits an ascendancy's authored grid into a box, keeping one grid step between nodes. The root comes first. */
public final class AscendancyTreeLayout {
    private static final float MAX_SPACING = 28.0F;

    public record Placed(AscendancyNode node, boolean deep, float x, float y, float radius) {
        public boolean contains(double pointX, double pointY) {
            double dx = pointX - x;
            double dy = pointY - y;
            float reach = Math.max(radius + 1.0F, 4.0F);
            return dx * dx + dy * dy <= reach * reach;
        }
    }

    private AscendancyTreeLayout() { }

    public static List<Placed> fit(Ascendancy ascendancy, float left, float top, float right, float bottom) {
        return fit(ascendancy, left, top, right, bottom, MAX_SPACING);
    }

    /** As {@link #fit(Ascendancy, float, float, float, float)}, with a wider grid step allowed when the box has room. */
    public static List<Placed> fit(Ascendancy ascendancy, float left, float top, float right, float bottom, float maxSpacing) {
        List<AscendancyNode> nodes = new ArrayList<>();
        nodes.add(ascendancy.root());
        nodes.addAll(ascendancy.nodes().values());
        int minX = nodes.stream().mapToInt(AscendancyNode::x).min().orElse(0);
        int maxX = nodes.stream().mapToInt(AscendancyNode::x).max().orElse(0);
        int minY = nodes.stream().mapToInt(AscendancyNode::y).min().orElse(0);
        int maxY = nodes.stream().mapToInt(AscendancyNode::y).max().orElse(0);
        float spacing = Math.min(maxSpacing, Math.min((right - left) / (maxX - minX + 1.0F), (bottom - top) / (maxY - minY + 1.0F)));
        float originX = (left + right) / 2.0F - (maxX - minX) * spacing / 2.0F;
        float originY = (top + bottom) / 2.0F - (maxY - minY) * spacing / 2.0F;
        List<Placed> placed = new ArrayList<>(nodes.size());
        for (AscendancyNode node : nodes) {
            boolean deep = ascendancy.isDeep(node);
            // Radii stay under half a grid step, so neighbouring nodes never overlap.
            float share = switch (node.kind()) {
                case ROOT -> 0.42F;
                case NOTABLE -> deep ? 0.40F : 0.36F;
                case SMALL -> 0.24F;
            };
            float cap = switch (node.kind()) {
                case ROOT -> 9.0F;
                case NOTABLE -> 8.0F;
                case SMALL -> 5.0F;
            };
            placed.add(new Placed(node, deep, originX + (node.x() - minX) * spacing, originY + (node.y() - minY) * spacing,
                    Math.max(1.5F, Math.min(cap, spacing * share))));
        }
        return List.copyOf(placed);
    }
}
