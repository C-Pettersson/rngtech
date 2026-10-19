package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.BioGeneratorBlockEntity;
import com.rngtech.content.blockentity.BioGeneratorVisualState;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws the live parts of the Bio Generator: the gas holder dome rising with stored biogas, the engine flywheel, the
 * installed Bio Chamber's culture canister, the Fluid Pumps and Biogas Membranes in the gas-line slots, digester slurry and bubbles in the sight glass, the gas-train gauge needle,
 * and the feed waiting in the hopper. Positions are in model pixels and match tools/blockmodels/bio_generator.py.
 */
public class BioGeneratorRenderer implements BlockEntityRenderer<BioGeneratorBlockEntity> {
    private static final ResourceLocation SLURRY = RNGTech.id("textures/block/bio_generator/fx/slurry.png");
    private static final ResourceLocation SOLID = RNGTech.id("textures/block/bio_generator/fx/solid.png");
    private static final ModelResourceLocation DOME = standalone("block/bio_generator/dome");
    private static final ModelResourceLocation FLYWHEEL = standalone("block/bio_generator/flywheel");
    private static final ModelResourceLocation CULTURE = standalone("block/bio_generator/culture");
    private static final ModelResourceLocation PUMP = standalone("block/bio_generator/pump");
    private static final ModelResourceLocation GAS_MEMBRANE = standalone("block/bio_generator/gas_membrane");
    /** The second gas-line slot sits this far behind the first, matching GAS_LINE_STEP in the model script. */
    private static final float GAS_LINE_STEP = 2.8F;
    private static final float DOME_BASE = 11.0F;
    private static final float DOME_SLACK = 0.45F;
    private static final float DOME_RISE = 0.75F;
    private static final float FLYWHEEL_X = 3.375F;
    private static final float FLYWHEEL_Y = 4.75F;
    private static final float DEGREES_PER_TICK_AT_RATED = 24.0F;
    private static final float GLASS_X0 = 8.0F;
    private static final float GLASS_X1 = 11.0F;
    private static final float GLASS_Y0 = 3.5F;
    private static final float GLASS_Y1 = 8.0F;
    private static final float SLURRY_FRONT = 3.62F;
    private static final float SLURRY_BACK = 3.93F;
    private static final float GAUGE_X = 3.375F;
    private static final float GAUGE_Y = 8.5F;
    private static final float GAUGE_Z = 0.94F;
    private static final float NEEDLE_LENGTH = 0.62F;
    private static final float HOPPER_X = 13.75F;
    private static final float HOPPER_Y = 8.6F;
    private static final float HOPPER_Z = 2.5F;
    private static final float HOPPER_ITEM_SCALE = 0.25F;

    private final ModelBlockRenderer modelRenderer;
    private final ItemRenderer itemRenderer;

    public BioGeneratorRenderer(BlockEntityRendererProvider.Context context) {
        modelRenderer = context.getBlockRenderDispatcher().getModelRenderer();
        itemRenderer = context.getItemRenderer();
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(DOME);
        event.register(FLYWHEEL);
        event.register(CULTURE);
        event.register(PUMP);
        event.register(GAS_MEMBRANE);
    }

    private static ModelResourceLocation standalone(String path) {
        return new ModelResourceLocation(RNGTech.id(path), ModelResourceLocation.STANDALONE_VARIANT);
    }

    @Override
    public void render(
            BioGeneratorBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        BioGeneratorVisualState state = blockEntity.visualState();
        BioGeneratorVisualState.Animation animation = blockEntity.animation();
        // The block entity's own client tick count: level game time jumps whenever the server resyncs the clock.
        float time = animation.clientTicks + partialTick;
        animate(animation, state, time);

        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);

