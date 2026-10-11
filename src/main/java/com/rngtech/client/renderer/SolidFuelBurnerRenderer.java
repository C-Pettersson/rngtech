package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.SolidFuelBurnerBlockEntity;
import com.rngtech.content.blockentity.SolidFuelBurnerVisualState;
import com.rngtech.content.energy.FuelBoxMaterial;
import com.rngtech.content.energy.HeatCoreMaterial;

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
import java.util.Map;

/**
 * Draws the working parts of a Solid Fuel Burner: the installed Heat Core fins and Fuel Box hopper in their materials,
 * the fuel in each hopper bin, the fuel bed and flames in the firebox with firelight on the brick, the fins glowing
 * hot at their base, and the Battery Cell charge meter. The flames stand taller with a stronger Heat Core, which
 * spends the same fuel faster. Positions are in model pixels and match tools/blockmodels/solid_fuel_burner.py.
 */
public class SolidFuelBurnerRenderer implements BlockEntityRenderer<SolidFuelBurnerBlockEntity> {
    private static final Map<String, ModelResourceLocation> HEAT_CORE_MODELS = new HashMap<>();
    private static final Map<String, ModelResourceLocation> FUEL_BOX_MODELS = new HashMap<>();
    private static final Map<String, ModelResourceLocation> DIVIDER_MODELS = new HashMap<>();
    private static final ResourceLocation FUEL = RNGTech.id("block/solid_fuel_burner/fx/fuel");
    private static final ResourceLocation EMBERS = RNGTech.id("block/solid_fuel_burner/fx/embers");
    private static final ResourceLocation LED = RNGTech.id("block/solid_fuel_burner/fx/led");
    private static final ResourceLocation FLAME = RNGTech.id("textures/block/solid_fuel_burner/fx/flame.png");
    private static final ResourceLocation GLOW = RNGTech.id("textures/block/solid_fuel_burner/fx/glow.png");
    private static final ResourceLocation HEAT = RNGTech.id("textures/block/solid_fuel_burner/fx/heat.png");
    // Hopper bins inside the Fuel Box walls.
    private static final float BIN_X0 = 0.875F;
    private static final float BIN_X1 = 2.625F;
    private static final float BIN_Z0 = 2.375F;
    private static final float BIN_Z1 = 8.125F;
    private static final float BIN_SPLIT = 5.25F;
    private static final float DIVIDER_HALF = 0.1875F;
    private static final float BIN_FLOOR = 5.5F;
    private static final float BIN_TOP = 12.0F;
    // Fuel bed on the grate, inside the firebrick cavity.
    private static final float BED_X0 = 4.75F;
    private static final float BED_X1 = 11.25F;
    private static final float BED_Z0 = 1.5F;
    private static final float BED_Z1 = 8.0F;
    private static final float BED_Y = 5.5F;
    private static final float BED_MAX = 1.75F;
    private static final float CAVITY_X0 = 4.5F;
    private static final float CAVITY_X1 = 11.5F;
    private static final float CAVITY_Y0 = 4.5F;
    private static final float CAVITY_Y1 = 10.0F;
    private static final float CAVITY_Z0 = 1.0F;
    private static final float CAVITY_Z1 = 8.5F;
    private static final float[] FLAME_ROWS = {2.5F, 4.25F, 6.0F, 7.5F};
    private static final int FLAMES_PER_ROW = 4;
    // Heat Core fins.
    private static final int FIN_COUNT = 8;
    private static final float FIN_X0 = 3.75F;
    private static final float FIN_X1 = 12.25F;
    private static final float FIN_WIDTH = 0.5F;
    private static final float FIN_Y0 = 12.0F;
    private static final float FIN_Z0 = 2.0F;
    private static final float FIN_Z1 = 8.0F;
    private static final float FIN_GLOW_HEIGHT = 1.75F;
    // Charge meter on the east side of the converter cabinet.
    private static final float METER_X = 14.0F;
    private static final float METER_Y0 = 3.0F;
    private static final float LED_Z0 = 12.0F;
    private static final float LED_Z1 = 13.0F;
    private static final float LED_PITCH = 0.875F;
    private static final float LED_HEIGHT = 0.625F;
    private static final float SURFACE = 0.01F;
    private static final int EMBER_HOT = 0xFFFFFF;
    private static final int EMBER_BANKED = 0x8C5A44;
    private static final int LED_COLOR = 0x8CF07A;

