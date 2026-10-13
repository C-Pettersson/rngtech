package com.rngtech.client.renderer;

import com.rngtech.content.blockentity.GasChemistryBlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** The Coal Gasifier, Syngas Combustor, and Steam Methane Reformer share one block entity type; each has its own renderer. */
public class GasChemistryRenderer implements BlockEntityRenderer<GasChemistryBlockEntity> {
    private final CoalGasifierRenderer gasifier;
    private final SyngasCombustorRenderer combustor;
    private final SteamMethaneReformerRenderer reformer;

    public GasChemistryRenderer(BlockEntityRendererProvider.Context context) {
        gasifier = new CoalGasifierRenderer(context);
        combustor = new SyngasCombustorRenderer(context);
        reformer = new SteamMethaneReformerRenderer(context);
    }

    @Override
    public void render(
            GasChemistryBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BlockEntityRenderer<GasChemistryBlockEntity> renderer = switch (blockEntity.machine()) {
            case COAL_GASIFIER -> gasifier;
            case SYNGAS_COMBUSTOR -> combustor;
            case STEAM_METHANE_REFORMER -> reformer;
        };
        renderer.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