        renderDome(animation, poseStack, bufferSource, packedLight, packedOverlay);
        renderFlywheel(animation, poseStack, bufferSource, packedLight, packedOverlay);
        if (state.chamber()) {
            renderModel(CULTURE, poseStack, bufferSource, packedLight, packedOverlay);
        }
        renderGasLine(state.lineA(), 0.0F, poseStack, bufferSource, packedLight, packedOverlay);
        renderGasLine(state.lineB(), GAS_LINE_STEP, poseStack, bufferSource, packedLight, packedOverlay);
        renderSlurry(animation, time, poseStack, bufferSource, packedLight, packedOverlay);
        renderNeedle(animation, poseStack, bufferSource, packedLight, packedOverlay);
        renderHopper(animation, state, blockEntity, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void animate(BioGeneratorVisualState.Animation animation, BioGeneratorVisualState state, float time) {
        if (Double.isNaN(animation.lastTime)) {
            animation.lastTime = time;
            animation.dome = state.gas();
            animation.slurry = slurryLevel(state);
        }
        float elapsed = Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        animation.dome = ease(animation.dome, state.gas(), 0.08F, elapsed);
        animation.flywheelSpeed = ease(animation.flywheelSpeed, state.engine(), 0.12F, elapsed);
        animation.flywheelAngle = (animation.flywheelAngle + animation.flywheelSpeed * DEGREES_PER_TICK_AT_RATED * elapsed) % 360.0F;
        animation.bubbles = ease(animation.bubbles, state.digestion(), 0.2F, elapsed);
        animation.slurry = ease(animation.slurry, slurryLevel(state), 0.1F, elapsed);
    }

    private static float slurryLevel(BioGeneratorVisualState state) {
        return state.feed() > 0.0F ? 0.25F + 0.7F * state.feed() : 0.25F;
    }

    /** Exponential approach toward the synced target, the same at any frame rate. */
    private static float ease(float current, float target, float ratePerTick, float elapsed) {
        return current + (target - current) * (1.0F - (float) Math.exp(-ratePerTick * elapsed));
    }

    /** The membrane sags when the holder is empty and swells as biogas builds, easing toward the synced fill. */
    private void renderDome(
            BioGeneratorVisualState.Animation animation,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        float scale = DOME_SLACK + DOME_RISE * Mth.clamp(animation.dome, 0.0F, 1.0F);
        poseStack.pushPose();
        poseStack.translate(0.0F, DOME_BASE, 0.0F);
        poseStack.scale(1.0F, scale, 1.0F);
        poseStack.translate(0.0F, -DOME_BASE, 0.0F);
        renderModel(DOME, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderFlywheel(
            BioGeneratorVisualState.Animation animation,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(FLYWHEEL_X, FLYWHEEL_Y, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(animation.flywheelAngle));
        poseStack.translate(-FLYWHEEL_X, -FLYWHEEL_Y, 0.0F);
        renderModel(FLYWHEEL, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderGasLine(int kind, float offset, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ModelResourceLocation model = switch (kind) {
            case BioGeneratorBlockEntity.GAS_LINE_PUMP -> PUMP;
            case BioGeneratorBlockEntity.GAS_LINE_MEMBRANE -> GAS_MEMBRANE;
            default -> null;
        };
        if (model == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, offset);
        renderModel(model, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderModel(ModelResourceLocation location, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
        poseStack.pushPose();
        poseStack.scale(16.0F, 16.0F, 16.0F);
        modelRenderer.renderModel(
                poseStack.last(),
                bufferSource.getBuffer(Sheets.cutoutBlockSheet()),
                null,
                model,
                1.0F,
                1.0F,
                1.0F,
                packedLight,
                packedOverlay,
                ModelData.EMPTY,
                RenderType.cutout()
        );
        poseStack.popPose();
    }

    /** Digester contents in the sight glass: the level falls as the current item digests, and bubbles rise with gas. */
    private static void renderSlurry(
            BioGeneratorVisualState.Animation animation,
            float time,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        float top = GLASS_Y0 + (GLASS_Y1 - GLASS_Y0) * Mth.clamp(animation.slurry, 0.0F, 1.0F);
        float x0 = GLASS_X0 + 0.02F;
        float x1 = GLASS_X1 - 0.02F;
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer slurry = bufferSource.getBuffer(RenderType.entitySolid(SLURRY));
        float u1 = (x1 - x0) / 16.0F;
        float v0 = (GLASS_Y1 - top) / 16.0F;
        float v1 = (GLASS_Y1 - GLASS_Y0) / 16.0F;
        vertex(slurry, pose, x1, GLASS_Y0, SLURRY_FRONT, 0.0F, v1, 255, 255, 255, packedLight, packedOverlay, 0.0F, 0.0F, -1.0F);
        vertex(slurry, pose, x0, GLASS_Y0, SLURRY_FRONT, u1, v1, 255, 255, 255, packedLight, packedOverlay, 0.0F, 0.0F, -1.0F);
        vertex(slurry, pose, x0, top, SLURRY_FRONT, u1, v0, 255, 255, 255, packedLight, packedOverlay, 0.0F, 0.0F, -1.0F);
        vertex(slurry, pose, x1, top, SLURRY_FRONT, 0.0F, v0, 255, 255, 255, packedLight, packedOverlay, 0.0F, 0.0F, -1.0F);
        float scum = (SLURRY_BACK - SLURRY_FRONT) / 16.0F;
        vertex(slurry, pose, x1, top, SLURRY_FRONT, 0.0F, 0.0F, 225, 230, 170, packedLight, packedOverlay, 0.0F, 1.0F, 0.0F);
        vertex(slurry, pose, x0, top, SLURRY_FRONT, u1, 0.0F, 225, 230, 170, packedLight, packedOverlay, 0.0F, 1.0F, 0.0F);
        vertex(slurry, pose, x0, top, SLURRY_BACK, u1, scum, 225, 230, 170, packedLight, packedOverlay, 0.0F, 1.0F, 0.0F);
        vertex(slurry, pose, x1, top, SLURRY_BACK, 0.0F, scum, 225, 230, 170, packedLight, packedOverlay, 0.0F, 1.0F, 0.0F);

        float intensity = Mth.clamp(animation.bubbles, 0.0F, 2.0F);
        int count = intensity <= 0.02F ? 0 : 2 + Math.round(2.5F * Math.min(intensity, 1.6F));
        float span = top - GLASS_Y0 - 0.45F;
        if (count == 0 || span <= 0.2F) {
            return;
        }
        VertexConsumer bubbles = bufferSource.getBuffer(RenderType.entityCutoutNoCull(SOLID));
        float z = SLURRY_FRONT - 0.01F;
        for (int index = 0; index < count; index++) {
            float seed = index * 0.618034F % 1.0F;
            float rise = (time * 0.025F * (0.7F + 0.5F * intensity) * (0.8F + 0.4F * seed) + index * 0.37F) % 1.0F;
            float size = 0.22F + 0.14F * seed;
            float x = x0 + 0.3F + (x1 - x0 - 0.9F) * ((seed * 1.7F + index * 0.23F) % 1.0F) + 0.12F * Mth.sin(time * 0.21F + index);
            float y = GLASS_Y0 + 0.1F + span * rise;
            quad(bubbles, pose, x, y, x + size, y + size, z, 214, 222, 160, packedLight, packedOverlay);
        }
    }

    /** Gas-train pressure: the needle sweeps from the empty stop across the dial as the holder fills. */
    private static void renderNeedle(
            BioGeneratorVisualState.Animation animation,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        // Dial angles run clockwise on the face from 135 degrees; the face is seen from the north, so u maps to -x.
        float angle = (float) Math.toRadians(135.0F + 270.0F * Mth.clamp(animation.dome, 0.0F, 1.0F));
        float dx = -Mth.cos(angle);
        float dy = -Mth.sin(angle);
        float nx = -dy * 0.06F;
        float ny = dx * 0.06F;
        float tipX = GAUGE_X + dx * NEEDLE_LENGTH;
        float tipY = GAUGE_Y + dy * NEEDLE_LENGTH;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(SOLID));
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, GAUGE_X - nx, GAUGE_Y - ny, GAUGE_Z, 0.0F, 0.0F, 190, 46, 36, packedLight, packedOverlay, 0.0F, 0.0F, -1.0F);
        vertex(consumer, pose, GAUGE_X + nx, GAUGE_Y + ny, GAUGE_Z, 1.0F, 0.0F, 190, 46, 36, packedLight, packedOverlay, 0.0F, 0.0F, -1.0F);
        vertex(consumer, pose, tipX + nx * 0.3F, tipY + ny * 0.3F, GAUGE_Z, 1.0F, 1.0F, 190, 46, 36, packedLight, packedOverlay, 0.0F, 0.0F, -1.0F);
        vertex(consumer, pose, tipX - nx * 0.3F, tipY - ny * 0.3F, GAUGE_Z, 0.0F, 1.0F, 190, 46, 36, packedLight, packedOverlay, 0.0F, 0.0F, -1.0F);
    }

    /** Feed waiting in the hopper, tipped into the funnel. */
    private void renderHopper(
            BioGeneratorVisualState.Animation animation,
            BioGeneratorVisualState state,
            BioGeneratorBlockEntity blockEntity,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (state.hopper() == Items.AIR) {
            return;
        }
        if (animation.hopper.getItem() != state.hopper()) {
            animation.hopper = new ItemStack(state.hopper());
        }
        poseStack.pushPose();
        poseStack.translate(HOPPER_X, HOPPER_Y, HOPPER_Z);
        poseStack.scale(16.0F * HOPPER_ITEM_SCALE, 16.0F * HOPPER_ITEM_SCALE, 16.0F * HOPPER_ITEM_SCALE);
        poseStack.mulPose(Axis.YP.rotationDegrees(25.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-60.0F));
        itemRenderer.renderStatic(
                animation.hopper,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                blockEntity.getLevel(),
                (int) blockEntity.getBlockPos().asLong()
        );
        poseStack.popPose();
    }

    private static void quad(
            VertexConsumer consumer, PoseStack.Pose pose,
            float x0, float y0, float x1, float y1, float z,
            int red, int green, int blue, int light, int overlay
    ) {
        vertex(consumer, pose, x0, y0, z, 0.0F, 1.0F, red, green, blue, light, overlay, 0.0F, 0.0F, -1.0F);
        vertex(consumer, pose, x1, y0, z, 1.0F, 1.0F, red, green, blue, light, overlay, 0.0F, 0.0F, -1.0F);
        vertex(consumer, pose, x1, y1, z, 1.0F, 0.0F, red, green, blue, light, overlay, 0.0F, 0.0F, -1.0F);
        vertex(consumer, pose, x0, y1, z, 0.0F, 0.0F, red, green, blue, light, overlay, 0.0F, 0.0F, -1.0F);
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