    static {
        for (HeatCoreMaterial material : HeatCoreMaterial.values()) {
            HEAT_CORE_MODELS.put(material.getSerializedName(), part("heat_core_" + material.getSerializedName()));
        }
        HEAT_CORE_MODELS.put("unique", part("heat_core_unique"));
        for (FuelBoxMaterial material : FuelBoxMaterial.values()) {
            FUEL_BOX_MODELS.put(material.getSerializedName(), part("fuel_box_" + material.getSerializedName()));
            DIVIDER_MODELS.put(material.getSerializedName(), part("fuel_box_divider_" + material.getSerializedName()));
        }
    }

    private final ModelBlockRenderer modelRenderer;

    public SolidFuelBurnerRenderer(BlockEntityRendererProvider.Context context) {
        modelRenderer = context.getBlockRenderDispatcher().getModelRenderer();
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        HEAT_CORE_MODELS.values().forEach(event::register);
        FUEL_BOX_MODELS.values().forEach(event::register);
        DIVIDER_MODELS.values().forEach(event::register);
    }

    private static ModelResourceLocation part(String name) {
        return new ModelResourceLocation(RNGTech.id("block/solid_fuel_burner/" + name), ModelResourceLocation.STANDALONE_VARIANT);
    }

    @Override
    public void render(
            SolidFuelBurnerBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        SolidFuelBurnerVisualState state = blockEntity.visualState();
        SolidFuelBurnerVisualState.Animation animation = blockEntity.animation();
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        animate(animation, state, time);

        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        renderPart(poseStack, cutout, HEAT_CORE_MODELS.get(state.heatCore()), packedLight, packedOverlay);
        renderPart(poseStack, cutout, FUEL_BOX_MODELS.get(state.fuelBox()), packedLight, packedOverlay);
        if (state.bins() > 1) {
            renderPart(poseStack, cutout, DIVIDER_MODELS.get(state.fuelBox()), packedLight, packedOverlay);
        }

        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        PoseStack.Pose pose = poseStack.last();
        if (FUEL_BOX_MODELS.containsKey(state.fuelBox())) {
            renderFuel(cutout, pose, state, packedLight, packedOverlay);
        }
        if (state.burn() > 0.001F) {
            renderBed(cutout, pose, state, animation, time, packedOverlay);
        }
        if (state.charge() >= 0.0F) {
            renderCharge(cutout, pose, state.chargeSegments(), packedOverlay);
        }
        if (animation.fire > 0.01F) {
            renderFirelight(bufferSource.getBuffer(RenderType.eyes(GLOW)), pose, animation, time, packedOverlay);
            renderFlames(bufferSource.getBuffer(RenderType.eyes(FLAME)), pose, state, animation, time, packedOverlay);
        }
        if (animation.heat > 0.01F && HEAT_CORE_MODELS.containsKey(state.heatCore())) {
            renderFinHeat(bufferSource.getBuffer(RenderType.eyes(HEAT)), pose, animation.heat, packedOverlay);
        }
        poseStack.popPose();
    }

    /** The fire flares up quickly and dies down slowly; the fins heat and cool slower still. */
    private static void animate(SolidFuelBurnerVisualState.Animation animation, SolidFuelBurnerVisualState state, float time) {
        float elapsed = Double.isNaN(animation.lastTime) ? 0.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        float fire = state.burning() ? 1.0F : 0.0F;
        animation.fire += (fire - animation.fire) * Math.min(1.0F, elapsed * (fire > animation.fire ? 0.15F : 0.05F));
        float heat = state.burning() ? Mth.clamp(0.35F + 0.65F * state.rate(), 0.0F, 1.0F) : 0.0F;
        animation.heat += (heat - animation.heat) * Math.min(1.0F, elapsed * (heat > animation.heat ? 0.02F : 0.008F));
    }

    private void renderPart(PoseStack poseStack, VertexConsumer consumer, ModelResourceLocation location, int light, int overlay) {
        if (location == null) {
            return;
        }
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
        modelRenderer.renderModel(poseStack.last(), consumer, null, model, 1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY, RenderType.cutout());
    }

    /** Fuel heaped in each hopper bin, tinted by the fuel item. */
    private static void renderFuel(VertexConsumer consumer, PoseStack.Pose pose, SolidFuelBurnerVisualState state, int light, int overlay) {
        TextureAtlasSprite sprite = sprite(FUEL);
        int bins = state.bins();
        for (int bin = 0; bin < bins; bin++) {
            float fill = Mth.clamp(state.fuel(bin), 0.0F, 1.0F);
            if (fill <= 0.001F) {
                continue;
            }
            float z0 = bins > 1 && bin == 1 ? BIN_SPLIT + DIVIDER_HALF : BIN_Z0;
            float z1 = bins > 1 && bin == 0 ? BIN_SPLIT - DIVIDER_HALF : BIN_Z1;
            float y1 = BIN_FLOOR + (BIN_TOP - BIN_FLOOR) * fill;
            quad(consumer, pose, new Vector3f(0.0F, 1.0F, 0.0F), state.fuelTint(bin), 255, light, overlay,
                    new float[] {BIN_X0, y1, z0, sprite.getU0(), sprite.getV0()},
                    new float[] {BIN_X1, y1, z0, sprite.getU1(), sprite.getV0()},
                    new float[] {BIN_X1, y1, z1, sprite.getU1(), sprite.getV1()},
                    new float[] {BIN_X0, y1, z1, sprite.getU0(), sprite.getV1()});
        }
    }

