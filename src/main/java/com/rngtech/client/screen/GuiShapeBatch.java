package com.rngtech.client.screen;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/**
 * Collects untextured GUI shapes into one draw call. {@link GuiGraphics#fill} flushes every rectangle, which is too slow
 * for large graphs. Shapes outside the clip rectangle are skipped before any vertices are emitted.
 */
final class GuiShapeBatch {
    private static final int[] SEGMENT_COUNTS = {8, 12, 16, 24, 32, 48, 64, 96, 128, 192, 256, 384, 512};
    private static final float[][] COS = new float[SEGMENT_COUNTS.length][];
    private static final float[][] SIN = new float[SEGMENT_COUNTS.length][];
    private static final float[] SAGITTA = new float[SEGMENT_COUNTS.length];
    private static final float CIRCLE_TOLERANCE = 0.1F;

    static {
        for (int level = 0; level < SEGMENT_COUNTS.length; level++) {
            int segments = SEGMENT_COUNTS[level];
            COS[level] = new float[segments + 1];
            SIN[level] = new float[segments + 1];
            for (int index = 0; index <= segments; index++) {
                double angle = Math.PI * 2.0D * index / segments;
                COS[level][index] = (float) Math.cos(angle);
                SIN[level][index] = (float) Math.sin(angle);
            }
            SAGITTA[level] = (float) (1.0D - Math.cos(Math.PI / segments));
        }
    }

    private final GuiGraphics graphics;
    private final Matrix4f pose;
    private final VertexConsumer buffer;
    private final float clipLeft;
    private final float clipTop;
    private final float clipRight;
    private final float clipBottom;

    GuiShapeBatch(GuiGraphics graphics, float clipLeft, float clipTop, float clipRight, float clipBottom) {
        this.graphics = graphics;
        this.pose = graphics.pose().last().pose();
        this.buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        this.clipLeft = clipLeft;
        this.clipTop = clipTop;
        this.clipRight = clipRight;
        this.clipBottom = clipBottom;
    }

    boolean visible(float minX, float minY, float maxX, float maxY) {
        return maxX >= clipLeft && minX <= clipRight && maxY >= clipTop && minY <= clipBottom;
    }

    boolean circleVisible(float centerX, float centerY, float radius) {
        return visible(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
    }

    void line(float fromX, float fromY, float toX, float toY, float thickness, int color) {
        float half = thickness / 2.0F;
        if (!visible(Math.min(fromX, toX) - half, Math.min(fromY, toY) - half, Math.max(fromX, toX) + half, Math.max(fromY, toY) + half)) {
            return;
        }
        float dx = toX - fromX;
        float dy = toY - fromY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-4F) {
            rect(fromX - half, fromY - half, fromX + half, fromY + half, color);
            return;
        }
        float alongX = dx / length * half;
        float alongY = dy / length * half;
        float startX = fromX - alongX;
        float startY = fromY - alongY;
        float endX = toX + alongX;
        float endY = toY + alongY;
        quad(startX - alongY, startY + alongX, startX + alongY, startY - alongX, endX + alongY, endY - alongX, endX - alongY, endY + alongX, color);
    }

    void disc(float centerX, float centerY, float radius, int color) {
        if (radius <= 0.0F || !circleVisible(centerX, centerY, radius)) {
            return;
        }
        if (radius < 1.0F) {
            rect(centerX - radius, centerY - radius, centerX + radius, centerY + radius, color);
            return;
        }
        int level = level(radius);
        float[] cos = COS[level];
        float[] sin = SIN[level];
        for (int index = 0; index < SEGMENT_COUNTS[level]; index += 2) {
            quad(
                    centerX,
                    centerY,
                    centerX + cos[index] * radius,
                    centerY + sin[index] * radius,
                    centerX + cos[index + 1] * radius,
                    centerY + sin[index + 1] * radius,
                    centerX + cos[index + 2] * radius,
                    centerY + sin[index + 2] * radius,
                    color
            );
        }
    }

    void ring(float centerX, float centerY, float innerRadius, float outerRadius, int color) {
        innerRadius = Math.max(0.0F, innerRadius);
        if (outerRadius <= innerRadius || !circleVisible(centerX, centerY, outerRadius) || farthestClipDistance(centerX, centerY) < innerRadius) {
            return;
        }
        int level = level(outerRadius);
        float[] cos = COS[level];
        float[] sin = SIN[level];
        for (int index = 0; index < SEGMENT_COUNTS[level]; index++) {
            float innerX1 = centerX + cos[index] * innerRadius;
            float innerY1 = centerY + sin[index] * innerRadius;
            float innerX2 = centerX + cos[index + 1] * innerRadius;
            float innerY2 = centerY + sin[index + 1] * innerRadius;
            float outerX1 = centerX + cos[index] * outerRadius;
            float outerY1 = centerY + sin[index] * outerRadius;
            float outerX2 = centerX + cos[index + 1] * outerRadius;
            float outerY2 = centerY + sin[index + 1] * outerRadius;
            if (visible(
                    Math.min(Math.min(innerX1, innerX2), Math.min(outerX1, outerX2)),
                    Math.min(Math.min(innerY1, innerY2), Math.min(outerY1, outerY2)),
                    Math.max(Math.max(innerX1, innerX2), Math.max(outerX1, outerX2)),
                    Math.max(Math.max(innerY1, innerY2), Math.max(outerY1, outerY2))
            )) {
                quad(innerX1, innerY1, innerX2, innerY2, outerX2, outerY2, outerX1, outerY1, color);
            }
        }
    }

    void diamond(float centerX, float centerY, float radius, int color) {
        if (radius > 0.0F && circleVisible(centerX, centerY, radius)) {
            quad(centerX, centerY - radius, centerX + radius, centerY, centerX, centerY + radius, centerX - radius, centerY, color);
        }
    }

    void rect(float minX, float minY, float maxX, float maxY, int color) {
        if (visible(minX, minY, maxX, maxY)) {
            quad(minX, minY, minX, maxY, maxX, maxY, maxX, minY, color);
        }
    }

    void flush() {
        graphics.flush();
    }

    private void quad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        // The GUI render type culls back faces, so match the winding used by GuiGraphics#fill.
        float doubleArea = x0 * y1 - x1 * y0 + x1 * y2 - x2 * y1 + x2 * y3 - x3 * y2 + x3 * y0 - x0 * y3;
        if (doubleArea > 0.0F) {
            vertex(x0, y0, color);
            vertex(x3, y3, color);
            vertex(x2, y2, color);
            vertex(x1, y1, color);
        } else {
            vertex(x0, y0, color);
            vertex(x1, y1, color);
            vertex(x2, y2, color);
            vertex(x3, y3, color);
        }
    }

    private void vertex(float x, float y, int color) {
        buffer.addVertex(pose, x, y, 0.0F).setColor(color);
    }

    private float farthestClipDistance(float centerX, float centerY) {
        float dx = Math.max(Math.abs(clipLeft - centerX), Math.abs(clipRight - centerX));
        float dy = Math.max(Math.abs(clipTop - centerY), Math.abs(clipBottom - centerY));
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static int level(float radius) {
        for (int level = 0; level < SEGMENT_COUNTS.length; level++) {
            if (radius * SAGITTA[level] <= CIRCLE_TOLERANCE) {
                return level;
            }
        }
        return SEGMENT_COUNTS.length - 1;
    }
}
