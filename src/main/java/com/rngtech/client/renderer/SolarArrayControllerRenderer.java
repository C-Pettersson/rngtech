package com.rngtech.client.renderer;

import com.rngtech.content.blockentity.SolarArrayControllerBlockEntity;
import com.rngtech.content.blockentity.SolarPanelBlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class SolarArrayControllerRenderer implements BlockEntityRenderer<SolarArrayControllerBlockEntity> {
    private static final float EMPTY_R = 0.45F;
    private static final float EMPTY_G = 0.55F;
    private static final float EMPTY_B = 0.48F;
    private static final float PANEL_R = 1.0F;
    private static final float PANEL_G = 0.78F;
    private static final float PANEL_B = 0.25F;
    private static final float CONTROLLER_R = 0.35F;
    private static final float CONTROLLER_G = 0.95F;
    private static final float CONTROLLER_B = 0.75F;
    private static final int FILL_ALPHA = 38;
    private static final float OUTLINE_ALPHA = 0.85F;

    public SolarArrayControllerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            SolarArrayControllerBlockEntity controller,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (!controller.shouldPreviewRange()) {
            return;
        }

        Level level = controller.getLevel();
        if (level == null) {
            return;
        }

        int range = controller.panelRange();
        BlockPos origin = controller.getBlockPos();
        renderCell(poseStack, bufferSource, 0, 0, CONTROLLER_R, CONTROLLER_G, CONTROLLER_B);
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                BlockPos pos = origin.offset(x, 0, z);
                boolean panel = level.getBlockEntity(pos) instanceof SolarPanelBlockEntity;
                renderCell(
                        poseStack,
                        bufferSource,
                        x,
                        z,
                        panel ? PANEL_R : EMPTY_R,
                        panel ? PANEL_G : EMPTY_G,
                        panel ? PANEL_B : EMPTY_B
                );
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(SolarArrayControllerBlockEntity controller) {
        return controller.shouldPreviewRange();
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    private static void renderCell(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int x,
            int z,
            float red,
            float green,
            float blue
    ) {
        renderFilledCell(poseStack, bufferSource.getBuffer(RenderType.debugQuads()), x, z, red, green, blue);
        AABB outline = new AABB(x + 0.03D, 0.03D, z + 0.03D, x + 0.97D, 1.04D, z + 0.97D);
        LevelRenderer.renderLineBox(
                poseStack,
                bufferSource.getBuffer(RenderType.lines()),
                outline,
                red,
                green,
                blue,
                OUTLINE_ALPHA
        );
    }

    private static void renderFilledCell(
            PoseStack poseStack,
            VertexConsumer consumer,
            int x,
            int z,
            float red,
            float green,
            float blue
    ) {
        PoseStack.Pose pose = poseStack.last();
        int redByte = colorByte(red);
        int greenByte = colorByte(green);
        int blueByte = colorByte(blue);
        float minX = x + 0.04F;
        float minY = 0.04F;
        float minZ = z + 0.04F;
        float maxX = x + 0.96F;
        float maxY = 1.02F;
        float maxZ = z + 0.96F;
        quad(consumer, pose, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, redByte, greenByte, blueByte);
        quad(consumer, pose, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, redByte, greenByte, blueByte);
        quad(consumer, pose, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, redByte, greenByte, blueByte);
        quad(consumer, pose, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, redByte, greenByte, blueByte);
        quad(consumer, pose, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ, redByte, greenByte, blueByte);
        quad(consumer, pose, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, redByte, greenByte, blueByte);
    }

    private static void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4,
            int red,
            int green,
            int blue
    ) {
        consumer.addVertex(pose, x1, y1, z1).setColor(red, green, blue, FILL_ALPHA);
        consumer.addVertex(pose, x2, y2, z2).setColor(red, green, blue, FILL_ALPHA);
        consumer.addVertex(pose, x3, y3, z3).setColor(red, green, blue, FILL_ALPHA);
        consumer.addVertex(pose, x4, y4, z4).setColor(red, green, blue, FILL_ALPHA);
    }

    private static int colorByte(float color) {
        return Math.round(color * 255.0F);
    }
}
