package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.AmmoniaFuelCellBlockEntity;
import com.rngtech.content.blockentity.AmmoniaFuelCellVisualState;
import com.rngtech.content.registry.ModFluids;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Vector3f;

/**
 * Draws the state-driven parts of the Ammonia Fuel Cell: the Fuel Cell Membranes between the bipolar plates when one is
 * installed, the Ammonia level in the tank, and the Corrosion Residue collected in the trap. Positions are in model
 * pixels and match tools/blockmodels/ammonia_fuel_cell.py.
 */
public class AmmoniaFuelCellRenderer implements BlockEntityRenderer<AmmoniaFuelCellBlockEntity> {
    private static final ResourceLocation MEMBRANE = RNGTech.id("block/ammonia_fuel_cell/membrane");
    private static final ResourceLocation RESIDUE = RNGTech.id("block/ammonia_fuel_cell/fx/residue");
    private static final float STACK_X0 = 3.75F;
    private static final float GAP = 0.25F;
    private static final float PLATE = 0.5F;
    private static final int CELLS = 12;
    private static final float MEMBRANE_Y0 = 3.5F;
    private static final float MEMBRANE_Y1 = 11.875F;
    private static final float MEMBRANE_Z0 = 1.625F;
    private static final float MEMBRANE_Z1 = 9.375F;
    private static final float TANK_X0 = 0.95F;
    private static final float TANK_X1 = 3.3F;
    private static final float TANK_Y0 = 3.01F;
    private static final float TANK_Y1 = 12.4F;
    private static final float TANK_Z0 = 11.7F;
    private static final float TANK_Z1 = 15.3F;
    private static final float TRAP_X0 = 12.7F;
    private static final float TRAP_X1 = 15.05F;
    private static final float TRAP_Y0 = 2.76F;
    private static final float TRAP_Y1 = 8.15F;
    private static final float TRAP_Z0 = 11.7F;
    private static final float TRAP_Z1 = 15.3F;

