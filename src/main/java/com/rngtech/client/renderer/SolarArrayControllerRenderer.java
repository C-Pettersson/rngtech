package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.SolarArrayControllerBlockEntity;
import com.rngtech.content.blockentity.SolarPanelBlockEntity;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.SolarArrayExtenderItem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

/**
 * Draws what the Solar Array Controller shows beyond its baked model: the array map on the top display (one LED per
 * panel spot in range, lit amber while that panel generates in sunlight and pale blue in moonlight), the installed
 * Battery Cell behind the bay glass, the installed Solar Array Extender's card on the rear bracket, and, when toggled
 * on the Process tab, the in-world range outline. Positions are in model pixels and match
 * tools/blockmodels/solar.py.
 */
public class SolarArrayControllerRenderer implements BlockEntityRenderer<SolarArrayControllerBlockEntity> {
    private static final float EMPTY_R = 0.45F;
    private static final float EMPTY_G = 0.55F;
    private static final float EMPTY_B = 0.48F;
    private static final float PANEL_R = 1.0F;
    private static final float PANEL_G = 0.78F;
    private static final float PANEL_B = 0.25F;
    private static final float CONTROLLER_R = 0.35F;
    private static final float CONTROLLER_G = 0.95F;
    private static final float CONTROLLER_B = 0.75F;
    private static final int FILL_ALPHA = 38;
    private static final float OUTLINE_ALPHA = 0.85F;

    private static final ResourceLocation LED = RNGTech.id("block/solar_array_controller/fx/led");
    private static final ResourceLocation FLANGE = RNGTech.id("block/solar_array_controller/flange");
    /** The display's centre sits one pixel toward the front of the block. */
    private static final float DISPLAY_FORWARD = 1.0F;
    private static final float DISPLAY_TOP = 12.5F;
    private static final float LED_PITCH = 1.5F;
    private static final float LED_SIZE = 1.2F;
    private static final int LED_CONTROLLER = 0x5AF2BE;
    private static final int LED_SUN = 0xFFBE46;
    private static final int LED_MOON = 0xAABFFF;
    private static final int LED_IDLE = 0x6B4A14;
    private static final int LED_EMPTY = 0x26323A;
    private static final float CELL_X = 10.75F;
    private static final float CELL_Y = 9.25F;
    private static final float CELL_Z = 1.7F;
    private static final float CELL_SIZE = 2.6F;
    private static final float HINGE_Y = 12.75F;
    private static final float HINGE_Z = 12.25F;
    private static final float CARD_TILT = 25.0F;
    private static final float CARD_HALF_WIDTH = 3.5F;
    private static final float CARD_DEPTH = 2.6F;
    private static final float CARD_THICKNESS = 0.4F;

