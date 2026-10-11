package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.VacuumCollapseGeneratorBlockEntity;
import com.rngtech.content.blockentity.VacuumCollapseVisualState;
import com.rngtech.content.energy.CollapseNozzleMaterial;
import com.rngtech.content.energy.VacuumCollapsePartMaterial;

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
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws the installed Gear and the collapse of the Vacuum Collapse Generator: the Void Chamber, Dimensional Stabilizer
 * pylons, and Collapse Nozzle in their materials, the core inside the chamber (bright in the chamber's glow colour at the
 * start of a collapse, shrinking to black by its end), the cap rims pulsing faster as the collapse runs and flickering
 * with instability, and the stabilizer lenses. Positions are in model pixels and match
 * tools/blockmodels/vacuum_collapse_generator.py.
 */
public class VacuumCollapseGeneratorRenderer implements BlockEntityRenderer<VacuumCollapseGeneratorBlockEntity> {
    private static final ResourceLocation CORE = RNGTech.id("block/vacuum_collapse_generator/fx/core");
    private static final ResourceLocation RIM = RNGTech.id("textures/block/vacuum_collapse_generator/fx/rim.png");
    private static final ResourceLocation LENS = RNGTech.id("textures/block/vacuum_collapse_generator/fx/lens.png");
    private static final Map<String, ModelResourceLocation> CHAMBER_MODELS = new HashMap<>();
    private static final Map<String, ModelResourceLocation> STABILIZER_MODELS = new HashMap<>();
    private static final Map<String, ModelResourceLocation> NOZZLE_MODELS = new HashMap<>();
    /** Glow colour per part material, matching the part item textures. */
    private static final Map<String, Integer> GLOW = Map.of(
            "steel", 0xC4CCD0,
            "titanium", 0xB6C8DF,
            "tungstensteel", 0x7CAEFF,
            "nullite", 0xA05CFF,
            "exotic", 0x46FFB6
    );
    private static final int VOID = 0x08060E;
    private static final float CX = 8.0F;
    private static final float CZ = 6.5F;
    private static final float CORE_Y = 7.75F;
    private static final float CAP_HALF = 4.0F;
    private static final float[] RIM_YS = {3.5F, 11.75F};
    private static final float RIM_HEIGHT = 0.5F;
    private static final float LENS_WEST_X = 3.75F;
    private static final float LENS_EAST_X = 12.25F;
    private static final float LENS_Y = 7.5F;
    private static final float LENS_HALF = 0.85F;

    static {
        for (VacuumCollapsePartMaterial material : VacuumCollapsePartMaterial.values()) {
            CHAMBER_MODELS.put(material.getSerializedName(), partModel("chamber", material.getSerializedName()));
            STABILIZER_MODELS.put(material.getSerializedName(), partModel("stabilizer", material.getSerializedName()));
        }
        for (CollapseNozzleMaterial material : CollapseNozzleMaterial.values()) {
            if (material.vacuumCollapseCompatible()) {
                NOZZLE_MODELS.put(material.getSerializedName(), partModel("nozzle", material.getSerializedName()));
            }
        }
    }

    private final ModelBlockRenderer modelRenderer;

    public VacuumCollapseGeneratorRenderer(BlockEntityRendererProvider.Context context) {
        modelRenderer = context.getBlockRenderDispatcher().getModelRenderer();
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        CHAMBER_MODELS.values().forEach(event::register);
        STABILIZER_MODELS.values().forEach(event::register);
        NOZZLE_MODELS.values().forEach(event::register);
    }

    private static ModelResourceLocation partModel(String part, String material) {
        return new ModelResourceLocation(RNGTech.id("block/vacuum_collapse_generator/" + part + "_" + material), ModelResourceLocation.STANDALONE_VARIANT);
    }

    @Override
    public void render(
            VacuumCollapseGeneratorBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        VacuumCollapseVisualState state = blockEntity.visualState();
        VacuumCollapseVisualState.Animation animation = blockEntity.animation();
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        animate(animation, state, time);

        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        renderPart(CHAMBER_MODELS.get(state.chamber()), poseStack, cutout, packedLight, packedOverlay);
        renderPart(STABILIZER_MODELS.get(state.stabilizer()), poseStack, cutout, packedLight, packedOverlay);
        renderPart(NOZZLE_MODELS.get(state.nozzle()), poseStack, cutout, packedLight, packedOverlay);

        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        if (animation.intensity > 0.01F && !state.chamber().isEmpty()) {
            int glow = GLOW.getOrDefault(state.chamber(), 0xFFFFFF);
            renderCore(bufferSource.getBuffer(Sheets.translucentCullBlockSheet()), poseStack, animation, glow, packedOverlay);
            float brightness = animation.intensity * (0.55F + 0.45F * Mth.sin(animation.pulse)) * flicker(state.instability(), time);
            renderRims(bufferSource.getBuffer(RenderType.eyes(RIM)), poseStack.last(), glow, brightness, packedOverlay);
        }
        if (animation.intensity > 0.01F && !state.stabilizer().isEmpty()) {
            int glow = GLOW.getOrDefault(state.stabilizer(), 0xFFFFFF);
            float brightness = animation.intensity * (0.8F + 0.2F * Mth.sin(time * 0.2F));
            renderLenses(bufferSource.getBuffer(RenderType.eyes(LENS)), poseStack.last(), glow, brightness, packedOverlay);
        }
        poseStack.popPose();
    }

