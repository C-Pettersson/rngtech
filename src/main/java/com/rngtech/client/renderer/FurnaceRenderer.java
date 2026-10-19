package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.FurnaceBlock;
import com.rngtech.content.blockentity.FurnaceBlockEntity;
import com.rngtech.content.blockentity.FurnaceVisualState;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.machine.FurnaceChassisMaterial;

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
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws the working parts of a Furnace: each lane's item on the hearth, the chamber glow and heating elements in the
 * colour of the lane's heat, swinging with its wobble, the heat bar against the Heat Core's limit with the recipe's
 * target and the setpoint marked, the installed Heat Core fins and Battery Cell with its charge meter, the Nitrogen
 * cylinder's fill and lit valve, and the Stage 0 Furnace's fire. Positions are in model pixels
 * and match tools/blockmodels/furnace.py.
 */
public class FurnaceRenderer implements BlockEntityRenderer<FurnaceBlockEntity> {
    private static final Map<String, ModelResourceLocation> HEAT_CORE_MODELS = new HashMap<>();
    private static final ResourceLocation LED = RNGTech.id("block/solid_fuel_burner/fx/led");
    private static final ResourceLocation EMBERS = RNGTech.id("block/solid_fuel_burner/fx/embers");
    private static final ResourceLocation GLOW = RNGTech.id("textures/block/furnace/fx/glow.png");
    private static final ResourceLocation COIL = RNGTech.id("textures/block/furnace/fx/coil.png");
    private static final ResourceLocation FLAME = RNGTech.id("textures/block/solid_fuel_burner/fx/flame.png");
    // Electric chamber, its heating element rods along both side walls, and the setter the items stand on.
    private static final float[] CHAMBER = {3.0F, 4.5F, 1.0F, 10.5F, 10.5F, 10.0F};
    private static final float[] ROD_YS = {5.75F, 7.5F, 9.25F};
    private static final float ROD_SIZE = 0.5F;
    private static final float ROD_Z0 = 1.75F;
    private static final float ROD_Z1 = 9.5F;
    private static final float SETTER_X0 = 3.75F;
    private static final float SETTER_X1 = 9.75F;
    private static final float SETTER_TOP = 5.0F;
    private static final float SETTER_Z = 5.5F;
    // Lead Furnace chambers by lane: the top row is lanes 0 and 1, the bottom row 2 and 3.
    private static final float[][] QUAD_COLUMNS = {{2.25F, 6.25F}, {7.25F, 11.25F}};
    private static final float[][] QUAD_ROWS = {{8.25F, 11.25F}, {4.25F, 7.25F}};
    private static final float QUAD_Z0 = 1.0F;
    private static final float QUAD_Z1 = 9.0F;
    // Heat bars under the doors.
    private static final float BAR_Y0 = 2.5F;
    private static final float BAR_Y1 = 3.5F;
    private static final float BAR_X0 = 2.5F;
    private static final float BAR_X1 = 11.0F;
    private static final float BAR_Z = 0.5F;
    // Battery Cell standing in the east cradle; its part model is 6 pixels wide and 9.5 tall.
    private static final float CELL_X = 14.0F;
    private static final float CELL_Z = 5.25F;
    private static final float CELL_FLOOR = 2.75F;
    private static final float CELL_WIDTH_SCALE = 2.6F / 6.0F;
    private static final float CELL_HEIGHT_SCALE = 0.6F;
    // Charge meter on the cradle's front post.
    private static final float METER_X0 = 13.375F;
    private static final float METER_X1 = 14.625F;
    private static final float METER_Y0 = 3.5F;
    private static final float METER_Z = 1.0F;
    private static final float LED_PITCH = 0.6875F;
    private static final float LED_HEIGHT = 0.5F;
    // Stage 0 kiln: the fire mouth below and the oven above.
    private static final float[] FIRE_MOUTH = {5.0F, 1.0F, 1.0F, 11.0F, 4.5F, 9.0F};
    private static final float[] OVEN = {4.0F, 6.0F, 1.0F, 12.0F, 10.0F, 10.0F};
    private static final float BED_MAX = 1.5F;
    private static final float[] FLAME_ROWS = {3.0F, 5.0F, 7.0F};
    private static final int FLAMES_PER_ROW = 3;
    // Nitrogen cylinder behind the cradle: the fill window on its east face and the valve collar on top.
    private static final float CYLINDER_X = 14.0F;
    private static final float CYLINDER_Z = 12.25F;
    private static final float WINDOW_X = 15.6F;
    private static final float WINDOW_Z0 = 11.875F;
    private static final float WINDOW_Z1 = 12.625F;
    private static final float WINDOW_Y0 = 3.6F;
    private static final float WINDOW_Y1 = 10.4F;
    private static final float COLLAR_Y0 = 12.25F;
    private static final float COLLAR_Y1 = 13.0F;
    private static final float SURFACE = 0.02F;
    private static final int EMBER_HOT = 0xFFFFFF;
    private static final int LED_COLOR = 0x8CF07A;

