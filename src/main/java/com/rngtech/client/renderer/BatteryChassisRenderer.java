package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.BatteryChassisBlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public class BatteryChassisRenderer implements BlockEntityRenderer<BatteryChassisBlockEntity> {
    private static final float FACE_OFFSET = -0.501F;

    public BatteryChassisRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            BatteryChassisBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);
        String materialId = blockEntity.block().material().blockId();
        int slots = Math.min(blockEntity.getInventory().getSlots(), blockEntity.block().material().slots());
        for (int slot = 0; slot < slots; slot++) {
            if (!blockEntity.getInventory().getStackInSlot(slot).isEmpty()) {
                renderSlotOverlay(slotTexture(materialId, slot), facing, poseStack, bufferSource, packedLight, packedOverlay);
            }
        }
    }

    private static ResourceLocation slotTexture(String materialId, int slot) {
        return RNGTech.id("textures/block/battery_chassis/slot_overlays/"
                + materialId
                + "_slot_"
                + String.format("%02d", slot + 1)
                + ".png");
    }

    private static void renderSlotOverlay(
            ResourceLocation texture,
            Direction facing,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRotation(facing)));
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, -0.5F, 0.5F, FACE_OFFSET, 0.0F, 0.0F, packedLight, packedOverlay);
        vertex(consumer, pose, -0.5F, -0.5F, FACE_OFFSET, 0.0F, 1.0F, packedLight, packedOverlay);
        vertex(consumer, pose, 0.5F, -0.5F, FACE_OFFSET, 1.0F, 1.0F, packedLight, packedOverlay);
        vertex(consumer, pose, 0.5F, 0.5F, FACE_OFFSET, 1.0F, 0.0F, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static float yRotation(Direction facing) {
        if (facing == Direction.EAST) {
            return 90.0F;
        }
        if (facing == Direction.SOUTH) {
            return 180.0F;
        }
        if (facing == Direction.WEST) {
            return 270.0F;
        }
        return 0.0F;
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            int packedLight,
            int packedOverlay
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 0.0F, -1.0F);
    }
}