    public AmmoniaFuelCellRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            AmmoniaFuelCellBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        AmmoniaFuelCellVisualState state = blockEntity.visualState();
        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        PoseStack.Pose pose = poseStack.last();

        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        if (state.membrane()) {
            renderMembranes(cutout, pose, packedLight, packedOverlay);
        }
        renderResidue(cutout, pose, state.residue(), packedLight, packedOverlay);
        renderAmmonia(bufferSource.getBuffer(Sheets.translucentCullBlockSheet()), pose, state.ammonia(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** One membrane in each gap between the plates, its edge standing a little proud like a gasket. */
    private static void renderMembranes(VertexConsumer consumer, PoseStack.Pose pose, int light, int overlay) {
        TextureAtlasSprite sprite = sprite(MEMBRANE);
        float u0 = sprite.getU0();
        float u1 = sprite.getU(1.0F / 16.0F);
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        Vector3f north = new Vector3f(0.0F, 0.0F, -1.0F);
        Vector3f south = new Vector3f(0.0F, 0.0F, 1.0F);
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        for (int cell = 0; cell < CELLS; cell++) {
            float x0 = STACK_X0 + cell * (GAP + PLATE);
            float x1 = x0 + GAP;
            quad(consumer, pose, north, 0xFFFFFF, 255, light, overlay,
                    new float[] {x0, MEMBRANE_Y1, MEMBRANE_Z0, u0, v0}, new float[] {x1, MEMBRANE_Y1, MEMBRANE_Z0, u1, v0},
                    new float[] {x1, MEMBRANE_Y0, MEMBRANE_Z0, u1, v1}, new float[] {x0, MEMBRANE_Y0, MEMBRANE_Z0, u0, v1});
            quad(consumer, pose, south, 0xFFFFFF, 255, light, overlay,
                    new float[] {x0, MEMBRANE_Y1, MEMBRANE_Z1, u0, v0}, new float[] {x1, MEMBRANE_Y1, MEMBRANE_Z1, u1, v0},
                    new float[] {x1, MEMBRANE_Y0, MEMBRANE_Z1, u1, v1}, new float[] {x0, MEMBRANE_Y0, MEMBRANE_Z1, u0, v1});
            quad(consumer, pose, up, 0xFFFFFF, 255, light, overlay,
                    new float[] {x0, MEMBRANE_Y1, MEMBRANE_Z0, u0, v0}, new float[] {x1, MEMBRANE_Y1, MEMBRANE_Z0, u1, v0},
                    new float[] {x1, MEMBRANE_Y1, MEMBRANE_Z1, u1, v1}, new float[] {x0, MEMBRANE_Y1, MEMBRANE_Z1, u0, v1});
        }
    }

    private static void renderAmmonia(VertexConsumer consumer, PoseStack.Pose pose, float fill, int light, int overlay) {
        if (fill <= 0.001F) {
            return;
        }
        IClientFluidTypeExtensions fluidType = IClientFluidTypeExtensions.of(ModFluids.AMMONIA_SOURCE.get());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidType.getStillTexture());
        float y1 = TANK_Y0 + (TANK_Y1 - TANK_Y0) * Mth.clamp(fill, 0.0F, 1.0F);
        cube(consumer, pose, sprite, 16.0F, TANK_X0, TANK_Y0, TANK_Z0, TANK_X1, y1, TANK_Z1, fluidType.getTintColor() & 0xFFFFFF, 220, light, overlay);
    }

    /** Corrosion Residue from each cycle settles in the trap, rising with the residue slot. */
    private static void renderResidue(VertexConsumer consumer, PoseStack.Pose pose, float residue, int light, int overlay) {
        if (residue <= 0.001F) {
            return;
        }
        float y1 = TRAP_Y0 + (TRAP_Y1 - TRAP_Y0) * Mth.clamp(residue, 0.0F, 1.0F);
        cube(consumer, pose, sprite(RESIDUE), 16.0F, TRAP_X0, TRAP_Y0, TRAP_Z0, TRAP_X1, y1, TRAP_Z1, 0xFFFFFF, 255, light, overlay);
    }

    /** A box whose faces show {@code span} model pixels per full sprite, cropped to the face size. */
    private static void cube(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, float span,
            float x0, float y0, float z0, float x1, float y1, float z1,
            int color, int alpha, int light, int overlay
    ) {
        float u0 = sprite.getU0();
        float v0 = sprite.getV0();
        float uw = sprite.getU1() - u0;
        float vh = sprite.getV1() - v0;
        float du = Math.min(1.0F, (x1 - x0) / span);
        float dz = Math.min(1.0F, (z1 - z0) / span);
        float dy = Math.min(1.0F, (y1 - y0) / span);
        float ux = u0 + uw * du;
        float uz = u0 + uw * dz;
        float vy = v0 + vh * dy;
        float vz = v0 + vh * dz;
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), color, alpha, light, overlay,
                new float[] {x0, y0, z0, u0, vy}, new float[] {x1, y0, z0, ux, vy}, new float[] {x1, y1, z0, ux, v0}, new float[] {x0, y1, z0, u0, v0});
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, 1.0F), color, alpha, light, overlay,
                new float[] {x0, y0, z1, u0, vy}, new float[] {x1, y0, z1, ux, vy}, new float[] {x1, y1, z1, ux, v0}, new float[] {x0, y1, z1, u0, v0});
        quad(consumer, pose, new Vector3f(-1.0F, 0.0F, 0.0F), color, alpha, light, overlay,
                new float[] {x0, y0, z0, u0, vy}, new float[] {x0, y0, z1, uz, vy}, new float[] {x0, y1, z1, uz, v0}, new float[] {x0, y1, z0, u0, v0});
        quad(consumer, pose, new Vector3f(1.0F, 0.0F, 0.0F), color, alpha, light, overlay,
                new float[] {x1, y0, z0, u0, vy}, new float[] {x1, y0, z1, uz, vy}, new float[] {x1, y1, z1, uz, v0}, new float[] {x1, y1, z0, u0, v0});
        quad(consumer, pose, new Vector3f(0.0F, 1.0F, 0.0F), color, alpha, light, overlay,
                new float[] {x0, y1, z0, u0, v0}, new float[] {x1, y1, z0, ux, v0}, new float[] {x1, y1, z1, ux, vz}, new float[] {x0, y1, z1, u0, vz});
        quad(consumer, pose, new Vector3f(0.0F, -1.0F, 0.0F), color, alpha, light, overlay,
                new float[] {x0, y0, z0, u0, v0}, new float[] {x1, y0, z0, ux, v0}, new float[] {x1, y0, z1, ux, vz}, new float[] {x0, y0, z1, u0, vz});
    }

    /**
     * One quad from four {x, y, z, u, v} corners in perimeter order. The winding is flipped when needed so the quad
     * faces along {@code normal} and survives back-face culling.
     */
    private static void quad(
            VertexConsumer consumer, PoseStack.Pose pose, Vector3f normal,
            int color, int alpha, int light, int overlay,
            float[] a, float[] b, float[] c, float[] d
    ) {
        Vector3f edge1 = new Vector3f(b[0] - a[0], b[1] - a[1], b[2] - a[2]);
        Vector3f edge2 = new Vector3f(c[0] - a[0], c[1] - a[1], c[2] - a[2]);
        boolean flip = edge1.cross(edge2).dot(normal) < 0.0F;
        float[][] corners = flip ? new float[][] {a, d, c, b} : new float[][] {a, b, c, d};
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        for (float[] corner : corners) {
            consumer.addVertex(pose, corner[0], corner[1], corner[2])
                    .setColor(red, green, blue, alpha)
                    .setUv(corner[3], corner[4])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose, normal.x(), normal.y(), normal.z());
        }
    }

    private static TextureAtlasSprite sprite(ResourceLocation id) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(id);
    }
}
