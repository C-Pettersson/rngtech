package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Draws a warning sign, an exclamation mark in a triangle, on the front of a machine whose block state is
 * {@link BaseMachineBlock#BLOCKED}, then lets the machine's own renderer, if it has one, draw the rest. The sign sits
 * just in front of the frontmost part of the model it covers, so it stays visible on shaped models; a machine too low
 * for it, such as a Solar Panel, shows it flat on its top instead.
 */
public class MachineStatusRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private static final ResourceLocation SIGN = RNGTech.id("block/status/blocked");
    /** Side of the square sign, in model pixels. */
    private static final float SIZE = 8.0F;
    private static final float MARGIN = 1.0F;
    private static final float LIFT = 0.15F;
    private static final Map<BakedModel, Placement> PLACEMENTS = new WeakHashMap<>();

    private final BlockEntityRenderer<T> machine;

    private MachineStatusRenderer(BlockEntityRenderer<T> machine) {
        this.machine = machine;
    }

    /** The sign on its own, for a machine without a renderer. */
    public static <T extends BlockEntity> BlockEntityRendererProvider<T> sign() {
        return context -> new MachineStatusRenderer<>(null);
    }

    /** The sign over the machine's own renderer. */
    public static <T extends BlockEntity> BlockEntityRendererProvider<T> around(BlockEntityRendererProvider<T> machine) {
        return context -> new MachineStatusRenderer<>(machine.create(context));
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (machine != null) {
            machine.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }
        BlockState state = blockEntity.getBlockState();
        if (state.hasProperty(BaseMachineBlock.BLOCKED) && state.getValue(BaseMachineBlock.BLOCKED)) {
            renderSign(state, poseStack, bufferSource, packedOverlay);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(T blockEntity) {
        return machine != null && machine.shouldRenderOffScreen(blockEntity);
    }

    @Override
    public int getViewDistance() {
        return machine != null ? machine.getViewDistance() : BlockEntityRenderer.super.getViewDistance();
    }

    @Override
    public boolean shouldRender(T blockEntity, Vec3 cameraPos) {
        return machine != null ? machine.shouldRender(blockEntity, cameraPos) : BlockEntityRenderer.super.shouldRender(blockEntity, cameraPos);
    }

    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        return machine != null ? machine.getRenderBoundingBox(blockEntity) : BlockEntityRenderer.super.getRenderBoundingBox(blockEntity);
    }

    private static void renderSign(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int overlay) {
        Direction facing = state.getValue(BaseMachineBlock.FACING);
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        Placement placement = PLACEMENTS.computeIfAbsent(model, ignored -> Placement.of(state, model, facing));
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(SIGN);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        VertexConsumer consumer = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        PoseStack.Pose pose = poseStack.last();
        float x0 = 8.0F - SIZE / 2.0F;
        float x1 = 8.0F + SIZE / 2.0F;
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        // Seen from the front the model's +x is on the viewer's left.
        if (placement.onTop()) {
            float y = placement.plane() + LIFT;
            float z0 = placement.center() - SIZE / 2.0F;
            float z1 = placement.center() + SIZE / 2.0F;
            quad(consumer, pose, new Vector3f(0.0F, 1.0F, 0.0F), overlay,
                    new float[] {x1, y, z0, u0, v1},
                    new float[] {x0, y, z0, u1, v1},
                    new float[] {x0, y, z1, u1, v0},
                    new float[] {x1, y, z1, u0, v0});
        } else {
            float z = placement.plane() - LIFT;
            float y0 = placement.center() - SIZE / 2.0F;
            float y1 = placement.center() + SIZE / 2.0F;
            quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), overlay,
                    new float[] {x1, y0, z, u0, v1},
                    new float[] {x0, y0, z, u1, v1},
                    new float[] {x0, y1, z, u1, v0},
                    new float[] {x1, y1, z, u0, v0});
        }
        poseStack.popPose();
    }

    /**
     * One quad from four {x, y, z, u, v} corners in perimeter order, full bright. The winding is flipped when needed
     * so the quad faces along {@code normal} and survives back-face culling.
     */
    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, Vector3f normal, int overlay, float[] a, float[] b, float[] c, float[] d) {
        Vector3f edge1 = new Vector3f(b[0] - a[0], b[1] - a[1], b[2] - a[2]);
        Vector3f edge2 = new Vector3f(c[0] - a[0], c[1] - a[1], c[2] - a[2]);
        boolean flip = edge1.cross(edge2).dot(normal) < 0.0F;
        float[][] corners = flip ? new float[][] {a, d, c, b} : new float[][] {a, b, c, d};
        for (float[] corner : corners) {
            consumer.addVertex(pose, corner[0], corner[1], corner[2])
                    .setColor(255, 255, 255, 255)
                    .setUv(corner[3], corner[4])
                    .setOverlay(overlay)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(pose, normal.x(), normal.y(), normal.z());
        }
    }

    /**
     * Where the sign goes, in model pixels of the north-facing frame: on the front at depth {@code plane}, centred at
     * height {@code center}, or on top at height {@code plane}, centred at depth {@code center}.
     */
    private record Placement(boolean onTop, float plane, float center) {
        private static final int STRIDE = DefaultVertexFormat.BLOCK.getVertexSize() / Integer.BYTES;

        static Placement of(BlockState state, BakedModel model, Direction facing) {
            List<float[]> front = new ArrayList<>();
            List<float[]> top = new ArrayList<>();
            RandomSource random = RandomSource.create(42L);
            for (Direction side : sidesAndNull()) {
                for (BakedQuad quad : model.getQuads(state, side, random, ModelData.EMPTY, null)) {
                    if (quad.getDirection() == facing) {
                        front.add(bounds(quad, facing));
                    } else if (quad.getDirection() == Direction.UP) {
                        top.add(bounds(quad, facing));
                    }
                }
            }
            float frontBottom = 16.0F;
            float frontTop = 0.0F;
            for (float[] box : front) {
                frontBottom = Math.min(frontBottom, box[2]);
                frontTop = Math.max(frontTop, box[3]);
            }
            if (frontTop - frontBottom >= SIZE + 2.0F * MARGIN) {
                float center = (frontBottom + frontTop) / 2.0F;
                float plane = Float.MAX_VALUE;
                for (float[] box : front) {
                    if (overlaps(box[0], box[1], box[2], box[3], center)) {
                        plane = Math.min(plane, box[4]);
                    }
                }
                return new Placement(false, plane == Float.MAX_VALUE ? 0.0F : plane, center);
            }
            float plane = Float.NEGATIVE_INFINITY;
            for (float[] box : top) {
                if (overlaps(box[0], box[1], box[4], box[5], 8.0F)) {
                    plane = Math.max(plane, box[3]);
                }
            }
            return new Placement(true, plane == Float.NEGATIVE_INFINITY ? 16.0F : plane, 8.0F);
        }

        /** Whether a surface spanning x0..x1 across and a0..a1 along the other axis lies under the sign's square. */
        private static boolean overlaps(float x0, float x1, float a0, float a1, float center) {
            return x0 < 8.0F + SIZE / 2.0F && x1 > 8.0F - SIZE / 2.0F && a0 < center + SIZE / 2.0F && a1 > center - SIZE / 2.0F;
        }

        /** A quad's extent in the north-facing frame: x0, x1, y0, y1, z0, z1, with z growing away from the front. */
        private static float[] bounds(BakedQuad quad, Direction facing) {
            Direction right = facing.getClockWise();
            int[] vertices = quad.getVertices();
            float[] box = {Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE};
            for (int i = 0; i < 4; i++) {
                float x = Float.intBitsToFloat(vertices[i * STRIDE]) - 0.5F;
                float y = Float.intBitsToFloat(vertices[i * STRIDE + 1]) * 16.0F;
                float z = Float.intBitsToFloat(vertices[i * STRIDE + 2]) - 0.5F;
                float localX = (0.5F + x * right.getStepX() + z * right.getStepZ()) * 16.0F;
                float localZ = (0.5F - x * facing.getStepX() - z * facing.getStepZ()) * 16.0F;
                box[0] = Math.min(box[0], localX);
                box[1] = Math.max(box[1], localX);
                box[2] = Math.min(box[2], y);
                box[3] = Math.max(box[3], y);
                box[4] = Math.min(box[4], localZ);
                box[5] = Math.max(box[5], localZ);
            }
            return box;
        }

        private static Direction[] sidesAndNull() {
            Direction[] sides = new Direction[Direction.values().length + 1];
            System.arraycopy(Direction.values(), 0, sides, 0, Direction.values().length);
            return sides;
        }
    }
}
