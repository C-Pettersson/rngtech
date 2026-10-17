package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.CompressorTankBlockEntity;
import com.rngtech.content.blockentity.CompressorTankVisualState;
import com.rngtech.content.machine.CompressorTankMaterial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws the live parts of a Fluid or Compressor Tank and a connector port on each side a cable or Universal Connector
 * faces. A Fluid Tank shows its fluid behind the glass. A Compressor Tank shows its loose fluid in the four corner feed
 * tubes, the compressed fill in the vessel's sight window, the gauge needle reading that fill, each installed Servo as a
 * pump in its own metal and accent colour, and the Battery Cell in its cradle behind the vessel. While it compresses,
 * the pumps stroke, the impeller on the vessel head spins, the needle pulses with the strokes, and bubbles rise in the
 * compressed fill.
 * Positions are in model pixels and match tools/blockmodels/compressor_tank.py.
 */
public class CompressorTankRenderer implements BlockEntityRenderer<CompressorTankBlockEntity> {
    private static final String[] SERVOS = {"steel", "titanium", "tungstensteel", "nullite", "exotic", "unique"};
    private static final Map<String, ModelResourceLocation> SERVO_MODELS = new HashMap<>();
    private static final Map<String, ModelResourceLocation> PORT_MODELS = new HashMap<>();
    private static final ResourceLocation ROD = RNGTech.id("block/compressor_tank/pipe");
    private static final ResourceLocation BLADE = RNGTech.id("block/compressor_tank/trim/steel");
    private static final ResourceLocation HUB = RNGTech.id("block/compressor_tank/servo/steel_accent");
    // A Fluid Tank's fluid fills the inside of the glass.
    private static final float FLUID_LO = 2.0F;
    private static final float FLUID_HI = 14.0F;
    private static final float FLOOR = 2.01F;
    private static final float FLUID_TANK_TOP = 14.0F;
    // A Compressor Tank's loose fluid fills the glass feed tube in each corner, as x0 and z0 of the tube's inside.
    private static final float[][] TUBES = {{1.3F, 1.3F}, {13.3F, 1.3F}, {1.3F, 13.3F}, {13.3F, 13.3F}};
    private static final float TUBE_WIDTH = 1.4F;
    private static final float TUBE_Y0 = 2.76F;
    private static final float TUBE_Y1 = 12.7F;
    // The vessel's sight window and the compressed fill drawn on its gauge glass.
    private static final float WINDOW_X0 = 6.75F;
    private static final float WINDOW_X1 = 9.25F;
    private static final float WINDOW_Y0 = 3.5F;
    private static final float WINDOW_Y1 = 10.5F;
    private static final float WINDOW_Z = 2.55F;
    /** Compressed fluid is denser, so it reads darker than the loose fluid. */
    private static final float COMPRESSED_SHADE = 0.7F;
    private static final int LOOSE_ALPHA = 200;
    // Pressure gauge on the vessel's shoulder; its needle reads the compressed fill.
    private static final float DIAL_X = 8.0F;
    private static final float DIAL_Y = 12.5F;
    private static final float DIAL_Z = 2.45F;
    private static final float NEEDLE_LENGTH = 1.0F;
    private static final float NEEDLE_WIDTH = 0.09F;
    private static final int NEEDLE_COLOR = 0xC43428;
    // A pump stands on each corner feed tube; its rod and crosshead ride the stroke above the cylinder head.
    private static final float[][] PUMP_CENTRES = {{2.0F, 2.0F}, {14.0F, 2.0F}, {2.0F, 14.0F}, {14.0F, 14.0F}};
    private static final float PUMP_BASE = 13.5F;
    private static final float PUMP_HEAD = 15.0F;
    private static final float CROSSHEAD_Y0 = 15.05F;
    private static final float CROSSHEAD_H = 0.35F;
    private static final float CROSSHEAD_HALF = 0.9F;
    private static final float ROD_HALF = 0.2F;
    private static final float STROKE = 0.6F;
    // Impeller on the vessel head's manway flange, hidden under a top port plate.
    private static final float IMPELLER_Y0 = 15.0F;
    private static final float IMPELLER_Y1 = 15.2F;
    private static final float BLADE_LENGTH = 2.2F;
    private static final float BLADE_HALF = 0.3F;
    private static final float HUB_HALF = 0.45F;
    private static final float HUB_TOP = 15.4F;
    private static final float IMPELLER_SPEED = 2.5F;
    /** How far the needle kicks with each pump stroke, as a share of full scale. */
    private static final float NEEDLE_PULSE = 0.03F;
    // Bubbles rising in the compressed fill.
    private static final float[] BUBBLE_X = {7.35F, 8.1F, 8.7F};
    private static final float BUBBLE_SIZE = 0.3F;
    private static final float BUBBLE_Z = 2.53F;
    private static final float BUBBLE_RISE_PER_TICK = 0.015F;
    /** Crank degrees per tick at full speed; the pumps run a quarter turn apart. */
    private static final float DEGREES_PER_TICK = 30.0F;
    // The Battery Cell lies on the cradle behind the vessel. Its part model is 6 pixels wide and 10 tall.
    private static final float CELL_X = 8.0F;
    private static final float CELL_Y = 3.65F;
    private static final float CELL_Z = 14.375F;
    private static final float CELL_SCALE = 2.25F / 6.0F;
    private static final float CELL_HALF_LENGTH = 5.0F;
    // Face masks for box().
    private static final int NORTH = 1;
    private static final int SOUTH = 2;
    private static final int WEST = 4;
    private static final int EAST = 8;
    private static final int UP = 16;
    private static final int DOWN = 32;
    private static final int OPEN_BOTTOM = NORTH | SOUTH | WEST | EAST | UP;

