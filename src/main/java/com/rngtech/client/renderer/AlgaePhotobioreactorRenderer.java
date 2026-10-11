package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.AlgaeCultureDynamics;
import com.rngtech.content.blockentity.AlgaePhotobioreactorBlockEntity;
import com.rngtech.content.blockentity.AlgaeVisualState;

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
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Vector3f;

/**
 * Draws the living parts of the Algae Photobioreactor: the culture in each glass column (coloured by density), rising
 * bubbles while it grows, both sight-glass levels, the harvest settled in the rear tank, the rotameter float at the
 * selected aeration, and the light sensor glow. Positions are in model pixels and match
 * tools/blockmodels/algae_photobioreactor.py.
 */
public class AlgaePhotobioreactorRenderer implements BlockEntityRenderer<AlgaePhotobioreactorBlockEntity> {
    private static final ResourceLocation CULTURE = RNGTech.id("block/algae_photobioreactor/fx/culture");
    private static final ResourceLocation BUBBLE = RNGTech.id("block/algae_photobioreactor/fx/bubble");
    private static final ResourceLocation SLUDGE = RNGTech.id("block/algae_photobioreactor/fx/sludge");
    private static final ResourceLocation FLOAT = RNGTech.id("block/algae_photobioreactor/flange");
    private static final ResourceLocation GLOW = RNGTech.id("textures/block/algae_photobioreactor/fx/glow.png");
    private static final float[] COLUMN_XS = {4.0F, 8.0F, 12.0F};
    private static final float COLUMN_Z = 5.0F;
    private static final float GLASS_Y0 = 4.0F;
    private static final float GLASS_Y1 = 13.5F;
    private static final float CULTURE_APOTHEM = 1.55F;
    private static final float CULTURE_RADIUS = CULTURE_APOTHEM / Mth.cos((float) Math.toRadians(22.5));
    private static final int BUBBLES_PER_COLUMN = 5;
    private static final float BUBBLE_SIZE = 0.45F;
    private static final float BUBBLE_WOBBLE = 0.55F;
    /** Bubble rises per tick at rated aeration. */
    private static final float BUBBLE_SPEED = 0.02F;
    private static final float WATER_X = 0.75F;
    private static final float CARBON_X = 13.25F;
    private static final float SIGHT_Z0 = 12.75F;
    private static final float SIGHT_Z1 = 15.25F;
    private static final float SIGHT_Y0 = 3.5F;
    private static final float SIGHT_Y1 = 11.5F;
    private static final float SETTLER_X0 = 4.0F;
    private static final float SETTLER_X1 = 12.0F;
    private static final float SETTLER_Y0 = 8.0F;
    private static final float SETTLER_Y1 = 14.0F;
    private static final float SETTLER_Z0 = 10.5F;
    private static final float SETTLER_Z1 = 15.0F;
    private static final float ROTAMETER_X0 = 13.5F;
    private static final float ROTAMETER_X1 = 15.0F;
    private static final float ROTAMETER_Y0 = 3.0F;
    private static final float ROTAMETER_Y1 = 9.0F;
    private static final float ROTAMETER_Z0 = 0.25F;
    private static final float ROTAMETER_Z1 = 1.75F;
    private static final float SENSOR_X0 = 7.0F;
    private static final float SENSOR_X1 = 9.0F;
    private static final float SENSOR_TOP = 15.27F;
    private static final float SENSOR_Z0 = 12.0F;
    private static final float SENSOR_Z1 = 14.0F;
    private static final int CULTURE_PALE = 0xD8EC8E;
    private static final int CULTURE_DENSE = 0x48B83E;

    public AlgaePhotobioreactorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            AlgaePhotobioreactorBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        AlgaeVisualState state = blockEntity.visualState();
        AlgaeVisualState.Animation animation = blockEntity.animation();
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        animate(animation, state, time);

        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        PoseStack.Pose pose = poseStack.last();

