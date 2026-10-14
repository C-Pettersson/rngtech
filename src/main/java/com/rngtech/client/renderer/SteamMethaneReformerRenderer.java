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
 * Draws the working parts of the Steam Methane Reformer: the catalyst packing in the reformer tubes when a Reforming
 * Catalyst Bed is installed, the tubes and firebox glowing red-orange while it runs, the burner flames above the installed
 * Heat Core, the feed gas, Hydrogen, Carbon Monoxide, and drum water levels, the drum pressure needle, the installed Servo
 * working the feed valve, and the installed Battery Cell with its charge. GasChemistryRenderer routes only reformers here.
 * Positions are in model pixels and match tools/blockmodels/steam_methane_reformer.py.
 */
public class SteamMethaneReformerRenderer implements BlockEntityRenderer<GasChemistryBlockEntity> {
    private static final ResourceLocation FLAME = RNGTech.id("textures/block/steam_methane_reformer/fx/flame.png");
    private static final ResourceLocation GLOW = RNGTech.id("textures/block/steam_methane_reformer/fx/glow.png");
    private static final ResourceLocation TUBE_GLOW = RNGTech.id("textures/block/steam_methane_reformer/fx/tube_glow.png");
    private static final ResourceLocation CATALYST = RNGTech.id("block/steam_methane_reformer/fx/catalyst");
    private static final ResourceLocation HEAT_CORE = RNGTech.id("block/steam_methane_reformer/fx/heat_core");
    private static final ResourceLocation BOLT = RNGTech.id("block/steam_methane_reformer/bolt");
    private static final ResourceLocation SERVO = RNGTech.id("block/steam_methane_reformer/fx/servo");
    private static final ResourceLocation HORN = RNGTech.id("block/steam_methane_reformer/fx/horn");
    private static final ResourceLocation CELL = RNGTech.id("block/steam_methane_reformer/fx/cell");
    private static final ResourceLocation CHARGE = RNGTech.id("block/steam_methane_reformer/fx/charge");
    private static final float[] TUBE_XS = {5.5F, 7.5F, 9.5F, 11.5F};
    private static final float TUBE_Z = 3.0F;
    /** Between the collars at the floor and the roof. */
    private static final float TUBE_Y0 = 5.75F;
    private static final float TUBE_Y1 = 11.5F;
    private static final float PACKING_HALF = 0.6F;
    private static final float GLOW_HALF = 0.77F;
    /** The window opening, lit from its back wall. */
    private static final float OPENING_X0 = 4.5F;
    private static final float OPENING_X1 = 12.5F;
    private static final float OPENING_Y0 = 5.25F;
    private static final float OPENING_Y1 = 12.0F;
    private static final float OPENING_Z = 4.98F;
    private static final float[] BURNER_XS = {6.5F, 8.5F, 10.5F};
    /** Between the window glass (z 0.75) and the tubes (z 2.25), on top of the burner nozzles. */
    private static final float FLAME_Z = 2.0F;
    private static final float FLAME_Y0 = 5.65F;
    private static final float FLAME_HEIGHT = 1.75F;
    private static final float FLAME_HALF = 0.8F;
    private static final float[] HEAT_CORE_FROM = {5.5F, 2.0F, 1.5F};
    private static final float[] HEAT_CORE_TO = {11.5F, 4.5F, 4.5F};
    private static final int HEAT_CORE_FINS = 7;
    private static final float SIGHT_X = 0.75F;
    private static final float SIGHT_Y0 = 3.0F;
    private static final float SIGHT_Y1 = 11.0F;
    private static final float[] FEED_Z = {1.25F, 3.75F};
    private static final float[] HYDROGEN_Z = {5.25F, 7.75F};
    private static final float[] MONOXIDE_Z = {9.5F, 12.0F};
    private static final float[] WATER_FROM = {8.25F, 13.25F, 8.25F};
    private static final float[] WATER_TO = {10.25F, 15.25F, 9.75F};
    private static final float[] SERVO_FROM = {0.875F, 13.625F, 1.625F};
    private static final float[] SERVO_TO = {3.125F, 15.875F, 3.875F};
    private static final float[] CELL_FROM = {13.75F, 2.5F, 1.5F};
    private static final float[] CELL_TO = {15.75F, 7.5F, 4.0F};
    private static final float DIAL_X = 6.0F;
    private static final float DIAL_Y = 14.0F;
    private static final float DIAL_Z = 9.24F;
    /** Servo stroke phase, in degrees per tick at a processing speed of 1. */
    private static final float STROKE_PER_TICK = 6.3F;
    private static final int TUBE_DULL = 0x8C1E08;
    private static final int TUBE_HOT = 0xFF7A2A;
    private static final int FIREBOX_GLOW = 0xFF5A20;
    private static final int BURNER_FLAME = 0x4F8CFF;
    private static final int NEEDLE = 0xC43428;
    private static final Map<String, Integer> HEAT_CORE_COLORS = Map.ofEntries(
            Map.entry("iron", 0xB0B0A8),
            Map.entry("copper", 0xD0763C),
            Map.entry("bronze", 0xB67A38),
            Map.entry("steel", 0x767E82),
            Map.entry("aluminum", 0xBAC6D6),
            Map.entry("sparksteel", 0xE0C060),
            Map.entry("titanium", 0x96A4B4),
            Map.entry("exotic", 0x34AA84),
            Map.entry("unique", 0xAF6025)
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

    public SteamMethaneReformerRenderer(BlockEntityRendererProvider.Context context) {
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
        if (blockEntity.machine() != GasChemistryMachine.STEAM_METHANE_REFORMER || blockEntity.getLevel() == null) {
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
        if (state.catalyst()) {
            renderCatalyst(cutout, poseStack.last(), packedLight, packedOverlay);
        }
        renderHeatCore(cutout, poseStack.last(), state.heatCore(), packedLight, packedOverlay);
        renderServo(cutout, poseStack, state.servo(), animation, packedLight, packedOverlay);
        renderCell(cutout, poseStack.last(), state.cell(), state.charge(), packedLight, packedOverlay);
        renderNeedle(cutout, poseStack.last(), animation.heat * Mth.clamp(0.5F + 0.25F * state.speed(), 0.0F, 1.0F), packedLight, packedOverlay);

        VertexConsumer translucent = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
        PoseStack.Pose pose = poseStack.last();
        renderTank(translucent, pose, state.inputFluid(), state.input(),
                SIGHT_X, SIGHT_Y0, FEED_Z[0], SIGHT_X + 2.0F, SIGHT_Y1, FEED_Z[1], packedLight, packedOverlay);
        renderTank(translucent, pose, ModFluids.HYDROGEN_SOURCE.get(), state.output(),
                SIGHT_X, SIGHT_Y0, HYDROGEN_Z[0], SIGHT_X + 2.0F, SIGHT_Y1, HYDROGEN_Z[1], packedLight, packedOverlay);
        renderTank(translucent, pose, ModFluids.CARBON_MONOXIDE_SOURCE.get(), state.secondary(),
                SIGHT_X, SIGHT_Y0, MONOXIDE_Z[0], SIGHT_X + 2.0F, SIGHT_Y1, MONOXIDE_Z[1], packedLight, packedOverlay);
        renderTank(translucent, pose, Fluids.WATER, state.water(),
                WATER_FROM[0], WATER_FROM[1], WATER_FROM[2], WATER_TO[0], WATER_TO[1], WATER_TO[2], packedLight, packedOverlay);

        if (animation.heat > 0.01F) {
            renderTubeGlow(bufferSource.getBuffer(RenderType.eyes(TUBE_GLOW)), pose, animation.heat, packedOverlay);
            renderFireboxGlow(bufferSource.getBuffer(RenderType.eyes(GLOW)), pose, animation.heat, packedOverlay);
        }
        if (animation.spin > 0.01F && !state.heatCore().isEmpty()) {
            renderFlames(bufferSource.getBuffer(RenderType.eyes(FLAME)), pose, animation.spin, time, packedOverlay);
        }
        poseStack.popPose();
    }

    /**
     * The burners light and go out within a second; the tubes heat up over a few seconds and cool more slowly still. The
     * Servo strokes the feed valve faster at higher processing speed.
     */
    private static void animate(GasProcessorVisualState.Animation animation, GasProcessorVisualState state, float time) {
        float elapsed = Double.isNaN(animation.lastTime) ? 0.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        float heat = state.active() ? 1.0F : 0.0F;
        float rate = heat > animation.heat ? 0.03F : 0.012F;
        animation.heat += (heat - animation.heat) * Math.min(1.0F, elapsed * rate);
        animation.spin += (heat - animation.spin) * Math.min(1.0F, elapsed * 0.15F);
        float speed = Math.max(0.25F, state.speed());
        animation.angle = (animation.angle + animation.spin * speed * STROKE_PER_TICK * elapsed) % 360.0F;
    }

    /** Pale catalyst pellets packed inside each tube, seen through the slots in the tube walls. */
    private static void renderCatalyst(VertexConsumer consumer, PoseStack.Pose pose, int light, int overlay) {
        TextureAtlasSprite sprite = sprite(CATALYST);
        for (float x : TUBE_XS) {
            cube(consumer, pose, sprite, 2.0F, x - PACKING_HALF, TUBE_Y0, TUBE_Z - PACKING_HALF, x + PACKING_HALF, TUBE_Y1, TUBE_Z + PACKING_HALF,
                    0xFFFFFF, 255, light, overlay);
        }
    }

    /** The Heat Core sits in the burner bay as a finned block in its material. */
    private static void renderHeatCore(VertexConsumer consumer, PoseStack.Pose pose, String heatCore, int light, int overlay) {
        if (heatCore.isEmpty()) {
            return;
        }
        int color = HEAT_CORE_COLORS.getOrDefault(heatCore, 0xFFFFFF);
        TextureAtlasSprite sprite = sprite(HEAT_CORE);
        float baseTop = HEAT_CORE_FROM[1] + 0.5F;
        cube(consumer, pose, sprite, 4.0F, HEAT_CORE_FROM[0], HEAT_CORE_FROM[1], HEAT_CORE_FROM[2], HEAT_CORE_TO[0], baseTop, HEAT_CORE_TO[2],
                color, 255, light, overlay);
        float pitch = (HEAT_CORE_TO[0] - HEAT_CORE_FROM[0] - 0.9F) / (HEAT_CORE_FINS - 1);
        for (int i = 0; i < HEAT_CORE_FINS; i++) {
            float x0 = HEAT_CORE_FROM[0] + 0.25F + i * pitch;
            cube(consumer, pose, sprite, 4.0F, x0, baseTop, HEAT_CORE_FROM[2] + 0.25F, x0 + 0.4F, HEAT_CORE_TO[1] - 0.1F, HEAT_CORE_TO[2] - 0.25F,
                    color, 255, light, overlay);
        }
    }

    /** Each tube glows dull red as it heats and orange once hot. */
    private static void renderTubeGlow(VertexConsumer consumer, PoseStack.Pose pose, float heat, int overlay) {
        int color = scale(lerpColor(heat, TUBE_DULL, TUBE_HOT), heat);
        for (float x : TUBE_XS) {
            float x0 = x - GLOW_HALF;
            float x1 = x + GLOW_HALF;
            float z0 = TUBE_Z - GLOW_HALF;
            float z1 = TUBE_Z + GLOW_HALF;
            quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), color, 255, LightTexture.FULL_BRIGHT, overlay,
                    new float[] {x1, TUBE_Y0, z0, 0.0F, 1.0F}, new float[] {x0, TUBE_Y0, z0, 1.0F, 1.0F},
                    new float[] {x0, TUBE_Y1, z0, 1.0F, 0.0F}, new float[] {x1, TUBE_Y1, z0, 0.0F, 0.0F});
            quad(consumer, pose, new Vector3f(0.0F, 0.0F, 1.0F), color, 255, LightTexture.FULL_BRIGHT, overlay,
                    new float[] {x0, TUBE_Y0, z1, 0.0F, 1.0F}, new float[] {x1, TUBE_Y0, z1, 1.0F, 1.0F},
                    new float[] {x1, TUBE_Y1, z1, 1.0F, 0.0F}, new float[] {x0, TUBE_Y1, z1, 0.0F, 0.0F});
            quad(consumer, pose, new Vector3f(-1.0F, 0.0F, 0.0F), color, 255, LightTexture.FULL_BRIGHT, overlay,
                    new float[] {x0, TUBE_Y0, z0, 0.0F, 1.0F}, new float[] {x0, TUBE_Y0, z1, 1.0F, 1.0F},
                    new float[] {x0, TUBE_Y1, z1, 1.0F, 0.0F}, new float[] {x0, TUBE_Y1, z0, 0.0F, 0.0F});
            quad(consumer, pose, new Vector3f(1.0F, 0.0F, 0.0F), color, 255, LightTexture.FULL_BRIGHT, overlay,
                    new float[] {x1, TUBE_Y0, z1, 0.0F, 1.0F}, new float[] {x1, TUBE_Y0, z0, 1.0F, 1.0F},
                    new float[] {x1, TUBE_Y1, z0, 1.0F, 0.0F}, new float[] {x1, TUBE_Y1, z1, 0.0F, 0.0F});
        }
    }

