package com.rngtech.client.screen;

/**
 * Where the Ascendancy crest sits around a start node. The starts ring the tree's center, so the side facing away from the
 * center is usually open; the crest sits there whenever it has room. The crest keeps a fixed screen size, so each
 * candidate direction is tested at several zoom levels against the tree's links and nodes, and a blocked crest turns to
 * the nearest direction with room.
 */
public final class MasteryCrestPlacement {
    /** Native crest texture size, drawn at exactly half when the start node is small on screen so it stays crisp. */
    public static final int CREST_SIZE = 32;
    /** Half-width of the crest's gem in texture pixels. */
    public static final float CREST_GEM_HALF = 14.0F;
    private static final float OFFSET_SCALE = 1.25F;
    private static final double[] REFERENCE_ZOOMS = {0.5D, 0.7D, 1.0D, 1.5D, 2.0D};
    private static final int CANDIDATES = 72;
    /** Room of half a crest half-width, about 7 pixels at full size, already reads as clear of a link. */
    private static final double CLEAR_ROOM = 0.5D;
    /** Turning a full half turn from the outward direction costs this much of a crest's clearance. */
    private static final double TURN_PENALTY = 0.15D;

    private MasteryCrestPlacement() { }

    /** The crest's drawn size for a start node of this on-screen radius. */
    public static int crestSize(float startRadius) {
        return startRadius >= CREST_GEM_HALF ? CREST_SIZE : CREST_SIZE / 2;
    }

    public static float crestHalf(float startRadius) {
        return CREST_GEM_HALF * crestSize(startRadius) / CREST_SIZE;
    }

    /** Distance from the start node's center to the crest's center, in screen pixels. */
    public static float offset(float startRadius) {
        return OFFSET_SCALE * (startRadius + crestHalf(startRadius));
    }

    /** The direction, in radians with y pointing down, from the center of the tree's nodes through the start node. */
    public static double outward(float[] centerX, float[] centerY, int start) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (int node = 0; node < centerX.length; node++) {
            minX = Math.min(minX, centerX[node]);
            minY = Math.min(minY, centerY[node]);
            maxX = Math.max(maxX, centerX[node]);
            maxY = Math.max(maxY, centerY[node]);
        }
        double dx = centerX[start] - (minX + maxX) / 2.0D;
        double dy = centerY[start] - (minY + maxY) / 2.0D;
        return dx == 0.0D && dy == 0.0D ? -Math.PI / 2.0D : Math.atan2(dy, dx);
    }

    /**
     * The direction, in radians with y pointing down, that gives the crest the most room. Clearance at each zoom is
     * measured in crest half-widths and capped at {@link #CLEAR_ROOM}, so every clear direction scores the same and the
     * one nearest the outward direction wins.
     */
    public static double angle(float[] centerX, float[] centerY, float[] radius, float[] segments, int start) {
        double preferred = outward(centerX, centerY, start);
        double best = preferred;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int candidate = 0; candidate < CANDIDATES; candidate++) {
            double angle = preferred + Math.PI * 2.0D * candidate / CANDIDATES;
            double room = Double.POSITIVE_INFINITY;
            for (double zoom : REFERENCE_ZOOMS) {
                float startRadius = Math.max(1.0F, (float) (radius[start] * zoom));
                room = Math.min(room, clearance(centerX, centerY, radius, segments, start, angle, zoom) / crestHalf(startRadius));
            }
            double turn = Math.abs(Math.IEEEremainder(angle - preferred, Math.PI * 2.0D)) / Math.PI;
            double score = Math.min(room, CLEAR_ROOM) - TURN_PENALTY * turn;
            if (score > bestScore + 1.0E-9) {
                best = angle;
                bestScore = score;
            }
        }
        return best;
    }

    /**
     * Screen pixels between the crest, with its Seal sockets underneath, and the nearest link or other node at this zoom.
     * Negative values overlap.
     */
    public static double clearance(float[] centerX, float[] centerY, float[] radius, float[] segments, int start, double angle, double zoom) {
        float startRadius = Math.max(1.0F, (float) (radius[start] * zoom));
        float half = crestHalf(startRadius);
        float offset = offset(startRadius);
        // Crest and sockets, in screen pixels relative to the start node's center.
        double gemX = Math.cos(angle) * offset;
        double gemY = Math.sin(angle) * offset;
        double[][] footprint = {{gemX, gemY, half}, {gemX, gemY + half * 1.25D, half * 0.55D}};
        double reach = offset + half * 2.5D;
        double room = Double.POSITIVE_INFINITY;
        for (int index = 0; index + 3 < segments.length; index += 4) {
            double x1 = (segments[index] - centerX[start]) * zoom;
            double y1 = (segments[index + 1] - centerY[start]) * zoom;
            double x2 = (segments[index + 2] - centerX[start]) * zoom;
            double y2 = (segments[index + 3] - centerY[start]) * zoom;
            if (segmentDistance(0.0D, 0.0D, x1, y1, x2, y2) > reach) {
                continue;
            }
            for (double[] circle : footprint) {
                room = Math.min(room, segmentDistance(circle[0], circle[1], x1, y1, x2, y2) - circle[2]);
            }
        }
        for (int node = 0; node < centerX.length; node++) {
            if (node == start) {
                continue;
            }
            double x = (centerX[node] - centerX[start]) * zoom;
            double y = (centerY[node] - centerY[start]) * zoom;
            double nodeRadius = Math.max(1.0D, radius[node] * zoom);
            for (double[] circle : footprint) {
                room = Math.min(room, Math.hypot(circle[0] - x, circle[1] - y) - nodeRadius - circle[2]);
            }
        }
        return room;
    }

    private static double segmentDistance(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSquared = dx * dx + dy * dy;
        double t = lengthSquared <= 0.0D ? 0.0D : Math.max(0.0D, Math.min(1.0D, ((px - x1) * dx + (py - y1) * dy) / lengthSquared));
        return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
    }
}