    /** The fuel item on the grate: glowing while it burns, dull and banked while a Fuel Governor holds it. */
    private static void renderBed(
            VertexConsumer consumer, PoseStack.Pose pose, SolidFuelBurnerVisualState state,
            SolidFuelBurnerVisualState.Animation animation, float time, int overlay
    ) {
        float height = 0.3F + (BED_MAX - 0.3F) * Mth.clamp(state.burn(), 0.0F, 1.0F);
        float flicker = 0.9F + 0.1F * Mth.sin(time * 0.37F) * Mth.sin(time * 0.13F + 1.0F);
        int color = lerpColor(Mth.clamp(animation.fire * flicker, 0.0F, 1.0F), EMBER_BANKED, EMBER_HOT);
        cube(consumer, pose, sprite(EMBERS), 4.0F, BED_X0, BED_Y, BED_Z0, BED_X1, BED_Y + height, BED_Z1,
                color, 255, LightTexture.FULL_BRIGHT, overlay);
    }

    /** Flame tongues in rows across the bed; a stronger Heat Core burns hotter and taller. */
    private static void renderFlames(
            VertexConsumer consumer, PoseStack.Pose pose, SolidFuelBurnerVisualState state,
            SolidFuelBurnerVisualState.Animation animation, float time, int overlay
    ) {
        float base = BED_Y + 0.3F + (BED_MAX - 0.3F) * Mth.clamp(state.burn(), 0.0F, 1.0F) * 0.6F;
        float tall = (1.6F + 2.4F * Mth.clamp(state.rate(), 0.0F, 1.25F)) * animation.fire;
        float width = (BED_X1 - BED_X0) / FLAMES_PER_ROW;
        Vector3f front = new Vector3f(0.0F, 0.0F, -1.0F);
        for (int row = 0; row < FLAME_ROWS.length; row++) {
            float z = FLAME_ROWS[row];
            for (int i = 0; i < FLAMES_PER_ROW; i++) {
                float seed = i * 1.7F + row * 2.9F;
                float lick = 0.75F + 0.25F * Mth.sin(time * (0.45F + 0.07F * i) + seed) * Mth.sin(time * 0.21F + seed * 0.5F);
                float sway = Mth.sin(time * 0.3F + seed) * 0.25F;
                float x0 = BED_X0 + width * i - width * 0.2F + (row % 2) * width * 0.5F;
                float x1 = x0 + width * 1.4F;
                float y1 = Math.min(CAVITY_Y1 - 0.25F, base + tall * lick * (row == 0 ? 0.85F : 1.0F));
                int intensity = Math.round(255.0F * Mth.clamp(animation.fire * (0.65F + 0.35F * lick), 0.0F, 1.0F));
                quad(consumer, pose, front, intensity << 16 | intensity << 8 | intensity, 255, LightTexture.FULL_BRIGHT, overlay,
                        new float[] {x0, base, z, 0.0F, 1.0F},
                        new float[] {x1, base, z, 1.0F, 1.0F},
                        new float[] {x1 + sway, y1, z, 1.0F, 0.0F},
                        new float[] {x0 + sway, y1, z, 0.0F, 0.0F});
            }
        }
    }

