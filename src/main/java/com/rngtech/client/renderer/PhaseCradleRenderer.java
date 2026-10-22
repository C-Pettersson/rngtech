package com.rngtech.client.renderer;

import com.rngtech.content.block.PhaseCradleBlock;
import com.rngtech.content.blockentity.PhaseCradleBlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** The target item, hanging in the Cradle's bore while it sits in the chamber, turning slowly and lifting a little. */
public class PhaseCradleRenderer implements BlockEntityRenderer<PhaseCradleBlockEntity> {
    /** Fits the eight-pixel bore on the diagonal while it turns. */
    private static final float ITEM_SIZE = 5.5F;
    /** The bore runs from the mouth at z 2 to the seat at z 10.5 in model pixels; the item hangs mid-way. */
    private static final float BORE_CENTRE_Z = 6.25F;
    private static final float TURN_DEGREES_PER_TICK = 1.5F;
    private static final float LIFT = 0.02F;

    private final ItemRenderer itemRenderer;

    public PhaseCradleRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(PhaseCradleBlockEntity cradle, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack stack = cradle.clientTarget();
        BlockState state = cradle.getBlockState();
        if (stack.isEmpty() || cradle.getLevel() == null || !state.hasProperty(PhaseCradleBlock.FACING)) {
            return;
        }
        Direction facing = state.getValue(PhaseCradleBlock.FACING);
        float time = cradle.getLevel().getGameTime() + partialTick;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees((facing.toYRot() + 180.0F) % 360.0F));
        poseStack.translate(0.0F, LIFT * Mth.sin(time / 12.0F), (BORE_CENTRE_Z - 8.0F) / 16.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * TURN_DEGREES_PER_TICK));
        poseStack.scale(ITEM_SIZE / 16.0F, ITEM_SIZE / 16.0F, ITEM_SIZE / 16.0F);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, bufferSource,
                cradle.getLevel(), (int) cradle.getBlockPos().asLong());
        poseStack.popPose();
    }
}
