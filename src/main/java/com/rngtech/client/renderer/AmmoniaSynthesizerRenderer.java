package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.AmmoniaSynthesizerBlockEntity;
import com.rngtech.content.blockentity.AmmoniaSynthesizerVisualState;
import com.rngtech.content.registry.ModFluids;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Vector3f;

/**
 * Draws the working parts of the Ammonia Synthesizer: the catalyst beds behind the converter's sight port when a Catalyst
 * Bed is installed, their glow while the converter runs, the compressor's flywheel and crosshead spinning up and coasting
 * down with the processing speed, the Nitrogen, Hydrogen, and Ammonia levels, and the high-pressure gauge needle.
 * Positions are in model pixels and match tools/blockmodels/ammonia_synthesizer.py.
 */
public class AmmoniaSynthesizerRenderer implements BlockEntityRenderer<AmmoniaSynthesizerBlockEntity> {
    private static final ResourceLocation GLOW = RNGTech.id("textures/block/ammonia_synthesizer/fx/glow.png");
    private static final ResourceLocation CATALYST = RNGTech.id("block/ammonia_synthesizer/fx/catalyst");
    private static final ResourceLocation FLYWHEEL = RNGTech.id("block/ammonia_synthesizer/flywheel");
    private static final ResourceLocation CROSSHEAD = RNGTech.id("block/ammonia_synthesizer/crosshead");
    private static final ResourceLocation ROD = RNGTech.id("block/ammonia_synthesizer/rod");
    private static final ResourceLocation BOLT = RNGTech.id("block/ammonia_synthesizer/bolt");
    private static final float PORT_X0 = 8.25F;
    private static final float PORT_X1 = 10.25F;
    /** Bottom bed first; the syngas enters at the top, so the top bed runs hottest. */
    private static final float[][] WINDOWS = {{5.0F, 6.5F}, {7.0F, 8.5F}, {9.0F, 10.5F}};
    private static final float[] BED_HEAT = {0.7F, 0.85F, 1.0F};
    /** Between the liner (z 1.95) and the glass (z 1.6) on the converter's front flat. */
    private static final float CATALYST_Z = 1.85F;
    private static final float GLOW_Z = 1.8F;
    private static final int GLOW_COLOR = 0xFF7A30;
    private static final float FLY_X = 3.0F;
    private static final float FLY_Y = 4.75F;
    private static final float FLY_R = 2.25F;
    private static final float FLY_Z0 = 0.75F;
    private static final float FLY_Z1 = 1.5F;
    private static final float RIM = 0.6F;
    private static final float SPOKE = 0.25F;
    private static final float CYL_X = 3.0F;
    private static final float CYL_Z = 6.25F;
    private static final float CROSSHEAD_Y0 = 7.25F;
    private static final float CROSSHEAD_H = 0.75F;
    private static final float CROSSHEAD_HALF = 0.9F;
    private static final float STROKE = 1.2F;
    /** The gland flange under the cylinder, where the piston rod disappears. */
    private static final float GLAND_Y = 9.25F;
    /** Flywheel degrees per tick at a processing speed of 1. */
    private static final float DEGREES_PER_TICK = 20.0F;
    private static final float FEED_X0 = 1.15F;
    private static final float FEED_X1 = 2.85F;
    private static final float NITROGEN_Z0 = 11.15F;
    private static final float NITROGEN_Z1 = 12.85F;
    private static final float HYDROGEN_Z0 = 13.65F;
    private static final float HYDROGEN_Z1 = 15.35F;
    private static final float SIGHT_Y0 = 3.5F;
    private static final float SIGHT_Y1 = 11.0F;
    private static final float RECEIVER_X0 = 12.7F;
    private static final float RECEIVER_X1 = 15.05F;
    private static final float RECEIVER_Y0 = 3.01F;
    private static final float RECEIVER_Y1 = 10.4F;
    private static final float RECEIVER_Z0 = 11.7F;
    private static final float RECEIVER_Z1 = 15.3F;
    private static final float DIAL_X = 14.25F;
    private static final float DIAL_Y = 9.25F;
    private static final float DIAL_Z = 0.99F;
    /** The gauge reads full scale at this processing speed. */
    private static final float GAUGE_MAX = 2.0F;
    private static final int NEEDLE = 0xC43428;