    /**
     * Eases toward the synced state. The rim pulse speeds up as the collapse runs, from about every 2.6 seconds at the
     * start to about every half second at the end.
     */
    private static void animate(VacuumCollapseVisualState.Animation animation, VacuumCollapseVisualState state, float time) {
        float elapsed = Double.isNaN(animation.lastTime) ? 0.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        animation.intensity += ((state.collapsing() ? 1.0F : 0.0F) - animation.intensity) * Math.min(1.0F, elapsed * 0.08F);
        animation.progress += (state.progress() - animation.progress) * Math.min(1.0F, elapsed * 0.08F);
        animation.pulse = (animation.pulse + elapsed * (0.12F + 0.45F * animation.progress)) % Mth.TWO_PI;
        animation.spin = (animation.spin + elapsed * (1.0F + 4.0F * animation.progress)) % 360.0F;
    }

    /** Instability makes the rim light stutter; a stable collapse pulses cleanly. */
    private static float flicker(float instability, float time) {
        float unstable = Mth.clamp(instability, 0.0F, 1.0F);
        return 1.0F - unstable * 0.6F * Math.max(0.0F, Mth.sin(time * 1.9F) * Mth.sin(time * 3.7F + 1.0F));
    }

    private void renderPart(ModelResourceLocation location, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        if (location == null) {
            return;
        }
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
        modelRenderer.renderModel(poseStack.last(), consumer, null, model, 1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY, RenderType.cutout());
    }

    /**
     * The core: a glowing shell that shrinks and darkens to black as the collapse runs, around a void that grows to fill
     * it. Both turn about the chamber axis, faster near the end, and stay inside the glass.
     */
    private static void renderCore(
            VertexConsumer consumer,
            PoseStack poseStack,
            VacuumCollapseVisualState.Animation animation,
            int glow,
            int overlay
    ) {
        TextureAtlasSprite sprite = sprite(CORE);
        float progress = Mth.clamp(animation.progress, 0.0F, 1.0F);
        poseStack.pushPose();
        poseStack.translate(CX, CORE_Y, CZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(animation.spin));
        PoseStack.Pose pose = poseStack.last();
        float voidHalf = 0.35F + 1.25F * progress;
        cube(consumer, pose, sprite, voidHalf, VOID, Math.round(255.0F * animation.intensity), overlay);
        float shellHalf = 2.3F - 1.1F * progress;
        int shell = lerpColor(progress * 0.85F, glow, VOID);
        cube(consumer, pose, sprite, shellHalf, shell, Math.round((150.0F + 60.0F * progress) * animation.intensity), overlay);
        poseStack.popPose();
    }

