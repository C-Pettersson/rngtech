package com.rngtech.client.renderer;

import com.rngtech.RNGTechClientConfig;
import com.rngtech.content.block.SolarPanelBlock;
import com.rngtech.content.blockentity.SolarPanelBlockEntity;
import com.rngtech.content.energy.SolarTracker;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Turns a Solar Panel's deck about its torque tube. The baked blockstate model is only the mount; this draws the
 * deck model at the tracker angle, which follows the world time, so the server never sends an update as the sun moves.
 * A stowed panel eases back to flat. With animation off in the client config, the baked model carries the deck and
 * this draws nothing.
 */
public class SolarPanelRenderer implements BlockEntityRenderer<SolarPanelBlockEntity> {
    private static final float PIVOT_Y = SolarTracker.PIVOT_Y / 16.0F;

    public SolarPanelRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            SolarPanelBlockEntity panel,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        Level level = panel.getLevel();
        if (level == null || !RNGTechClientConfig.animatedSolarPanels()) {
            return;
        }
        BlockState state = panel.getBlockState();
        if (!(state.getBlock() instanceof SolarPanelBlock)) {
            return;
        }
        float target = state.getValue(SolarPanelBlock.TRACKER) == SolarTracker.STOWED
                ? 0.0F
                : SolarTracker.tilt(level.getDayTime() + (double) partialTick);
        float angle = panel.easeTrackAngle(target, level.getGameTime() + partialTick);

        poseStack.pushPose();
        poseStack.translate(0.5F, PIVOT_Y, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
        poseStack.translate(-0.5F, -PIVOT_Y, -0.5F);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                poseStack.last(),
                bufferSource.getBuffer(RenderType.solid()),
                state,
                SolarPanelModels.deckModel(panel.material()),
                1.0F,
                1.0F,
                1.0F,
                packedLight,
                packedOverlay,
                ModelData.EMPTY,
                RenderType.solid()
        );
        poseStack.popPose();
    }

    /** The deck is the panel's most visible part, so it draws as far as the world does. */
    @Override
    public int getViewDistance() {
        return Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
    }
}
