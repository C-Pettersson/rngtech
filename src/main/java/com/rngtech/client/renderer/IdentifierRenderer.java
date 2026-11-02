package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.IdentifierBlockEntity;
import com.rngtech.content.blockentity.IdentifierVisualState;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/**
 * Draws the working parts of an Identifier: the item on the scanning bed, which lifts and turns under the lens while it
 * is read; a fan of teal light from the lens that sweeps across the bed, once there and back per reading; the reading's
 * progress on the cabinet readout; and the Identifying Fluid in the sight vial at the tank's level. Positions are in
 * model pixels and match tools/blockmodels/identifier.py.
 */
public class IdentifierRenderer implements BlockEntityRenderer<IdentifierBlockEntity> {
    private static final ResourceLocation BEAM = RNGTech.id("textures/block/resonance_calibrator/fx/beam.png");
    private static final ResourceLocation FLUID = RNGTech.id("block/identifier/fluid");
    private static final ResourceLocation LED = RNGTech.id("block/crusher/fx/led");
    // Scanning bed and the lens over it.
    private static final float BED_TOP = 5.0F;
    private static final float BED_X0 = 4.5F;
    private static final float BED_X1 = 11.5F;
    private static final float BED_Z0 = 2.5F;
    private static final float BED_Z1 = 7.5F;
    private static final float LENS_X = 8.0F;
    private static final float LENS_Z = 4.75F;
    private static final float LENS_HALF = 1.6F;
    private static final float LENS_FACE_Y = 8.65F;
    // The item rests on the bed, and floats this high while it is read.
    private static final float ITEM_SIZE = 6.0F;
    private static final float BLOCK_ITEM_SIZE = 4.5F;
    private static final float ITEM_LIFT = 1.5F;
    private static final float ITEM_BOB = 0.25F;
    private static final float TURNS_PER_TICK = 1.0F / 90.0F;
    /** Half a reading, the scan's pass one way, kept between a flicker and a crawl. */
    private static final float MIN_SWEEP_TICKS = 20.0F;
    private static final float MAX_SWEEP_TICKS = 80.0F;
    // Sight vial inside its rails, from its bottom cap to its top cap.
    private static final float VIAL_X0 = 12.5F;
    private static final float VIAL_X1 = 14.5F;
    private static final float VIAL_Z0 = 1.5F;
    private static final float VIAL_Z1 = 3.5F;
    private static final float VIAL_Y0 = 3.0F;
    private static final float VIAL_Y1 = 11.0F;
    // Progress bar along the bottom of the readout on the cabinet front.
    private static final float READOUT_X0 = 3.0F;
    private static final float READOUT_X1 = 6.0F;
    private static final float READOUT_Y0 = 6.4F;
    private static final float READOUT_Y1 = 6.9F;
    private static final float READOUT_Z = 7.75F;
    private static final float SURFACE = 0.03F;
    private static final int SCAN_COLOR = 0x7FE6D2;
    private static final int FAN_COLOR = 0x2F6A62;

    private final ItemRenderer itemRenderer;

    public IdentifierRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(
            IdentifierBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        BlockState blockState = blockEntity.getBlockState();
        boolean working = blockState.getValue(BaseMachineBlock.ACTIVE);
        IdentifierVisualState state = blockEntity.visualState();
        IdentifierVisualState.Animation animation = blockEntity.animation();
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        animate(animation, state, working, time);

        Direction facing = blockState.getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        PoseStack.Pose pose = poseStack.last();

        renderVial(bufferSource.getBuffer(Sheets.cutoutBlockSheet()), pose, state.fluidFill(), packedOverlay);
        if (!state.input().isEmpty()) {
            renderItem(state.input(), animation, time, blockEntity, poseStack, bufferSource, packedLight, packedOverlay);
        }
        if (animation.lift > 0.05F) {
            float reading = animation.sweep / 2.0F;
            float across = reading < 0.5F ? reading * 2.0F : 2.0F - reading * 2.0F;
            float z = Mth.lerp(across, BED_Z0, BED_Z1);
            int alpha = Math.round(255 * animation.lift);
            renderScan(bufferSource.getBuffer(RenderType.eyes(BEAM)), pose, z, alpha, packedOverlay);
            renderReadout(bufferSource.getBuffer(Sheets.cutoutBlockSheet()), pose, reading, packedOverlay);
        }
        poseStack.popPose();
    }