    /** Firelight flickering on the firebrick walls, the crown underside, and the ash floor. */
    private static void renderFirelight(
            VertexConsumer consumer, PoseStack.Pose pose, SolidFuelBurnerVisualState.Animation animation, float time, int overlay
    ) {
        float flicker = 0.8F + 0.2F * Mth.sin(time * 0.53F) * Mth.sin(time * 0.17F + 2.0F);
        int intensity = Math.round(255.0F * Mth.clamp(animation.fire * flicker * 0.85F, 0.0F, 1.0F));
        int color = intensity << 16 | intensity << 8 | intensity;
        float back = CAVITY_Z1 - SURFACE;
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), color, 255, LightTexture.FULL_BRIGHT, overlay,
                new float[] {CAVITY_X0, CAVITY_Y0, back, 0.0F, 1.0F}, new float[] {CAVITY_X1, CAVITY_Y0, back, 1.0F, 1.0F},
                new float[] {CAVITY_X1, CAVITY_Y1, back, 1.0F, 0.0F}, new float[] {CAVITY_X0, CAVITY_Y1, back, 0.0F, 0.0F});
        for (float x : new float[] {CAVITY_X0 + SURFACE, CAVITY_X1 - SURFACE}) {
            Vector3f normal = new Vector3f(x < 8.0F ? 1.0F : -1.0F, 0.0F, 0.0F);
            quad(consumer, pose, normal, color, 255, LightTexture.FULL_BRIGHT, overlay,
                    new float[] {x, CAVITY_Y0, CAVITY_Z0, 0.0F, 1.0F}, new float[] {x, CAVITY_Y0, CAVITY_Z1, 1.0F, 1.0F},
                    new float[] {x, CAVITY_Y1, CAVITY_Z1, 1.0F, 0.0F}, new float[] {x, CAVITY_Y1, CAVITY_Z0, 0.0F, 0.0F});
        }
        float crown = CAVITY_Y1 - SURFACE;
        quad(consumer, pose, new Vector3f(0.0F, -1.0F, 0.0F), color, 255, LightTexture.FULL_BRIGHT, overlay,
                new float[] {CAVITY_X0, crown, CAVITY_Z0, 0.0F, 0.0F}, new float[] {CAVITY_X1, crown, CAVITY_Z0, 1.0F, 0.0F},
                new float[] {CAVITY_X1, crown, CAVITY_Z1, 1.0F, 1.0F}, new float[] {CAVITY_X0, crown, CAVITY_Z1, 0.0F, 1.0F});
    }

    /** Each fin glows red-hot at its root while the fire runs and cools slowly after. */
    private static void renderFinHeat(VertexConsumer consumer, PoseStack.Pose pose, float heat, int overlay) {
        int intensity = Math.round(255.0F * Mth.clamp(heat, 0.0F, 1.0F));
        int color = intensity << 16 | intensity << 8 | intensity;
        float pitch = (FIN_X1 - FIN_X0 - FIN_WIDTH) / (FIN_COUNT - 1);
        float y0 = FIN_Y0;
        float y1 = FIN_Y0 + FIN_GLOW_HEIGHT;
        for (int i = 0; i < FIN_COUNT; i++) {
            float x0 = FIN_X0 + pitch * i - SURFACE;
            float x1 = FIN_X0 + pitch * i + FIN_WIDTH + SURFACE;
            for (float x : new float[] {x0, x1}) {
                Vector3f normal = new Vector3f(x == x0 ? -1.0F : 1.0F, 0.0F, 0.0F);
                quad(consumer, pose, normal, color, 255, LightTexture.FULL_BRIGHT, overlay,
                        new float[] {x, y0, FIN_Z0, 0.0F, 1.0F}, new float[] {x, y0, FIN_Z1, 1.0F, 1.0F},
                        new float[] {x, y1, FIN_Z1, 1.0F, 0.0F}, new float[] {x, y1, FIN_Z0, 0.0F, 0.0F});
            }
            float front = FIN_Z0 - SURFACE;
            quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), color, 255, LightTexture.FULL_BRIGHT, overlay,
                    new float[] {x0, y0, front, 0.0F, 1.0F}, new float[] {x1, y0, front, 1.0F, 1.0F},
                    new float[] {x1, y1, front, 1.0F, 0.0F}, new float[] {x0, y1, front, 0.0F, 0.0F});
        }
    }

    /** Lit segments of the charge meter, bottom up. */
    private static void renderCharge(VertexConsumer consumer, PoseStack.Pose pose, int segments, int overlay) {
        TextureAtlasSprite sprite = sprite(LED);
        float x = METER_X + SURFACE;
        Vector3f east = new Vector3f(1.0F, 0.0F, 0.0F);
        for (int i = 0; i < segments; i++) {
            float y0 = METER_Y0 + 0.5F + i * LED_PITCH;
            float y1 = y0 + LED_HEIGHT;
            quad(consumer, pose, east, LED_COLOR, 255, LightTexture.FULL_BRIGHT, overlay,
                    new float[] {x, y0, LED_Z0, sprite.getU0(), sprite.getV1()},
                    new float[] {x, y0, LED_Z1, sprite.getU1(), sprite.getV1()},
                    new float[] {x, y1, LED_Z1, sprite.getU1(), sprite.getV0()},
                    new float[] {x, y1, LED_Z0, sprite.getU0(), sprite.getV0()});
        }
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
}