    public AmmoniaSynthesizerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            AmmoniaSynthesizerBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        AmmoniaSynthesizerVisualState state = blockEntity.visualState();
        AmmoniaSynthesizerVisualState.Animation animation = blockEntity.animation();
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        animate(animation, state, time);

        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);

        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        if (state.catalyst()) {
            renderCatalyst(cutout, poseStack.last(), packedLight, packedOverlay);
        }
        renderFlywheel(cutout, poseStack, animation.angle, packedLight, packedOverlay);
        renderCrosshead(cutout, poseStack.last(), animation.angle, packedLight, packedOverlay);
        renderNeedle(cutout, poseStack.last(), animation.spin, packedLight, packedOverlay);

        VertexConsumer translucent = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
        PoseStack.Pose pose = poseStack.last();
        renderFluid(translucent, pose, ModFluids.NITROGEN_SOURCE.get(), state.nitrogen(),
                FEED_X0, SIGHT_Y0, NITROGEN_Z0, FEED_X1, SIGHT_Y1, NITROGEN_Z1, packedLight, packedOverlay);
        renderFluid(translucent, pose, ModFluids.HYDROGEN_SOURCE.get(), state.hydrogen(),
                FEED_X0, SIGHT_Y0, HYDROGEN_Z0, FEED_X1, SIGHT_Y1, HYDROGEN_Z1, packedLight, packedOverlay);
        renderFluid(translucent, pose, ModFluids.AMMONIA_SOURCE.get(), state.ammonia(),
                RECEIVER_X0, RECEIVER_Y0, RECEIVER_Z0, RECEIVER_X1, RECEIVER_Y1, RECEIVER_Z1, packedLight, packedOverlay);

        if (state.catalyst() && animation.heat > 0.01F) {
            renderGlow(bufferSource.getBuffer(RenderType.eyes(GLOW)), poseStack.last(), animation.heat, time, packedOverlay);
        }
        poseStack.popPose();
    }

    /** The beds warm over a few seconds and cool more slowly; the compressor spins up with the speed and coasts down. */
    private static void animate(AmmoniaSynthesizerVisualState.Animation animation, AmmoniaSynthesizerVisualState state, float time) {
        float elapsed = Double.isNaN(animation.lastTime) ? 0.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        float heat = state.active() && state.catalyst() ? 1.0F : 0.0F;
        float heatRate = heat > animation.heat ? 0.03F : 0.015F;
        animation.heat += (heat - animation.heat) * Math.min(1.0F, elapsed * heatRate);
        float spin = state.active() ? Math.max(0.0F, state.speed()) : 0.0F;
        float spinRate = spin > animation.spin ? 0.06F : 0.02F;
        animation.spin += (spin - animation.spin) * Math.min(1.0F, elapsed * spinRate);
        animation.angle = (animation.angle + animation.spin * DEGREES_PER_TICK * elapsed) % 360.0F;
    }

    /** One catalyst bed behind each window, each showing a different stretch of the granule texture. */
    private static void renderCatalyst(VertexConsumer consumer, PoseStack.Pose pose, int light, int overlay) {
        TextureAtlasSprite sprite = sprite(CATALYST);
        Vector3f north = new Vector3f(0.0F, 0.0F, -1.0F);
        for (int i = 0; i < WINDOWS.length; i++) {
            float y0 = WINDOWS[i][0];
            float y1 = WINDOWS[i][1];
            float v0 = sprite.getV(i * 0.125F);
            float v1 = sprite.getV(i * 0.125F + 0.75F);
            quad(consumer, pose, north, 0xFFFFFF, 255, light, overlay,
                    new float[] {PORT_X1, y0, CATALYST_Z, sprite.getU0(), v1},
                    new float[] {PORT_X0, y0, CATALYST_Z, sprite.getU1(), v1},
                    new float[] {PORT_X0, y1, CATALYST_Z, sprite.getU1(), v0},
                    new float[] {PORT_X1, y1, CATALYST_Z, sprite.getU0(), v0});
        }
    }

    /** The beds glow from within, hottest at the top, with a slow shimmer per bed. */
    private static void renderGlow(VertexConsumer consumer, PoseStack.Pose pose, float heat, float time, int overlay) {
        Vector3f north = new Vector3f(0.0F, 0.0F, -1.0F);
        for (int i = 0; i < WINDOWS.length; i++) {
            float shimmer = 0.9F + 0.1F * Mth.sin(time * 0.07F + i * 1.9F);
            int color = scale(GLOW_COLOR, heat * BED_HEAT[i] * shimmer);
            float y0 = WINDOWS[i][0];
            float y1 = WINDOWS[i][1];
            float v0 = i * 0.125F;
            float v1 = v0 + 0.75F;
            quad(consumer, pose, north, color, 255, LightTexture.FULL_BRIGHT, overlay,
                    new float[] {PORT_X1, y0, GLOW_Z, 0.0F, v1},
                    new float[] {PORT_X0, y0, GLOW_Z, 1.0F, v1},
                    new float[] {PORT_X0, y1, GLOW_Z, 1.0F, v0},
                    new float[] {PORT_X1, y1, GLOW_Z, 0.0F, v0});
        }
    }

    /** An octagonal rim on four spokes around a hub, turned about the crankshaft. */
    private static void renderFlywheel(VertexConsumer consumer, PoseStack poseStack, float angle, int light, int overlay) {
        TextureAtlasSprite flywheel = sprite(FLYWHEEL);
        float half = FLY_R * (float) Math.tan(Math.toRadians(22.5));
        poseStack.pushPose();
        poseStack.translate(FLY_X, FLY_Y, 0.0F);
        poseStack.mulPose(Axis.ZN.rotationDegrees(angle));
        PoseStack.Pose pose = poseStack.last();
        cube(consumer, pose, flywheel, 16.0F, -SPOKE, -FLY_R + RIM, FLY_Z0 + 0.15F, SPOKE, FLY_R - RIM, FLY_Z1 - 0.15F, 0xFFFFFF, 255, light, overlay);
        cube(consumer, pose, flywheel, 16.0F, -FLY_R + RIM, -SPOKE, FLY_Z0 + 0.15F, FLY_R - RIM, SPOKE, FLY_Z1 - 0.15F, 0xFFFFFF, 255, light, overlay);
        cube(consumer, pose, sprite(BOLT), 16.0F, -0.5F, -0.5F, FLY_Z0 - 0.25F, 0.5F, 0.5F, FLY_Z1 + 0.1F, 0xFFFFFF, 255, light, overlay);
        for (int i = 0; i < 8; i++) {
            cube(consumer, poseStack.last(), flywheel, 16.0F, -half, FLY_R - RIM, FLY_Z0, half, FLY_R, FLY_Z1, 0xFFFFFF, 255, light, overlay);
            poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        }
        poseStack.popPose();
    }

    /** The crosshead rides the crank: bottom of its stroke at angle 0, top at 180. */
    private static void renderCrosshead(VertexConsumer consumer, PoseStack.Pose pose, float angle, int light, int overlay) {
        float lift = STROKE * 0.5F * (1.0F - Mth.cos((float) Math.toRadians(angle)));
        float y0 = CROSSHEAD_Y0 + lift;
        float y1 = y0 + CROSSHEAD_H;
        cube(consumer, pose, sprite(CROSSHEAD), 16.0F, CYL_X - CROSSHEAD_HALF, y0, CYL_Z - CROSSHEAD_HALF, CYL_X + CROSSHEAD_HALF, y1,
                CYL_Z + CROSSHEAD_HALF, 0xFFFFFF, 255, light, overlay);
        cube(consumer, pose, sprite(ROD), 16.0F, CYL_X - 0.25F, y1, CYL_Z - 0.25F, CYL_X + 0.25F, GLAND_Y, CYL_Z + 0.25F, 0xFFFFFF, 255, light, overlay);
    }

    /** The gauge reads discharge pressure, which follows the compressor: rest at the bottom left, full scale at the red line. */
    private static void renderNeedle(VertexConsumer consumer, PoseStack.Pose pose, float spin, int light, int overlay) {
        float fraction = Mth.clamp(spin / GAUGE_MAX, 0.0F, 1.0F);
        // The dial face is seen from the north, so the scale sweeps clockwise from the viewer's lower left.
        float angle = (float) Math.toRadians(225.0F - 270.0F * fraction);
        float dx = -Mth.cos(angle);
        float dy = Mth.sin(angle);
        float length = 0.85F;
        float width = 0.09F;
        float px = -dy * width;
        float py = dx * width;
        TextureAtlasSprite sprite = sprite(BOLT);
        float tipX = DIAL_X + dx * length;
        float tipY = DIAL_Y + dy * length;
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), NEEDLE, 255, light, overlay,
                new float[] {DIAL_X - px, DIAL_Y - py, DIAL_Z, sprite.getU0(), sprite.getV0()},
                new float[] {DIAL_X + px, DIAL_Y + py, DIAL_Z, sprite.getU1(), sprite.getV0()},
                new float[] {tipX + px, tipY + py, DIAL_Z, sprite.getU1(), sprite.getV1()},
                new float[] {tipX - px, tipY - py, DIAL_Z, sprite.getU0(), sprite.getV1()});
    }

    private static void renderFluid(
            VertexConsumer consumer, PoseStack.Pose pose, Fluid fluid, float fill,
            float x0, float y0, float z0, float x1, float y1, float z1, int light, int overlay
    ) {
        if (fill <= 0.001F) {
            return;
        }
        IClientFluidTypeExtensions fluidType = IClientFluidTypeExtensions.of(fluid);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidType.getStillTexture());
        float top = y0 + (y1 - y0) * Mth.clamp(fill, 0.0F, 1.0F);
        cube(consumer, pose, sprite, 16.0F, x0, y0, z0, x1, top, z1, fluidType.getTintColor() & 0xFFFFFF, 220, light, overlay);
    }

    /** A box whose faces show {@code span} model pixels per full sprite, cropped to the face size. */
    private static void cube(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, float span,
            float x0, float y0, float z0, float x1, float y1, float z1,
            int color, int alpha, int light, int overlay
    ) {
        float u0 = sprite.getU0();
        float v0 = sprite.getV0();
        float uw = sprite.getU1() - u0;
        float vh = sprite.getV1() - v0;
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
    private static void quad(
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

    private static TextureAtlasSprite sprite(ResourceLocation id) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(id);
    }

    private static int scale(int color, float factor) {
        float f = Mth.clamp(factor, 0.0F, 1.0F);
        int red = Math.round((color >> 16 & 0xFF) * f);
        int green = Math.round((color >> 8 & 0xFF) * f);
        int blue = Math.round((color & 0xFF) * f);
        return red << 16 | green << 8 | blue;
    }
}