    private static void renderRims(VertexConsumer consumer, PoseStack.Pose pose, int glow, float brightness, int overlay) {
        int color = scaleColor(glow, brightness);
        float near = CZ - CAP_HALF - 0.02F;
        float far = CZ + CAP_HALF + 0.02F;
        float west = CX - CAP_HALF - 0.02F;
        float east = CX + CAP_HALF + 0.02F;
        for (float y0 : RIM_YS) {
            float y1 = y0 + RIM_HEIGHT;
            quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), color, 255, LightTexture.FULL_BRIGHT, overlay,
                    new float[] {west, y0, near, 0.0F, 1.0F}, new float[] {east, y0, near, 1.0F, 1.0F},
                    new float[] {east, y1, near, 1.0F, 0.0F}, new float[] {west, y1, near, 0.0F, 0.0F});
            quad(consumer, pose, new Vector3f(0.0F, 0.0F, 1.0F), color, 255, LightTexture.FULL_BRIGHT, overlay,
                    new float[] {west, y0, far, 0.0F, 1.0F}, new float[] {east, y0, far, 1.0F, 1.0F},
                    new float[] {east, y1, far, 1.0F, 0.0F}, new float[] {west, y1, far, 0.0F, 0.0F});
            quad(consumer, pose, new Vector3f(-1.0F, 0.0F, 0.0F), color, 255, LightTexture.FULL_BRIGHT, overlay,
                    new float[] {west, y0, near, 0.0F, 1.0F}, new float[] {west, y0, far, 1.0F, 1.0F},
                    new float[] {west, y1, far, 1.0F, 0.0F}, new float[] {west, y1, near, 0.0F, 0.0F});
            quad(consumer, pose, new Vector3f(1.0F, 0.0F, 0.0F), color, 255, LightTexture.FULL_BRIGHT, overlay,
                    new float[] {east, y0, near, 0.0F, 1.0F}, new float[] {east, y0, far, 1.0F, 1.0F},
                    new float[] {east, y1, far, 1.0F, 0.0F}, new float[] {east, y1, near, 0.0F, 0.0F});
        }
    }

    /** A diamond of light on each emitter head's inner face, matching the head's 45 degree turn. */
    private static void renderLenses(VertexConsumer consumer, PoseStack.Pose pose, int glow, float brightness, int overlay) {
        int color = scaleColor(glow, brightness);
        float[][] xs = {{LENS_WEST_X + 0.02F, 1.0F}, {LENS_EAST_X - 0.02F, -1.0F}};
        for (float[] lens : xs) {
            float x = lens[0];
            quad(consumer, pose, new Vector3f(lens[1], 0.0F, 0.0F), color, 255, LightTexture.FULL_BRIGHT, overlay,
                    new float[] {x, LENS_Y - LENS_HALF, CZ, 0.0F, 1.0F},
                    new float[] {x, LENS_Y, CZ + LENS_HALF, 1.0F, 1.0F},
                    new float[] {x, LENS_Y + LENS_HALF, CZ, 1.0F, 0.0F},
                    new float[] {x, LENS_Y, CZ - LENS_HALF, 0.0F, 0.0F});
        }
    }

    /** A cube of the given half size around the current origin, drawn full bright. */
    private static void cube(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, float half, int color, int alpha, int overlay) {
        float u0 = sprite.getU0();
        float v0 = sprite.getV0();
        float u1 = sprite.getU1();
        float v1 = sprite.getV1();
        int light = LightTexture.FULL_BRIGHT;
        float a = -half;
        float b = half;
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), color, alpha, light, overlay,
                new float[] {a, a, a, u0, v1}, new float[] {b, a, a, u1, v1}, new float[] {b, b, a, u1, v0}, new float[] {a, b, a, u0, v0});
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, 1.0F), color, alpha, light, overlay,
                new float[] {a, a, b, u0, v1}, new float[] {b, a, b, u1, v1}, new float[] {b, b, b, u1, v0}, new float[] {a, b, b, u0, v0});
        quad(consumer, pose, new Vector3f(-1.0F, 0.0F, 0.0F), color, alpha, light, overlay,
                new float[] {a, a, a, u0, v1}, new float[] {a, a, b, u1, v1}, new float[] {a, b, b, u1, v0}, new float[] {a, b, a, u0, v0});
        quad(consumer, pose, new Vector3f(1.0F, 0.0F, 0.0F), color, alpha, light, overlay,
                new float[] {b, a, a, u0, v1}, new float[] {b, a, b, u1, v1}, new float[] {b, b, b, u1, v0}, new float[] {b, b, a, u0, v0});
        quad(consumer, pose, new Vector3f(0.0F, 1.0F, 0.0F), color, alpha, light, overlay,
                new float[] {a, b, a, u0, v0}, new float[] {b, b, a, u1, v0}, new float[] {b, b, b, u1, v1}, new float[] {a, b, b, u0, v1});
        quad(consumer, pose, new Vector3f(0.0F, -1.0F, 0.0F), color, alpha, light, overlay,
                new float[] {a, a, a, u0, v0}, new float[] {b, a, a, u1, v0}, new float[] {b, a, b, u1, v1}, new float[] {a, a, b, u0, v1});
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

    private static int scaleColor(int color, float scale) {
        float clamped = Mth.clamp(scale, 0.0F, 1.0F);
        int red = Math.round((color >> 16 & 0xFF) * clamped);
        int green = Math.round((color >> 8 & 0xFF) * clamped);
        int blue = Math.round((color & 0xFF) * clamped);
        return red << 16 | green << 8 | blue;
    }

    private static int lerpColor(float t, int from, int to) {
        int red = Math.round(Mth.lerp(t, from >> 16 & 0xFF, to >> 16 & 0xFF));
        int green = Math.round(Mth.lerp(t, from >> 8 & 0xFF, to >> 8 & 0xFF));
        int blue = Math.round(Mth.lerp(t, from & 0xFF, to & 0xFF));
        return red << 16 | green << 8 | blue;
    }
}