        VertexConsumer translucent = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
        float level = Mth.clamp(state.water() * 4.0F, 0.0F, 1.0F);
        if (level > 0.001F) {
            renderCulture(translucent, pose, animation, level, packedLight, packedOverlay);
            if (animation.flow > 0.05F && level > 0.1F) {
                renderBubbles(translucent, pose, animation, level, packedLight, packedOverlay);
            }
        }
        renderTank(translucent, pose, Fluids.WATER, state.water(), WATER_X, packedLight, packedOverlay);
        renderTank(translucent, pose, state.carbonFluid(), state.carbon(), CARBON_X, packedLight, packedOverlay);

        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        renderHarvest(cutout, pose, state.harvest(), packedLight, packedOverlay);
        renderFloat(cutout, pose, state.aeration(), animation.flow, time, packedLight, packedOverlay);
        if (state.lit()) {
            renderSensorGlow(bufferSource.getBuffer(RenderType.eyes(GLOW)), pose, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void animate(AlgaeVisualState.Animation animation, AlgaeVisualState state, float time) {
        float elapsed = Double.isNaN(animation.lastTime) ? 0.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        if (animation.density < 0.0F) {
            animation.density = state.density();
        }
        animation.density += (state.density() - animation.density) * Math.min(1.0F, elapsed * 0.05F);
        float flow = state.growing() ? state.aeration() / (float) AlgaeCultureDynamics.RATED_AERATION : 0.0F;
        animation.flow += (flow - animation.flow) * Math.min(1.0F, elapsed * 0.1F);
        animation.bubbleClock = (animation.bubbleClock + animation.flow * BUBBLE_SPEED * elapsed) % 1000.0F;
    }

    /** An octagonal prism of culture in each column: pale and thin when young, deep green and nearly opaque when dense. */
    private static void renderCulture(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            AlgaeVisualState.Animation animation,
            float level,
            int light,
            int overlay
    ) {
        TextureAtlasSprite sprite = sprite(CULTURE);
        float density = Mth.clamp(animation.density, 0.0F, 1.0F);
        int color = lerpColor(density, CULTURE_PALE, CULTURE_DENSE);
        int alpha = Math.round(255.0F * (0.72F + 0.25F * density));
        float y0 = GLASS_Y0;
        float y1 = GLASS_Y0 + (GLASS_Y1 - GLASS_Y0 - 0.25F) * level;
        for (float cx : COLUMN_XS) {
            float[] xs = new float[8];
            float[] zs = new float[8];
            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(22.5 + 45.0 * i);
                xs[i] = cx + CULTURE_RADIUS * (float) Math.cos(angle);
                zs[i] = COLUMN_Z + CULTURE_RADIUS * (float) Math.sin(angle);
            }
            for (int i = 0; i < 8; i++) {
                int j = (i + 1) % 8;
                Vector3f normal = new Vector3f((xs[i] + xs[j]) / 2.0F - cx, 0.0F, (zs[i] + zs[j]) / 2.0F - COLUMN_Z).normalize();
                float u0 = sprite.getU(i % 4 / 4.0F);
                float u1 = sprite.getU((i % 4 + 1) / 4.0F);
                float vTop = sprite.getV(1.0F - Math.min(1.0F, (y1 - y0) / 16.0F));
                quad(consumer, pose, normal, color, alpha, light, overlay,
                        new float[] {xs[i], y0, zs[i], u0, sprite.getV1()},
                        new float[] {xs[j], y0, zs[j], u1, sprite.getV1()},
                        new float[] {xs[j], y1, zs[j], u1, vTop},
                        new float[] {xs[i], y1, zs[i], u0, vTop});
            }
            Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
            float uc = (sprite.getU0() + sprite.getU1()) / 2.0F;
            float vc = (sprite.getV0() + sprite.getV1()) / 2.0F;
            for (int i = 0; i < 8; i++) {
                int j = (i + 1) % 8;
                quad(consumer, pose, up, color, alpha, light, overlay,
                        new float[] {cx, y1, COLUMN_Z, uc, vc},
                        new float[] {xs[i], y1, zs[i], sprite.getU0(), sprite.getV0()},
                        new float[] {xs[j], y1, zs[j], sprite.getU1(), sprite.getV0()},
                        new float[] {xs[j], y1, zs[j], sprite.getU1(), sprite.getV0()});
            }
        }
    }

    /** Bubbles rise from the sparger at a rate that follows aeration, wobbling as they climb. */
    private static void renderBubbles(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            AlgaeVisualState.Animation animation,
            float level,
            int light,
            int overlay
    ) {
        TextureAtlasSprite sprite = sprite(BUBBLE);
        float height = (GLASS_Y1 - GLASS_Y0 - 0.25F) * level;
        float clock = animation.bubbleClock;
        int alpha = Math.round(255.0F * Mth.clamp(animation.flow, 0.0F, 1.0F));
        for (int column = 0; column < COLUMN_XS.length; column++) {
            float cx = COLUMN_XS[column];
            for (int i = 0; i < BUBBLES_PER_COLUMN; i++) {
                float phase = (i + column * 0.37F) / BUBBLES_PER_COLUMN;
                float seed = i * 2.3F + column * 1.7F;
                float rise = (clock + phase) % 1.0F;
                float x = cx + Mth.sin(seed + clock * 9.0F) * BUBBLE_WOBBLE;
                float z = COLUMN_Z + Mth.cos(seed * 1.9F + clock * 7.0F) * BUBBLE_WOBBLE;
                float y = GLASS_Y0 + 0.3F + rise * Math.max(0.0F, height - 0.6F - BUBBLE_SIZE);
                float half = BUBBLE_SIZE / 2.0F;
                cube(consumer, pose, sprite, BUBBLE_SIZE, x - half, y, z - half, x + half, y + BUBBLE_SIZE, z + half, 0xFFFFFF, alpha, light, overlay);
            }
        }
    }

    private static void renderTank(VertexConsumer consumer, PoseStack.Pose pose, Fluid fluid, float fill, float x, int light, int overlay) {
        if (fluid == Fluids.EMPTY || fill <= 0.001F) {
            return;
        }
        IClientFluidTypeExtensions fluidType = IClientFluidTypeExtensions.of(fluid);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidType.getStillTexture());
        int tint = fluidType.getTintColor();
        float y1 = SIGHT_Y0 + (SIGHT_Y1 - SIGHT_Y0) * Mth.clamp(fill, 0.0F, 1.0F);
        cube(consumer, pose, sprite, 16.0F, x + 0.15F, SIGHT_Y0, SIGHT_Z0 + 0.15F, x + 1.85F, y1, SIGHT_Z1 - 0.15F, tint & 0xFFFFFF, 220, light, overlay);
    }

