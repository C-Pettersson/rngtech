package com.rngtech.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Vector3f;

/**
 * Drawing shared by the Furnace, Alloy Furnace, Metal Press, and Melter renderers: the incandescent colour of a heat, the
 * wobble, the heat bar, the glow inside an open-fronted chamber, the Nitrogen cylinder's fill and valve collar, and the
 * quad and box helpers, which the Resonance Calibrator renderer also uses. Positions are in model pixels.
 */
final class FurnaceGlow {
    /** Heats where the glow colour changes: dull red, cherry, orange, yellow, and near white. */
    private static final int[] HEATS = {450, 800, 1050, 1350, 1700, 2000};
    private static final int[] COLORS = {0x5A0A04, 0xC02810, 0xFF6A10, 0xFFA830, 0xFFD870, 0xFFF4D0};
    private static final int GLOW_START = 350;
    private static final int GLOW_FULL = 1050;
    private static final int TARGET_MARK = 0xF2F6F8;
    private static final int SETPOINT_MARK = 0xF2D23C;
    private static final int OVERHEAT_BAR = 0xD0302A;
    private static final int COLD_BAR = 0x2FB3C2;
    /** Nitrogen and the lit gauge rim, as on the Process tab. */
    private static final int NITROGEN = 0x8BA4C9;
    private static final int NITROGEN_ACTIVE = 0xB9D3F2;
    /** Hydrogen in an annealing tank, as on the Process-tab gauge. */
    private static final int HYDROGEN = 0xD8E6F2;
    /** How far chamber glow sits inside the chamber walls, so it never fights them for depth. */
    private static final float GLOW_INSET = 0.02F;

    private FurnaceGlow() {
    }

    /** How strongly something at this heat glows, 0 below a dull red and 1 from orange up. */
    static float strength(float heat) {
        return Mth.clamp((heat - GLOW_START) / (GLOW_FULL - GLOW_START), 0.0F, 1.0F);
    }

    /** The incandescent colour of this heat, from dull red through orange and yellow to near white. */
    static int color(float heat) {
        if (heat <= HEATS[0]) {
            return COLORS[0];
        }
        for (int i = 1; i < HEATS.length; i++) {
            if (heat <= HEATS[i]) {
                return lerpColor((heat - HEATS[i - 1]) / (HEATS[i] - HEATS[i - 1]), COLORS[i - 1], COLORS[i]);
            }
        }
        return COLORS[COLORS.length - 1];
    }

    /** {@link #color} dimmed by {@link #strength}, for additive glow. */
    static int glow(float heat, float brightness) {
        return scale(color(heat), strength(heat) * brightness);
    }

    /** The heat a lane shows this frame: its temperature swinging around the setpoint by the synced wobble. */
    static float wobbled(float heat, int wobble, float time, int seed) {
        if (wobble <= 0) {
            return heat;
        }
        float swing = Mth.sin(time * 0.31F + seed * 1.7F) * 0.7F + Mth.sin(time * 0.83F + seed * 0.9F) * 0.3F;
        return heat + wobble * swing;
    }

    /**
     * A heat bar on a north-facing strip, read like the Process tab's: filled from the left to {@code heat} on a scale
     * of {@code scale} in the colour of that heat (teal while cold, red while overheating), with a white mark at the
     * recipe's {@code target} and a yellow mark at the {@code setpoint} when the slider pushes past the target.
     */
    static void heatBar(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
            float x0, float y0, float x1, float y1, float z,
            float heat, int target, int setpoint, boolean overheating, int scale, int light, int overlay
    ) {
        if (scale <= 0) {
            return;
        }
        float width = x1 - x0;
        float fill = Mth.clamp(heat / scale, 0.0F, 1.0F);
        Vector3f north = new Vector3f(0.0F, 0.0F, -1.0F);
        if (fill > 0.01F) {
            int color = overheating ? OVERHEAT_BAR : strength(heat) > 0.0F ? color(Math.max(heat, GLOW_START + 150.0F)) : COLD_BAR;
            flat(consumer, pose, sprite, north, x0, y0, x0 + width * fill, y1, z, color, light, overlay);
        }
        if (target > 0) {
            mark(consumer, pose, sprite, north, x0, width, y0, y1, z, target / (float) scale, TARGET_MARK, light, overlay);
        }
        if (setpoint > target && target > 0) {
            mark(consumer, pose, sprite, north, x0, width, y0, y1, z, setpoint / (float) scale, SETPOINT_MARK, light, overlay);
        }
    }

    private static void mark(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, Vector3f north,
            float x0, float width, float y0, float y1, float z, float share, int color, int light, int overlay
    ) {
        float mark = x0 + width * Mth.clamp(share, 0.0F, 1.0F);
        flat(consumer, pose, sprite, north, Math.max(x0, mark - 0.15F), y0 - 0.2F, Math.min(x0 + width, mark + 0.15F), y1 + 0.2F,
                z - 0.005F, color, light, overlay);
    }

