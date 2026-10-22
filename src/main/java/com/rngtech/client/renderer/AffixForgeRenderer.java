package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.AffixForgeBlock;
import com.rngtech.content.blockentity.AffixForgeBlockEntity;
import com.rngtech.content.blockentity.AffixForgeVisualState;
import com.rngtech.content.item.RefinementLensItem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

/**
 * Draws what an Affix Forge holds: the target standing on the anvil plate, the catalyst in the dish, and the focus,
 * a lens clipped to the Lens Array's loupe or a crystal in the Modifier Socket, or lying on the bare mount when that
 * upgrade is missing. An installed Resonance Matrix glows faintly. A successful Reforge flares the lapis ring, beams
 * from the loupe onto the target, flares the matrix, and turns the target once. Positions are in model pixels and
 * match tools/blockmodels/affix_forge.py.
 */
public class AffixForgeRenderer implements BlockEntityRenderer<AffixForgeBlockEntity> {
    private static final ModelResourceLocation MATRIX_GLOW = new ModelResourceLocation(
            RNGTech.id("block/affix_forge/matrix_glow"), ModelResourceLocation.STANDALONE_VARIANT);
    private static final ResourceLocation RING = RNGTech.id("textures/block/affix_forge/fx/ring.png");
    private static final ResourceLocation BEAM = RNGTech.id("textures/block/resonance_calibrator/fx/beam.png");
    // Target standing on the cup at the plate's centre.
    private static final float TARGET_X = 8.0F;
    private static final float TARGET_Z = 7.5F;
    private static final float TARGET_FLOOR = 7.8F;
    private static final float TARGET_SIZE = 4.0F;
    // Lapis ring inlaid in the plate: the flare quad spans the ring texture, whose ring has a radius of 30 of 32 texels.
    private static final float PLATE_TOP = 7.5F;
    private static final float RING_HALF = 3.75F * 32.0F / 30.0F;
    // Catalyst lying in the dish.
    private static final float DISH_X = 8.0F;
    private static final float DISH_Y = 2.78F;
    private static final float DISH_Z = 1.75F;
    private static final float DISH_ITEM = 2.6F;
    // Lens Array: the glass under the loupe, the clip on its front, and the bare mount on the back wall.
    private static final float GLASS_Y = 13.2F;
    private static final float CLIP_Y = 13.6F;
    private static final float CLIP_Z = 5.0F;
    private static final float LENS_ITEM = 2.0F;
    private static final float WALL_MOUNT_Y = 13.02F;
    private static final float WALL_MOUNT_Z = 14.0F;
    // Modifier Socket stand: the crystal turns over the cup, or lies on the bare mount.
    private static final float SOCKET_X = 2.0F;
    private static final float SOCKET_Z = 8.5F;
    private static final float SOCKET_CRYSTAL_Y = 10.85F;
    private static final float SOCKET_MOUNT_Y = 9.02F;
    private static final float CRYSTAL_ITEM = 2.2F;
    private static final float SURFACE = 0.02F;
    private static final int LAPIS = 0x3050C8;
    private static final int FLARE = 0xE0ECFF;
    private static final int MATRIX = 0x8A48D0;
    private static final int MATRIX_FLARE = 0xF0DCFF;

    private final ModelBlockRenderer modelRenderer;
    private final ItemRenderer itemRenderer;

    public AffixForgeRenderer(BlockEntityRendererProvider.Context context) {
        modelRenderer = context.getBlockRenderDispatcher().getModelRenderer();
        itemRenderer = context.getItemRenderer();
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(MATRIX_GLOW);
    }

    @Override
    public void render(
            AffixForgeBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }
        BlockState blockState = blockEntity.getBlockState();
        boolean lensArray = blockState.getValue(AffixForgeBlock.HAS_LENS_ARRAY);
        boolean socket = blockState.getValue(AffixForgeBlock.HAS_MODIFIER_SOCKET);
        boolean matrix = blockState.getValue(AffixForgeBlock.HAS_RESONANCE_MATRIX);
        AffixForgeVisualState state = blockEntity.visualState();
        AffixForgeVisualState.Animation animation = blockEntity.animation();
        float time = level.getGameTime() + partialTick;
        animate(animation, state, time);