    static {
        for (HeatCoreMaterial material : HeatCoreMaterial.values()) {
            HEAT_CORE_MODELS.put(material.getSerializedName(), part("heat_core_" + material.getSerializedName()));
        }
        HEAT_CORE_MODELS.put("unique", part("heat_core_unique"));
    }

    private final ModelBlockRenderer modelRenderer;
    private final ItemRenderer itemRenderer;

    public FurnaceRenderer(BlockEntityRendererProvider.Context context) {
        modelRenderer = context.getBlockRenderDispatcher().getModelRenderer();
        itemRenderer = context.getItemRenderer();
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        HEAT_CORE_MODELS.values().forEach(event::register);
    }

    private static ModelResourceLocation part(String name) {
        return new ModelResourceLocation(RNGTech.id("block/furnace/" + name), ModelResourceLocation.STANDALONE_VARIANT);
    }

    /** The Battery Chassis renderer registers every cell part model. */
    private static ModelResourceLocation cellModel(String kind) {
        return new ModelResourceLocation(RNGTech.id("block/battery_chassis/cell_" + kind), ModelResourceLocation.STANDALONE_VARIANT);
    }

    @Override
    public void render(
            FurnaceBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        FurnaceChassisMaterial material = blockEntity.getBlockState().getBlock() instanceof FurnaceBlock furnace
                ? furnace.material()
                : FurnaceChassisMaterial.PRIMITIVE;
        FurnaceVisualState state = blockEntity.visualState();
        FurnaceVisualState.Animation animation = blockEntity.animation();
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        animate(animation, state, time);

        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        if (material.electric()) {
            renderPart(poseStack, cutout, HEAT_CORE_MODELS.get(state.heatCore()), packedLight, packedOverlay);
            if (!state.cell().isEmpty()) {
                renderCell(poseStack, cutout, state.cell(), packedLight, packedOverlay);
            }
        }
        boolean quad = material.processingSlots() > 1;
        renderItems(blockEntity, poseStack, bufferSource, state, animation, material, quad, packedLight, packedOverlay);

        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        PoseStack.Pose pose = poseStack.last();
        if (!material.electric()) {
            renderKiln(bufferSource, pose, state, animation, time, packedOverlay);
        } else if (quad) {
            renderQuad(bufferSource, pose, state, animation, time, packedOverlay);
        } else {
            renderChamber(bufferSource, pose, state, animation, time, packedOverlay);
        }
        if (material.electric() && state.charge() >= 0.0F) {
            renderCharge(cutout, pose, state.chargeSegments(), packedOverlay);
        }
        if (material.electric()) {
            TextureAtlasSprite led = FurnaceGlow.sprite(LED);
            FurnaceGlow.nitrogenFill(cutout, pose, led, true, WINDOW_X + SURFACE / 2.0F, WINDOW_Z0, WINDOW_Z1, WINDOW_Y0, WINDOW_Y1,
                    animation.nitrogen, packedOverlay);
            FurnaceGlow.collarGlow(cutout, pose, led, CYLINDER_X, CYLINDER_Z, COLLAR_Y0, COLLAR_Y1, animation.atmosphere, packedOverlay);
        }
        poseStack.popPose();
    }

    /** Lane heat eases toward the synced value; the Stage 0 fire flares quickly and dies down slowly. */
    private static void animate(FurnaceVisualState.Animation animation, FurnaceVisualState state, float time) {
        boolean first = Double.isNaN(animation.lastTime);
        float elapsed = first ? 0.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        for (int lane = 0; lane < FurnaceVisualState.LANES; lane++) {
            float heat = state.heat(lane);
            animation.heat[lane] = first ? heat : animation.heat[lane] + (heat - animation.heat[lane]) * Math.min(1.0F, elapsed * 0.08F);
            if (animation.inputs[lane].getItem() != state.input(lane)) {
                animation.inputs[lane] = state.input(lane) == Items.AIR ? ItemStack.EMPTY : new ItemStack(state.input(lane));
            }
        }
        float fire = state.burn() > 0.0F && state.working() ? 1.0F : 0.0F;
        animation.fire += (fire - animation.fire) * Math.min(1.0F, elapsed * (fire > animation.fire ? 0.15F : 0.05F));
        float nitrogen = Math.max(0.0F, state.nitrogen());
        animation.nitrogen = first ? nitrogen : animation.nitrogen + (nitrogen - animation.nitrogen) * Math.min(1.0F, elapsed * 0.1F);
        float atmosphere = state.atmosphere() ? 1.0F : 0.0F;
        animation.atmosphere += (atmosphere - animation.atmosphere) * Math.min(1.0F, elapsed * 0.2F);
    }