    static {
        for (String servo : SERVOS) {
            SERVO_MODELS.put(servo, part("servo_" + servo));
        }
        for (CompressorTankMaterial material : CompressorTankMaterial.values()) {
            for (String plate : new String[] {"port", "port_flat"}) {
                String name = plate + "_" + material.getSerializedName();
                PORT_MODELS.put(name, part(name));
            }
        }
    }

    private final ModelBlockRenderer modelRenderer;

    public CompressorTankRenderer(BlockEntityRendererProvider.Context context) {
        modelRenderer = context.getBlockRenderDispatcher().getModelRenderer();
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        SERVO_MODELS.values().forEach(event::register);
        PORT_MODELS.values().forEach(event::register);
    }

    private static ModelResourceLocation part(String name) {
        return new ModelResourceLocation(RNGTech.id("block/compressor_tank/" + name), ModelResourceLocation.STANDALONE_VARIANT);
    }

    /** The Battery Chassis renderer registers every cell part model. */
    private static ModelResourceLocation cellModel(String kind) {
        return new ModelResourceLocation(RNGTech.id("block/battery_chassis/cell_" + kind), ModelResourceLocation.STANDALONE_VARIANT);
    }

    @Override
    public void render(
            CompressorTankBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        CompressorTankVisualState state = blockEntity.visualState();
        CompressorTankVisualState.Animation animation = blockEntity.animation();
        CompressorTankMaterial material = blockEntity.material();
        boolean compressor = material.supportsCompression();
        boolean active = blockEntity.getBlockState().getValue(BaseMachineBlock.ACTIVE);
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        animate(animation, state, active, time);
        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());

        for (Direction local : Direction.values()) {
            if (state.connector(local.getAxis().isVertical() ? local : toWorld(local, facing))) {
                renderPort(poseStack, cutout, material.getSerializedName(), local, packedLight, packedOverlay);
            }
        }
        List<String> servos = state.servos();
        if (compressor) {
            for (int slot = 0; slot < servos.size() && slot < PUMP_CENTRES.length; slot++) {
                if (!servos.get(slot).isEmpty()) {
                    renderPump(poseStack, cutout, servos.get(slot), PUMP_CENTRES[slot], packedLight, packedOverlay);
                }
            }
            if (!state.cell().isEmpty()) {
                renderCell(poseStack, cutout, state.cell(), packedLight, packedOverlay);
            }
        }

        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        PoseStack.Pose pose = poseStack.last();
        if (compressor) {
            for (int slot = 0; slot < servos.size() && slot < PUMP_CENTRES.length; slot++) {
                if (!servos.get(slot).isEmpty()) {
                    renderStroke(cutout, pose, servos.get(slot), PUMP_CENTRES[slot], animation.angle + slot * 90.0F, packedLight,
                            packedOverlay);
                }
            }
            if (!state.connector(Direction.UP)) {
                renderImpeller(cutout, poseStack, animation.angle * IMPELLER_SPEED, packedLight, packedOverlay);
            }
            float pulse = NEEDLE_PULSE * animation.spin * Mth.sin((float) Math.toRadians(animation.angle * 4.0F));
            renderNeedle(cutout, pose, animation.compressed + pulse, packedLight, packedOverlay);
        }

