package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.CombustorVisualState;
import com.rngtech.content.blockentity.GasChemistryBlockEntity;
import com.rngtech.content.chemistry.GasChemistryMachine;
import com.rngtech.content.energy.CombustionMixture;
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
 * Draws the working parts of the Syngas Combustor: the flame behind the view ports (orange on Syngas, blue on Carbon
 * Monoxide; bluer when lean and yellower when rich), the glow in the stack mouth, the shaft and coupling spinning with
 * the generated power, both sight-glass levels, the tachometer needle, the installed Servo trimming the fuel, and the
 * installed Battery Cell with its charge. GasChemistryRenderer routes only combustors here. Positions are in model pixels
 * and match tools/blockmodels/syngas_combustor.py.
 */
public class SyngasCombustorRenderer implements BlockEntityRenderer<GasChemistryBlockEntity> {
    private static final ResourceLocation FLAME = RNGTech.id("textures/block/syngas_combustor/fx/flame.png");
    private static final ResourceLocation GLOW = RNGTech.id("textures/block/syngas_combustor/fx/glow.png");
    private static final ResourceLocation COUPLING = RNGTech.id("block/syngas_combustor/coupling");
    private static final ResourceLocation SHAFT = RNGTech.id("block/syngas_combustor/flange");
    private static final ResourceLocation BOLT = RNGTech.id("block/syngas_combustor/bolt");
    private static final ResourceLocation SERVO = RNGTech.id("block/syngas_combustor/fx/servo");
    private static final ResourceLocation HORN = RNGTech.id("block/syngas_combustor/fx/horn");
    private static final ResourceLocation CELL = RNGTech.id("block/syngas_combustor/fx/cell");
    private static final ResourceLocation CHARGE = RNGTech.id("block/syngas_combustor/fx/charge");
    private static final float PORT_X0 = 7.0F;
    private static final float PORT_X1 = 9.0F;
    private static final float[][] WINDOWS = {{5.5F, 7.25F}, {7.75F, 9.5F}};
    /** Between the liner (z 1.2) and the glass (z 0.95) on the can's front face. */
    private static final float FLAME_Z = 1.1F;
    private static final float STACK_X0 = 6.5F;
    private static final float STACK_X1 = 9.5F;
    private static final float STACK_Z0 = 7.75F;
    private static final float STACK_Z1 = 10.5F;
    private static final float STACK_TOP = 15.27F;
    private static final float AXIS_X = 8.0F;
    private static final float AXIS_Y = 8.0F;
    private static final float SHAFT_Z0 = 10.5F;
    private static final float SHAFT_Z1 = 12.0F;
    private static final float SHAFT_R = 0.6F;
    private static final float COUPLING_R = 1.9F;
    private static final float COUPLING_Z0 = 10.9F;
    private static final float COUPLING_Z1 = 11.6F;
    /** Shaft degrees per tick at a processing speed of 1. */
    private static final float DEGREES_PER_TICK = 18.0F;
    private static final float FUEL_X = 0.75F;
    private static final float FUEL_Z0 = 1.5F;
    private static final float FUEL_Z1 = 4.0F;
    private static final float EXHAUST_X = 13.25F;
    private static final float EXHAUST_Z0 = 8.0F;
    private static final float EXHAUST_Z1 = 10.5F;
    private static final float SIGHT_Y0 = 3.5F;
    private static final float SIGHT_Y1 = 11.5F;
    private static final float[] SERVO_FROM = {2.25F, 5.5F, 8.0F};
    private static final float[] SERVO_TO = {4.75F, 8.5F, 10.0F};
    private static final float[] CELL_FROM = {12.75F, 2.5F, 12.0F};
    private static final float[] CELL_TO = {15.25F, 7.5F, 14.5F};
    private static final float DIAL_X = 14.0F;
    private static final float DIAL_Y = 3.25F;
    private static final float DIAL_Z = 0.24F;
    /** The tachometer reads full scale at this processing speed. */
    private static final float TACHOMETER_MAX = 2.0F;
    private static final int SYNGAS_FLAME = 0xFF9A3C;
    private static final int MONOXIDE_FLAME = 0x5A8CFF;
    private static final int LEAN_FLAME = 0x8CC8FF;
    private static final int RICH_FLAME = 0xFFD25A;
    /** How far the flame colour moves toward the lean or rich tint at the ends of the mixture range. */
    private static final float MIXTURE_TINT = 0.65F;
    private static final int NEEDLE = 0xC43428;
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