    private void renderPart(PoseStack poseStack, VertexConsumer consumer, ModelResourceLocation location, int light, int overlay) {
        if (location == null) {
            return;
        }
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
        modelRenderer.renderModel(poseStack.last(), consumer, null, model, 1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY, RenderType.cutout());
    }

    /** The cell stands upright in the cradle. */
    private void renderCell(PoseStack poseStack, VertexConsumer consumer, String kind, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(CELL_X / 16.0F, CELL_FLOOR / 16.0F, CELL_Z / 16.0F);
        poseStack.scale(CELL_WIDTH_SCALE, CELL_HEIGHT_SCALE, CELL_WIDTH_SCALE);
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        renderPart(poseStack, consumer, cellModel(kind), light, overlay);
        poseStack.popPose();
    }

    /** Each lane's input item stands in its chamber, lit by the chamber's own glow. */
    private void renderItems(
            FurnaceBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, FurnaceVisualState state,
            FurnaceVisualState.Animation animation, FurnaceChassisMaterial material, boolean quad, int light, int overlay
    ) {
        int lanes = state.lanes();
        for (int lane = 0; lane < lanes; lane++) {
            ItemStack stack = animation.inputs[lane];
            if (stack.isEmpty()) {
                continue;
            }
            float x;
            float floor;
            float z;
            float size;
            if (quad) {
                float[] column = QUAD_COLUMNS[lane % 2];
                float[] row = QUAD_ROWS[lane / 2];
                x = (column[0] + column[1]) / 2.0F;
                floor = row[0];
                z = (QUAD_Z0 + QUAD_Z1) / 2.0F;
                size = 2.5F;
            } else if (!material.electric()) {
                float span = (OVEN[3] - OVEN[0] - 1.0F) / lanes;
                x = OVEN[0] + 0.5F + span * (lane + 0.5F);
                floor = OVEN[1];
                z = (OVEN[2] + OVEN[5]) / 2.0F;
                size = Math.min(3.5F, span * 0.9F);
            } else {
                float span = (SETTER_X1 - SETTER_X0) / lanes;
                x = SETTER_X0 + span * (lane + 0.5F);
                floor = SETTER_TOP;
                z = SETTER_Z;
                size = Math.min(4.0F, span * 0.9F);
            }
            int glowLight = Math.round(15.0F * FurnaceGlow.strength(animation.heat[lane]));
            int itemLight = LightTexture.pack(Math.max(LightTexture.block(light), glowLight), LightTexture.sky(light));
            poseStack.pushPose();
            poseStack.translate(x / 16.0F, (floor + size / 2.0F) / 16.0F, z / 16.0F);
            poseStack.scale(size / 16.0F, size / 16.0F, size / 16.0F);
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, itemLight, overlay, poseStack, bufferSource,
                    blockEntity.getLevel(), (int) blockEntity.getBlockPos().asLong() + lane);
            poseStack.popPose();
        }
    }

    /** The single chamber glows in the colour of its hottest lane; the bar reads that lane against the core's limit. */
    private static void renderChamber(
            MultiBufferSource bufferSource, PoseStack.Pose pose, FurnaceVisualState state,
            FurnaceVisualState.Animation animation, float time, int overlay
    ) {
        int lane = state.hottestLane();
        float heat = shownHeat(state, animation, lane, time);
        if (FurnaceGlow.strength(heat) > 0.0F) {
            FurnaceGlow.chamberGlow(bufferSource.getBuffer(RenderType.eyes(GLOW)), pose, CHAMBER, FurnaceGlow.glow(heat, 0.75F), overlay);
            VertexConsumer coils = bufferSource.getBuffer(RenderType.eyes(COIL));
            int color = FurnaceGlow.glow(heat, 1.0F);
            for (float y : ROD_YS) {
                for (float x0 : new float[] {CHAMBER[0], CHAMBER[3] - ROD_SIZE}) {
                    FurnaceGlow.cube(coils, pose, 16.0F, x0 - SURFACE, y - SURFACE, ROD_Z0 - SURFACE,
                            x0 + ROD_SIZE + SURFACE, y + ROD_SIZE + SURFACE, ROD_Z1, color, LightTexture.FULL_BRIGHT, overlay);
                }
            }
        }
        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        FurnaceGlow.heatBar(cutout, pose, FurnaceGlow.sprite(LED), BAR_X0, BAR_Y0, BAR_X1, BAR_Y1, BAR_Z - SURFACE,
                heat, state.target(lane), state.setpoint(lane), state.overheating(lane), state.maxHeat(), LightTexture.FULL_BRIGHT, overlay);
    }

    /** A working lane's heat swings around its setpoint by the synced wobble; an idle lane holds still. */
    private static float shownHeat(FurnaceVisualState state, FurnaceVisualState.Animation animation, int lane, float time) {
        return FurnaceGlow.wobbled(animation.heat[lane], state.setpoint(lane) > 0 ? state.wobble() : 0, time, lane);
    }

    /** Each Lead Furnace chamber glows by its own lane; each column's bar is split between its two chambers. */
    private static void renderQuad(
            MultiBufferSource bufferSource, PoseStack.Pose pose, FurnaceVisualState state,
            FurnaceVisualState.Animation animation, float time, int overlay
    ) {
        // One pass per render type: asking for the coil buffer ends the glow buffer, so they must not interleave.
        VertexConsumer glow = bufferSource.getBuffer(RenderType.eyes(GLOW));
        for (int lane = 0; lane < state.lanes(); lane++) {
            float heat = shownHeat(state, animation, lane, time);
            if (FurnaceGlow.strength(heat) > 0.0F) {
                float[] column = QUAD_COLUMNS[lane % 2];
                float[] row = QUAD_ROWS[lane / 2];
                FurnaceGlow.chamberGlow(glow, pose, new float[] {column[0], row[0], QUAD_Z0, column[1], row[1], QUAD_Z1},
                        FurnaceGlow.glow(heat, 0.75F), overlay);
            }
        }
        VertexConsumer coils = bufferSource.getBuffer(RenderType.eyes(COIL));
        for (int lane = 0; lane < state.lanes(); lane++) {
            float heat = shownHeat(state, animation, lane, time);
            if (FurnaceGlow.strength(heat) <= 0.0F) {
                continue;
            }
            float[] column = QUAD_COLUMNS[lane % 2];
            float[] row = QUAD_ROWS[lane / 2];
            int color = FurnaceGlow.glow(heat, 1.0F);
            for (float x0 : new float[] {column[0], column[1] - ROD_SIZE}) {
                FurnaceGlow.cube(coils, pose, 16.0F, x0 - SURFACE, row[1] - 1.0F - SURFACE, ROD_Z0 - SURFACE,
                        x0 + ROD_SIZE + SURFACE, row[1] - 0.5F + SURFACE, QUAD_Z1 - 0.5F, color, LightTexture.FULL_BRIGHT, overlay);
            }
        }
        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        TextureAtlasSprite led = FurnaceGlow.sprite(LED);
        float half = (BAR_Y1 - BAR_Y0) / 2.0F;
        for (int column = 0; column < 2; column++) {
            float x0 = QUAD_COLUMNS[column][0] + 0.25F;
            float x1 = QUAD_COLUMNS[column][1] - 0.25F;
            for (int row = 0; row < 2; row++) {
                int lane = row * 2 + column;
                if (lane >= state.lanes()) {
                    continue;
                }
                float y0 = row == 0 ? BAR_Y0 + half + 0.05F : BAR_Y0;
                float y1 = row == 0 ? BAR_Y1 : BAR_Y0 + half - 0.05F;
                FurnaceGlow.heatBar(cutout, pose, led, x0, y0, x1, y1, BAR_Z - SURFACE, shownHeat(state, animation, lane, time),
                        state.target(lane), state.setpoint(lane), state.overheating(lane), state.maxHeat(), LightTexture.FULL_BRIGHT, overlay);
            }
        }
    }

    /** The Stage 0 kiln: a fuel bed and flames in the fire mouth while it burns, and the oven glowing with its heat. */
    private static void renderKiln(
            MultiBufferSource bufferSource, PoseStack.Pose pose, FurnaceVisualState state,
            FurnaceVisualState.Animation animation, float time, int overlay
    ) {
        if (state.burn() > 0.001F) {
            float height = 0.3F + (BED_MAX - 0.3F) * Mth.clamp(state.burn(), 0.0F, 1.0F);
            float flicker = 0.9F + 0.1F * Mth.sin(time * 0.37F) * Mth.sin(time * 0.13F + 1.0F);
            int color = FurnaceGlow.lerpColor(Mth.clamp(animation.fire * flicker, 0.0F, 1.0F), 0x8C5A44, EMBER_HOT);
            FurnaceGlow.cube(bufferSource.getBuffer(Sheets.cutoutBlockSheet()), pose, FurnaceGlow.sprite(EMBERS), 4.0F,
                    FIRE_MOUTH[0] + 0.5F, FIRE_MOUTH[1], FIRE_MOUTH[2] + 1.0F, FIRE_MOUTH[3] - 0.5F, FIRE_MOUTH[1] + height, FIRE_MOUTH[5] - 1.0F,
                    color, 255, LightTexture.FULL_BRIGHT, overlay);
        }
        if (animation.fire > 0.01F) {
            float flicker = 0.8F + 0.2F * Mth.sin(time * 0.53F) * Mth.sin(time * 0.17F + 2.0F);
            int firelight = FurnaceGlow.scale(0xFF7A20, animation.fire * flicker * 0.85F);
            FurnaceGlow.chamberGlow(bufferSource.getBuffer(RenderType.eyes(GLOW)), pose, FIRE_MOUTH, firelight, overlay);
            renderFlames(bufferSource.getBuffer(RenderType.eyes(FLAME)), pose, state, animation, time, overlay);
        }
        float heat = animation.heat[state.hottestLane()];
        if (FurnaceGlow.strength(heat) > 0.0F) {
            FurnaceGlow.chamberGlow(bufferSource.getBuffer(RenderType.eyes(GLOW)), pose, OVEN, FurnaceGlow.glow(heat, 0.6F), overlay);
        }
    }

    private static void renderFlames(
            VertexConsumer consumer, PoseStack.Pose pose, FurnaceVisualState state,
            FurnaceVisualState.Animation animation, float time, int overlay
    ) {
        float x0Bed = FIRE_MOUTH[0] + 0.5F;
        float width = (FIRE_MOUTH[3] - 0.5F - x0Bed) / FLAMES_PER_ROW;
        float base = FIRE_MOUTH[1] + 0.3F + (BED_MAX - 0.3F) * Mth.clamp(state.burn(), 0.0F, 1.0F) * 0.6F;
        float tall = 2.2F * animation.fire;
        Vector3f front = new Vector3f(0.0F, 0.0F, -1.0F);
        for (int row = 0; row < FLAME_ROWS.length; row++) {
            float z = FLAME_ROWS[row];
            for (int i = 0; i < FLAMES_PER_ROW; i++) {
                float seed = i * 1.7F + row * 2.9F;
                float lick = 0.75F + 0.25F * Mth.sin(time * (0.45F + 0.07F * i) + seed) * Mth.sin(time * 0.21F + seed * 0.5F);
                float sway = Mth.sin(time * 0.3F + seed) * 0.25F;
                float x0 = x0Bed + width * i - width * 0.2F + (row % 2) * width * 0.4F;
                float x1 = x0 + width * 1.4F;
                float y1 = Math.min(FIRE_MOUTH[4] - 0.25F, base + tall * lick);
                int intensity = Math.round(255.0F * Mth.clamp(animation.fire * (0.65F + 0.35F * lick), 0.0F, 1.0F));
                FurnaceGlow.quad(consumer, pose, front, intensity << 16 | intensity << 8 | intensity, 255, LightTexture.FULL_BRIGHT, overlay,
                        new float[] {x0, base, z, 0.0F, 1.0F},
                        new float[] {x1, base, z, 1.0F, 1.0F},
                        new float[] {x1 + sway, y1, z, 1.0F, 0.0F},
                        new float[] {x0 + sway, y1, z, 0.0F, 0.0F});
            }
        }
    }

    /** Lit segments of the charge meter, bottom up. */
    private static void renderCharge(VertexConsumer consumer, PoseStack.Pose pose, int segments, int overlay) {
        TextureAtlasSprite sprite = FurnaceGlow.sprite(LED);
        Vector3f north = new Vector3f(0.0F, 0.0F, -1.0F);
        for (int i = 0; i < segments; i++) {
            float y0 = METER_Y0 + i * LED_PITCH;
            FurnaceGlow.flat(consumer, pose, sprite, north, METER_X0, y0, METER_X1, y0 + LED_HEIGHT, METER_Z - SURFACE,
                    LED_COLOR, LightTexture.FULL_BRIGHT, overlay);
        }
    }
}