        Fluid fluid = fluid(state.fluid());
        if (fluid != Fluids.EMPTY) {
            IClientFluidTypeExtensions fluidType = IClientFluidTypeExtensions.of(fluid);
            TextureAtlasSprite still = sprite(fluidType.getStillTexture());
            int tint = fluidType.getTintColor() & 0xFFFFFF;
            VertexConsumer translucent = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
            if (compressor) {
                renderTubes(translucent, pose, still, tint, animation.loose, packedLight, packedOverlay);
                renderCompressed(translucent, pose, still, tint, animation.compressed, packedLight, packedOverlay);
                renderBubbles(translucent, pose, animation.compressed, animation.spin, time, packedLight, packedOverlay);
            } else if (animation.loose > 0.002F) {
                float top = FLOOR + (FLUID_TANK_TOP - FLOOR) * Mth.clamp(animation.loose, 0.0F, 1.0F);
                box(translucent, pose, still, FLUID_LO, FLOOR, FLUID_LO, FLUID_HI, top, FLUID_HI, OPEN_BOTTOM, tint, LOOSE_ALPHA,
                        packedLight, packedOverlay);
            }
        }
        poseStack.popPose();
    }

    /** Levels and the gauge glide to each update; the pumps run up while the tank compresses and coast down after. */
    private static void animate(CompressorTankVisualState.Animation animation, CompressorTankVisualState state, boolean active, float time) {
        float elapsed = Double.isNaN(animation.lastTime) ? 0.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        float compressed = Math.max(0.0F, state.compressed());
        if (Float.isNaN(animation.loose)) {
            animation.loose = state.loose();
            animation.compressed = compressed;
        }
        float ease = Math.min(1.0F, elapsed * 0.15F);
        animation.loose += (state.loose() - animation.loose) * ease;
        animation.compressed += (compressed - animation.compressed) * ease;
        float spin = active ? 1.0F : 0.0F;
        float spinRate = spin > animation.spin ? 0.08F : 0.03F;
        animation.spin += (spin - animation.spin) * Math.min(1.0F, elapsed * spinRate);
        animation.angle = (animation.angle + animation.spin * DEGREES_PER_TICK * elapsed) % 360.0F;
    }

    private static Fluid fluid(String id) {
        ResourceLocation location = id.isEmpty() ? null : ResourceLocation.tryParse(id);
        return location == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(location);
    }

    /** The loose fluid stands at the same level in all four feed tubes. */
    private static void renderTubes(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, int tint, float fill, int light, int overlay
    ) {
        if (fill <= 0.002F) {
            return;
        }
        float top = TUBE_Y0 + (TUBE_Y1 - TUBE_Y0) * Mth.clamp(fill, 0.0F, 1.0F);
        for (float[] tube : TUBES) {
            box(consumer, pose, sprite, tube[0], TUBE_Y0, tube[1], tube[0] + TUBE_WIDTH, top, tube[1] + TUBE_WIDTH, OPEN_BOTTOM,
                    tint, LOOSE_ALPHA, light, overlay);
        }
    }

    /** The compressed fill on the vessel's gauge glass, from the bottom of the window. */
    private static void renderCompressed(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, int tint, float fill, int light, int overlay
    ) {
        if (fill <= 0.002F) {
            return;
        }
        float top = WINDOW_Y0 + (WINDOW_Y1 - WINDOW_Y0) * Mth.clamp(fill, 0.0F, 1.0F);
        box(consumer, pose, sprite, WINDOW_X0, WINDOW_Y0, WINDOW_Z, WINDOW_X1, top, WINDOW_Z, NORTH, scale(tint, COMPRESSED_SHADE), 255,
                light, overlay);
    }

    /** Bubbles rise through the compressed fill while the pumps run, fading in and out with them. */
    private static void renderBubbles(
            VertexConsumer consumer, PoseStack.Pose pose, float fill, float spin, float time, int light, int overlay
    ) {
        float top = WINDOW_Y0 + (WINDOW_Y1 - WINDOW_Y0) * Mth.clamp(fill, 0.0F, 1.0F);
        if (spin < 0.05F || top - WINDOW_Y0 < 1.0F) {
            return;
        }
        TextureAtlasSprite sprite = sprite(HUB);
        int alpha = Math.round(170 * spin);
        float span = top - WINDOW_Y0 - BUBBLE_SIZE;
        for (int i = 0; i < BUBBLE_X.length; i++) {
            float phase = (time * BUBBLE_RISE_PER_TICK * (1.0F + 0.3F * i) + i / 3.0F) % 1.0F;
            float y = WINDOW_Y0 + phase * span;
            float x = BUBBLE_X[i];
            box(consumer, pose, sprite, x - BUBBLE_SIZE / 2, y, BUBBLE_Z, x + BUBBLE_SIZE / 2, y + BUBBLE_SIZE, BUBBLE_Z, NORTH, 0xFFFFFF,
                    alpha, light, overlay);
        }
    }

    /** The gauge reads the compressed fill: rest at the lower left, the red line at full, sweeping clockwise. */
    private static void renderNeedle(VertexConsumer consumer, PoseStack.Pose pose, float fill, int light, int overlay) {
        float fraction = Mth.clamp(fill, 0.0F, 1.0F);
        // The dial is seen from the north, where +x is the viewer's left.
        float angle = (float) Math.toRadians(225.0F - 270.0F * fraction);
        float dx = -Mth.cos(angle);
        float dy = Mth.sin(angle);
        float px = -dy * NEEDLE_WIDTH;
        float py = dx * NEEDLE_WIDTH;
        float tipX = DIAL_X + dx * NEEDLE_LENGTH;
        float tipY = DIAL_Y + dy * NEEDLE_LENGTH;
        TextureAtlasSprite sprite = sprite(HUB);
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), NEEDLE_COLOR, 255, light, overlay,
                new float[] {DIAL_X - px, DIAL_Y - py, DIAL_Z, sprite.getU0(), sprite.getV0()},
                new float[] {DIAL_X + px, DIAL_Y + py, DIAL_Z, sprite.getU1(), sprite.getV0()},
                new float[] {tipX + px, tipY + py, DIAL_Z, sprite.getU1(), sprite.getV1()},
                new float[] {tipX - px, tipY - py, DIAL_Z, sprite.getU0(), sprite.getV1()});
    }

    private void renderPump(PoseStack poseStack, VertexConsumer consumer, String key, float[] centre, int light, int overlay) {
        ModelResourceLocation location = SERVO_MODELS.getOrDefault(key, SERVO_MODELS.get("steel"));
        poseStack.pushPose();
        poseStack.translate((centre[0] - 8.0F) / 16.0F, PUMP_BASE / 16.0F, (centre[1] - 8.0F) / 16.0F);
        renderModel(poseStack, consumer, location, light, overlay);
        poseStack.popPose();
    }

    /** The piston rod and crosshead ride the crank: bottom of the stroke at angle 0, top at 180. */
    private static void renderStroke(
            VertexConsumer consumer, PoseStack.Pose pose, String servo, float[] centre, float angle, int light, int overlay
    ) {
        float lift = STROKE * 0.5F * (1.0F - Mth.cos((float) Math.toRadians(angle)));
        float y0 = CROSSHEAD_Y0 + lift;
        float x = centre[0];
        float z = centre[1];
        box(consumer, pose, sprite(ROD), x - ROD_HALF, PUMP_HEAD, z - ROD_HALF, x + ROD_HALF, y0, z + ROD_HALF, NORTH | SOUTH | WEST | EAST,
                0xFFFFFF, 255, light, overlay);
        box(consumer, pose, sprite(accent(servo)), x - CROSSHEAD_HALF, y0, z - CROSSHEAD_HALF, x + CROSSHEAD_HALF, y0 + CROSSHEAD_H,
                z + CROSSHEAD_HALF, OPEN_BOTTOM | DOWN, 0xFFFFFF, 255, light, overlay);
    }

    /** Four blades turning on the vessel head, around a hub. */
    private static void renderImpeller(VertexConsumer consumer, PoseStack poseStack, float angle, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(8.0F, 0.0F, 8.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        PoseStack.Pose pose = poseStack.last();
        TextureAtlasSprite blade = sprite(BLADE);
        box(consumer, pose, blade, -BLADE_LENGTH, IMPELLER_Y0, -BLADE_HALF, BLADE_LENGTH, IMPELLER_Y1, BLADE_HALF, OPEN_BOTTOM, 0xFFFFFF, 255,
                light, overlay);
        box(consumer, pose, blade, -BLADE_HALF, IMPELLER_Y0, -BLADE_LENGTH, BLADE_HALF, IMPELLER_Y1, BLADE_LENGTH, OPEN_BOTTOM, 0xFFFFFF, 255,
                light, overlay);
        box(consumer, pose, sprite(HUB), -HUB_HALF, IMPELLER_Y0, -HUB_HALF, HUB_HALF, HUB_TOP, HUB_HALF, OPEN_BOTTOM, 0xFFFFFF, 255,
                light, overlay);
        poseStack.popPose();
    }

    private static ResourceLocation accent(String servo) {
        return RNGTech.id("block/compressor_tank/servo/" + (SERVO_MODELS.containsKey(servo) ? servo : "steel") + "_accent");
    }

    /** The cell lies on its side across the cradle, its axis along x. */
    private void renderCell(PoseStack poseStack, VertexConsumer consumer, String kind, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(CELL_X / 16.0F, CELL_Y / 16.0F, CELL_Z / 16.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.scale(CELL_SCALE, CELL_SCALE, CELL_SCALE);
        poseStack.translate(-0.5F, -CELL_HALF_LENGTH / 16.0F, -0.5F);
        renderModel(poseStack, consumer, cellModel(kind), light, overlay);
        poseStack.popPose();
    }

    /** A port model built on the north face, turned to {@code local}; top and bottom use the hatch variant. */
    private void renderPort(PoseStack poseStack, VertexConsumer consumer, String metal, Direction local, int light, int overlay) {
        boolean vertical = local.getAxis().isVertical();
        ModelResourceLocation location = PORT_MODELS.get((vertical ? "port_flat_" : "port_") + metal);
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

    /** A box with the chosen faces, each showing 16 model pixels per full sprite, cropped to the face size. */
    private static void box(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
            float x0, float y0, float z0, float x1, float y1, float z1, int faces,
            int color, int alpha, int light, int overlay
    ) {
        float u0 = sprite.getU0();
        float v0 = sprite.getV0();
        float uw = sprite.getU1() - u0;
        float vh = sprite.getV1() - v0;
        float ux = u0 + uw * Math.min(1.0F, (x1 - x0) / 16.0F);
        float uz = u0 + uw * Math.min(1.0F, (z1 - z0) / 16.0F);
        float vy = v0 + vh * Math.min(1.0F, (y1 - y0) / 16.0F);
        float vz = v0 + vh * Math.min(1.0F, (z1 - z0) / 16.0F);
        if ((faces & NORTH) != 0) {
            quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), color, alpha, light, overlay,
                    new float[] {x0, y0, z0, u0, vy}, new float[] {x1, y0, z0, ux, vy}, new float[] {x1, y1, z0, ux, v0}, new float[] {x0, y1, z0, u0, v0});
        }
        if ((faces & SOUTH) != 0) {
            quad(consumer, pose, new Vector3f(0.0F, 0.0F, 1.0F), color, alpha, light, overlay,
                    new float[] {x0, y0, z1, u0, vy}, new float[] {x1, y0, z1, ux, vy}, new float[] {x1, y1, z1, ux, v0}, new float[] {x0, y1, z1, u0, v0});
        }
        if ((faces & WEST) != 0) {
            quad(consumer, pose, new Vector3f(-1.0F, 0.0F, 0.0F), color, alpha, light, overlay,
                    new float[] {x0, y0, z0, u0, vy}, new float[] {x0, y0, z1, uz, vy}, new float[] {x0, y1, z1, uz, v0}, new float[] {x0, y1, z0, u0, v0});
        }
        if ((faces & EAST) != 0) {
            quad(consumer, pose, new Vector3f(1.0F, 0.0F, 0.0F), color, alpha, light, overlay,
                    new float[] {x1, y0, z0, u0, vy}, new float[] {x1, y0, z1, uz, vy}, new float[] {x1, y1, z1, uz, v0}, new float[] {x1, y1, z0, u0, v0});
        }
        if ((faces & UP) != 0) {
            quad(consumer, pose, new Vector3f(0.0F, 1.0F, 0.0F), color, alpha, light, overlay,
                    new float[] {x0, y1, z0, u0, v0}, new float[] {x1, y1, z0, ux, v0}, new float[] {x1, y1, z1, ux, vz}, new float[] {x0, y1, z1, u0, vz});
        }
        if ((faces & DOWN) != 0) {
            quad(consumer, pose, new Vector3f(0.0F, -1.0F, 0.0F), color, alpha, light, overlay,
                    new float[] {x0, y0, z0, u0, v0}, new float[] {x1, y0, z0, ux, v0}, new float[] {x1, y0, z1, ux, vz}, new float[] {x0, y0, z1, u0, vz});
        }
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

    private static int scale(int color, float factor) {
        int red = Math.round((color >> 16 & 0xFF) * factor);
        int green = Math.round((color >> 8 & 0xFF) * factor);
        int blue = Math.round((color & 0xFF) * factor);
        return red << 16 | green << 8 | blue;
    }
}
