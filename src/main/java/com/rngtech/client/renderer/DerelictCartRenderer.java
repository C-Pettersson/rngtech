package com.rngtech.client.renderer;

import com.rngtech.content.dormant.DormantPhase;
import com.rngtech.content.entity.DerelictCartEntity;
import com.rngtech.content.registry.ModItems;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/** Draws a derelict Forestry Companion: the cart model with its dark no-power face, sagging a little to one side. */
public class DerelictCartRenderer extends EntityRenderer<DerelictCartEntity> {
    /** The Forestry Companion item model's no-power face. */
    private static final int NO_POWER_CUSTOM_MODEL_DATA = 11;
    private static final float SAG_DEGREES = 4.0F;

    private final ItemRenderer itemRenderer;
    private final ItemStack cart;

    public DerelictCartRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.cart = new ItemStack(ModItems.FORESTRY_CART.get());
        cart.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(NO_POWER_CUSTOM_MODEL_DATA));
    }

    @Override
    public void render(DerelictCartEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.37F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.phase() == DormantPhase.SPENT ? SAG_DEGREES * 2 : SAG_DEGREES));
        poseStack.scale(1.0F, 0.9F, 1.0F);
        itemRenderer.renderStatic(cart, ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(DerelictCartEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