        Direction facing = blockState.getValue(AffixForgeBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        renderItems(blockEntity, poseStack, bufferSource, state, animation, lensArray, socket, time, packedLight, packedOverlay);

        if (matrix) {
            float glow = Math.max(0.3F + 0.12F * Mth.sin(time * 0.08F), animation.flash);
            int color = FurnaceGlow.scale(FurnaceGlow.lerpColor(animation.flash, MATRIX, MATRIX_FLARE), glow);
            BakedModel model = Minecraft.getInstance().getModelManager().getModel(MATRIX_GLOW);
            modelRenderer.renderModel(poseStack.last(), bufferSource.getBuffer(RenderType.eyes(InventoryMenu.BLOCK_ATLAS)), null, model,
                    (color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F,
                    LightTexture.FULL_BRIGHT, packedOverlay, ModelData.EMPTY, RenderType.cutout());
        }

        if (animation.flash > 0.01F) {
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            PoseStack.Pose pose = poseStack.last();
            int color = FurnaceGlow.scale(FurnaceGlow.lerpColor(animation.flash, LAPIS, FLARE), animation.flash);
            renderRing(bufferSource.getBuffer(RenderType.eyes(RING)), pose, color, packedOverlay);
            if (lensArray && state.target() != Items.AIR) {
                renderBeam(bufferSource.getBuffer(RenderType.eyes(BEAM)), pose, color, packedOverlay);
            }
        }
        poseStack.popPose();
    }

    /** A successful Reforge sets off the flash, which fades over about two seconds, and turns the target once. */
    private static void animate(AffixForgeVisualState.Animation animation, AffixForgeVisualState state, float time) {
        float elapsed = Double.isNaN(animation.lastTime) ? 0.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        if (animation.reforges != state.reforges()) {
            if (animation.reforges >= 0) {
                animation.flash = 1.0F;
                animation.turn = 360.0F;
            }
            animation.reforges = state.reforges();
        }
        animation.flash *= (float) Math.pow(0.93, elapsed);
        animation.turn = Math.max(0.0F, animation.turn - elapsed * 18.0F);
    }

    private void renderItems(
            AffixForgeBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, AffixForgeVisualState state,
            AffixForgeVisualState.Animation animation, boolean lensArray, boolean socket, float time, int light, int overlay
    ) {
        Level level = blockEntity.getLevel();
        int seed = (int) blockEntity.getBlockPos().asLong();
        ItemStack target = stack(state.target());
        if (!target.isEmpty()) {
            int flareLight = LightTexture.pack(Math.max(LightTexture.block(light), Math.round(15.0F * animation.flash)), LightTexture.sky(light));
            poseStack.pushPose();
            poseStack.translate(TARGET_X / 16.0F, (TARGET_FLOOR + TARGET_SIZE / 2.0F) / 16.0F, TARGET_Z / 16.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(animation.turn));
            poseStack.scale(TARGET_SIZE / 16.0F, TARGET_SIZE / 16.0F, TARGET_SIZE / 16.0F);
            itemRenderer.renderStatic(target, ItemDisplayContext.FIXED, flareLight, overlay, poseStack, bufferSource, level, seed);
            poseStack.popPose();
        }
        ItemStack catalyst = stack(state.catalyst());
        if (!catalyst.isEmpty()) {
            renderFlat(catalyst, DISH_X, DISH_Y, DISH_Z, DISH_ITEM, poseStack, bufferSource, level, light, overlay, seed + 1);
        }
        ItemStack focus = stack(state.focus());
        if (focus.isEmpty()) {
            return;
        }
        if (focus.getItem() instanceof RefinementLensItem) {
            if (lensArray) {
                poseStack.pushPose();
                poseStack.translate(TARGET_X / 16.0F, CLIP_Y / 16.0F, CLIP_Z / 16.0F);
                poseStack.scale(LENS_ITEM / 16.0F, LENS_ITEM / 16.0F, LENS_ITEM / 16.0F);
                itemRenderer.renderStatic(focus, ItemDisplayContext.FIXED, light, overlay, poseStack, bufferSource, level, seed + 2);
                poseStack.popPose();
            } else {
                renderFlat(focus, TARGET_X, WALL_MOUNT_Y, WALL_MOUNT_Z, LENS_ITEM, poseStack, bufferSource, level, light, overlay, seed + 2);
            }
        } else if (socket) {
            poseStack.pushPose();
            poseStack.translate(SOCKET_X / 16.0F, (SOCKET_CRYSTAL_Y + 0.15F * Mth.sin(time * 0.1F)) / 16.0F, SOCKET_Z / 16.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.5F % 360.0F));
            poseStack.scale(CRYSTAL_ITEM / 16.0F, CRYSTAL_ITEM / 16.0F, CRYSTAL_ITEM / 16.0F);
            itemRenderer.renderStatic(focus, ItemDisplayContext.FIXED, light, overlay, poseStack, bufferSource, level, seed + 2);
            poseStack.popPose();
        } else {
            renderFlat(focus, SOCKET_X, SOCKET_MOUNT_Y, SOCKET_Z, CRYSTAL_ITEM, poseStack, bufferSource, level, light, overlay, seed + 2);
        }
    }

