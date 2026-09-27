package com.rngtech.rpg.progression;

import java.util.ArrayList;
import java.util.List;

public final class PassiveTreeLayouts {
    public static final List<Integer> POE_SKILLS_PER_ORBIT = List.of(1, 6, 16, 16, 40);
    public static final List<Integer> POE_ORBIT_RADII = List.of(0, 48, 86, 126, 168);

    public record Point(int x, int y) {
        public static Point of(int x, int y) {
            return new Point(x, y);
        }

        public Point offset(int xOffset, int yOffset) {
            return new Point(x + xOffset, y + yOffset);
        }

        public NodePosition node(int size) {
            return new NodePosition(x - size / 2, y - size / 2);
        }
    }

    public record NodePosition(int x, int y) {
    }

    public record Shape(List<Point> centers) {
        public Shape {
            centers = List.copyOf(centers);
            if (centers.isEmpty()) {
                throw new IllegalArgumentException("Passive tree shape must contain at least one point");
            }
        }

        public Point point(int index) {
            return centers.get(index);
        }

        public NodePosition node(int index, int size) {
            return point(index).node(size);
        }

        public int size() {
            return centers.size();
        }
    }

    public record TwoPathCluster(Shape firstPath, Shape secondPath, Point notableCenter) {
        public Point first(int index) {
            return firstPath.point(index);
        }

        public Point second(int index) {
            return secondPath.point(index);
        }

        public Point notable() {
            return notableCenter;
        }
    }

    public record BranchedPath(Shape trunkPath, Point branchCenter) {
        public BranchedPath {
            if (trunkPath == null) {
                throw new IllegalArgumentException("Passive tree branched path must contain a trunk path");
            }
            if (branchCenter == null) {
                throw new IllegalArgumentException("Passive tree branched path must contain a branch point");
            }
        }

        public Point trunk(int index) {
            return trunkPath.point(index);
        }

        public Point branch() {
            return branchCenter;
        }
    }

