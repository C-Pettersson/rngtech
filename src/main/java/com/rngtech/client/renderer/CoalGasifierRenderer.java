package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.GasChemistryBlockEntity;
import com.rngtech.content.blockentity.GasProcessorVisualState;
import com.rngtech.content.chemistry.GasChemistryMachine;
import com.rngtech.content.registry.ModFluids;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Vector3f;

import java.util.Map;

/**
 * Draws the working parts of the Coal Gasifier: the glowing bed behind the view port, the Coal Dust in the feed hopper,
 * the rotary feed valve's hub turning with the processing speed, the installed Servo driving it, the installed Heat Core
 * in its socket, the water and Syngas sight-glass levels, the Gasification Residue in the bin, the pressure gauge
 * needle, and the installed Battery Cell with its charge. GasChemistryRenderer routes only gasifiers here. Positions
 * are in model pixels and match tools/blockmodels/coal_gasifier.py.
 */
public class CoalGasifierRenderer implements BlockEntityRenderer<GasChemistryBlockEntity> {
    private static final ResourceLocation GLOW = RNGTech.id("textures/block/coal_gasifier/fx/glow.png");
    private static final ResourceLocation COAL = RNGTech.id("block/coal_gasifier/fx/coal");
    private static final ResourceLocation RESIDUE = RNGTech.id("block/coal_gasifier/fx/residue");
    private static final ResourceLocation COUPLING = RNGTech.id("block/coal_gasifier/coupling");
    private static final ResourceLocation BOLT = RNGTech.id("block/coal_gasifier/bolt");
    private static final ResourceLocation CORE = RNGTech.id("block/coal_gasifier/fx/core");
    private static final ResourceLocation SERVO = RNGTech.id("block/coal_gasifier/fx/servo");
    private static final ResourceLocation CELL = RNGTech.id("block/coal_gasifier/fx/cell");
    private static final ResourceLocation CHARGE = RNGTech.id("block/coal_gasifier/fx/charge");
    private static final float PORT_X0 = 7.0F;
    private static final float PORT_X1 = 9.0F;
    private static final float PORT_Y0 = 7.5F;
    private static final float PORT_Y1 = 9.5F;
    /** Between the liner (z 4.95) and the glass (z 4.7) on the vessel's front face. */
    private static final float GLOW_Z = 4.85F;
    private static final float HOPPER_X0 = 5.5F;
    private static final float HOPPER_X1 = 10.5F;
    private static final float HOPPER_Z0 = 6.0F;
    private static final float HOPPER_Z1 = 11.0F;
    private static final float HOPPER_FLOOR = 14.5F;
    private static final float HOPPER_DEPTH = 1.25F;
    private static final float BIN_X0 = 5.5F;
    private static final float BIN_X1 = 10.5F;
    private static final float BIN_Z0 = 1.25F;
    private static final float BIN_Z1 = 4.0F;
    private static final float BIN_FLOOR = 2.0F;
    private static final float BIN_LAYER = 1.2F;
    /** The heap under the ash chute stands this much above the layer when the bin is full. */
    private static final float BIN_MOUND = 0.5F;
    private static final float VALVE_Y = 12.875F;
    private static final float VALVE_Z = 8.5F;
    private static final float HUB_X0 = 5.55F;
    private static final float HUB_X1 = 6.5F;
    private static final float HUB_HALF = 0.75F;
    /** Valve degrees per tick at a processing speed of 1. */
    private static final float DEGREES_PER_TICK = 6.0F;
    private static final float[] SERVO_FROM = {3.0F, 11.85F, 7.5F};
    private static final float[] SERVO_TO = {5.5F, 13.9F, 9.5F};
    private static final float[] CORE_FROM = {1.5F, 7.5F, 7.75F};
    private static final float[] CORE_TO = {3.5F, 9.25F, 9.25F};
    private static final float[] FIN_XS = {1.75F, 2.375F, 3.0F};
    private static final float FIN_WIDTH = 0.25F;
    private static final float FIN_Y0 = 6.9F;
    private static final float FIN_Y1 = 9.85F;
    private static final float FIN_Z0 = 7.4F;
    private static final float FIN_Z1 = 9.6F;
    private static final float WATER_X = 0.75F;
    private static final float SYNGAS_X = 13.25F;
    private static final float SIGHT_Z0 = 1.5F;
    private static final float SIGHT_Z1 = 4.0F;
    private static final float SIGHT_Y0 = 3.5F;
    private static final float SIGHT_Y1 = 10.5F;
    private static final float[] CELL_FROM = {12.75F, 2.0F, 12.5F};
    private static final float[] CELL_TO = {15.25F, 7.0F, 15.0F};
    private static final float DIAL_X = 14.25F;
    private static final float DIAL_Y = 12.85F;
    private static final float DIAL_Z = 1.24F;
    /** The pressure gauge reads full scale at this processing speed. */
    private static final float GAUGE_MAX = 2.0F;
    private static final int BED_GLOW = 0xFFB060;
    private static final int NEEDLE = 0xC43428;
    private static final Map<String, Integer> HEAT_CORE_COLORS = Map.of(
            "aluminum", 0xBAC6D6,
            "sparksteel", 0xE0C060,
            "titanium", 0x96A4B4,
            "exotic", 0x34AA84,
            "unique", 0xAF6025
    );
    private static final Map<String, Integer> SERVO_COLORS = Map.of(
            "steel", 0x9AA2A6,
            "titanium", 0xA9B8CC,
            "tungstensteel", 0x5A6470,
            "nullite", 0x6E5F8C,
            "exotic", 0x8A5AA8,
            "unique", 0xAF6025
    );
    private static final Map<String, Integer> CELL_COLORS = Map.ofEntries(
            Map.entry("potato", 0xC9A35A),
            Map.entry("iron", 0xA9A4AE),
            Map.entry("copper", 0xC0704E),
            Map.entry("lead", 0x5E5D72),
            Map.entry("invar", 0x9AA3B8),
            Map.entry("sparksteel", 0xD6D2A0),
            Map.entry("arclite", 0xB84A52),
            Map.entry("nullite", 0x4F6FA8),
            Map.entry("aethergold", 0xD6A640),
            Map.entry("exotic", 0x7E72A8),
            Map.entry("unique", 0xAF6025)
    );

