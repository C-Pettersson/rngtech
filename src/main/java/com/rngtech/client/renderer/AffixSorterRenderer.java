package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.AffixSorterBlockEntity;
import com.rngtech.content.blockentity.AffixSorterBlockEntity.FaceRoute;
import com.rngtech.content.blockentity.AffixSorterVisualState;

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
 * Draws the working parts on an Affix Sorter's front: the item in the input slot, hanging in the reader bay with a
 * sheet of teal light sweeping up and down over it while it is read; the diverter flap, which swings toward the outlet
 * of the route an item takes and eases back; and that route's lamp, which flashes. Positions are in model pixels and
 * match tools/blockmodels/affix_sorter.py.
 */
public class AffixSorterRenderer implements BlockEntityRenderer<AffixSorterBlockEntity> {
    private static final ResourceLocation BEAM = RNGTech.id("textures/block/resonance_calibrator/fx/beam.png");
    private static final ResourceLocation FLAP = RNGTech.id("block/affix_sorter/flap");
    private static final ResourceLocation LED = RNGTech.id("block/crusher/fx/led");
    // Reader bay between the emitter posts, in front of the reader plate.
    private static final float BAY_X0 = 4.75F;
    private static final float BAY_X1 = 11.25F;
    private static final float BAY_Z0 = 4.6F;
    private static final float READER_Z = 6.9F;
    private static final float ITEM_X = 8.0F;
    private static final float ITEM_Y = 9.5F;
    private static final float ITEM_Z = 5.6F;
    private static final float ITEM_SIZE = 3.75F;
    private static final float BLOCK_ITEM_SIZE = 2.5F;
    private static final float SCAN_Y0 = 7.6F;
    private static final float SCAN_Y1 = 11.4F;
    /** Half a reading, one pass of the scan, kept between a flicker and a crawl. */
    private static final float MIN_SWEEP_TICKS = 10.0F;
    private static final float MAX_SWEEP_TICKS = 60.0F;
    private static final int READ_LIGHT = 12;
    private static final int SCAN_COLOR = 0x7FE6D2;
    private static final int SHEET_COLOR = 0x2F6A62;
    // Diverter flap: its hinge pin, and the plate hanging below it.
    private static final float FLAP_PIVOT_X = 8.0F;
    private static final float FLAP_PIVOT_Y = 6.0F;
    private static final float FLAP_HALF_WIDTH = 0.5F;
    private static final float FLAP_LENGTH = 3.25F;
    private static final float FLAP_Z0 = 0.4F;
    private static final float FLAP_Z1 = 0.8F;
    private static final float FLAP_TEXTURE_SPAN = 4.0F;
    private static final float FLAP_DEGREES = 30.0F;
    /** How long the flap holds toward a route after a sort before it eases back. */
    private static final float FLAP_HOLD_TICKS = 6.0F;
    private static final float FLAP_OUT_RATE = 0.5F;
    private static final float FLAP_BACK_RATE = 0.12F;
    // Route lamps on the deck top, behind each outlet mouth: Pass on the left (east), Reject on the right (west).
    private static final float PASS_LAMP_X = 11.375F;
    private static final float REJECT_LAMP_X = 4.625F;
    private static final float LAMP_HALF_WIDTH = 0.625F;
    private static final float LAMP_Y0 = 6.5F;
    private static final float LAMP_Y1 = 7.1F;
    private static final float LAMP_Z0 = 2.3F;
    private static final float LAMP_Z1 = 3.3F;
    private static final float FLASH_TICKS = 12.0F;
    private static final int PASS = 0x4E8A4A;
    private static final int REJECT = 0xA6473E;
    private static final int PASS_FLASH = 0xA6F09C;
    private static final int REJECT_FLASH = 0xFF9A84;
    private static final float SURFACE = 0.03F;

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
        BlockState blockState = blockEntity.getBlockState();
        boolean working = blockState.getValue(BaseMachineBlock.ACTIVE);
        ItemStack input = blockEntity.displayInput();
        AffixSorterVisualState.Animation animation = blockEntity.animation();
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        float sinceSort = time - blockEntity.flashTime();
        FaceRoute route = blockEntity.flashRoute();
        animate(animation, blockEntity.visualState(), working && !input.isEmpty(), route, sinceSort, time);