    /** The item rises off the bed as reading starts and settles when it stops; the scan runs while it is up. */
    private static void animate(IdentifierVisualState.Animation animation, IdentifierVisualState state, boolean working, float time) {
        float elapsed = Double.isNaN(animation.lastTime) ? 0.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        float target = working && !state.input().isEmpty() ? 1.0F : 0.0F;
        animation.lift += (target - animation.lift) * Math.min(1.0F, elapsed * (target > animation.lift ? 0.1F : 0.06F));
        animation.turn = (animation.turn + TURNS_PER_TICK * animation.lift * elapsed) % 1.0F;
        float sweepTicks = Mth.clamp(state.cycleTicks() / 2.0F, MIN_SWEEP_TICKS, MAX_SWEEP_TICKS);
        animation.sweep = (animation.sweep + elapsed / sweepTicks) % 2.0F;
    }

    private void renderItem(
            ItemStack stack, IdentifierVisualState.Animation animation, float time, IdentifierBlockEntity blockEntity,
            PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay
    ) {
        boolean block = stack.getItem() instanceof BlockItem;
        float size = block ? BLOCK_ITEM_SIZE : ITEM_SIZE;
        float bob = Mth.sin(time * 0.12F) * ITEM_BOB * animation.lift;
        poseStack.pushPose();
        poseStack.translate(LENS_X, BED_TOP + (block ? size / 4.0F : 0.25F) + ITEM_LIFT * animation.lift + bob, LENS_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(animation.turn * 360.0F + (blockEntity.getBlockPos().hashCode() & 0xFF)));
        if (!block) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        }
        poseStack.scale(size, size, size);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, poseStack, bufferSource, blockEntity.getLevel(),
                (int) blockEntity.getBlockPos().asLong());
        poseStack.popPose();
    }

    /** A bright line on the bed at {@code z}, and a faint fan of light from the lens face down to it. */
    private static void renderScan(VertexConsumer consumer, PoseStack.Pose pose, float z, int alpha, int overlay) {
        int light = LightTexture.FULL_BRIGHT;
        float y = BED_TOP + SURFACE;
        FurnaceGlow.quad(consumer, pose, new Vector3f(0.0F, 1.0F, 0.0F), SCAN_COLOR, alpha, light, overlay,
                new float[] {BED_X0, y, z - 0.35F, 0.0F, 0.0F}, new float[] {BED_X1, y, z - 0.35F, 1.0F, 0.0F},
                new float[] {BED_X1, y, z + 0.35F, 1.0F, 1.0F}, new float[] {BED_X0, y, z + 0.35F, 0.0F, 1.0F});
        float[] top0 = {LENS_X - LENS_HALF, LENS_FACE_Y, LENS_Z, 0.0F, 0.0F};
        float[] top1 = {LENS_X + LENS_HALF, LENS_FACE_Y, LENS_Z, 1.0F, 0.0F};
        float[] foot1 = {BED_X1, y, z, 1.0F, 1.0F};
        float[] foot0 = {BED_X0, y, z, 0.0F, 1.0F};
        for (float side : new float[] {-1.0F, 1.0F}) {
            FurnaceGlow.quad(consumer, pose, new Vector3f(0.0F, 0.0F, side), FAN_COLOR, alpha, light, overlay, top0, top1, foot1, foot0);
        }
    }

    /** The reading's progress as a bar filling along the readout. */
    private static void renderReadout(VertexConsumer consumer, PoseStack.Pose pose, float reading, int overlay) {
        TextureAtlasSprite sprite = FurnaceGlow.sprite(LED);
        float x1 = Mth.lerp(reading, READOUT_X0, READOUT_X1);
        float z = READOUT_Z - SURFACE;
        FurnaceGlow.quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), SCAN_COLOR, 255, LightTexture.FULL_BRIGHT, overlay,
                new float[] {READOUT_X0, READOUT_Y0, z, sprite.getU0(), sprite.getV1()},
                new float[] {x1, READOUT_Y0, z, sprite.getU1(), sprite.getV1()},
                new float[] {x1, READOUT_Y1, z, sprite.getU1(), sprite.getV0()},
                new float[] {READOUT_X0, READOUT_Y1, z, sprite.getU0(), sprite.getV0()});
    }

    /** The Identifying Fluid standing in the vial at the tank's share of its capacity. */
    private static void renderVial(VertexConsumer consumer, PoseStack.Pose pose, float fill, int overlay) {
        if (fill <= 0.005F) {
            return;
        }
        float top = VIAL_Y0 + (VIAL_Y1 - VIAL_Y0) * Mth.clamp(fill, 0.0F, 1.0F);
        FurnaceGlow.cube(consumer, pose, FurnaceGlow.sprite(FLUID), 16.0F, VIAL_X0, VIAL_Y0, VIAL_Z0, VIAL_X1, top, VIAL_Z1,
                0xFFFFFF, 255, LightTexture.FULL_BRIGHT, overlay);
    }
}