    /**
     * The Nitrogen fill in a cylinder's window, from {@code y0} up to {@code y1} when full. The window faces north at
     * depth {@code depth} when {@code east} is false, else east at x {@code depth}; it spans {@code a0} to {@code a1}.
     */
    static void nitrogenFill(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, boolean east,
            float depth, float a0, float a1, float y0, float y1, float fill, int overlay
    ) {
        gasFill(consumer, pose, sprite, east, depth, a0, a1, y0, y1, fill, NITROGEN, overlay);
    }

    /** The gauge colour of the gas in an atmosphere tank. */
    static int gasColor(boolean hydrogen) {
        return hydrogen ? HYDROGEN : NITROGEN;
    }

    /** {@link #nitrogenFill} in the colour of the gas the tank holds. */
    static void gasFill(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, boolean east,
            float depth, float a0, float a1, float y0, float y1, float fill, int color, int overlay
    ) {
        if (fill <= 0.005F) {
            return;
        }
        float top = y0 + (y1 - y0) * Mth.clamp(fill, 0.0F, 1.0F);
        int light = LightTexture.FULL_BRIGHT;
        if (east) {
            quad(consumer, pose, new Vector3f(1.0F, 0.0F, 0.0F), color, 255, light, overlay,
                    new float[] {depth, y0, a0, sprite.getU0(), sprite.getV1()},
                    new float[] {depth, y0, a1, sprite.getU1(), sprite.getV1()},
                    new float[] {depth, top, a1, sprite.getU1(), sprite.getV0()},
                    new float[] {depth, top, a0, sprite.getU0(), sprite.getV0()});
        } else {
            flat(consumer, pose, sprite, new Vector3f(0.0F, 0.0F, -1.0F), a0, y0, a1, top, depth, color, light, overlay);
        }
    }

    /** The valve collar lights up while a lane uses Nitrogen, like the gauge rim on the Process tab. */
    static void collarGlow(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
            float cx, float cz, float y0, float y1, float brightness, int overlay
    ) {
        if (brightness <= 0.01F) {
            return;
        }
        int color = lerpColor(Mth.clamp(brightness, 0.0F, 1.0F), 0x46505A, NITROGEN_ACTIVE);
        float half = 0.62F;
        cube(consumer, pose, sprite, 16.0F, cx - half, y0, cz - half, cx + half, y1, cz + half, color, 255, LightTexture.FULL_BRIGHT, overlay);
    }

