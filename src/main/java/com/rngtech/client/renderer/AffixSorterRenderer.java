package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.AffixSorterBlockEntity;
import com.rngtech.content.blockentity.AffixSorterBlockEntity.FaceRoute;
import com.rngtech.content.blockentity.AffixSorterBlockEntity.RelativeFace;
import com.rngtech.content.blockentity.AffixSorterVisualState;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
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

/**
 * Draws an Affix Sorter's route markings and the item it reads. Each face's port collar is enamelled in its route,
 * green for Pass and red for Reject; input faces keep the bare trim collar of the block model. When an item is sorted,
 * the collars of its route flash bright and fade. The item in the input slot lies on the reader in the roof port and
 * turns slowly while the sorter works. Positions are in model pixels and match tools/blockmodels/affix_sorter.py.
 */
public class AffixSorterRenderer implements BlockEntityRenderer<AffixSorterBlockEntity> {
    private static final ResourceLocation ENAMEL = RNGTech.id("block/affix_sorter/enamel");
    private static final int PASS = 0x4E8A4A;
    private static final int REJECT = 0xA6473E;
    private static final int PASS_FLASH = 0xA6F09C;
    private static final int REJECT_FLASH = 0xFF9A84;
    private static final float FLASH_TICKS = 10.0F;
    // Collar rings as {u0, v0, u1, v1} across a face and {d0, d1} inward from its outer plane.
    private static final float[] SIDE_RING = {3.5F, 3.25F, 12.5F, 12.25F};
    private static final float[] SIDE_DEPTH = {0.5F, 1.5F};
    private static final float[] TOP_RING = {3.5F, 3.5F, 12.5F, 12.5F};
    private static final float[] TOP_DEPTH = {1.0F, 2.5F};
    private static final float[] BOTTOM_DEPTH = {-0.1F, 0.0F};
    private static final float RING_WIDTH = 2.0F;
    private static final float SURFACE = 0.02F;
    // The reader floor in the roof port, where the input lies.
    private static final float READER_Y = 13.6F;
    private static final float INPUT_SIZE = 4.0F;
    private static final float TURN_DEGREES_PER_TICK = 2.0F;
    private static final int READER_LIGHT = 13;

    private final ItemRenderer itemRenderer;

    public AffixSorterRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(
            AffixSorterBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        AffixSorterVisualState state = blockEntity.visualState();
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        boolean active = blockEntity.getBlockState().getValue(BaseMachineBlock.ACTIVE);
        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        ItemStack input = blockEntity.displayInput();
        if (!input.isEmpty()) {
            renderInput(blockEntity, input, active, time, poseStack, bufferSource, packedLight, packedOverlay);
        }

        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        TextureAtlasSprite sprite = FurnaceGlow.sprite(ENAMEL);
        float flash = Mth.clamp(1.0F - (time - blockEntity.flashTime()) / FLASH_TICKS, 0.0F, 1.0F);
        for (RelativeFace face : RelativeFace.values()) {
            FaceRoute route = state.route(face);
            if (route == FaceRoute.NONE || !state.connected(face)) {
                continue;
            }
            boolean pass = route == FaceRoute.PASS;
            int color = pass ? PASS : REJECT;
            int light = packedLight;
            if (flash > 0.0F && route == blockEntity.flashRoute()) {
                color = FurnaceGlow.lerpColor(flash, color, pass ? PASS_FLASH : REJECT_FLASH);
                light = LightTexture.pack(Math.max(LightTexture.block(packedLight), Math.round(15.0F * flash)), LightTexture.sky(packedLight));
            }
            renderCollar(cutout, pose, sprite, face, color, light, packedOverlay);
        }
        poseStack.popPose();
    }

    /** The four bars of a face's collar ring, each wrapped in enamel just over the model's trim bar. */
    private static void renderCollar(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, RelativeFace face, int color, int light, int overlay
    ) {
        float[] ring = switch (face) {
            case TOP, BOTTOM -> TOP_RING;
            default -> SIDE_RING;
        };
        float[] depth = switch (face) {
            case TOP -> TOP_DEPTH;
            case BOTTOM -> BOTTOM_DEPTH;
            default -> SIDE_DEPTH;
        };
        float u0 = ring[0] - SURFACE;
        float v0 = ring[1] - SURFACE;
        float u1 = ring[2] + SURFACE;
        float v1 = ring[3] + SURFACE;
        float w = RING_WIDTH + SURFACE * 2.0F;
        float[][] bars = {
                {u0, v0, u1, v0 + w},
                {u0, v1 - w, u1, v1},
                {u0, v0 + w, u0 + w, v1 - w},
                {u1 - w, v0 + w, u1, v1 - w},
        };
        for (float[] bar : bars) {
            float[] box = oriented(face, bar[0], bar[1], bar[2], bar[3], depth[0] - SURFACE, depth[1] + SURFACE);
            FurnaceGlow.cube(consumer, pose, sprite, 16.0F, box[0], box[1], box[2], box[3], box[4], box[5], color, 255, light, overlay);
        }
    }

    /**
     * A box across a face (u, v) and inward from its outer plane (d), as {x0, y0, z0, x1, y1, z1} in the north-facing
     * model, where the left face is east and the right face west.
     */
    private static float[] oriented(RelativeFace face, float u0, float v0, float u1, float v1, float d0, float d1) {
        return switch (face) {
            case FRONT -> new float[] {u0, v0, d0, u1, v1, d1};
            case BACK -> new float[] {u0, v0, 16.0F - d1, u1, v1, 16.0F - d0};
            case LEFT -> new float[] {16.0F - d1, v0, u0, 16.0F - d0, v1, u1};
            case RIGHT -> new float[] {d0, v0, u0, d1, v1, u1};
            case TOP -> new float[] {u0, 16.0F - d1, v0, u1, 16.0F - d0, v1};
            case BOTTOM -> new float[] {u0, d0, v0, u1, d1, v1};
        };
    }

    /** The input lies on the reader, lit by it and turning while the sorter works; a block item sits as a small cube. */
    private void renderInput(
            AffixSorterBlockEntity blockEntity, ItemStack input, boolean active, float time,
            PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay
    ) {
        boolean block = input.getItem() instanceof BlockItem;
        float yaw = active ? time * TURN_DEGREES_PER_TICK % 360.0F : 25.0F;
        int readerLight = active
                ? LightTexture.pack(Math.max(LightTexture.block(light), READER_LIGHT), LightTexture.sky(light))
                : light;
        poseStack.pushPose();
        poseStack.translate(0.5F, (READER_Y + (block ? INPUT_SIZE / 4.0F : 0.2F)) / 16.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        if (!block) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        }
        poseStack.scale(INPUT_SIZE / 16.0F, INPUT_SIZE / 16.0F, INPUT_SIZE / 16.0F);
        itemRenderer.renderStatic(input, ItemDisplayContext.FIXED, readerLight, overlay, poseStack, bufferSource,
                blockEntity.getLevel(), (int) blockEntity.getBlockPos().asLong());
        poseStack.popPose();
    }
}