    public static Shape line(Point firstCenter, int xStep, int yStep, int count) {
        requirePositiveCount(count);
        List<Point> centers = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            centers.add(firstCenter.offset(xStep * index, yStep * index));
        }
        return new Shape(centers);
    }

    public static Shape travelPath(Point fromCenter, Point toCenter, int travelNodeCount) {
        requirePositiveCount(travelNodeCount);
        List<Point> centers = new ArrayList<>(travelNodeCount);
        for (int index = 1; index <= travelNodeCount; index++) {
            double progress = index / (double) (travelNodeCount + 1);
            centers.add(interpolate(fromCenter, toCenter, progress));
        }
        return new Shape(centers);
    }

    public static BranchedPath branchedTravelPath(
            Point fromCenter,
            Point toCenter,
            int travelNodeCount,
            int xBranchOffset,
            int yBranchOffset
    ) {
        return branchedTravelPath(
                fromCenter,
                toCenter,
                travelNodeCount,
                travelNodeCount - 1,
                xBranchOffset,
                yBranchOffset
        );
    }

    public static BranchedPath branchedTravelPath(
            Point fromCenter,
            Point toCenter,
            int travelNodeCount,
            int branchIndex,
            int xBranchOffset,
            int yBranchOffset
    ) {
        Shape trunkPath = travelPath(fromCenter, toCenter, travelNodeCount);
        if (branchIndex < 0 || branchIndex >= trunkPath.size()) {
            throw new IllegalArgumentException("Passive tree branch index must target a trunk point");
        }
        return new BranchedPath(trunkPath, trunkPath.point(branchIndex).offset(xBranchOffset, yBranchOffset));
    }

    public static Shape arc(Point center, int radius, double startDegrees, double sweepDegrees, int count) {
        requirePositiveCount(count);
        if (radius < 0) {
            throw new IllegalArgumentException("Passive tree radius must be non-negative");
        }
        List<Point> centers = new ArrayList<>(count);
        double divisor = Math.max(1, count - 1);
        for (int index = 0; index < count; index++) {
            centers.add(pointOnCircle(center, radius, startDegrees + sweepDegrees * index / divisor));
        }
        return new Shape(centers);
    }

    public static Shape halfCircle(Point center, int radius, double startDegrees, int count) {
        return arc(center, radius, startDegrees, 180.0D, count);
    }

    public static Shape circle(Point center, int radius, double startDegrees, int count) {
        requirePositiveCount(count);
        if (radius < 0) {
            throw new IllegalArgumentException("Passive tree radius must be non-negative");
        }
        List<Point> centers = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            centers.add(pointOnCircle(center, radius, startDegrees + 360.0D * index / count));
        }
        return new Shape(centers);
    }

    public static Point orbitPoint(Point groupCenter, int orbit, int orbitIndex) {
        return orbitPoint(groupCenter, orbit, orbitIndex, POE_SKILLS_PER_ORBIT, POE_ORBIT_RADII);
    }

    public static Point orbitPoint(
            Point groupCenter,
            int orbit,
            int orbitIndex,
            List<Integer> skillsPerOrbit,
            List<Integer> orbitRadii
    ) {
        if (orbit < 0 || orbit >= skillsPerOrbit.size()) {
            throw new IllegalArgumentException("Passive tree orbit index is out of range: " + orbit);
        }
        if (orbit >= orbitRadii.size()) {
            throw new IllegalArgumentException("Passive tree orbit radius is missing for orbit: " + orbit);
        }
        int skillsInOrbit = skillsPerOrbit.get(orbit);
        if (orbitIndex < 0 || orbitIndex >= skillsInOrbit) {
            throw new IllegalArgumentException("Passive tree orbit slot is out of range: " + orbitIndex);
        }
        double radians = Math.toRadians(orbitAngleDegrees(skillsInOrbit, orbitIndex));
        int radius = orbitRadii.get(orbit);
        int x = groupCenter.x() + (int) Math.round(Math.sin(radians) * radius);
        int y = groupCenter.y() - (int) Math.round(Math.cos(radians) * radius);
        return new Point(x, y);
    }

    public static TwoPathCluster twoPathDeadEnd(
            Point notableCenter,
            int pathNodeCount,
            int xStepFromNotable,
            int yStepFromNotable,
            int xBranchOffset,
            int yBranchOffset
    ) {
        requirePositiveCount(pathNodeCount);
        List<Point> firstPath = new ArrayList<>(pathNodeCount);
        List<Point> secondPath = new ArrayList<>(pathNodeCount);
        for (int index = 0; index < pathNodeCount; index++) {
            int stepsFromNotable = pathNodeCount - index;
            Point base = notableCenter.offset(xStepFromNotable * stepsFromNotable, yStepFromNotable * stepsFromNotable);
            firstPath.add(base.offset(xBranchOffset, yBranchOffset));
            secondPath.add(base.offset(-xBranchOffset, -yBranchOffset));
        }
        return new TwoPathCluster(new Shape(firstPath), new Shape(secondPath), notableCenter);
    }

    private static Point interpolate(Point fromCenter, Point toCenter, double progress) {
        int x = (int) Math.round(fromCenter.x() + (toCenter.x() - fromCenter.x()) * progress);
        int y = (int) Math.round(fromCenter.y() + (toCenter.y() - fromCenter.y()) * progress);
        return new Point(x, y);
    }

    private static Point pointOnCircle(Point center, int radius, double degrees) {
        double radians = Math.toRadians(degrees);
        int x = center.x() + (int) Math.round(Math.cos(radians) * radius);
        int y = center.y() + (int) Math.round(Math.sin(radians) * radius);
        return new Point(x, y);
    }

    private static double orbitAngleDegrees(int skillsInOrbit, int orbitIndex) {
        if (skillsInOrbit == 16) {
            return new int[] { 0, 30, 45, 60, 90, 120, 135, 150, 180, 210, 225, 240, 270, 300, 315, 330 }[
                    orbitIndex];
        }
        if (skillsInOrbit == 40) {
            return new int[] {
                    0,
                    10,
                    20,
                    30,
                    40,
                    45,
                    50,
                    60,
                    70,
                    80,
                    90,
                    100,
                    110,
                    120,
                    130,
                    135,
                    140,
                    150,
                    160,
                    170,
                    180,
                    190,
                    200,
                    210,
                    220,
                    225,
                    230,
                    240,
                    250,
                    260,
                    270,
                    280,
                    290,
                    300,
                    310,
                    315,
                    320,
                    330,
                    340,
                    350
            }[orbitIndex];
        }
        return 360.0D * orbitIndex / skillsInOrbit;
    }

    private static void requirePositiveCount(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Passive tree shape point count must be positive");
        }
    }

    private PassiveTreeLayouts() {
    }
}