    /** Glow on the back wall, side walls, ceiling, and floor of an open-fronted chamber {x0, y0, z0, x1, y1, z1}. */
    static void chamberGlow(VertexConsumer consumer, PoseStack.Pose pose, float[] chamber, int color, int overlay) {
        float x0 = chamber[0];
        float y0 = chamber[1];
        float z0 = chamber[2];
        float x1 = chamber[3];
        float y1 = chamber[4];
        float z1 = chamber[5];
        int light = LightTexture.FULL_BRIGHT;
        float back = z1 - GLOW_INSET;
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), color, 255, light, overlay,
                new float[] {x0, y0, back, 0.0F, 1.0F}, new float[] {x1, y0, back, 1.0F, 1.0F},
                new float[] {x1, y1, back, 1.0F, 0.0F}, new float[] {x0, y1, back, 0.0F, 0.0F});
        for (float x : new float[] {x0 + GLOW_INSET, x1 - GLOW_INSET}) {
            Vector3f normal = new Vector3f(x < (x0 + x1) / 2.0F ? 1.0F : -1.0F, 0.0F, 0.0F);
            quad(consumer, pose, normal, color, 255, light, overlay,
                    new float[] {x, y0, z0, 0.0F, 1.0F}, new float[] {x, y0, z1, 1.0F, 1.0F},
                    new float[] {x, y1, z1, 1.0F, 0.0F}, new float[] {x, y1, z0, 0.0F, 0.0F});
        }
        for (float y : new float[] {y0 + GLOW_INSET, y1 - GLOW_INSET}) {
            Vector3f normal = new Vector3f(0.0F, y < (y0 + y1) / 2.0F ? 1.0F : -1.0F, 0.0F);
            quad(consumer, pose, normal, color, 255, light, overlay,
                    new float[] {x0, y, z0, 0.0F, 0.0F}, new float[] {x1, y, z0, 1.0F, 0.0F},
                    new float[] {x1, y, z1, 1.0F, 1.0F}, new float[] {x0, y, z1, 0.0F, 1.0F});
        }
    }

    /** A flat north-facing rectangle at depth z. */
    static void flat(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, Vector3f normal,
            float x0, float y0, float x1, float y1, float z, int color, int light, int overlay
    ) {
        quad(consumer, pose, normal, color, 255, light, overlay,
                new float[] {x0, y0, z, sprite.getU0(), sprite.getV1()},
                new float[] {x1, y0, z, sprite.getU1(), sprite.getV1()},
                new float[] {x1, y1, z, sprite.getU1(), sprite.getV0()},
                new float[] {x0, y1, z, sprite.getU0(), sprite.getV0()});
    }

    /** A box whose faces show {@code span} model pixels per full sprite, cropped to the face size. */
    static void cube(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, float span,
            float x0, float y0, float z0, float x1, float y1, float z1,
            int color, int alpha, int light, int overlay
    ) {
        cube(consumer, pose, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), span, x0, y0, z0, x1, y1, z1,
                color, alpha, light, overlay);
    }

    /** {@link #cube} for a whole standalone texture, such as one drawn with {@code RenderType.eyes}. */
    static void cube(
            VertexConsumer consumer, PoseStack.Pose pose, float span,
            float x0, float y0, float z0, float x1, float y1, float z1,
            int color, int light, int overlay
    ) {
        cube(consumer, pose, 0.0F, 0.0F, 1.0F, 1.0F, span, x0, y0, z0, x1, y1, z1, color, 255, light, overlay);
    }

    private static void cube(
            VertexConsumer consumer, PoseStack.Pose pose, float u0, float v0, float u1, float v1, float span,
            float x0, float y0, float z0, float x1, float y1, float z1,
            int color, int alpha, int light, int overlay
    ) {
        float uw = u1 - u0;
        float vh = v1 - v0;
        float ux = u0 + uw * Math.min(1.0F, (x1 - x0) / span);
        float uz = u0 + uw * Math.min(1.0F, (z1 - z0) / span);
        float vy = v0 + vh * Math.min(1.0F, (y1 - y0) / span);
        float vz = v0 + vh * Math.min(1.0F, (z1 - z0) / span);
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), color, alpha, light, overlay,
                new float[] {x0, y0, z0, u0, vy}, new float[] {x1, y0, z0, ux, vy}, new float[] {x1, y1, z0, ux, v0}, new float[] {x0, y1, z0, u0, v0});
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, 1.0F), color, alpha, light, overlay,
                new float[] {x0, y0, z1, u0, vy}, new float[] {x1, y0, z1, ux, vy}, new float[] {x1, y1, z1, ux, v0}, new float[] {x0, y1, z1, u0, v0});
        quad(consumer, pose, new Vector3f(-1.0F, 0.0F, 0.0F), color, alpha, light, overlay,
                new float[] {x0, y0, z0, u0, vy}, new float[] {x0, y0, z1, uz, vy}, new float[] {x0, y1, z1, uz, v0}, new float[] {x0, y1, z0, u0, v0});
        quad(consumer, pose, new Vector3f(1.0F, 0.0F, 0.0F), color, alpha, light, overlay,
                new float[] {x1, y0, z0, u0, vy}, new float[] {x1, y0, z1, uz, vy}, new float[] {x1, y1, z1, uz, v0}, new float[] {x1, y1, z0, u0, v0});
        quad(consumer, pose, new Vector3f(0.0F, 1.0F, 0.0F), color, alpha, light, overlay,
                new float[] {x0, y1, z0, u0, v0}, new float[] {x1, y1, z0, ux, v0}, new float[] {x1, y1, z1, ux, vz}, new float[] {x0, y1, z1, u0, vz});
        quad(consumer, pose, new Vector3f(0.0F, -1.0F, 0.0F), color, alpha, light, overlay,
                new float[] {x0, y0, z0, u0, v0}, new float[] {x1, y0, z0, ux, v0}, new float[] {x1, y0, z1, ux, vz}, new float[] {x0, y0, z1, u0, vz});
    }

    /**
     * One quad from four {x, y, z, u, v} corners in perimeter order. The winding is flipped when needed so the quad
     * faces along {@code normal} and survives back-face culling.
     */
    static void quad(
            VertexConsumer consumer, PoseStack.Pose pose, Vector3f normal,
            int color, int alpha, int light, int overlay,
            float[] a, float[] b, float[] c, float[] d
    ) {
        Vector3f edge1 = new Vector3f(b[0] - a[0], b[1] - a[1], b[2] - a[2]);
        Vector3f edge2 = new Vector3f(c[0] - a[0], c[1] - a[1], c[2] - a[2]);
        boolean flip = edge1.cross(edge2).dot(normal) < 0.0F;
        float[][] corners = flip ? new float[][] {a, d, c, b} : new float[][] {a, b, c, d};
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        for (float[] corner : corners) {
            consumer.addVertex(pose, corner[0], corner[1], corner[2])
                    .setColor(red, green, blue, alpha)
                    .setUv(corner[3], corner[4])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose, normal.x(), normal.y(), normal.z());
        }
    }

    static TextureAtlasSprite sprite(ResourceLocation id) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(id);
    }

    static int scale(int color, float factor) {
        float clamped = Mth.clamp(factor, 0.0F, 1.0F);
        int red = Math.round((color >> 16 & 0xFF) * clamped);
        int green = Math.round((color >> 8 & 0xFF) * clamped);
        int blue = Math.round((color & 0xFF) * clamped);
        return red << 16 | green << 8 | blue;
    }

    static int lerpColor(float t, int from, int to) {
        int red = Math.round(Mth.lerp(t, from >> 16 & 0xFF, to >> 16 & 0xFF));
        int green = Math.round(Mth.lerp(t, from >> 8 & 0xFF, to >> 8 & 0xFF));
        int blue = Math.round(Mth.lerp(t, from & 0xFF, to & 0xFF));
        return red << 16 | green << 8 | blue;
    }
}
