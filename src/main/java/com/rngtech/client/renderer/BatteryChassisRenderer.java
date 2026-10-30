package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.BatteryChassisBlockEntity;
import com.rngtech.content.blockentity.BatteryChassisVisualState;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.energy.BatteryChassisMaterial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws the working parts of a Battery Chassis: a cell in each active slot, scaled to the layout, with its charge
 * window filled to its own charge; the sockets and the straps that tie the cells to the bus; the charge meter on the
 * corner posts; the status lamps on the lid; and on each side with an FE neighbour, a connector port for a cable or
 * Universal Connector or a smooth contact wall for anything else, flush in the hatch on top and underneath. The Wooden
 * crate shows only its cells, their wires to its copper bar, and its redstone lamp, lit while FE flows in or out.
 * Positions are in model pixels and match tools/blockmodels/battery_chassis.py.
 */
public class BatteryChassisRenderer implements BlockEntityRenderer<BatteryChassisBlockEntity> {
    private static final Map<String, ModelResourceLocation> CELL_MODELS = new HashMap<>();
    /** Plate models by plate, placement, and metal, such as {@code contact_flat_iron}. */
    private static final Map<String, ModelResourceLocation> PLATE_MODELS = new HashMap<>();
    private static final Map<String, ResourceLocation> CELL_GLOWS = new HashMap<>();
    private static final String[] PORT_METALS =
            {"iron", "copper", "gold", "steel", "sparksteel", "arclite", "nullite", "aethergold", "naquadah"};
    private static final ResourceLocation LED = RNGTech.id("block/battery_chassis/fx/led");
    private static final ResourceLocation SOCKET = RNGTech.id("block/battery_chassis/socket");
    private static final ResourceLocation CRATE_BAR = RNGTech.id("block/battery_chassis/crate/bar");
    private static final ResourceLocation CRATE_LAMP = RNGTech.id("block/battery_chassis/crate/lamp_on");
    /** Interior the cells are laid out in, as x0, x1, z0, z1. */
    private static final float[] RACK_INTERIOR = {2.0F, 14.0F, 2.0F, 14.0F};
    private static final float[] CRATE_INTERIOR = {2.0F, 14.0F, 2.0F, 14.0F};
    private static final float PLINTH_TOP = 2.0F;
    private static final float SOCKET_TOP = 2.25F;
    private static final float CRATE_FLOOR = 1.0F;
    private static final float CRATE_CELL_SCALE = 0.85F;
    // Canonical cell part model: a 6 pixel footprint centred on (8, 8), 10 pixels tall.
    private static final float CELL_WIDTH = 6.0F;
    private static final float CELL_R = 3.0F;
    private static final float CELL_HEIGHT = 10.0F;
    private static final float POTATO_TOP = 7.5F;
    private static final float COIN_TOP = 9.0F;
    private static final float ANODE_TOP = 8.0F;
    private static final float CELL_MAX_DIAMETER = 6.0F;
    private static final float CELL_PITCH_SHARE = 0.78F;
    private static final float WINDOW_HALF = 0.75F;
    private static final float WINDOW_Y0 = 2.5F;
    private static final float WINDOW_Y1 = 7.0F;
    // Row straps under the bus rails, and wires up to the Wooden crate's copper bar.
    private static final float RAIL_OUT = 0.5F;
    private static final float STRAP_Y0 = 12.25F;
    private static final float STRAP_Y1 = 12.75F;
    private static final float STRAP_HALF = 0.3F;
    private static final float WIRE_HALF = 0.2F;
    private static final float CRATE_BAR_Y0 = 9.5F;
    private static final float CRATE_BAR_Z0 = 7.25F;
    private static final float CRATE_BAR_Z1 = 8.75F;
    // Redstone lamp on the Wooden crate's front rim.
    private static final float[] CRATE_LAMP_BOX = {10.5F, 8.0F, 0.25F, 12.5F, 9.25F, 1.75F};
    // Charge meter on the outward faces of each corner post.
    private static final float POST = 2.0F;
    private static final float LED_Y0 = 3.1875F;
    private static final float LED_PITCH = 1.25F;
    private static final float LED_HEIGHT = 0.875F;
    private static final float LED_HALF = 0.5F;
    // Status lamp lens in the middle of each lid side.
    private static final float LAMP_X0 = 6.5F;
    private static final float LAMP_X1 = 9.5F;
    private static final float LAMP_Y0 = 14.875F;
    private static final float LAMP_Y1 = 15.625F;
    private static final float SURFACE = 0.02F;
    private static final int LED_EMPTY = 0xFF5A3C;
    private static final int LED_LOW = 0xFFB040;
    private static final int LED_FULL = 0x8CF07A;
    private static final int LAMP_CHARGING = 0x8CF07A;
    private static final int LAMP_DISCHARGING = 0xFFB040;
    private static final int LAMP_BALANCED = 0x2FB3C2;

