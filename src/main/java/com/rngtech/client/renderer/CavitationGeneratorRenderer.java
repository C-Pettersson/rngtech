package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.CavitationGeneratorBlockEntity;
import com.rngtech.content.blockentity.CavitationVisualState;
import com.rngtech.content.energy.CavitationRotorDynamics;
import com.rngtech.content.energy.CavitationRotorMaterial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws the moving parts of the Cavitation Generator: the installed rotor, the chamber glow while it cavitates, heat
 * glow on the fins, and both tank levels. Positions are in model pixels and match tools/blockmodels/cavitation_generator.py.
 */
public class CavitationGeneratorRenderer implements BlockEntityRenderer<CavitationGeneratorBlockEntity> {
    private static final ResourceLocation GLOW = RNGTech.id("textures/block/cavitation_generator/fx/glow.png");
    private static final ResourceLocation HEAT = RNGTech.id("textures/block/cavitation_generator/fx/heat.png");
    private static final Map<String, ModelResourceLocation> ROTOR_MODELS = new HashMap<>();
    private static final float AXIS_X = 8.0F;
    private static final float AXIS_Y = 9.0F;
    private static final float DEGREES_PER_TICK_AT_MAX = 30.0F;
    private static final float[] FIN_XS = {4.0F, 5.25F, 6.5F, 7.75F, 9.0F, 10.25F, 11.5F};
    private static final float TANK_BOTTOM = 3.5F;
    private static final float TANK_HEIGHT = 8.5F;

    static {
        for (CavitationRotorMaterial material : CavitationRotorMaterial.values()) {
            ROTOR_MODELS.put(material.getSerializedName(), new ModelResourceLocation(
                    RNGTech.id("block/cavitation_generator/rotor_" + material.getSerializedName()),
                    ModelResourceLocation.STANDALONE_VARIANT
            ));
        }
    }

    private final ModelBlockRenderer modelRenderer;

    public CavitationGeneratorRenderer(BlockEntityRendererProvider.Context context) {
        modelRenderer = context.getBlockRenderDispatcher().getModelRenderer();
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        ROTOR_MODELS.values().forEach(event::register);
    }

    @Override
    public void render(
            CavitationGeneratorBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        CavitationVisualState state = blockEntity.visualState();
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        animate(blockEntity.animation(), state, time);

        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);

        renderRotor(blockEntity.animation(), state, poseStack, bufferSource, packedLight, packedOverlay);
        renderGlow(blockEntity.animation(), state.calm(), time, poseStack, bufferSource, packedOverlay);
        renderHeat(state.strain(), poseStack, bufferSource, packedOverlay);
        renderTank(state.inputFluid(), state.input(), 0.7F, 2.3F, poseStack, bufferSource, packedLight, packedOverlay);
        renderTank(state.outputFluid(), state.output(), 13.7F, 15.3F, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void animate(CavitationVisualState.Animation animation, CavitationVisualState state, float time) {
        float elapsed = Double.isNaN(animation.lastTime) ? 0.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        animation.speed += (state.speed() - animation.speed) * Math.min(1.0F, elapsed * 0.35F);
        animation.angle = (animation.angle + animation.speed * DEGREES_PER_TICK_AT_MAX * elapsed) % 360.0F;
        float glow = state.cavitating() ? (float) (CavitationRotorDynamics.generation(animation.speed) / CavitationRotorDynamics.MAX_GENERATION) : 0.0F;
        animation.glow += (glow - animation.glow) * Math.min(1.0F, elapsed * 0.3F);
    }

    private void renderRotor(
            CavitationVisualState.Animation animation,
            CavitationVisualState state,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        ModelResourceLocation location = ROTOR_MODELS.get(state.rotor());
        if (location == null) {
            return;
        }
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
        float tint = 1.0F - 0.4F * Mth.clamp(state.wear(), 0.0F, 1.0F);
        poseStack.pushPose();
        poseStack.translate(AXIS_X, AXIS_Y, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(animation.angle));
        poseStack.translate(-AXIS_X, -AXIS_Y, 0.0F);
        poseStack.scale(16.0F, 16.0F, 16.0F);
        modelRenderer.renderModel(
                poseStack.last(),
                bufferSource.getBuffer(Sheets.cutoutBlockSheet()),
                null,
                model,
                tint,
                tint * 0.97F,
                tint * 0.94F,
                packedLight,
                packedOverlay,
                ModelData.EMPTY,
                RenderType.cutout()
        );
        poseStack.popPose();
    }

    /**
     * Bubble collapse lights the cavity: cyan at rated speed, shifting toward white in supercavitation. Off resonance
     * the collapse is ragged and the glow flickers hard; near resonance it settles into a steady glow.
     */
    private static void renderGlow(
            CavitationVisualState.Animation animation,
            float calm,
            float time,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedOverlay
    ) {
        if (animation.glow <= 0.01F) {
            return;
        }
        float agitation = 0.04F + 0.36F * (1.0F - Mth.clamp(calm, 0.0F, 1.0F));
        float flicker = 1.0F - agitation + agitation * Mth.sin(time * 1.35F) * Mth.sin(time * 2.9F + 1.0F);
        float superMix = Mth.clamp((animation.speed - (float) CavitationRotorDynamics.RATED_SPEED) / (1.0F - (float) CavitationRotorDynamics.RATED_SPEED), 0.0F, 1.0F);
        float strength = animation.glow * flicker * 1.6F;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.eyes(GLOW));
        PoseStack.Pose pose = poseStack.last();
        int red = color(strength * (0.7F + 0.3F * superMix));
        int green = color(strength);
        int blue = color(strength * (1.0F - 0.2F * superMix));
        float z = 2.95F;
        quad(consumer, pose, AXIS_X + 4.5F, AXIS_Y - 4.5F, z, AXIS_X - 4.5F, AXIS_Y + 4.5F, z, red, green, blue, packedOverlay, 0.0F, 0.0F, -1.0F);
    }

