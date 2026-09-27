package com.rngtech.client.renderer;

import com.rngtech.content.item.ModularToolItem;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.tool.FieldToolAssembly;
import com.rngtech.content.tool.ToolHeadFamily;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ModularToolItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final float STACK_RENDER_ORIGIN = 0.5F;
    private static final float HEAD_Z_OFFSET = 0.015F;
    private static final PartTransform ROD_TRANSFORM = new PartTransform(0.0F, 0.0F, 1.0F);
    private static final PartTransform PICK_HEAD_TRANSFORM = new PartTransform(0.125F, 0.063F, 0.875F);
    private static final PartTransform HAMMER_HEAD_TRANSFORM = new PartTransform(0.125F, 0.063F, 0.9F);
    private static final PartTransform SHOVEL_HEAD_TRANSFORM = new PartTransform(0.125F, 0.063F, 0.875F);
    private static final PartTransform DIGGER_HEAD_TRANSFORM = new PartTransform(0.125F, 0.063F, 0.9F);
    private static final PartTransform AXE_HEAD_TRANSFORM = new PartTransform(0.125F, 0.063F, 0.9F);
    private static final PartTransform TREEFELLER_HEAD_TRANSFORM = new PartTransform(0.094F, 0.063F, 0.9F);

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
        if (!assembly.isValidFor(tool.family())) {
            renderStack(fallbackStack(tool.family()), poseStack, buffer, packedLight, packedOverlay, 0.0F, ROD_TRANSFORM);
            return;
        }

        renderStack(assembly.rod(), poseStack, buffer, packedLight, packedOverlay, 0.0F, ROD_TRANSFORM);
        renderStack(assembly.head(), poseStack, buffer, packedLight, packedOverlay, HEAD_Z_OFFSET, headTransform(tool.family()));
    }

    private void renderStack(
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            float zOffset,
            PartTransform transform
    ) {
        if (stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(STACK_RENDER_ORIGIN, STACK_RENDER_ORIGIN, STACK_RENDER_ORIGIN + zOffset);
        poseStack.translate(transform.x(), transform.y(), 0.0F);
        poseStack.scale(transform.scale(), transform.scale(), 1.0F);
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        renderer.renderStatic(stack, ItemDisplayContext.NONE, packedLight, packedOverlay, poseStack, buffer, null, 0);
        poseStack.popPose();
    }

    private ItemStack fallbackStack(ToolHeadFamily family) {
        return switch (family) {
            case PICK -> new ItemStack(Items.IRON_PICKAXE);
            case HAMMER -> new ItemStack(ModItems.FORMING_HAMMER.get());
            case SHOVEL, DIGGER -> new ItemStack(Items.IRON_SHOVEL);
            case AXE, TREEFELLER -> new ItemStack(Items.IRON_AXE);
        };
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