    public SyngasCombustorRenderer(BlockEntityRendererProvider.Context context) {
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
        if (blockEntity.machine() != GasChemistryMachine.SYNGAS_COMBUSTOR || blockEntity.getLevel() == null) {
            return;
        }
        CombustorVisualState state = blockEntity.visualState();
        CombustorVisualState.Animation animation = blockEntity.animation();
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        animate(animation, state, time);

        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);

        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        renderShaft(cutout, poseStack, animation.angle, packedLight, packedOverlay);
        renderServo(cutout, poseStack, state.servo(), animation, time, packedLight, packedOverlay);
        renderCell(cutout, poseStack.last(), state.cell(), state.charge(), packedLight, packedOverlay);
        renderNeedle(cutout, poseStack.last(), animation.spin, packedLight, packedOverlay);

        VertexConsumer translucent = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
        renderTank(translucent, poseStack.last(), state.fuelFluid(), state.fuel(), FUEL_X, FUEL_Z0, FUEL_Z1, packedLight, packedOverlay);
        renderTank(translucent, poseStack.last(), state.exhaustFluid(), state.exhaust(), EXHAUST_X, EXHAUST_Z0, EXHAUST_Z1, packedLight, packedOverlay);

        if (animation.flame > 0.01F) {
            int flame = flameColor(state.fuelFluid() == ModFluids.CARBON_MONOXIDE_SOURCE.get() ? MONOXIDE_FLAME : SYNGAS_FLAME, state.mixture());
            renderFlame(bufferSource.getBuffer(RenderType.eyes(FLAME)), poseStack.last(), flame, animation.flame, time, packedOverlay);
            renderStackGlow(bufferSource.getBuffer(RenderType.eyes(GLOW)), poseStack.last(), animation.flame, packedOverlay);
        }
        poseStack.popPose();
    }

    /** The flame lights and fades over a second; the shaft spins up with it and coasts down after it goes out. */
    private static void animate(CombustorVisualState.Animation animation, CombustorVisualState state, float time) {
        float elapsed = Double.isNaN(animation.lastTime) ? 0.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        float flame = state.firing() ? 1.0F : 0.0F;
        animation.flame += (flame - animation.flame) * Math.min(1.0F, elapsed * 0.12F);
        float spin = state.firing() ? Math.max(0.0F, state.speed()) * (float) CombustionMixture.power(state.mixture()) : 0.0F;
        float rate = spin > animation.spin ? 0.06F : 0.02F;
        animation.spin += (spin - animation.spin) * Math.min(1.0F, elapsed * rate);
        animation.angle = (animation.angle + animation.spin * DEGREES_PER_TICK * elapsed) % 360.0F;
    }

    /** A lean flame burns bluer and a rich one yellower and sootier. */
    private static int flameColor(int base, int mixture) {
        int rated = CombustionMixture.RATED_MIXTURE;
        if (mixture < rated) {
            float lean = (rated - mixture) / (float) (rated - CombustionMixture.MIN_MIXTURE);
            return lerpColor(Mth.clamp(lean, 0.0F, 1.0F) * MIXTURE_TINT, base, LEAN_FLAME);
        }
        float rich = (mixture - rated) / (float) (CombustionMixture.MAX_MIXTURE - rated);
        return lerpColor(Mth.clamp(rich, 0.0F, 1.0F) * MIXTURE_TINT, base, RICH_FLAME);
    }

    /** Each view port flickers on its own, like a turbulent flame seen through two windows. */
    private static void renderFlame(VertexConsumer consumer, PoseStack.Pose pose, int color, float strength, float time, int overlay) {
        Vector3f north = new Vector3f(0.0F, 0.0F, -1.0F);
        for (int i = 0; i < WINDOWS.length; i++) {
            float flicker = 0.82F + 0.18F * Mth.sin(time * 0.9F + i * 2.1F) * Mth.sin(time * 2.3F + i);
            int light = scale(color, strength * flicker);
            float y0 = WINDOWS[i][0];
            float y1 = WINDOWS[i][1];
            float v0 = i * 0.5F;
            float v1 = v0 + 0.5F;
            quad(consumer, pose, north, light, 255, LightTexture.FULL_BRIGHT, overlay,
                    new float[] {PORT_X1, y0, FLAME_Z, 0.0F, v1},
                    new float[] {PORT_X0, y0, FLAME_Z, 1.0F, v1},
                    new float[] {PORT_X0, y1, FLAME_Z, 1.0F, v0},
                    new float[] {PORT_X1, y1, FLAME_Z, 0.0F, v0});
        }
    }

    private static void renderStackGlow(VertexConsumer consumer, PoseStack.Pose pose, float strength, int overlay) {
        quad(consumer, pose, new Vector3f(0.0F, 1.0F, 0.0F), scale(0xFFFFFF, strength * 0.8F), 255, LightTexture.FULL_BRIGHT, overlay,
                new float[] {STACK_X0, STACK_TOP, STACK_Z0, 0.0F, 0.0F},
                new float[] {STACK_X0, STACK_TOP, STACK_Z1, 0.0F, 1.0F},
                new float[] {STACK_X1, STACK_TOP, STACK_Z1, 1.0F, 1.0F},
                new float[] {STACK_X1, STACK_TOP, STACK_Z0, 1.0F, 0.0F});
    }

    /** The shaft and its flanged coupling between turbine and alternator, with four bolt heads to show the turn. */
    private static void renderShaft(VertexConsumer consumer, PoseStack poseStack, float angle, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(AXIS_X, AXIS_Y, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
        PoseStack.Pose pose = poseStack.last();
        cube(consumer, pose, sprite(SHAFT), 16.0F, -SHAFT_R, -SHAFT_R, SHAFT_Z0, SHAFT_R, SHAFT_R, SHAFT_Z1, 0xFFFFFF, 255, light, overlay);
        TextureAtlasSprite coupling = sprite(COUPLING);
        cube(consumer, pose, coupling, 16.0F, -COUPLING_R, -COUPLING_R, COUPLING_Z0, COUPLING_R, COUPLING_R, COUPLING_Z1, 0xFFFFFF, 255, light, overlay);
        TextureAtlasSprite bolt = sprite(BOLT);
        for (int i = 0; i < 4; i++) {
            float a = (float) Math.toRadians(90.0 * i);
            float x = Mth.cos(a) * 1.25F;
            float y = Mth.sin(a) * 1.25F;
            cube(consumer, pose, bolt, 16.0F, x - 0.3F, y - 0.3F, COUPLING_Z0 - 0.2F, x + 0.3F, y + 0.3F, COUPLING_Z1 + 0.2F, 0xFFFFFF, 255, light, overlay);
        }
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        cube(consumer, poseStack.last(), coupling, 16.0F, -COUPLING_R, -COUPLING_R, COUPLING_Z0 + 0.01F, COUPLING_R, COUPLING_R, COUPLING_Z1 - 0.01F,
                0xFFFFFF, 255, light, overlay);
        poseStack.popPose();
    }

    /** The Servo is the fuel governor: its horn trims back and forth while the combustor fires. */
    private static void renderServo(
            VertexConsumer consumer,
            PoseStack poseStack,
            String servo,
            CombustorVisualState.Animation animation,
            float time,
            int light,
            int overlay
    ) {
        if (servo.isEmpty()) {
            return;
        }
        int color = SERVO_COLORS.getOrDefault(servo, 0xFFFFFF);
        cube(consumer, poseStack.last(), sprite(SERVO), 3.0F, SERVO_FROM[0], SERVO_FROM[1], SERVO_FROM[2], SERVO_TO[0], SERVO_TO[1], SERVO_TO[2],
                color, 255, light, overlay);
        float centerY = (SERVO_FROM[1] + SERVO_TO[1]) / 2.0F;
        float centerZ = (SERVO_FROM[2] + SERVO_TO[2]) / 2.0F;
        float swing = 28.0F * Mth.sin(time * 0.11F) * Mth.clamp(animation.flame, 0.0F, 1.0F);
        poseStack.pushPose();
        poseStack.translate(SERVO_FROM[0], centerY, centerZ);
        poseStack.mulPose(Axis.XP.rotationDegrees(swing));
        TextureAtlasSprite horn = sprite(HORN);
        PoseStack.Pose pose = poseStack.last();
        cube(consumer, pose, horn, 16.0F, -0.35F, -0.6F, -0.6F, 0.0F, 0.6F, 0.6F, 0xFFFFFF, 255, light, overlay);
        cube(consumer, pose, horn, 16.0F, -0.3F, -0.3F, -1.3F, -0.05F, 0.3F, 1.3F, 0xFFFFFF, 255, light, overlay);
        poseStack.popPose();
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
        float z0 = 12.75F;
        float z1 = 13.75F;
        float v1 = sprite.getV1();
        float v0 = Mth.lerp(fill, v1, sprite.getV0());
        quad(consumer, pose, new Vector3f(1.0F, 0.0F, 0.0F), 0xFFFFFF, 255, LightTexture.FULL_BRIGHT, overlay,
                new float[] {x, y0, z0, sprite.getU0(), v1},
                new float[] {x, y0, z1, sprite.getU1(), v1},
                new float[] {x, y1, z1, sprite.getU1(), v0},
                new float[] {x, y1, z0, sprite.getU0(), v0});
    }

    /** The tachometer reads shaft speed: rest at the bottom left, full scale at the red line. */
    private static void renderNeedle(VertexConsumer consumer, PoseStack.Pose pose, float spin, int light, int overlay) {
        float fraction = Mth.clamp(spin / TACHOMETER_MAX, 0.0F, 1.0F);
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

    private static void renderTank(
            VertexConsumer consumer, PoseStack.Pose pose, Fluid fluid, float fill, float x, float z0, float z1, int light, int overlay
    ) {
        if (fluid == Fluids.EMPTY || fill <= 0.001F) {
            return;
        }
        IClientFluidTypeExtensions fluidType = IClientFluidTypeExtensions.of(fluid);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidType.getStillTexture());
        int tint = fluidType.getTintColor();
        float y1 = SIGHT_Y0 + (SIGHT_Y1 - SIGHT_Y0) * Mth.clamp(fill, 0.0F, 1.0F);
        cube(consumer, pose, sprite, 16.0F, x + 0.15F, SIGHT_Y0, z0 + 0.15F, x + 1.85F, y1, z1 - 0.15F, tint & 0xFFFFFF, 220, light, overlay);
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

    private static int lerpColor(float t, int from, int to) {
        int red = Math.round(Mth.lerp(t, from >> 16 & 0xFF, to >> 16 & 0xFF));
        int green = Math.round(Mth.lerp(t, from >> 8 & 0xFF, to >> 8 & 0xFF));
        int blue = Math.round(Mth.lerp(t, from & 0xFF, to & 0xFF));
        return red << 16 | green << 8 | blue;
    }

    private static int scale(int color, float factor) {
        float f = Mth.clamp(factor, 0.0F, 1.0F);
        int red = Math.round((color >> 16 & 0xFF) * f);
        int green = Math.round((color >> 8 & 0xFF) * f);
        int blue = Math.round((color & 0xFF) * f);
        return red << 16 | green << 8 | blue;
    }
}