    public CoalGasifierRenderer(BlockEntityRendererProvider.Context context) {
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
        if (blockEntity.machine() != GasChemistryMachine.COAL_GASIFIER || blockEntity.getLevel() == null) {
            return;
        }
        GasProcessorVisualState state = blockEntity.processorVisualState();
        GasProcessorVisualState.Animation animation = blockEntity.processorAnimation();
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        animate(animation, state, time);

        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);

        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        renderCoal(cutout, poseStack.last(), state.feed(), packedLight, packedOverlay);
        renderResidue(cutout, poseStack.last(), state.residue(), packedLight, packedOverlay);
        renderHub(cutout, poseStack, animation.angle, packedLight, packedOverlay);
        renderServo(cutout, poseStack.last(), state.servo(), packedLight, packedOverlay);
        renderHeatCore(cutout, poseStack.last(), state.heatCore(), packedLight, packedOverlay);
        renderCell(cutout, poseStack.last(), state.cell(), state.charge(), packedLight, packedOverlay);
        renderNeedle(cutout, poseStack.last(), gaugeReading(animation, state, time), packedLight, packedOverlay);

        VertexConsumer translucent = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
        renderTank(translucent, poseStack.last(), Fluids.WATER, state.water(), WATER_X, packedLight, packedOverlay);
        renderTank(translucent, poseStack.last(), ModFluids.SYNGAS_SOURCE.get(), state.output(), SYNGAS_X, packedLight, packedOverlay);