    /** The lining behind the tubes glows from the floor up. */
    private static void renderFireboxGlow(VertexConsumer consumer, PoseStack.Pose pose, float heat, int overlay) {
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), scale(FIREBOX_GLOW, heat * 0.55F), 255, LightTexture.FULL_BRIGHT, overlay,
                new float[] {OPENING_X1, OPENING_Y0, OPENING_Z, 0.0F, 1.0F},
                new float[] {OPENING_X0, OPENING_Y0, OPENING_Z, 1.0F, 1.0F},
                new float[] {OPENING_X0, OPENING_Y1, OPENING_Z, 1.0F, 0.0F},
                new float[] {OPENING_X1, OPENING_Y1, OPENING_Z, 0.0F, 0.0F});
    }

    /** A low blue flame on each burner nozzle, each flickering on its own. */
    private static void renderFlames(VertexConsumer consumer, PoseStack.Pose pose, float strength, float time, int overlay) {
        Vector3f north = new Vector3f(0.0F, 0.0F, -1.0F);
        for (int i = 0; i < BURNER_XS.length; i++) {
            float flicker = 0.85F + 0.15F * Mth.sin(time * 0.9F + i * 2.1F) * Mth.sin(time * 2.3F + i);
            int color = scale(BURNER_FLAME, strength * flicker);
            float x0 = BURNER_XS[i] - FLAME_HALF;
            float x1 = BURNER_XS[i] + FLAME_HALF;
            float y1 = FLAME_Y0 + FLAME_HEIGHT * (0.6F + 0.4F * strength) * flicker;
            quad(consumer, pose, north, color, 255, LightTexture.FULL_BRIGHT, overlay,
                    new float[] {x1, FLAME_Y0, FLAME_Z, 0.0F, 1.0F},
                    new float[] {x0, FLAME_Y0, FLAME_Z, 1.0F, 1.0F},
                    new float[] {x0, y1, FLAME_Z, 1.0F, 0.0F},
                    new float[] {x1, y1, FLAME_Z, 0.0F, 0.0F});
        }
    }

    /** The Servo is the feed valve actuator: its horn strokes back and forth while the reformer runs. */
    private static void renderServo(
            VertexConsumer consumer,
            PoseStack poseStack,
            String servo,
            GasProcessorVisualState.Animation animation,
            int light,
            int overlay
    ) {
        if (servo.isEmpty()) {
            return;
        }
        int color = SERVO_COLORS.getOrDefault(servo, 0xFFFFFF);
        cube(consumer, poseStack.last(), sprite(SERVO), 3.0F, SERVO_FROM[0], SERVO_FROM[1], SERVO_FROM[2], SERVO_TO[0], SERVO_TO[1], SERVO_TO[2],
                color, 255, light, overlay);
        float centerX = (SERVO_FROM[0] + SERVO_TO[0]) / 2.0F;
        float centerY = (SERVO_FROM[1] + SERVO_TO[1]) / 2.0F;
        float swing = 28.0F * Mth.sin((float) Math.toRadians(animation.angle)) * Mth.clamp(animation.spin, 0.0F, 1.0F);
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, SERVO_FROM[2]);
        poseStack.mulPose(Axis.ZP.rotationDegrees(swing));
        TextureAtlasSprite horn = sprite(HORN);
        PoseStack.Pose pose = poseStack.last();
        cube(consumer, pose, horn, 16.0F, -0.6F, -0.6F, -0.35F, 0.6F, 0.6F, 0.0F, 0xFFFFFF, 255, light, overlay);
        cube(consumer, pose, horn, 16.0F, -0.3F, -0.95F, -0.3F, 0.3F, 0.95F, -0.05F, 0xFFFFFF, 255, light, overlay);
        poseStack.popPose();
    }

    /** The Battery Cell in its cradle, with a charge window on its front face. */
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
        float z = CELL_FROM[2] - 0.01F;
        float x0 = 14.25F;
        float x1 = 15.25F;
        float v1 = sprite.getV1();
        float v0 = Mth.lerp(fill, v1, sprite.getV0());
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), 0xFFFFFF, 255, LightTexture.FULL_BRIGHT, overlay,
                new float[] {x1, y0, z, sprite.getU0(), v1},
                new float[] {x0, y0, z, sprite.getU1(), v1},
                new float[] {x0, y1, z, sprite.getU1(), v0},
                new float[] {x1, y1, z, sprite.getU0(), v0});
    }

    /** The drum pressure gauge: rest at the bottom left, rising with the firebox heat and further at higher speed. */
    private static void renderNeedle(VertexConsumer consumer, PoseStack.Pose pose, float fraction, int light, int overlay) {
        // The dial face is seen from the north, so the scale sweeps clockwise from the viewer's lower left.
        float angle = (float) Math.toRadians(225.0F - 270.0F * Mth.clamp(fraction, 0.0F, 1.0F));
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

    /** A fluid column filling the inside of a sight glass from its bottom. */
    private static void renderTank(
            VertexConsumer consumer, PoseStack.Pose pose, Fluid fluid, float fill,
            float x0, float y0, float z0, float x1, float y1, float z1, int light, int overlay
    ) {
        if (fluid == Fluids.EMPTY || fill <= 0.001F) {
            return;
        }
        IClientFluidTypeExtensions fluidType = IClientFluidTypeExtensions.of(fluid);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidType.getStillTexture());
        int tint = fluidType.getTintColor();
        float top = y0 + (y1 - y0) * Mth.clamp(fill, 0.0F, 1.0F);
        cube(consumer, pose, sprite, 16.0F, x0 + 0.15F, y0, z0 + 0.15F, x1 - 0.15F, top, z1 - 0.15F, tint & 0xFFFFFF, 220, light, overlay);
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
