package com.rngtech.client.renderer;

import com.rngtech.content.item.ModularToolItem;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.tool.FieldToolAssembly;
import com.rngtech.content.tool.ToolHeadFamily;
import com.rngtech.content.tool.ToolHeadMaterial;
import com.rngtech.content.tool.ToolRodMaterial;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ModularToolItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final float STACK_RENDER_ORIGIN = 0.5F;
    private static final float ROD_DEPTH = 1.0F;
    private static final float HEAD_DEPTH = 1.5F;
    // Whole-pixel shifts generated with the head textures by tools/itemmodels/tool_heads.py.
    private static final PartTransform ROD_TRANSFORM = new PartTransform(-0.0625F, -0.0625F, 1.0F);
    private static final PartTransform PICK_HEAD_TRANSFORM = new PartTransform(0.0625F, -0.0625F, 1.0F);
    private static final PartTransform HAMMER_HEAD_TRANSFORM = new PartTransform(0.1875F, 0.125F, 1.0F);
    private static final PartTransform SHOVEL_HEAD_TRANSFORM = new PartTransform(0.25F, 0.125F, 1.0F);
    private static final PartTransform DIGGER_HEAD_TRANSFORM = new PartTransform(0.25F, 0.125F, 1.0F);
    private static final PartTransform AXE_HEAD_TRANSFORM = new PartTransform(0.0F, 0.1875F, 1.0F);
    private static final PartTransform TREEFELLER_HEAD_TRANSFORM = new PartTransform(0.125F, 0.0625F, 1.0F);

    public ModularToolItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        if (!(stack.getItem() instanceof ModularToolItem tool)) {
            return;
        }

        FieldToolAssembly assembly = ModularToolItem.assembly(stack);
        boolean assembled = assembly.isValidFor(tool.family());
        ItemStack rod = assembled ? assembly.rod() : new ItemStack(ModItems.toolRod(ToolRodMaterial.WOODEN).get());
        ItemStack head = assembled ? assembly.head() : new ItemStack(ModItems.toolHead(tool.family(), ToolHeadMaterial.IRON).get());
        renderStack(rod, poseStack, buffer, packedLight, packedOverlay, ROD_DEPTH, ROD_TRANSFORM);
        renderStack(head, poseStack, buffer, packedLight, packedOverlay, HEAD_DEPTH, headTransform(tool.family()));
    }

    private void renderStack(
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            float depth,
            PartTransform transform
    ) {
        if (stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(STACK_RENDER_ORIGIN + transform.x(), STACK_RENDER_ORIGIN + transform.y(), STACK_RENDER_ORIGIN);
        // A head thicker than the rod encloses the rod's end from both sides instead of z-fighting with it.
        poseStack.scale(transform.scale(), transform.scale(), depth);
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        renderer.renderStatic(stack, ItemDisplayContext.NONE, packedLight, packedOverlay, poseStack, buffer, null, 0);
        poseStack.popPose();
    }

    private PartTransform headTransform(ToolHeadFamily family) {
        return switch (family) {
            case PICK -> PICK_HEAD_TRANSFORM;
            case HAMMER -> HAMMER_HEAD_TRANSFORM;
            case SHOVEL -> SHOVEL_HEAD_TRANSFORM;
            case DIGGER -> DIGGER_HEAD_TRANSFORM;
            case AXE -> AXE_HEAD_TRANSFORM;
            case TREEFELLER -> TREEFELLER_HEAD_TRANSFORM;
        };
    }

    private record PartTransform(float x, float y, float scale) {}
}