    /** Harvested Algae Biomass settles in the rear tank, rising with the output stack. */
    private static void renderHarvest(VertexConsumer consumer, PoseStack.Pose pose, float harvest, int light, int overlay) {
        if (harvest <= 0.001F) {
            return;
        }
        float y1 = SETTLER_Y0 + (SETTLER_Y1 - SETTLER_Y0 - 0.4F) * Mth.clamp(harvest, 0.0F, 1.0F);
        cube(consumer, pose, sprite(SLUDGE), 16.0F, SETTLER_X0 + 0.3F, SETTLER_Y0, SETTLER_Z0 + 0.3F, SETTLER_X1 - 0.3F, y1, SETTLER_Z1 - 0.3F, 0xFFFFFF, 255, light, overlay);
    }

    /** The rotameter float rides at the selected aeration and bobs while gas flows. */
    private static void renderFloat(VertexConsumer consumer, PoseStack.Pose pose, int aeration, float flow, float time, int light, int overlay) {
        float span = AlgaeCultureDynamics.MAX_AERATION - AlgaeCultureDynamics.MIN_AERATION;
        float fraction = (AlgaeCultureDynamics.clampAeration(aeration) - AlgaeCultureDynamics.MIN_AERATION) / span;
        float bob = Mth.sin(time * 0.3F) * 0.12F * Mth.clamp(flow, 0.0F, 1.0F);
        float y = ROTAMETER_Y0 + 0.3F + fraction * (ROTAMETER_Y1 - ROTAMETER_Y0 - 1.1F) + bob;
        cube(consumer, pose, sprite(FLOAT), 16.0F, ROTAMETER_X0 + 0.35F, y, ROTAMETER_Z0 + 0.35F, ROTAMETER_X1 - 0.35F, y + 0.5F, ROTAMETER_Z1 - 0.35F, 0xFFFFFF, 255, light, overlay);
    }

    private static void renderSensorGlow(VertexConsumer consumer, PoseStack.Pose pose, int overlay) {
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        quad(consumer, pose, up, 0xFFFFFF, 255, LightTexture.FULL_BRIGHT, overlay,
                new float[] {SENSOR_X0, SENSOR_TOP, SENSOR_Z0, 0.0F, 0.0F},
                new float[] {SENSOR_X0, SENSOR_TOP, SENSOR_Z1, 0.0F, 1.0F},
                new float[] {SENSOR_X1, SENSOR_TOP, SENSOR_Z1, 1.0F, 1.0F},
                new float[] {SENSOR_X1, SENSOR_TOP, SENSOR_Z0, 1.0F, 0.0F});
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
        float du = Math.min(1.0F, (x1 - x0) / span);
        float dz = Math.min(1.0F, (z1 - z0) / span);
        float dy = Math.min(1.0F, (y1 - y0) / span);
        float ux = u0 + uw * du;
        float uz = u0 + uw * dz;
        float vy = v0 + vh * dy;
        float vz = v0 + vh * dz;
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
        if (edge1.lengthSquared() < 1.0E-8F) {
            edge1.set(d[0] - a[0], d[1] - a[1], d[2] - a[2]);
        }
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

    private static int lerpColor(float t, int from, int to) {
        int red = Math.round(Mth.lerp(t, from >> 16 & 0xFF, to >> 16 & 0xFF));
        int green = Math.round(Mth.lerp(t, from >> 8 & 0xFF, to >> 8 & 0xFF));
        int blue = Math.round(Mth.lerp(t, from & 0xFF, to & 0xFF));
        return red << 16 | green << 8 | blue;
    }
}