    public SolarArrayControllerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            SolarArrayControllerBlockEntity controller,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        Level level = controller.getLevel();
        if (level == null) {
            return;
        }
        Direction facing = controller.getBlockState().getValue(BaseMachineBlock.FACING);
        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        renderMap(controller, level, facing, poseStack, cutout, packedOverlay);
        renderGear(controller, level, facing, poseStack, cutout, packedLight, packedOverlay);
        if (controller.shouldPreviewRange()) {
            renderRange(controller, level, poseStack, bufferSource);
        }
    }

    /** The map is laid out in world directions whatever way the controller faces, so north is always up the map. */
    private static void renderMap(
            SolarArrayControllerBlockEntity controller,
            Level level,
            Direction facing,
            PoseStack poseStack,
            VertexConsumer consumer,
            int overlay
    ) {
        TextureAtlasSprite sprite = sprite(LED);
        int range = controller.panelRange();
        boolean night = !level.isDay();
        float centreX = 8.0F + facing.getStepX() * DISPLAY_FORWARD;
        float centreZ = 8.0F + facing.getStepZ() * DISPLAY_FORWARD;
        float y = DISPLAY_TOP + 0.02F;
        float half = LED_SIZE / 2.0F;
        poseStack.pushPose();
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        PoseStack.Pose pose = poseStack.last();
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                int color = dx == 0 && dz == 0 ? LED_CONTROLLER : ledColor(controller.panelMapState(dx, dz), night);
                float x = centreX + dx * LED_PITCH;
                float z = centreZ + dz * LED_PITCH;
                quad(consumer, pose, up, color, LightTexture.FULL_BRIGHT, overlay,
                        new float[] {x - half, y, z - half, sprite.getU0(), sprite.getV0()},
                        new float[] {x - half, y, z + half, sprite.getU0(), sprite.getV1()},
                        new float[] {x + half, y, z + half, sprite.getU1(), sprite.getV1()},
                        new float[] {x + half, y, z - half, sprite.getU1(), sprite.getV0()});
            }
        }
        poseStack.popPose();
    }

    private static int ledColor(int state, boolean night) {
        if ((state & SolarArrayControllerBlockEntity.MAP_CLAIMED) == 0) {
            return LED_EMPTY;
        }
        if ((state & SolarArrayControllerBlockEntity.MAP_GENERATING) == 0) {
            return LED_IDLE;
        }
        return night ? LED_MOON : LED_SUN;
    }

    /** The installed Battery Cell stands in the front bay; the installed extender's card leans on the rear bracket. */
    private static void renderGear(
            SolarArrayControllerBlockEntity controller,
            Level level,
            Direction facing,
            PoseStack poseStack,
            VertexConsumer consumer,
            int light,
            int overlay
    ) {
        ItemStack cell = controller.getGearInventory().getStackInSlot(SolarArrayControllerBlockEntity.SLOT_BATTERY_CELL);
        ItemStack extender = controller.getGearInventory().getStackInSlot(SolarArrayControllerBlockEntity.SLOT_SOLAR_ARRAY_EXTENDER);
        boolean hasCell = BatteryCellItem.isBatteryCell(cell);
        boolean hasExtender = extender.getItem() instanceof SolarArrayExtenderItem;
        if (!hasCell && !hasExtender) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        if (hasCell) {
            TextureAtlasSprite sprite = Minecraft.getInstance().getItemRenderer().getModel(cell, level, null, 0).getParticleIcon();
            float half = CELL_SIZE / 2.0F;
            quad(consumer, poseStack.last(), new Vector3f(0.0F, 0.0F, -1.0F), 0xFFFFFF, light, overlay,
                    new float[] {CELL_X + half, CELL_Y - half, CELL_Z, sprite.getU0(), sprite.getV1()},
                    new float[] {CELL_X - half, CELL_Y - half, CELL_Z, sprite.getU1(), sprite.getV1()},
                    new float[] {CELL_X - half, CELL_Y + half, CELL_Z, sprite.getU1(), sprite.getV0()},
                    new float[] {CELL_X + half, CELL_Y + half, CELL_Z, sprite.getU0(), sprite.getV0()});
        }
        if (hasExtender) {
            String material = ((SolarArrayExtenderItem) extender.getItem()).material().getSerializedName();
            poseStack.translate(8.0F, HINGE_Y, HINGE_Z);
            poseStack.mulPose(Axis.XP.rotationDegrees(-CARD_TILT));
            card(consumer, poseStack.last(), sprite(RNGTech.id("block/solar_array_controller/fx/extender_" + material)), sprite(FLANGE), light, overlay);
        }
        poseStack.popPose();
    }

    /** The extender's reference cell: a thin plate hinged along its front edge, its cells on top. */
    private static void card(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            TextureAtlasSprite face,
            TextureAtlasSprite edge,
            int light,
            int overlay
    ) {
        float x0 = -CARD_HALF_WIDTH;
        float x1 = CARD_HALF_WIDTH;
        float y1 = CARD_THICKNESS;
        float z1 = CARD_DEPTH;
        quad(consumer, pose, new Vector3f(0.0F, 1.0F, 0.0F), 0xFFFFFF, light, overlay,
                new float[] {x0, y1, 0.0F, face.getU0(), face.getV0()},
                new float[] {x0, y1, z1, face.getU0(), face.getV1()},
                new float[] {x1, y1, z1, face.getU1(), face.getV1()},
                new float[] {x1, y1, 0.0F, face.getU1(), face.getV0()});
        float u0 = edge.getU0();
        float v0 = edge.getV0();
        float u1 = edge.getU1();
        float v1 = edge.getV0() + (edge.getV1() - edge.getV0()) / 16.0F;
        quad(consumer, pose, new Vector3f(0.0F, -1.0F, 0.0F), 0xB0B0B0, light, overlay,
                new float[] {x0, 0.0F, 0.0F, u0, v0}, new float[] {x1, 0.0F, 0.0F, u1, v0},
                new float[] {x1, 0.0F, z1, u1, v1}, new float[] {x0, 0.0F, z1, u0, v1});
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), 0xB0B0B0, light, overlay,
                new float[] {x0, 0.0F, 0.0F, u0, v1}, new float[] {x1, 0.0F, 0.0F, u1, v1},
                new float[] {x1, y1, 0.0F, u1, v0}, new float[] {x0, y1, 0.0F, u0, v0});
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, 1.0F), 0xB0B0B0, light, overlay,
                new float[] {x0, 0.0F, z1, u0, v1}, new float[] {x1, 0.0F, z1, u1, v1},
                new float[] {x1, y1, z1, u1, v0}, new float[] {x0, y1, z1, u0, v0});
        quad(consumer, pose, new Vector3f(-1.0F, 0.0F, 0.0F), 0xB0B0B0, light, overlay,
                new float[] {x0, 0.0F, 0.0F, u0, v1}, new float[] {x0, 0.0F, z1, u1, v1},
                new float[] {x0, y1, z1, u1, v0}, new float[] {x0, y1, 0.0F, u0, v0});
        quad(consumer, pose, new Vector3f(1.0F, 0.0F, 0.0F), 0xB0B0B0, light, overlay,
                new float[] {x1, 0.0F, 0.0F, u0, v1}, new float[] {x1, 0.0F, z1, u1, v1},
                new float[] {x1, y1, z1, u1, v0}, new float[] {x1, y1, 0.0F, u0, v0});
    }

    private static void renderRange(
            SolarArrayControllerBlockEntity controller,
            Level level,
            PoseStack poseStack,
            MultiBufferSource bufferSource
    ) {
        int range = controller.panelRange();
        BlockPos origin = controller.getBlockPos();
        renderCell(poseStack, bufferSource, 0, 0, CONTROLLER_R, CONTROLLER_G, CONTROLLER_B);
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                BlockPos pos = origin.offset(x, 0, z);
                boolean panel = level.getBlockEntity(pos) instanceof SolarPanelBlockEntity;
                renderCell(
                        poseStack,
                        bufferSource,
                        x,
                        z,
                        panel ? PANEL_R : EMPTY_R,
                        panel ? PANEL_G : EMPTY_G,
                        panel ? PANEL_B : EMPTY_B
                );
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(SolarArrayControllerBlockEntity controller) {
        return controller.shouldPreviewRange();
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    private static void renderCell(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int x,
            int z,
            float red,
            float green,
            float blue
    ) {
        renderFilledCell(poseStack, bufferSource.getBuffer(RenderType.debugQuads()), x, z, red, green, blue);
        AABB outline = new AABB(x + 0.03D, 0.03D, z + 0.03D, x + 0.97D, 1.04D, z + 0.97D);
        LevelRenderer.renderLineBox(
                poseStack,
                bufferSource.getBuffer(RenderType.lines()),
                outline,
                red,
                green,
                blue,
                OUTLINE_ALPHA
        );
    }

    private static void renderFilledCell(
            PoseStack poseStack,
            VertexConsumer consumer,
            int x,
            int z,
            float red,
            float green,
            float blue
    ) {
        PoseStack.Pose pose = poseStack.last();
        int redByte = colorByte(red);
        int greenByte = colorByte(green);
        int blueByte = colorByte(blue);
        float minX = x + 0.04F;
        float minY = 0.04F;
        float minZ = z + 0.04F;
        float maxX = x + 0.96F;
        float maxY = 1.02F;
        float maxZ = z + 0.96F;
        fillQuad(consumer, pose, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, redByte, greenByte, blueByte);
        fillQuad(consumer, pose, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, redByte, greenByte, blueByte);
        fillQuad(consumer, pose, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, redByte, greenByte, blueByte);
        fillQuad(consumer, pose, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, redByte, greenByte, blueByte);
        fillQuad(consumer, pose, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ, redByte, greenByte, blueByte);
        fillQuad(consumer, pose, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, redByte, greenByte, blueByte);
    }

    private static void fillQuad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4,
            int red,
            int green,
            int blue
    ) {
        consumer.addVertex(pose, x1, y1, z1).setColor(red, green, blue, FILL_ALPHA);
        consumer.addVertex(pose, x2, y2, z2).setColor(red, green, blue, FILL_ALPHA);
        consumer.addVertex(pose, x3, y3, z3).setColor(red, green, blue, FILL_ALPHA);
        consumer.addVertex(pose, x4, y4, z4).setColor(red, green, blue, FILL_ALPHA);
    }

    /**
     * One textured quad from four {x, y, z, u, v} corners in perimeter order. The winding is flipped when needed so the
     * quad faces along {@code normal} and survives back-face culling.
     */
    private static void quad(
            VertexConsumer consumer, PoseStack.Pose pose, Vector3f normal,
            int color, int light, int overlay,
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
                    .setColor(red, green, blue, 255)
                    .setUv(corner[3], corner[4])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose, normal.x(), normal.y(), normal.z());
        }
    }

    private static TextureAtlasSprite sprite(ResourceLocation id) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(id);
    }

    private static int colorByte(float color) {
        return Math.round(color * 255.0F);
    }
}
