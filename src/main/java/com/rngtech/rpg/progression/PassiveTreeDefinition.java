package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineModifierEffect;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.ModifierOperation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record PassiveTreeDefinition<N extends Enum<N> & PassiveNode>(
        Class<N> nodeType,
        List<MachinePassiveClass> classes,
        PassiveTreeConstants constants,
        List<PassiveTreeGroup> groups,
        List<PassiveTreeNodeSpec<N>> nodeSpecs,
        List<PassiveTreeLinkSpec<N>> linkSpecs
) {
    private static final double LINK_NODE_CLEARANCE = 4.0D;

    public PassiveTreeDefinition {
        nodeType = Objects.requireNonNull(nodeType, "nodeType");
        classes = List.copyOf(Objects.requireNonNull(classes, "classes"));
        constants = Objects.requireNonNull(constants, "constants");
        groups = List.copyOf(Objects.requireNonNull(groups, "groups"));
        nodeSpecs = List.copyOf(Objects.requireNonNull(nodeSpecs, "nodeSpecs"));
        linkSpecs = List.copyOf(Objects.requireNonNull(linkSpecs, "linkSpecs"));
    }

    public static <N extends Enum<N> & PassiveNode> PassiveTreeDefinition<N> of(
            Class<N> nodeType,
            List<MachinePassiveClass> classes,
            PassiveTreeConstants constants,
            List<PassiveTreeGroup> groups,
            List<PassiveTreeNodeSpec<N>> nodeSpecs
    ) {
        return new PassiveTreeDefinition<>(nodeType, classes, constants, groups, nodeSpecs, createLinkSpecs(nodeSpecs));
    }

    public void validate(N starter, int expectedTotalNodes, int expectedTravelNodes, int expectedNotables, int expectedKeystones) {
        Objects.requireNonNull(starter, "starter");
        N[] enumConstants = nodeType.getEnumConstants();
        if (enumConstants.length != expectedTotalNodes) {
            throw new IllegalStateException(
                    "Passive tree expected " + expectedTotalNodes + " enum nodes but found " + enumConstants.length);
        }
        if (nodeSpecs.size() != expectedTotalNodes) {
            throw new IllegalStateException(
                    "Passive tree expected " + expectedTotalNodes + " nodes but found " + nodeSpecs.size());
        }

        Map<N, PassiveTreeNodeSpec<N>> specsByNode = new EnumMap<>(nodeType);
        Map<String, PassiveTreeGroup> groupsById = new HashMap<>();
        for (PassiveTreeGroup group : groups) {
            PassiveTreeGroup previous = groupsById.put(group.id(), group);
            if (previous != null) {
                throw new IllegalStateException("Duplicate passive tree group id: " + group.id());
            }
        }

        Set<String> occupiedOrbitSlots = new HashSet<>();
        int starters = 0;
        int travels = 0;
        int notables = 0;
        int keystones = 0;
        for (PassiveTreeNodeSpec<N> spec : nodeSpecs) {
            PassiveTreeNodeSpec<N> previous = specsByNode.put(spec.node(), spec);
            if (previous != null) {
                throw new IllegalStateException("Duplicate passive tree node spec: " + spec.nodeId());
            }
            if (!groupsById.containsKey(spec.groupId())) {
                throw new IllegalStateException("Passive tree node references missing group: " + spec.nodeId());
            }
            if (spec.kind() != spec.node().kind()) {
                throw new IllegalStateException("Passive tree node kind is out of sync: " + spec.nodeId());
            }
            requireValidOrbit(spec);
            String slotKey = spec.groupId() + "|" + spec.orbit() + "|" + spec.orbitIndex();
            if (!occupiedOrbitSlots.add(slotKey)) {
                throw new IllegalStateException("Duplicate passive tree orbit slot: " + slotKey);
            }
            switch (spec.kind()) {
                case STARTER -> starters++;
                case TRAVEL -> travels++;
                case NOTABLE -> notables++;
                case KEYSTONE -> keystones++;
                case NODE -> {
                }
            }
        }

        if (starters != 1 || starter.kind() != PassiveNodeKind.STARTER) {
            throw new IllegalStateException("Passive tree must contain exactly one starter node");
        }
        if (travels != expectedTravelNodes) {
            throw new IllegalStateException("Passive tree expected " + expectedTravelNodes + " travel nodes but found " + travels);
        }
        if (notables != expectedNotables) {
            throw new IllegalStateException("Passive tree expected " + expectedNotables + " notables but found " + notables);
        }
        if (keystones != expectedKeystones) {
            throw new IllegalStateException("Passive tree expected " + expectedKeystones + " keystones but found " + keystones);
        }
        for (N node : enumConstants) {
            if (!specsByNode.containsKey(node)) {
                throw new IllegalStateException("Passive tree enum node is missing a spec: " + node.name());
            }
        }

        Map<N, List<N>> adjacency = createAdjacency(specsByNode);
        validateAlwaysAllocatedNodes(starter, specsByNode);
        validateTravelNodesUseOnlyAttributes(specsByNode);
        validateGroupsOwnNodeSpecs(specsByNode, groupsById);
        validateLinksReferenceSpecs(specsByNode);
        validateNodeDegrees(starter, specsByNode, adjacency);
        validateKeystoneDistances(starter, specsByNode, adjacency);
        validateNodeBoundsDoNotOverlap(specsByNode);
        validateLinksDoNotCrossNodeBounds(specsByNode);
        validateLinksDoNotOverlap();
        validateReachable(starter, specsByNode, adjacency);
    }

    public List<List<N>> createLinkLists() {
        N[] values = nodeType.getEnumConstants();
        List<List<N>> links = new ArrayList<>(values.length);
        for (int index = 0; index < values.length; index++) {
            links.add(new ArrayList<>());
        }
        for (PassiveTreeLinkSpec<N> link : linkSpecs) {
            addLink(links.get(link.first().ordinal()), link.second());
            addLink(links.get(link.second().ordinal()), link.first());
        }
        List<List<N>> immutable = new ArrayList<>(links.size());
        for (List<N> nodeLinks : links) {
            immutable.add(List.copyOf(nodeLinks));
        }
        return List.copyOf(immutable);
    }

    private void requireValidOrbit(PassiveTreeNodeSpec<N> spec) {
        if (spec.orbit() >= constants.skillsPerOrbit().size()) {
            throw new IllegalStateException("Passive tree node references missing orbit: " + spec.nodeId());
        }
        if (spec.orbitIndex() >= constants.skillsPerOrbit().get(spec.orbit())) {
            throw new IllegalStateException("Passive tree node references missing orbit slot: " + spec.nodeId());
        }
    }

    private void validateGroupsOwnNodeSpecs(
            Map<N, PassiveTreeNodeSpec<N>> specsByNode,
            Map<String, PassiveTreeGroup> groupsById
    ) {
        Map<String, Set<String>> specNodeIdsByGroup = new HashMap<>();
        for (PassiveTreeNodeSpec<N> spec : specsByNode.values()) {
            specNodeIdsByGroup.computeIfAbsent(spec.groupId(), key -> new HashSet<>()).add(spec.nodeId());
        }
        for (PassiveTreeGroup group : groupsById.values()) {
            Set<String> expectedNodeIds = specNodeIdsByGroup.getOrDefault(group.id(), Set.of());
            if (!expectedNodeIds.equals(new HashSet<>(group.nodeIds()))) {
                throw new IllegalStateException("Passive tree group node list is out of sync: " + group.id());
            }
            for (Map.Entry<Integer, List<Integer>> entry : group.orbitOccupancy().entrySet()) {
                int orbit = entry.getKey();
                if (orbit < 0 || orbit >= constants.skillsPerOrbit().size()) {
                    throw new IllegalStateException("Passive tree group references missing orbit: " + group.id());
                }
                int skillsInOrbit = constants.skillsPerOrbit().get(orbit);
                for (int orbitIndex : entry.getValue()) {
                    if (orbitIndex < 0 || orbitIndex >= skillsInOrbit) {
                        throw new IllegalStateException("Passive tree group references missing orbit slot: " + group.id());
                    }
                }
            }
        }
    }

    private void validateLinksReferenceSpecs(Map<N, PassiveTreeNodeSpec<N>> specsByNode) {
        Set<String> seenLinks = new HashSet<>();
        for (PassiveTreeLinkSpec<N> link : linkSpecs) {
            if (!specsByNode.containsKey(link.first()) || !specsByNode.containsKey(link.second())) {
                throw new IllegalStateException("Passive tree link references a node without a spec: " + link.key());
            }
            if (!seenLinks.add(link.key())) {
                throw new IllegalStateException("Duplicate passive tree link: " + link.key());
            }
        }
    }

    private void validateAlwaysAllocatedNodes(N starter, Map<N, PassiveTreeNodeSpec<N>> specsByNode) {
        if (!starter.alwaysAllocated()) {
            throw new IllegalStateException("Passive tree starter must be always allocated: " + starter.name());
        }
        for (N node : specsByNode.keySet()) {
            if (node != starter && node.alwaysAllocated()) {
                throw new IllegalStateException("Passive tree non-starter node is always allocated: " + node.name());
            }
        }
    }

    private void validateTravelNodesUseOnlyAttributes(Map<N, PassiveTreeNodeSpec<N>> specsByNode) {
        for (N node : specsByNode.keySet()) {
            if (node.kind() != PassiveNodeKind.TRAVEL) {
                continue;
            }
            if (!node.flags().isEmpty()) {
                throw new IllegalStateException("Passive tree travel node has behavior flags: " + node.name());
            }
            for (PassiveStatType stat : PassiveStatType.values()) {
                if (node.passiveStat(stat) != 0) {
                    throw new IllegalStateException("Passive tree travel node has passive stats: " + node.name());
                }
            }
            if (node.effects().isEmpty()) {
                throw new IllegalStateException("Passive tree travel node must grant a core attribute: " + node.name());
            }
            for (MachineModifierEffect effect : node.effects()) {
                if (!isTravelAttribute(effect.stat()) || effect.operation() != ModifierOperation.ADD) {
                    throw new IllegalStateException("Passive tree travel node has non-attribute effect: " + node.name());
                }
            }
        }
    }

    private static boolean isTravelAttribute(MachineStat stat) {
        return stat == MachineStat.CONTROL || stat == MachineStat.DRIVE || stat == MachineStat.RESERVE;
    }

    private void validateNodeDegrees(N starter, Map<N, PassiveTreeNodeSpec<N>> specsByNode, Map<N, List<N>> adjacency) {
        for (N node : specsByNode.keySet()) {
            int degree = adjacency.getOrDefault(node, List.of()).size();
            if (node == starter) {
                if (degree != 3) {
                    throw new IllegalStateException("Passive tree starter must have exactly three exits: " + degree);
                }
                continue;
            }
            int maximumDegree = maximumDegree(node.kind());
            if (node.kind() == PassiveNodeKind.KEYSTONE && degree != maximumDegree) {
                throw new IllegalStateException("Passive tree keystone must have exactly one link: " + node.name());
            }
            if (node.kind() != PassiveNodeKind.KEYSTONE && degree > maximumDegree) {
                throw new IllegalStateException(
                        "Passive tree node has too many links: " + node.name() + " has " + degree);
            }
        }
    }

    private int maximumDegree(PassiveNodeKind kind) {
        return switch (kind) {
            case STARTER -> 3;
            case TRAVEL -> 4;
            case NODE -> 3;
            case NOTABLE -> 2;
            case KEYSTONE -> 1;
        };
    }

    private void validateKeystoneDistances(N starter, Map<N, PassiveTreeNodeSpec<N>> specsByNode, Map<N, List<N>> adjacency) {
        Map<N, Integer> distances = shortestPathDistances(starter, adjacency);
        for (N node : specsByNode.keySet()) {
            if (node.kind() == PassiveNodeKind.KEYSTONE && distances.getOrDefault(node, Integer.MAX_VALUE) < 7) {
                throw new IllegalStateException("Passive tree keystone is too close to starter: " + node.name());
            }
        }
    }

    private void validateNodeBoundsDoNotOverlap(Map<N, PassiveTreeNodeSpec<N>> specsByNode) {
        List<N> nodes = new ArrayList<>(specsByNode.keySet());
        for (int firstIndex = 0; firstIndex < nodes.size(); firstIndex++) {
            N first = nodes.get(firstIndex);
            for (int secondIndex = firstIndex + 1; secondIndex < nodes.size(); secondIndex++) {
                N second = nodes.get(secondIndex);
                if (boundsOverlap(first, second)) {
                    throw new IllegalStateException("Passive tree nodes overlap: " + first.name() + " and " + second.name());
                }
            }
        }
    }

    private void validateLinksDoNotCrossNodeBounds(Map<N, PassiveTreeNodeSpec<N>> specsByNode) {
        for (PassiveTreeLinkSpec<N> link : linkSpecs) {
            List<PassiveTreeLayouts.Point> path = link.first().linkPathTo(link.second());
            for (N node : specsByNode.keySet()) {
                if (node == link.first() || node == link.second()) {
                    continue;
                }
                double minimumDistance = node.size() / 2.0D + LINK_NODE_CLEARANCE;
                for (int index = 1; index < path.size(); index++) {
                    if (distanceToSegment(nodeCenterX(node), nodeCenterY(node), path.get(index - 1), path.get(index)) < minimumDistance) {
                        throw new IllegalStateException(
                                "Passive tree link crosses node: " + link.key() + " through " + node.name());
                    }
                }
            }
        }
    }

    private void validateLinksDoNotOverlap() {
        for (int firstIndex = 0; firstIndex < linkSpecs.size(); firstIndex++) {
            PassiveTreeLinkSpec<N> first = linkSpecs.get(firstIndex);
            List<PassiveTreeLayouts.Point> firstPath = first.first().linkPathTo(first.second());
            for (int secondIndex = firstIndex + 1; secondIndex < linkSpecs.size(); secondIndex++) {
                PassiveTreeLinkSpec<N> second = linkSpecs.get(secondIndex);
                if (linksShareEndpoint(first, second)) {
                    continue;
                }
                List<PassiveTreeLayouts.Point> secondPath = second.first().linkPathTo(second.second());
                for (int firstSegment = 1; firstSegment < firstPath.size(); firstSegment++) {
                    for (int secondSegment = 1; secondSegment < secondPath.size(); secondSegment++) {
                        if (segmentsOverlap(
                                firstPath.get(firstSegment - 1), firstPath.get(firstSegment),
                                secondPath.get(secondSegment - 1), secondPath.get(secondSegment)
                        )) {
                            throw new IllegalStateException("Passive tree links overlap: "
                                    + first.key() + " and " + second.key());
                        }
                    }
                }
            }
        }
    }

    private boolean linksShareEndpoint(PassiveTreeLinkSpec<N> first, PassiveTreeLinkSpec<N> second) {
        return first.first() == second.first()
                || first.first() == second.second()
                || first.second() == second.first()
                || first.second() == second.second();
    }

    private void validateReachable(N starter, Map<N, PassiveTreeNodeSpec<N>> specsByNode, Map<N, List<N>> adjacency) {
        Map<N, Integer> distances = shortestPathDistances(starter, adjacency);
        long reachable = distances.values().stream().filter(distance -> distance != Integer.MAX_VALUE).count();
        if (reachable != specsByNode.size()) {
            throw new IllegalStateException(
                    "Passive tree has unreachable nodes: " + (specsByNode.size() - reachable));
        }
    }

    private Map<N, List<N>> createAdjacency(Map<N, PassiveTreeNodeSpec<N>> specsByNode) {
        Map<N, List<N>> adjacency = new EnumMap<>(nodeType);
        for (N node : specsByNode.keySet()) {
            adjacency.put(node, new ArrayList<>());
        }
        for (PassiveTreeLinkSpec<N> link : linkSpecs) {
            adjacency.get(link.first()).add(link.second());
            adjacency.get(link.second()).add(link.first());
        }
        return adjacency;
    }

    private Map<N, Integer> shortestPathDistances(N starter, Map<N, List<N>> adjacency) {
        Map<N, Integer> distances = new EnumMap<>(nodeType);
        for (N node : adjacency.keySet()) {
            distances.put(node, Integer.MAX_VALUE);
        }
        ArrayDeque<N> queue = new ArrayDeque<>();
        queue.add(starter);
        distances.put(starter, 0);
        while (!queue.isEmpty()) {
            N node = queue.removeFirst();
            for (N linked : adjacency.getOrDefault(node, List.of())) {
                if (distances.get(linked) == Integer.MAX_VALUE) {
                    distances.put(linked, distances.get(node) + 1);
                    queue.add(linked);
                }
            }
        }
        return distances;
    }

    private static <N extends Enum<N> & PassiveNode> List<PassiveTreeLinkSpec<N>> createLinkSpecs(
            List<PassiveTreeNodeSpec<N>> nodeSpecs
    ) {
        List<PassiveTreeLinkSpec<N>> result = new ArrayList<>();
        Set<String> seenLinks = new HashSet<>();
        for (PassiveTreeNodeSpec<N> spec : nodeSpecs) {
            for (N linkedNode : spec.linkedNodes()) {
                PassiveTreeLinkSpec<N> link = new PassiveTreeLinkSpec<>(spec.node(), linkedNode);
                if (seenLinks.add(link.key())) {
                    result.add(link);
                }
            }
        }
        return List.copyOf(result);
    }

    private static boolean boundsOverlap(PassiveNode first, PassiveNode second) {
        return first.x() < second.x() + second.size()
                && first.x() + first.size() > second.x()
                && first.y() < second.y() + second.size()
                && first.y() + first.size() > second.y();
    }

    private static boolean segmentsOverlap(
            PassiveTreeLayouts.Point firstStart,
            PassiveTreeLayouts.Point firstEnd,
            PassiveTreeLayouts.Point secondStart,
            PassiveTreeLayouts.Point secondEnd
    ) {
        double firstStartX = firstStart.x();
        double firstStartY = firstStart.y();
        double firstEndX = firstEnd.x();
        double firstEndY = firstEnd.y();
        double secondStartX = secondStart.x();
        double secondStartY = secondStart.y();
        double secondEndX = secondEnd.x();
        double secondEndY = secondEnd.y();

        return onSegment(firstStartX, firstStartY, firstEndX, firstEndY, secondStartX, secondStartY)
                || onSegment(firstStartX, firstStartY, firstEndX, firstEndY, secondEndX, secondEndY)
                || onSegment(secondStartX, secondStartY, secondEndX, secondEndY, firstStartX, firstStartY)
                || onSegment(secondStartX, secondStartY, secondEndX, secondEndY, firstEndX, firstEndY);
    }

    private static boolean onSegment(
            double startX,
            double startY,
            double endX,
            double endY,
            double pointX,
            double pointY
    ) {
        return Math.min(startX, endX) <= pointX
                && pointX <= Math.max(startX, endX)
                && Math.min(startY, endY) <= pointY
                && pointY <= Math.max(startY, endY)
                && cross(startX, startY, endX, endY, pointX, pointY) == 0.0D;
    }

    private static double cross(
            double startX,
            double startY,
            double endX,
            double endY,
            double pointX,
            double pointY
    ) {
        return (endX - startX) * (pointY - startY) - (endY - startY) * (pointX - startX);
    }

    private static double distanceToSegment(
            double pointX,
            double pointY,
            PassiveTreeLayouts.Point first,
            PassiveTreeLayouts.Point second
    ) {
        double firstX = first.x();
        double firstY = first.y();
        double secondX = second.x();
        double secondY = second.y();
        double segmentX = secondX - firstX;
        double segmentY = secondY - firstY;
        double pointOffsetX = pointX - firstX;
        double pointOffsetY = pointY - firstY;
        double projection = pointOffsetX * segmentX + pointOffsetY * segmentY;
        if (projection <= 0.0D) {
            return distance(pointX, pointY, firstX, firstY);
        }
        double segmentLengthSquared = segmentX * segmentX + segmentY * segmentY;
        if (projection >= segmentLengthSquared) {
            return distance(pointX, pointY, secondX, secondY);
        }
        double progress = projection / segmentLengthSquared;
        return distance(pointX, pointY, firstX + progress * segmentX, firstY + progress * segmentY);
    }

    private static double distance(double firstX, double firstY, double secondX, double secondY) {
        double deltaX = firstX - secondX;
        double deltaY = firstY - secondY;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    private static double nodeCenterX(PassiveNode node) {
        return node.x() + node.size() / 2.0D;
    }

    private static double nodeCenterY(PassiveNode node) {
        return node.y() + node.size() / 2.0D;
    }

    private static <N extends PassiveNode> void addLink(List<N> links, N node) {
        if (!links.contains(node)) {
            links.add(node);
        }
    }
}