        if (animation.heat > 0.01F) {
            renderGlow(bufferSource.getBuffer(RenderType.eyes(GLOW)), poseStack.last(), animation.heat, time, packedOverlay);
        }
        poseStack.popPose();
    }

    /** The bed glows up over a couple of seconds and cools more slowly; the feed valve starts and stops with it. */
    private static void animate(GasProcessorVisualState.Animation animation, GasProcessorVisualState state, float time) {
        float elapsed = Double.isNaN(animation.lastTime) ? 0.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        float heat = state.active() ? 1.0F : 0.0F;
        float heatRate = heat > animation.heat ? 0.04F : 0.015F;
        animation.heat += (heat - animation.heat) * Math.min(1.0F, elapsed * heatRate);
        float spin = state.active() ? Math.max(0.0F, state.speed()) : 0.0F;
        float spinRate = spin > animation.spin ? 0.08F : 0.05F;
        animation.spin += (spin - animation.spin) * Math.min(1.0F, elapsed * spinRate);
        animation.angle = (animation.angle + animation.spin * DEGREES_PER_TICK * elapsed) % 360.0F;
    }

    /** Pressure builds with the bed's heat and the processing speed, with a slight flutter while running. */
    private static float gaugeReading(GasProcessorVisualState.Animation animation, GasProcessorVisualState state, float time) {
        float load = Mth.clamp(Math.max(0.0F, state.speed()) / GAUGE_MAX, 0.25F, 1.0F);
        float flutter = 0.02F * Mth.sin(time * 0.7F) * animation.heat;
        return Mth.clamp(animation.heat * load + flutter, 0.0F, 1.0F);
    }

    /** The bed behind the view port, flickering as the coal burns down. */
    private static void renderGlow(VertexConsumer consumer, PoseStack.Pose pose, float strength, float time, int overlay) {
        float flicker = 0.85F + 0.15F * Mth.sin(time * 0.6F) * Mth.sin(time * 1.7F + 1.3F);
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), scale(BED_GLOW, strength * flicker), 255, LightTexture.FULL_BRIGHT, overlay,
                new float[] {PORT_X1, PORT_Y0, GLOW_Z, 0.0F, 1.0F},
                new float[] {PORT_X0, PORT_Y0, GLOW_Z, 1.0F, 1.0F},
                new float[] {PORT_X0, PORT_Y1, GLOW_Z, 1.0F, 0.0F},
                new float[] {PORT_X1, PORT_Y1, GLOW_Z, 0.0F, 0.0F});
    }

    /** Coal Dust fills the hopper from its floor toward the rim. */
    private static void renderCoal(VertexConsumer consumer, PoseStack.Pose pose, float feed, int light, int overlay) {
        float fill = Mth.clamp(feed, 0.0F, 1.0F);
        if (fill <= 0.001F) {
            return;
        }
        float top = HOPPER_FLOOR + Math.max(0.1F, HOPPER_DEPTH * fill);
        cube(consumer, pose, sprite(COAL), 16.0F, HOPPER_X0, HOPPER_FLOOR, HOPPER_Z0, HOPPER_X1, top, HOPPER_Z1, 0xFFFFFF, 255, light, overlay);
    }

    /** Residue spreads over the bin floor and heaps up under the ash chute. */
    private static void renderResidue(VertexConsumer consumer, PoseStack.Pose pose, float residue, int light, int overlay) {
        float fill = Mth.clamp(residue, 0.0F, 1.0F);
        if (fill <= 0.001F) {
            return;
        }
        TextureAtlasSprite sprite = sprite(RESIDUE);
        float layer = BIN_FLOOR + Math.max(0.1F, BIN_LAYER * fill);
        cube(consumer, pose, sprite, 16.0F, BIN_X0, BIN_FLOOR, BIN_Z0, BIN_X1, layer, BIN_Z1, 0xFFFFFF, 255, light, overlay);
        cube(consumer, pose, sprite, 16.0F, 6.75F, layer - 0.01F, 2.5F, 9.25F, layer + BIN_MOUND * fill, BIN_Z1, 0xFFFFFF, 255, light, overlay);
    }

    /** The rotary feed valve's hub: two squares turned 45 degrees apart and a bolt boss, so the turn reads. */
    private static void renderHub(VertexConsumer consumer, PoseStack poseStack, float angle, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(0.0F, VALVE_Y, VALVE_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(angle));
        TextureAtlasSprite coupling = sprite(COUPLING);
        cube(consumer, poseStack.last(), coupling, 16.0F, HUB_X0, -HUB_HALF, -HUB_HALF, HUB_X1, HUB_HALF, HUB_HALF, 0xFFFFFF, 255, light, overlay);
        TextureAtlasSprite bolt = sprite(BOLT);
        cube(consumer, poseStack.last(), bolt, 16.0F, HUB_X0 - 0.15F, -0.25F, 0.2F, HUB_X0, 0.25F, 0.6F, 0xFFFFFF, 255, light, overlay);
        cube(consumer, poseStack.last(), bolt, 16.0F, HUB_X0 - 0.15F, -0.25F, -0.6F, HUB_X0, 0.25F, -0.2F, 0xFFFFFF, 255, light, overlay);
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        cube(consumer, poseStack.last(), coupling, 16.0F, HUB_X0 + 0.01F, -HUB_HALF, -HUB_HALF, HUB_X1 - 0.01F, HUB_HALF, HUB_HALF,
                0xFFFFFF, 255, light, overlay);
        poseStack.popPose();
    }

    /** The Servo is the valve drive, bolted to the bracket against the hub. */
    private static void renderServo(VertexConsumer consumer, PoseStack.Pose pose, String servo, int light, int overlay) {
        if (servo.isEmpty()) {
            return;
        }
        int color = SERVO_COLORS.getOrDefault(servo, 0xFFFFFF);
        cube(consumer, pose, sprite(SERVO), 3.0F, SERVO_FROM[0], SERVO_FROM[1], SERVO_FROM[2], SERVO_TO[0], SERVO_TO[1], SERVO_TO[2],
                color, 255, light, overlay);
    }

    /** The Heat Core is the igniter: a finned element in the socket on the vessel's side. */
    private static void renderHeatCore(VertexConsumer consumer, PoseStack.Pose pose, String heatCore, int light, int overlay) {
        if (heatCore.isEmpty()) {
            return;
        }
        int color = HEAT_CORE_COLORS.getOrDefault(heatCore, 0xFFFFFF);
        TextureAtlasSprite sprite = sprite(CORE);
        cube(consumer, pose, sprite, 3.0F, CORE_FROM[0], CORE_FROM[1], CORE_FROM[2], CORE_TO[0], CORE_TO[1], CORE_TO[2], color, 255, light, overlay);
        for (float x : FIN_XS) {
            cube(consumer, pose, sprite, 3.0F, x, FIN_Y0, FIN_Z0, x + FIN_WIDTH, FIN_Y1, FIN_Z1, color, 255, light, overlay);
        }
        cube(consumer, pose, sprite(BOLT), 16.0F, CORE_FROM[0] - 0.35F, 8.0F, 8.1F, CORE_FROM[0], 8.75F, 8.9F, 0xFFFFFF, 255, light, overlay);
    }

    /** The Battery Cell in its cradle, with a charge window on its outer face. */
    private static void renderCell(VertexConsumer consumer, PoseStack.Pose pose, String cell, float charge, int light, int overlay) {
        if (cell.isEmpty()) {
            return;
        }
        int color = CELL_COLORS.getOrDefault(cell, 0xFFFFFF);
        cube(consumer, pose, sprite(CELL), 5.0F, CELL_FROM[0], CELL_FROM[1], CELL_FROM[2], CELL_TO[0], CELL_TO[1], CELL_TO[2], color, 255, light, overlay);
        float fill = Mth.clamp(charge, 0.0F, 1.0F);
        if (fill <= 0.01F) {
            return;
        }
        TextureAtlasSprite sprite = sprite(CHARGE);
        float y0 = CELL_FROM[1] + 0.75F;
        float y1 = y0 + (CELL_TO[1] - CELL_FROM[1] - 1.5F) * fill;
        float x = CELL_TO[0] + 0.01F;
        float z0 = 13.25F;
        float z1 = 14.25F;
        float v1 = sprite.getV1();
        float v0 = Mth.lerp(fill, v1, sprite.getV0());
        quad(consumer, pose, new Vector3f(1.0F, 0.0F, 0.0F), 0xFFFFFF, 255, LightTexture.FULL_BRIGHT, overlay,
                new float[] {x, y0, z0, sprite.getU0(), v1},
                new float[] {x, y0, z1, sprite.getU1(), v1},
                new float[] {x, y1, z1, sprite.getU1(), v0},
                new float[] {x, y1, z0, sprite.getU0(), v0});
    }

    /** The pressure gauge: rest at the bottom left, full scale at the red line. */
    private static void renderNeedle(VertexConsumer consumer, PoseStack.Pose pose, float fraction, int light, int overlay) {
        // The dial face is seen from the north, so the scale sweeps clockwise from the viewer's lower left.
        float angle = (float) Math.toRadians(225.0F - 270.0F * fraction);
        float dx = -Mth.cos(angle);
        float dy = Mth.sin(angle);
        float length = 0.95F;
        float width = 0.09F;
        float px = -dy * width;
        float py = dx * width;
        TextureAtlasSprite sprite = sprite(BOLT);
        float tipX = DIAL_X + dx * length;
        float tipY = DIAL_Y + dy * length;
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), NEEDLE, 255, light, overlay,
                new float[] {DIAL_X - px, DIAL_Y - py, DIAL_Z, sprite.getU0(), sprite.getV0()},
                new float[] {DIAL_X + px, DIAL_Y + py, DIAL_Z, sprite.getU1(), sprite.getV0()},
                new float[] {tipX + px, tipY + py, DIAL_Z, sprite.getU1(), sprite.getV1()},
                new float[] {tipX - px, tipY - py, DIAL_Z, sprite.getU0(), sprite.getV1()});
    }

    private static void renderTank(VertexConsumer consumer, PoseStack.Pose pose, Fluid fluid, float fill, float x, int light, int overlay) {
        if (fluid == Fluids.EMPTY || fill <= 0.001F) {
            return;
        }
        IClientFluidTypeExtensions fluidType = IClientFluidTypeExtensions.of(fluid);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidType.getStillTexture());
        int tint = fluidType.getTintColor();
        float y1 = SIGHT_Y0 + (SIGHT_Y1 - SIGHT_Y0) * Mth.clamp(fill, 0.0F, 1.0F);
        cube(consumer, pose, sprite, 16.0F, x + 0.15F, SIGHT_Y0, SIGHT_Z0 + 0.15F, x + 1.85F, y1, SIGHT_Z1 - 0.15F, tint & 0xFFFFFF, 220, light, overlay);
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
        float ux = u0 + uw * Math.min(1.0F, (x1 - x0) / span);
        float uz = u0 + uw * Math.min(1.0F, (z1 - z0) / span);
        float vy = v0 + vh * Math.min(1.0F, (y1 - y0) / span);
        float vz = v0 + vh * Math.min(1.0F, (z1 - z0) / span);
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

    private static int scale(int color, float factor) {
        float f = Mth.clamp(factor, 0.0F, 1.0F);
        int red = Math.round((color >> 16 & 0xFF) * f);
        int green = Math.round((color >> 8 & 0xFF) * f);
        int blue = Math.round((color & 0xFF) * f);
        return red << 16 | green << 8 | blue;
    }
}