    static {
        for (BatteryCellMaterial material : BatteryCellMaterial.values()) {
            String kind = material.getSerializedName();
            CELL_MODELS.put(kind, part("cell_" + kind));
            if (hasWindow(kind)) {
                CELL_GLOWS.put(kind, RNGTech.id("block/battery_chassis/cell/" + kind + "_glow"));
            }
        }
        for (String plate : new String[] {"port", "contact"}) {
            for (String placement : new String[] {"", "_flat"}) {
                for (String metal : PORT_METALS) {
                    String name = plate + placement + "_" + metal;
                    PLATE_MODELS.put(name, part(name));
                }
            }
        }
    }

    private final ModelBlockRenderer modelRenderer;

    public BatteryChassisRenderer(BlockEntityRendererProvider.Context context) {
        modelRenderer = context.getBlockRenderDispatcher().getModelRenderer();
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        CELL_MODELS.values().forEach(event::register);
        PLATE_MODELS.values().forEach(event::register);
    }

    private static ModelResourceLocation part(String name) {
        return new ModelResourceLocation(RNGTech.id("block/battery_chassis/" + name), ModelResourceLocation.STANDALONE_VARIANT);
    }

    /** The potato, coin-stack, and salt-zinc cells have no charge window. */
    private static boolean hasWindow(String kind) {
        return !kind.endsWith("potato") && !kind.equals(BatteryCellMaterial.BASTION_COIN_STACK.getSerializedName())
                && !kind.equals(BatteryCellMaterial.SHIPWRECK_SALT_ZINC.getSerializedName());
    }

    private static float cellTop(String kind) {
        if (kind.endsWith("potato")) {
            return POTATO_TOP;
        }
        if (kind.equals(BatteryCellMaterial.SHIPWRECK_SALT_ZINC.getSerializedName())) {
            return ANODE_TOP;
        }
        return kind.equals(BatteryCellMaterial.BASTION_COIN_STACK.getSerializedName()) ? COIN_TOP : CELL_HEIGHT;
    }

    /** The wiring metal of a metal chassis. */
    private static String metal(BatteryChassisMaterial material) {
        return material.materialId();
    }

    /**
     * Centre x, centre z, and diameter of each cell slot: a near-square grid, rows from the front, the last row centred.
     * Matches cell_layout in tools/blockmodels/battery_chassis.py.
     */
    static float[][] layout(int slots, float[] interior) {
        if (slots <= 0) {
            return new float[0][];
        }
        int cols = Mth.ceil(Math.sqrt(slots));
        int rows = Mth.ceil(slots / (float) cols);
        float pitch = Math.min((interior[1] - interior[0]) / cols, (interior[3] - interior[2]) / rows);
        float diameter = Math.min(CELL_MAX_DIAMETER, pitch * CELL_PITCH_SHARE);
        float midX = (interior[0] + interior[1]) / 2.0F;
        float midZ = (interior[2] + interior[3]) / 2.0F;
        float[][] cells = new float[slots][];
        for (int slot = 0; slot < slots; slot++) {
            int row = slot / cols;
            int col = slot % cols;
            int inRow = Math.min(cols, slots - row * cols);
            cells[slot] = new float[] {
                    midX + ((inRow - 1) / 2.0F - col) * pitch,
                    midZ + (row - (rows - 1) / 2.0F) * pitch,
                    diameter
            };
        }
        return cells;
    }