    /** One item lying face up at (x, y, z). */
    private void renderFlat(
            ItemStack stack, float x, float y, float z, float size, PoseStack poseStack, MultiBufferSource bufferSource,
            Level level, int light, int overlay, int seed
    ) {
        poseStack.pushPose();
        poseStack.translate(x / 16.0F, y / 16.0F, z / 16.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(size / 16.0F, size / 16.0F, size / 16.0F);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, poseStack, bufferSource, level, seed);
        poseStack.popPose();
    }

    private static ItemStack stack(Item item) {
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** The lapis ring flare, a quad over the inlay drawn additively. */
    private static void renderRing(VertexConsumer consumer, PoseStack.Pose pose, int color, int overlay) {
        float y = PLATE_TOP + SURFACE;
        float x0 = TARGET_X - RING_HALF;
        float x1 = TARGET_X + RING_HALF;
        float z0 = TARGET_Z - RING_HALF;
        float z1 = TARGET_Z + RING_HALF;
        FurnaceGlow.quad(consumer, pose, new Vector3f(0.0F, 1.0F, 0.0F), color, 255, LightTexture.FULL_BRIGHT, overlay,
                new float[] {x0, y, z0, 0.0F, 0.0F}, new float[] {x1, y, z0, 1.0F, 0.0F},
                new float[] {x1, y, z1, 1.0F, 1.0F}, new float[] {x0, y, z1, 0.0F, 1.0F});
    }

    /** A beam from the loupe's glass straight down onto the target: two crossed quads, bright along the middle. */
    private static void renderBeam(VertexConsumer consumer, PoseStack.Pose pose, int color, int overlay) {
        float half = 0.9F;
        float top = GLASS_Y - SURFACE;
        float bottom = TARGET_FLOOR;
        int light = LightTexture.FULL_BRIGHT;
        for (Vector3f normal : new Vector3f[] {new Vector3f(0.0F, 0.0F, -1.0F), new Vector3f(0.0F, 0.0F, 1.0F)}) {
            FurnaceGlow.quad(consumer, pose, normal, color, 255, light, overlay,
                    new float[] {TARGET_X - half, top, TARGET_Z, 0.0F, 0.0F}, new float[] {TARGET_X + half, top, TARGET_Z, 1.0F, 0.0F},
                    new float[] {TARGET_X + half, bottom, TARGET_Z, 1.0F, 1.0F}, new float[] {TARGET_X - half, bottom, TARGET_Z, 0.0F, 1.0F});
        }
        for (Vector3f normal : new Vector3f[] {new Vector3f(-1.0F, 0.0F, 0.0F), new Vector3f(1.0F, 0.0F, 0.0F)}) {
            FurnaceGlow.quad(consumer, pose, normal, color, 255, light, overlay,
                    new float[] {TARGET_X, top, TARGET_Z - half, 0.0F, 0.0F}, new float[] {TARGET_X, top, TARGET_Z + half, 1.0F, 0.0F},
                    new float[] {TARGET_X, bottom, TARGET_Z + half, 1.0F, 1.0F}, new float[] {TARGET_X, bottom, TARGET_Z - half, 0.0F, 1.0F});
        }
    }
}