    private static void renderHeat(float strain, PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        float heat = Mth.clamp((strain - 0.15F) / 0.85F, 0.0F, 1.0F);
        if (heat <= 0.01F) {
            return;
        }
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.eyes(HEAT));
        PoseStack.Pose pose = poseStack.last();
        int level = color(heat);
        for (float x : FIN_XS) {
            float west = x - 0.01F;
            float east = x + 0.51F;
            sideQuad(consumer, pose, west, 12.0F, 11.5F, 15.0F, 14.75F, level, packedOverlay, -1.0F);
            sideQuad(consumer, pose, east, 12.0F, 11.5F, 15.0F, 14.75F, level, packedOverlay, 1.0F);
            topQuad(consumer, pose, x, x + 0.5F, 15.01F, 11.5F, 14.75F, level, packedOverlay);
        }
    }

    private static void renderTank(
            Fluid fluid,
            float level,
            float x0,
            float x1,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (fluid == Fluids.EMPTY || level <= 0.001F) {
            return;
        }
        IClientFluidTypeExtensions fluidType = IClientFluidTypeExtensions.of(fluid);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidType.getStillTexture());
        int tint = fluidType.getTintColor();
        int red = tint >> 16 & 0xFF;
        int green = tint >> 8 & 0xFF;
        int blue = tint & 0xFF;
        float y0 = TANK_BOTTOM;
        float y1 = TANK_BOTTOM + TANK_HEIGHT * Mth.clamp(level, 0.0F, 1.0F);
        float z0 = 8.45F;
        float z1 = 10.55F;
        VertexConsumer consumer = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
        PoseStack.Pose pose = poseStack.last();
        float vTop = sprite.getV(1.0F - (y1 - y0) / 16.0F);
        float vBottom = sprite.getV(1.0F);
        fluidFace(consumer, pose, sprite, x1, y0, z0, x0, y1, z0, vTop, vBottom, red, green, blue, packedLight, packedOverlay, 0.0F, 0.0F, -1.0F);
        fluidFace(consumer, pose, sprite, x0, y0, z1, x1, y1, z1, vTop, vBottom, red, green, blue, packedLight, packedOverlay, 0.0F, 0.0F, 1.0F);
        fluidSide(consumer, pose, sprite, x0, y0, y1, z0, z1, vTop, vBottom, red, green, blue, packedLight, packedOverlay, -1.0F);
        fluidSide(consumer, pose, sprite, x1, y0, y1, z1, z0, vTop, vBottom, red, green, blue, packedLight, packedOverlay, 1.0F);
        float u0 = sprite.getU(0.0F);
        float u1 = sprite.getU((x1 - x0) / 16.0F);
        float v0 = sprite.getV(0.0F);
        float v1 = sprite.getV((z1 - z0) / 16.0F);
        vertex(consumer, pose, x0, y1, z0, u0, v0, red, green, blue, packedLight, packedOverlay, 0.0F, 1.0F, 0.0F);
        vertex(consumer, pose, x0, y1, z1, u0, v1, red, green, blue, packedLight, packedOverlay, 0.0F, 1.0F, 0.0F);
        vertex(consumer, pose, x1, y1, z1, u1, v1, red, green, blue, packedLight, packedOverlay, 0.0F, 1.0F, 0.0F);
        vertex(consumer, pose, x1, y1, z0, u1, v0, red, green, blue, packedLight, packedOverlay, 0.0F, 1.0F, 0.0F);
    }

    private static void fluidFace(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
            float xa, float y0, float z, float xb, float y1, float zb,
            float vTop, float vBottom, int red, int green, int blue, int light, int overlay,
            float nx, float ny, float nz
    ) {
        float u0 = sprite.getU(0.0F);
        float u1 = sprite.getU(Math.abs(xb - xa) / 16.0F);
        vertex(consumer, pose, xa, y0, z, u0, vBottom, red, green, blue, light, overlay, nx, ny, nz);
        vertex(consumer, pose, xb, y0, z, u1, vBottom, red, green, blue, light, overlay, nx, ny, nz);
        vertex(consumer, pose, xb, y1, z, u1, vTop, red, green, blue, light, overlay, nx, ny, nz);
        vertex(consumer, pose, xa, y1, z, u0, vTop, red, green, blue, light, overlay, nx, ny, nz);
    }

    private static void fluidSide(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
            float x, float y0, float y1, float za, float zb,
            float vTop, float vBottom, int red, int green, int blue, int light, int overlay, float nx
    ) {
        float u0 = sprite.getU(0.0F);
        float u1 = sprite.getU(Math.abs(zb - za) / 16.0F);
        vertex(consumer, pose, x, y0, za, u0, vBottom, red, green, blue, light, overlay, nx, 0.0F, 0.0F);
        vertex(consumer, pose, x, y0, zb, u1, vBottom, red, green, blue, light, overlay, nx, 0.0F, 0.0F);
        vertex(consumer, pose, x, y1, zb, u1, vTop, red, green, blue, light, overlay, nx, 0.0F, 0.0F);
        vertex(consumer, pose, x, y1, za, u0, vTop, red, green, blue, light, overlay, nx, 0.0F, 0.0F);
    }

    /** A quad in the z plane from (x0, y0) to (x1, y1), ordered to face along its normal. */
    private static void quad(
            VertexConsumer consumer, PoseStack.Pose pose,
            float x0, float y0, float z0, float x1, float y1, float z1,
            int red, int green, int blue, int overlay, float nx, float ny, float nz
    ) {
        int light = LightTexture.FULL_BRIGHT;
        vertex(consumer, pose, x0, y0, z0, 0.0F, 1.0F, red, green, blue, light, overlay, nx, ny, nz);
        vertex(consumer, pose, x1, y0, z0, 1.0F, 1.0F, red, green, blue, light, overlay, nx, ny, nz);
        vertex(consumer, pose, x1, y1, z1, 1.0F, 0.0F, red, green, blue, light, overlay, nx, ny, nz);
        vertex(consumer, pose, x0, y1, z1, 0.0F, 0.0F, red, green, blue, light, overlay, nx, ny, nz);
    }

    private static void sideQuad(VertexConsumer consumer, PoseStack.Pose pose, float x, float y0, float z0, float y1, float z1, int level, int overlay, float nx) {
        int light = LightTexture.FULL_BRIGHT;
        float za = nx < 0.0F ? z0 : z1;
        float zb = nx < 0.0F ? z1 : z0;
        vertex(consumer, pose, x, y0, za, 0.0F, 1.0F, level, level, level, light, overlay, nx, 0.0F, 0.0F);
        vertex(consumer, pose, x, y0, zb, 1.0F, 1.0F, level, level, level, light, overlay, nx, 0.0F, 0.0F);
        vertex(consumer, pose, x, y1, zb, 1.0F, 0.0F, level, level, level, light, overlay, nx, 0.0F, 0.0F);
        vertex(consumer, pose, x, y1, za, 0.0F, 0.0F, level, level, level, light, overlay, nx, 0.0F, 0.0F);
    }

    private static void topQuad(VertexConsumer consumer, PoseStack.Pose pose, float x0, float x1, float y, float z0, float z1, int level, int overlay) {
        int light = LightTexture.FULL_BRIGHT;
        vertex(consumer, pose, x0, y, z0, 0.0F, 0.0F, level, level, level, light, overlay, 0.0F, 1.0F, 0.0F);
        vertex(consumer, pose, x0, y, z1, 0.0F, 1.0F, level, level, level, light, overlay, 0.0F, 1.0F, 0.0F);
        vertex(consumer, pose, x1, y, z1, 1.0F, 1.0F, level, level, level, light, overlay, 0.0F, 1.0F, 0.0F);
        vertex(consumer, pose, x1, y, z0, 1.0F, 0.0F, level, level, level, light, overlay, 0.0F, 1.0F, 0.0F);
    }

    private static int color(float value) {
        return Mth.clamp(Math.round(value * 255.0F), 0, 255);
    }

    private static void vertex(
            VertexConsumer consumer, PoseStack.Pose pose,
            float x, float y, float z, float u, float v,
            int red, int green, int blue, int light, int overlay,
            float nx, float ny, float nz
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, 255)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