        Direction facing = blockState.getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);

        if (!input.isEmpty()) {
            renderItem(input, animation, time, blockEntity, poseStack, bufferSource, packedLight, packedOverlay);
        }
        if (animation.scan > 0.05F) {
            float across = animation.sweep < 1.0F ? animation.sweep : 2.0F - animation.sweep;
            float y = Mth.lerp(across, SCAN_Y1, SCAN_Y0);
            renderScan(bufferSource.getBuffer(RenderType.eyes(BEAM)), poseStack.last(), y, Math.round(255 * animation.scan), packedOverlay);
        }
        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        renderFlap(cutout, poseStack, animation.flap * FLAP_DEGREES, packedLight, packedOverlay);
        float flash = route == FaceRoute.NONE ? 0.0F : Mth.clamp(1.0F - sinceSort / FLASH_TICKS, 0.0F, 1.0F);
        if (flash > 0.0F) {
            renderLampFlash(cutout, poseStack.last(), route == FaceRoute.PASS, flash, packedOverlay);
        }
        poseStack.popPose();
    }

    /**
     * The scan fades in while an item is read and runs at the reading's pace. The flap swings out fast toward the route
     * of a fresh sort, holds there a moment, and eases back to hang straight.
     */
    private static void animate(
            AffixSorterVisualState.Animation animation, AffixSorterVisualState state, boolean reading,
            FaceRoute route, float sinceSort, float time
    ) {
        float elapsed = Double.isNaN(animation.lastTime) ? 0.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        float scanTarget = reading ? 1.0F : 0.0F;
        animation.scan += (scanTarget - animation.scan) * Math.min(1.0F, elapsed * 0.15F);
        float sweepTicks = Mth.clamp(state.cycleTicks() / 2.0F, MIN_SWEEP_TICKS, MAX_SWEEP_TICKS);
        animation.sweep = (animation.sweep + elapsed / sweepTicks) % 2.0F;

        float flapTarget = 0.0F;
        if (route != FaceRoute.NONE && sinceSort < FLAP_HOLD_TICKS) {
            flapTarget = route == FaceRoute.PASS ? 1.0F : -1.0F;
        }
        float rate = flapTarget == 0.0F ? FLAP_BACK_RATE : FLAP_OUT_RATE;
        animation.flap += (flapTarget - animation.flap) * Math.min(1.0F, elapsed * rate);
    }

    /** The input hangs in the bay facing out, lit by the reader and swaying while it is read. */
    private void renderItem(
            ItemStack stack, AffixSorterVisualState.Animation animation, float time, AffixSorterBlockEntity blockEntity,
            PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay
    ) {
        boolean block = stack.getItem() instanceof BlockItem;
        float size = block ? BLOCK_ITEM_SIZE : ITEM_SIZE;
        float sway = Mth.sin(time * 0.1F) * 15.0F * animation.scan;
        int readLight = LightTexture.pack(
                Math.max(LightTexture.block(light), Math.round(READ_LIGHT * animation.scan)), LightTexture.sky(light));
        poseStack.pushPose();
        poseStack.translate(ITEM_X, ITEM_Y, ITEM_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F + sway + (block ? 30.0F : 0.0F)));
        poseStack.scale(size, size, size);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, readLight, overlay, poseStack, bufferSource, blockEntity.getLevel(),
                (int) blockEntity.getBlockPos().asLong());
        poseStack.popPose();
    }

    /** A sheet of light across the bay at height {@code y}, and the bright line where it meets the reader plate. */
    private static void renderScan(VertexConsumer consumer, PoseStack.Pose pose, float y, int alpha, int overlay) {
        int light = LightTexture.FULL_BRIGHT;
        float z = READER_Z - SURFACE;
        FurnaceGlow.quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), SCAN_COLOR, alpha, light, overlay,
                new float[] {BAY_X0, y - 0.3F, z, 0.0F, 1.0F}, new float[] {BAY_X1, y - 0.3F, z, 1.0F, 1.0F},
                new float[] {BAY_X1, y + 0.3F, z, 1.0F, 0.0F}, new float[] {BAY_X0, y + 0.3F, z, 0.0F, 0.0F});
        float[] a = {BAY_X0, y, BAY_Z0, 0.0F, 0.0F};
        float[] b = {BAY_X1, y, BAY_Z0, 1.0F, 0.0F};
        float[] c = {BAY_X1, y, z, 1.0F, 1.0F};
        float[] d = {BAY_X0, y, z, 0.0F, 1.0F};
        for (float side : new float[] {-1.0F, 1.0F}) {
            FurnaceGlow.quad(consumer, pose, new Vector3f(0.0F, side, 0.0F), SHEET_COLOR, alpha, light, overlay, a, b, c, d);
        }
    }

    /** The flap turned about its hinge pin; a positive angle swings its tip toward the Pass outlet on the left (east). */
    private static void renderFlap(VertexConsumer consumer, PoseStack poseStack, float degrees, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(FLAP_PIVOT_X, FLAP_PIVOT_Y, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(degrees));
        FurnaceGlow.cube(consumer, poseStack.last(), FurnaceGlow.sprite(FLAP), FLAP_TEXTURE_SPAN,
                -FLAP_HALF_WIDTH, -FLAP_LENGTH, FLAP_Z0, FLAP_HALF_WIDTH, 0.0F, FLAP_Z1, 0xFFFFFF, 255, light, overlay);
        poseStack.popPose();
    }

    /** The lamp of the route just taken, bright at the sort and fading back to its dim glass. */
    private static void renderLampFlash(VertexConsumer consumer, PoseStack.Pose pose, boolean pass, float flash, int overlay) {
        float x = pass ? PASS_LAMP_X : REJECT_LAMP_X;
        int color = FurnaceGlow.lerpColor(flash, pass ? PASS : REJECT, pass ? PASS_FLASH : REJECT_FLASH);
        TextureAtlasSprite sprite = FurnaceGlow.sprite(LED);
        FurnaceGlow.cube(consumer, pose, sprite, 16.0F, x - LAMP_HALF_WIDTH - SURFACE, LAMP_Y0, LAMP_Z0 - SURFACE,
                x + LAMP_HALF_WIDTH + SURFACE, LAMP_Y1 + SURFACE, LAMP_Z1, color, 255, LightTexture.FULL_BRIGHT, overlay);
    }
}
