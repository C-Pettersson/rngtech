package com.rngtech.client.renderer;

import com.rngtech.content.block.ToolBenchBlock;
import com.rngtech.content.blockentity.ToolBenchBlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Lays the Tool slot's tool flat on the crafting grid of the worktop, or the loose Tool Head and Tool Rod side by side
 * when the Tool slot is empty. Positions are in model pixels and match tools/blockmodels/tool_bench.py.
 */
public class ToolBenchRenderer implements BlockEntityRenderer<ToolBenchBlockEntity> {
    private static final float WORKTOP_TOP = 16.0F;
    private static final float GRID_CENTER_Z = 8.0F;
    private static final float TOOL_X = 8.0F;
    private static final float TOOL_SIZE = 11.0F;
    private static final float HEAD_X = 5.75F;
    private static final float ROD_X = 10.25F;
    private static final float PART_SIZE = 8.0F;
    private static final float ITEM_YAW = 45.0F;

    private final ItemRenderer itemRenderer;

    public ToolBenchRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(
            ToolBenchBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        ItemStack tool = blockEntity.displayTool();
        ItemStack head = blockEntity.displayHead();
        ItemStack rod = blockEntity.displayRod();
        if (tool.isEmpty() && head.isEmpty() && rod.isEmpty()) {
            return;
        }

        Direction facing = blockEntity.getBlockState().getValue(ToolBenchBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        int seed = (int) blockEntity.getBlockPos().asLong();
        if (!tool.isEmpty()) {
            renderFlat(tool, TOOL_X, TOOL_SIZE, blockEntity, poseStack, bufferSource, packedLight, packedOverlay, seed);
        } else {
            renderFlat(head, HEAD_X, PART_SIZE, blockEntity, poseStack, bufferSource, packedLight, packedOverlay, seed);
            renderFlat(rod, ROD_X, PART_SIZE, blockEntity, poseStack, bufferSource, packedLight, packedOverlay, seed + 1);
        }
        poseStack.popPose();
    }

    /** One item lying face up on the worktop, its sprite diagonal turned to run along the bench. */
    private void renderFlat(
            ItemStack stack,
            float x,
            float size,
            ToolBenchBlockEntity blockEntity,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            int seed
    ) {
        if (stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(x, WORKTOP_TOP + size / 32.0F, GRID_CENTER_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(ITEM_YAW));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(size, size, size);
        itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                blockEntity.getLevel(),
                seed
        );
        poseStack.popPose();
    }
}