    @Override
    public void render(
            BatteryChassisBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        BatteryChassisVisualState state = blockEntity.visualState();
        BatteryChassisMaterial material = blockEntity.block().material();
        boolean crate = material == BatteryChassisMaterial.WOODEN;
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);
        TextureAtlasSprite wire = sprite(crate ? CRATE_BAR : RNGTech.id("block/battery_chassis/wire/" + metal(material)));

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());

        float[][] layout = layout(state.slots(), crate ? CRATE_INTERIOR : RACK_INTERIOR);
        float base = crate ? CRATE_FLOOR : SOCKET_TOP;
        float heightScale = crate ? CRATE_CELL_SCALE : 1.0F;
        List<BatteryChassisVisualState.Cell> cells = state.cells();
        for (int slot = 0; slot < layout.length; slot++) {
            BatteryChassisVisualState.Cell cell = cells.get(slot);
            if (!cell.empty()) {
                renderCell(poseStack, cutout, cell.kind(), layout[slot], base, heightScale, packedLight, packedOverlay);
            }
        }
        if (!crate) {
            for (Direction local : Direction.values()) {
                BatteryChassisVisualState.Plate plate = state.plate(local.getAxis().isVertical() ? local : toWorld(local, facing));
                if (plate != BatteryChassisVisualState.Plate.NONE) {
                    renderPlate(poseStack, cutout, plate, metal(material), local, packedLight, packedOverlay);
                }
            }
        }

        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        PoseStack.Pose pose = poseStack.last();
        for (int slot = 0; slot < layout.length; slot++) {
            BatteryChassisVisualState.Cell cell = cells.get(slot);
            float[] at = layout[slot];
            if (!crate) {
                float half = at[2] / 2.0F + 0.25F;
                cube(cutout, pose, sprite(SOCKET), 16.0F, at[0] - half, PLINTH_TOP, at[1] - half, at[0] + half, SOCKET_TOP, at[1] + half,
                        0xFFFFFF, packedLight, packedOverlay);
            }
            if (cell.empty()) {
                continue;
            }
            float top = base + cellTop(cell.kind()) * heightScale;
            if (crate) {
                renderCrateWire(cutout, pose, wire, at, top, packedLight, packedOverlay);
            } else if (top < STRAP_Y0) {
                cube(cutout, pose, wire, 16.0F, at[0] - WIRE_HALF, top, at[1] - WIRE_HALF, at[0] + WIRE_HALF, STRAP_Y0, at[1] + WIRE_HALF,
                        0xFFFFFF, packedLight, packedOverlay);
            }
            ResourceLocation glow = CELL_GLOWS.get(cell.kind());
            if (glow != null && cell.charge() > 0.0F) {
                renderWindows(cutout, pose, sprite(glow), at, base, heightScale, cell.charge(), packedOverlay);
            }
        }
        if (crate && state.flow() != BatteryChassisVisualState.Flow.IDLE) {
            float[] lamp = CRATE_LAMP_BOX;
            cube(cutout, pose, sprite(CRATE_LAMP), 16.0F, lamp[0] - SURFACE, lamp[1], lamp[2] - SURFACE, lamp[3] + SURFACE, lamp[4] + SURFACE,
                    lamp[5] + SURFACE, 0xFFFFFF, LightTexture.FULL_BRIGHT, packedOverlay);
        }
        if (!crate) {
            renderStraps(cutout, pose, wire, layout, cells, packedLight, packedOverlay);
            renderMeter(cutout, pose, state, time, packedOverlay);
            renderLamps(cutout, pose, state.flow(), time, packedOverlay);
        }
        poseStack.popPose();
    }

    /** The side of the block a model side faces once the blockstate turns the model to {@code facing}. */
    private static Direction toWorld(Direction local, Direction facing) {
        Direction world = local;
        for (int i = 0; i < clockwiseSteps(facing); i++) {
            world = world.getClockWise();
        }
        return world;
    }

    /** Quarter turns clockwise from north, as the blockstate's y rotation. */
    private static int clockwiseSteps(Direction side) {
        return Math.floorMod(side.get2DDataValue() - Direction.NORTH.get2DDataValue(), 4);
    }

    private void renderCell(
            PoseStack poseStack, VertexConsumer consumer, String kind, float[] at, float base, float heightScale, int light, int overlay
    ) {
        ModelResourceLocation location = CELL_MODELS.get(kind);
        if (location == null) {
            return;
        }
        float scale = at[2] / CELL_WIDTH;
        poseStack.pushPose();
        poseStack.translate(at[0] / 16.0F, base / 16.0F, at[1] / 16.0F);
        poseStack.scale(scale, heightScale, scale);
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        renderModel(poseStack, consumer, location, light, overlay);
        poseStack.popPose();
    }

    /** A plate model built on the north face, turned to {@code local}; top and bottom use the hatch-depth variant. */
    private void renderPlate(
            PoseStack poseStack, VertexConsumer consumer, BatteryChassisVisualState.Plate plate, String metal, Direction local,
            int light, int overlay
    ) {
        boolean vertical = local.getAxis().isVertical();
        String name = (plate == BatteryChassisVisualState.Plate.CONNECTOR ? "port" : "contact") + (vertical ? "_flat_" : "_") + metal;
        ModelResourceLocation location = PLATE_MODELS.get(name);
        if (location == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (vertical) {
            poseStack.mulPose(Axis.XP.rotationDegrees(local == Direction.UP ? 90.0F : -90.0F));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F * clockwiseSteps(local)));
        }
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        renderModel(poseStack, consumer, location, light, overlay);
        poseStack.popPose();
    }

    private void renderModel(PoseStack poseStack, VertexConsumer consumer, ModelResourceLocation location, int light, int overlay) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
        modelRenderer.renderModel(poseStack.last(), consumer, null, model, 1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY, RenderType.cutout());
    }

    /** The lit part of a cell's charge window on each of its four faces, filled from the bottom. */
    private static void renderWindows(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, float[] at, float base, float heightScale,
            float charge, int overlay
    ) {
        float scale = at[2] / CELL_WIDTH;
        float half = WINDOW_HALF * scale;
        float reach = CELL_R * scale + SURFACE;
        float y0 = base + WINDOW_Y0 * heightScale;
        float y1 = y0 + (WINDOW_Y1 - WINDOW_Y0) * heightScale * Math.max(0.08F, Mth.clamp(charge, 0.0F, 1.0F));
        float x = at[0];
        float z = at[1];
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        int light = LightTexture.FULL_BRIGHT;
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), 0xFFFFFF, light, overlay,
                new float[] {x - half, y0, z - reach, u0, v1}, new float[] {x + half, y0, z - reach, u1, v1},
                new float[] {x + half, y1, z - reach, u1, v0}, new float[] {x - half, y1, z - reach, u0, v0});
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, 1.0F), 0xFFFFFF, light, overlay,
                new float[] {x - half, y0, z + reach, u0, v1}, new float[] {x + half, y0, z + reach, u1, v1},
                new float[] {x + half, y1, z + reach, u1, v0}, new float[] {x - half, y1, z + reach, u0, v0});
        quad(consumer, pose, new Vector3f(-1.0F, 0.0F, 0.0F), 0xFFFFFF, light, overlay,
                new float[] {x - reach, y0, z - half, u0, v1}, new float[] {x - reach, y0, z + half, u1, v1},
                new float[] {x - reach, y1, z + half, u1, v0}, new float[] {x - reach, y1, z - half, u0, v0});
        quad(consumer, pose, new Vector3f(1.0F, 0.0F, 0.0F), 0xFFFFFF, light, overlay,
                new float[] {x + reach, y0, z - half, u0, v1}, new float[] {x + reach, y0, z + half, u1, v1},
                new float[] {x + reach, y1, z + half, u1, v0}, new float[] {x + reach, y1, z - half, u0, v0});
    }

    /** A strap in the chassis metal across the terminals of each row of cells, out to the side rails. */
    private static void renderStraps(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite wire, float[][] layout,
            List<BatteryChassisVisualState.Cell> cells, int light, int overlay
    ) {
        float lastZ = Float.NaN;
        for (int slot = 0; slot < layout.length; slot++) {
            float z = layout[slot][1];
            if (z == lastZ) {
                continue;
            }
            lastZ = z;
            if (!rowHasCell(layout, cells, z)) {
                continue;
            }
            cube(consumer, pose, wire, 16.0F, RAIL_OUT, STRAP_Y0, z - STRAP_HALF, 16.0F - RAIL_OUT, STRAP_Y1, z + STRAP_HALF,
                    0xFFFFFF, light, overlay);
        }
    }

    private static boolean rowHasCell(float[][] layout, List<BatteryChassisVisualState.Cell> cells, float z) {
        for (int slot = 0; slot < layout.length; slot++) {
            if (layout[slot][1] == z && !cells.get(slot).empty()) {
                return true;
            }
        }
        return false;
    }

    /** A copper wire from a cell's terminal up and across to the Wooden crate's bus bar. */
    private static void renderCrateWire(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite wire, float[] at, float top, int light, int overlay) {
        float x = at[0];
        float z = at[1];
        float bar = z < CRATE_BAR_Z0 ? CRATE_BAR_Z0 : z > CRATE_BAR_Z1 ? CRATE_BAR_Z1 : z;
        float y1 = CRATE_BAR_Y0;
        cube(consumer, pose, wire, 16.0F, x - WIRE_HALF, top, z - WIRE_HALF, x + WIRE_HALF, y1, z + WIRE_HALF, 0xFFFFFF, light, overlay);
        if (Math.abs(bar - z) > WIRE_HALF) {
            cube(consumer, pose, wire, 16.0F, x - WIRE_HALF, y1 - 2.0F * WIRE_HALF, Math.min(z, bar), x + WIRE_HALF, y1, Math.max(z, bar),
                    0xFFFFFF, light, overlay);
        }
    }

    /**
     * Lit charge-meter segments on the outward faces of every corner post, the lowest two warning colours. While charging,
     * the next segment blinks; while discharging, the top lit segment pulses.
     */
    private static void renderMeter(VertexConsumer consumer, PoseStack.Pose pose, BatteryChassisVisualState state, float time, int overlay) {
        int lit = state.meterSegments();
        int blinking = -1;
        float brightness = 1.0F;
        if (state.flow() == BatteryChassisVisualState.Flow.CHARGING && lit < BatteryChassisVisualState.METER_SEGMENTS) {
            blinking = lit;
            brightness = Mth.sin(time * 0.3F) > 0.0F ? 1.0F : 0.0F;
        } else if (state.flow() == BatteryChassisVisualState.Flow.DISCHARGING && lit > 0) {
            blinking = lit - 1;
            brightness = 0.45F + 0.55F * (0.5F + 0.5F * Mth.sin(time * 0.25F));
        }
        TextureAtlasSprite sprite = sprite(LED);
        for (int i = 0; i < BatteryChassisVisualState.METER_SEGMENTS; i++) {
            boolean on = i < lit || i == blinking;
            float level = i == blinking ? brightness : 1.0F;
            if (!on || level <= 0.01F) {
                continue;
            }
            int color = scale(i == 0 ? LED_EMPTY : i == 1 ? LED_LOW : LED_FULL, level);
            float y0 = LED_Y0 + i * LED_PITCH;
            float y1 = y0 + LED_HEIGHT;
            for (float x0 : new float[] {0.0F, 16.0F - POST}) {
                for (float z0 : new float[] {0.0F, 16.0F - POST}) {
                    float cx = x0 + POST / 2.0F;
                    float cz = z0 + POST / 2.0F;
                    float fz = z0 == 0.0F ? -SURFACE : 16.0F + SURFACE;
                    float fx = x0 == 0.0F ? -SURFACE : 16.0F + SURFACE;
                    quad(consumer, pose, new Vector3f(0.0F, 0.0F, z0 == 0.0F ? -1.0F : 1.0F), color, LightTexture.FULL_BRIGHT, overlay,
                            new float[] {cx - LED_HALF, y0, fz, sprite.getU0(), sprite.getV1()},
                            new float[] {cx + LED_HALF, y0, fz, sprite.getU1(), sprite.getV1()},
                            new float[] {cx + LED_HALF, y1, fz, sprite.getU1(), sprite.getV0()},
                            new float[] {cx - LED_HALF, y1, fz, sprite.getU0(), sprite.getV0()});
                    quad(consumer, pose, new Vector3f(x0 == 0.0F ? -1.0F : 1.0F, 0.0F, 0.0F), color, LightTexture.FULL_BRIGHT, overlay,
                            new float[] {fx, y0, cz - LED_HALF, sprite.getU0(), sprite.getV1()},
                            new float[] {fx, y0, cz + LED_HALF, sprite.getU1(), sprite.getV1()},
                            new float[] {fx, y1, cz + LED_HALF, sprite.getU1(), sprite.getV0()},
                            new float[] {fx, y1, cz - LED_HALF, sprite.getU0(), sprite.getV0()});
                }
            }
        }
    }

    /** The lid lamps: green while charging, amber while discharging, teal while FE passes through, dark when idle. */
    private static void renderLamps(VertexConsumer consumer, PoseStack.Pose pose, BatteryChassisVisualState.Flow flow, float time, int overlay) {
        int color = switch (flow) {
            case CHARGING -> LAMP_CHARGING;
            case DISCHARGING -> LAMP_DISCHARGING;
            case BALANCED -> LAMP_BALANCED;
            case IDLE -> -1;
        };
        if (color < 0) {
            return;
        }
        color = scale(color, 0.8F + 0.2F * Mth.sin(time * 0.2F));
        TextureAtlasSprite sprite = sprite(LED);
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        int light = LightTexture.FULL_BRIGHT;
        float near = -SURFACE;
        float far = 16.0F + SURFACE;
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), color, light, overlay,
                new float[] {LAMP_X0, LAMP_Y0, near, u0, v1}, new float[] {LAMP_X1, LAMP_Y0, near, u1, v1},
                new float[] {LAMP_X1, LAMP_Y1, near, u1, v0}, new float[] {LAMP_X0, LAMP_Y1, near, u0, v0});
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, 1.0F), color, light, overlay,
                new float[] {LAMP_X0, LAMP_Y0, far, u0, v1}, new float[] {LAMP_X1, LAMP_Y0, far, u1, v1},
                new float[] {LAMP_X1, LAMP_Y1, far, u1, v0}, new float[] {LAMP_X0, LAMP_Y1, far, u0, v0});
        quad(consumer, pose, new Vector3f(-1.0F, 0.0F, 0.0F), color, light, overlay,
                new float[] {near, LAMP_Y0, LAMP_X0, u0, v1}, new float[] {near, LAMP_Y0, LAMP_X1, u1, v1},
                new float[] {near, LAMP_Y1, LAMP_X1, u1, v0}, new float[] {near, LAMP_Y1, LAMP_X0, u0, v0});
        quad(consumer, pose, new Vector3f(1.0F, 0.0F, 0.0F), color, light, overlay,
                new float[] {far, LAMP_Y0, LAMP_X0, u0, v1}, new float[] {far, LAMP_Y0, LAMP_X1, u1, v1},
                new float[] {far, LAMP_Y1, LAMP_X1, u1, v0}, new float[] {far, LAMP_Y1, LAMP_X0, u0, v0});
    }

    /** A box whose faces show {@code span} model pixels per full sprite, cropped to the face size. */
    private static void cube(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, float span,
            float x0, float y0, float z0, float x1, float y1, float z1,
            int color, int light, int overlay
    ) {
        float u0 = sprite.getU0();
        float v0 = sprite.getV0();
        float uw = sprite.getU1() - u0;
        float vh = sprite.getV1() - v0;
        float ux = u0 + uw * Math.min(1.0F, (x1 - x0) / span);
        float uz = u0 + uw * Math.min(1.0F, (z1 - z0) / span);
        float vy = v0 + vh * Math.min(1.0F, (y1 - y0) / span);
        float vz = v0 + vh * Math.min(1.0F, (z1 - z0) / span);
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), color, light, overlay,
                new float[] {x0, y0, z0, u0, vy}, new float[] {x1, y0, z0, ux, vy}, new float[] {x1, y1, z0, ux, v0}, new float[] {x0, y1, z0, u0, v0});
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, 1.0F), color, light, overlay,
                new float[] {x0, y0, z1, u0, vy}, new float[] {x1, y0, z1, ux, vy}, new float[] {x1, y1, z1, ux, v0}, new float[] {x0, y1, z1, u0, v0});
        quad(consumer, pose, new Vector3f(-1.0F, 0.0F, 0.0F), color, light, overlay,
                new float[] {x0, y0, z0, u0, vy}, new float[] {x0, y0, z1, uz, vy}, new float[] {x0, y1, z1, uz, v0}, new float[] {x0, y1, z0, u0, v0});
        quad(consumer, pose, new Vector3f(1.0F, 0.0F, 0.0F), color, light, overlay,
                new float[] {x1, y0, z0, u0, vy}, new float[] {x1, y0, z1, uz, vy}, new float[] {x1, y1, z1, uz, v0}, new float[] {x1, y1, z0, u0, v0});
        quad(consumer, pose, new Vector3f(0.0F, 1.0F, 0.0F), color, light, overlay,
                new float[] {x0, y1, z0, u0, v0}, new float[] {x1, y1, z0, ux, v0}, new float[] {x1, y1, z1, ux, vz}, new float[] {x0, y1, z1, u0, vz});
        quad(consumer, pose, new Vector3f(0.0F, -1.0F, 0.0F), color, light, overlay,
                new float[] {x0, y0, z0, u0, v0}, new float[] {x1, y0, z0, ux, v0}, new float[] {x1, y0, z1, ux, vz}, new float[] {x0, y0, z1, u0, vz});
    }

    /**
     * One quad from four {x, y, z, u, v} corners in perimeter order. The winding is flipped when needed so the quad
     * faces along {@code normal} and survives back-face culling.
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

    private static int scale(int color, float level) {
        int red = Math.round((color >> 16 & 0xFF) * level);
        int green = Math.round((color >> 8 & 0xFF) * level);
        int blue = Math.round((color & 0xFF) * level);
        return red << 16 | green << 8 | blue;
    }
}
