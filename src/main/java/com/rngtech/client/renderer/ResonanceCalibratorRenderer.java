package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.ResonanceCalibratorBlock;
import com.rngtech.content.blockentity.ResonanceCalibratorBlockEntity;
import com.rngtech.content.blockentity.ResonanceCalibratorVisualState;
import com.rngtech.content.calibration.CalibrationGearMaterial;
import com.rngtech.content.calibration.ResonanceCalibratorChassis;

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
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws the working parts of a Resonance Calibrator: the installed Resonance Coil as one ring per lane, glowing while
 * it calibrates and flashing when a calibration finishes, the input turning on each lane's pin in the emitter's beam,
 * the active Calibration Pattern behind the monitor glass, the scope's waveform and stability range, the Harmonist
 * streak pips, the Control Board and its blinking LEDs, the Stabilizer Matrix turning between its prongs, the Battery
 * Cell with its charge meter, and the finished components in the tray. Positions are in model pixels and match
 * tools/blockmodels/resonance_calibrator.py.
 */
public class ResonanceCalibratorRenderer implements BlockEntityRenderer<ResonanceCalibratorBlockEntity> {
    private static final Map<String, ModelResourceLocation> COIL_MODELS = new HashMap<>();
    private static final Map<String, ModelResourceLocation> BOARD_MODELS = new HashMap<>();
    private static final Map<String, ModelResourceLocation> MATRIX_MODELS = new HashMap<>();
    private static final ModelResourceLocation COIL_GLOW = part("coil_glow");
    private static final ModelResourceLocation MATRIX_GLOW = part("matrix_glow");
    private static final ResourceLocation LED = RNGTech.id("block/solid_fuel_burner/fx/led");
    private static final ResourceLocation BEAM = RNGTech.id("textures/block/resonance_calibrator/fx/beam.png");
    // The coil ring as modelled, which each lane draws scaled about its centre.
    private static final float RING_X = 8.0F;
    private static final float RING_Y = 7.5F;
    private static final float PIN_Z = 3.5F;
    private static final float EMITTER_Z = 9.0F;
    // Lane layouts by lane count: ring centres, ring centre height, ring scale, item size, and emitter lenses.
    private static final float[][] LANE_X = {{8.0F}, {4.75F, 11.25F}, {3.0F, 8.0F, 13.0F}};
    private static final float[] LANE_Y = {7.5F, 7.0F, 6.75F};
    private static final float[] LANE_SCALE = {1.0F, 0.6875F, 0.55F};
    private static final float[] LANE_ITEM = {3.2F, 2.2F, 1.75F};
    private static final float[][] LENS_X = {{8.0F}, {4.75F, 11.25F}, {5.0F, 8.0F, 11.0F}};
    // Monitor: the pattern behind its glass, the scope's trace and stability bar, and the streak pips on the bezel.
    private static final float PATTERN_X = 4.25F;
    private static final float PATTERN_Y = 13.875F;
    private static final float PATTERN_Z = 9.8F;
    private static final float PATTERN_SIZE = 2.0F;
    private static final float SCREEN_Z = 9.98F;
    private static final float WAVE_X0 = 6.5F;
    private static final float WAVE_X1 = 11.75F;
    private static final float WAVE_Y0 = 13.0F;
    private static final float WAVE_Y1 = 14.75F;
    private static final int WAVE_COLUMNS = 21;
    private static final float BAR_X0 = 12.25F;
    private static final float BAR_X1 = 13.0F;
    private static final float BEZEL_Z = 9.5F;
    private static final float PIP_X0 = 6.5F;
    private static final float PIP_PITCH = 0.66F;
    private static final float PIP_WIDTH = 0.45F;
    private static final float PIP_Y0 = 12.2F;
    private static final float PIP_Y1 = 12.55F;
    // Control Board LEDs on its west face, along its top edge.
    private static final float BOARD_FACE_X = 1.25F;
    private static final float[][] BOARD_LEDS = {{11.75F, 12.25F}, {12.5F, 13.0F}, {13.25F, 13.75F}};
    private static final float BOARD_LED_Y0 = 8.5F;
    private static final float BOARD_LED_Y1 = 9.0F;
    // Stabilizer Matrix: the crystal turns about its vertical axis.
    private static final float MATRIX_X = 2.0F;
    private static final float MATRIX_Z = 7.75F;
    // Battery Cell standing in the east cradle; its part model is 6 pixels wide and 9.5 tall.
    private static final float CELL_X = 14.0F;
    private static final float CELL_Z = 7.75F;
    private static final float CELL_FLOOR = 3.0F;
    private static final float CELL_WIDTH_SCALE = 2.6F / 6.0F;
    private static final float CELL_HEIGHT_SCALE = 0.6F;
    // Charge meter on the cradle's front post.
    private static final float METER_X0 = 13.375F;
    private static final float METER_X1 = 14.625F;
    private static final float METER_Y0 = 3.5F;
    private static final float METER_Z = 5.0F;
    private static final float LED_PITCH = 0.6875F;
    private static final float LED_HEIGHT = 0.5F;
    // Output tray: the finished component lies on its floor.
    private static final float TRAY_X = 8.0F;
    private static final float TRAY_Y = 2.48F;
    private static final float TRAY_Z = 1.65F;
    private static final float TRAY_ITEM = 1.5F;
    private static final float SURFACE = 0.02F;
    private static final int FIELD = 0x2FB3C2;
    private static final int FIELD_FLASH = 0xD8FFFF;
    private static final int TRACE = 0x7CF0D8;
    private static final int TRACE_HEAD = 0xE8FFF8;
    private static final int TRACE_IDLE = 0x2E6A62;
    private static final int RANGE = 0x2FB3C2;
    private static final int ROLL = 0xF2F6F8;
    private static final int PIP_OFF = 0x1A2A2E;
    private static final int PIP_ON = 0xF2D23C;
    private static final int LED_COLOR = 0x8CF07A;

    static {
        for (CalibrationGearMaterial material : CalibrationGearMaterial.values()) {
            String name = material.getSerializedName();
            COIL_MODELS.put(name, part("coil_" + name));
            BOARD_MODELS.put(name, part("board_" + name));
            MATRIX_MODELS.put(name, part("matrix_" + name));
        }
        BOARD_MODELS.put("unique", part("board_unique"));
    }

    private final ModelBlockRenderer modelRenderer;
    private final ItemRenderer itemRenderer;

    public ResonanceCalibratorRenderer(BlockEntityRendererProvider.Context context) {
        modelRenderer = context.getBlockRenderDispatcher().getModelRenderer();
        itemRenderer = context.getItemRenderer();
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        COIL_MODELS.values().forEach(event::register);
        BOARD_MODELS.values().forEach(event::register);
        MATRIX_MODELS.values().forEach(event::register);
        event.register(COIL_GLOW);
        event.register(MATRIX_GLOW);
    }

    private static ModelResourceLocation part(String name) {
        return new ModelResourceLocation(RNGTech.id("block/resonance_calibrator/" + name), ModelResourceLocation.STANDALONE_VARIANT);
    }

    /** The Battery Chassis renderer registers every cell part model. */
    private static ModelResourceLocation cellModel(String kind) {
        return new ModelResourceLocation(RNGTech.id("block/battery_chassis/cell_" + kind), ModelResourceLocation.STANDALONE_VARIANT);
    }

    /** A Gear key with no part model of its own, such as a Unique in a slot without one, shows as Steel. */
    private static ModelResourceLocation gearModel(Map<String, ModelResourceLocation> models, String key) {
        return key.isEmpty() ? null : models.getOrDefault(key, models.get("steel"));
    }

    @Override
    public void render(
            ResonanceCalibratorBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }
        ResonanceCalibratorChassis chassis = blockEntity.getBlockState().getBlock() instanceof ResonanceCalibratorBlock block
                ? block.chassis()
                : ResonanceCalibratorChassis.IRON;
        int layout = Mth.clamp(chassis.lanes(), 1, LANE_X.length) - 1;
        ResonanceCalibratorVisualState state = blockEntity.visualState();
        ResonanceCalibratorVisualState.Animation animation = blockEntity.animation();
        float time = level.getGameTime() + partialTick;
        animate(animation, state, time);

        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        ModelResourceLocation coil = gearModel(COIL_MODELS, state.coil());
        if (coil != null) {
            for (float x : LANE_X[layout]) {
                poseStack.pushPose();
                laneRing(poseStack, x, LANE_Y[layout], LANE_SCALE[layout]);
                renderPart(poseStack, cutout, coil, packedLight, packedOverlay);
                poseStack.popPose();
            }
        }
        renderPart(poseStack, cutout, gearModel(BOARD_MODELS, state.board()), packedLight, packedOverlay);
        ModelResourceLocation matrix = gearModel(MATRIX_MODELS, state.matrix());
        if (matrix != null) {
            poseStack.pushPose();
            matrixPose(poseStack, animation, time);
            renderPart(poseStack, cutout, matrix, packedLight, packedOverlay);
            poseStack.popPose();
        }
        if (!state.cell().isEmpty()) {
            renderCell(poseStack, cutout, state.cell(), packedLight, packedOverlay);
        }

        renderItems(blockEntity, poseStack, bufferSource, state, animation, layout, packedLight, packedOverlay);

        // Field and core glow: the coil and matrix shells drawn again additively, tinted by the field.
        float field = Math.max(animation.field * (0.55F + 0.25F * Mth.sin(time * 0.35F)), animation.flash);
        if (field > 0.01F && (coil != null || matrix != null)) {
            VertexConsumer glow = bufferSource.getBuffer(RenderType.eyes(InventoryMenu.BLOCK_ATLAS));
            int color = FurnaceGlow.scale(FurnaceGlow.lerpColor(animation.flash, FIELD, FIELD_FLASH), field);
            if (coil != null) {
                for (float x : LANE_X[layout]) {
                    poseStack.pushPose();
                    laneRing(poseStack, x, LANE_Y[layout], LANE_SCALE[layout]);
                    renderTinted(poseStack, glow, COIL_GLOW, color, packedOverlay);
                    poseStack.popPose();
                }
            }
            if (matrix != null) {
                poseStack.pushPose();
                matrixPose(poseStack, animation, time);
                renderTinted(poseStack, glow, MATRIX_GLOW, color, packedOverlay);
                poseStack.popPose();
            }
        }

        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        PoseStack.Pose pose = poseStack.last();
        if (animation.field > 0.01F && state.lanes() > 0) {
            renderBeams(bufferSource.getBuffer(RenderType.eyes(BEAM)), pose, state, animation, layout, time, packedOverlay);
        }
        VertexConsumer panel = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        TextureAtlasSprite led = FurnaceGlow.sprite(LED);
        renderScope(panel, pose, led, state, animation, time, packedOverlay);
        renderPips(panel, pose, led, state, packedOverlay);
        if (!state.board().isEmpty() && state.working()) {
            renderBoardLeds(panel, pose, led, time, packedOverlay);
        }
        if (state.charge() >= 0.0F) {
            renderCharge(panel, pose, led, state.chargeSegments(), packedOverlay);
        }
        poseStack.popPose();
    }

    /** The field fades in while working and out when it stops; a finished calibration flashes it; the cycle runs on. */
    private static void animate(ResonanceCalibratorVisualState.Animation animation, ResonanceCalibratorVisualState state, float time) {
        boolean first = Double.isNaN(animation.lastTime);
        float elapsed = first ? 0.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        float field = state.working() ? 1.0F : 0.0F;
        animation.field = first ? field : animation.field + (field - animation.field) * Math.min(1.0F, elapsed * 0.1F);
        if (animation.completions != state.completions()) {
            if (animation.completions >= 0) {
                animation.flash = 1.0F;
            }
            animation.completions = state.completions();
        }
        animation.flash *= (float) Math.pow(0.9, elapsed);
        animation.spin = (animation.spin + elapsed * 3.0F * animation.field) % 360.0F;
        animation.matrixTurn = (animation.matrixTurn + elapsed * (0.6F + 1.8F * animation.field)) % 360.0F;
        if (!state.working() || state.cycleTicks() <= 0) {
            animation.progress = state.progress();
        } else {
            float advanced = animation.progress + elapsed / state.cycleTicks();
            boolean behind = state.progress() > advanced + 0.15F;
            boolean restarted = state.progress() < animation.progress - 0.5F;
            animation.progress = first || behind || restarted ? state.progress() : Math.min(1.0F, advanced);
        }
    }

    private static void laneRing(PoseStack poseStack, float x, float y, float scale) {
        poseStack.translate(x / 16.0F, y / 16.0F, 0.0F);
        poseStack.scale(scale, scale, 1.0F);
        poseStack.translate(-RING_X / 16.0F, -RING_Y / 16.0F, 0.0F);
    }

    private static void matrixPose(PoseStack poseStack, ResonanceCalibratorVisualState.Animation animation, float time) {
        float bob = 0.15F * Mth.sin(time * 0.08F);
        poseStack.translate(MATRIX_X / 16.0F, bob / 16.0F, MATRIX_Z / 16.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(animation.matrixTurn));
        poseStack.translate(-MATRIX_X / 16.0F, 0.0F, -MATRIX_Z / 16.0F);
    }

    private void renderPart(PoseStack poseStack, VertexConsumer consumer, ModelResourceLocation location, int light, int overlay) {
        if (location == null) {
            return;
        }
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
        modelRenderer.renderModel(poseStack.last(), consumer, null, model, 1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY, RenderType.cutout());
    }

    /** A glow shell whose faces carry a tint index, so the colour multiplier tints it. */
    private void renderTinted(PoseStack poseStack, VertexConsumer consumer, ModelResourceLocation location, int color, int overlay) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
        modelRenderer.renderModel(poseStack.last(), consumer, null, model,
                (color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F,
                LightTexture.FULL_BRIGHT, overlay, ModelData.EMPTY, RenderType.cutout());
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

    /**
     * The input turns on each working lane's pin, the pattern stands behind the monitor glass, and the output lies in
     * the tray. A fixed item faces north, toward the front.
     */
    private void renderItems(
            ResonanceCalibratorBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource,
            ResonanceCalibratorVisualState state, ResonanceCalibratorVisualState.Animation animation, int layout, int light, int overlay
    ) {
        Level level = blockEntity.getLevel();
        int seed = (int) blockEntity.getBlockPos().asLong();
        ItemStack input = stack(state.input());
        if (!input.isEmpty()) {
            int fieldLight = LightTexture.pack(Math.max(LightTexture.block(light), Math.round(12.0F * animation.field)), LightTexture.sky(light));
            float size = LANE_ITEM[layout];
            for (int lane = 0; lane < Math.min(state.lanes(), LANE_X[layout].length); lane++) {
                poseStack.pushPose();
                poseStack.translate(LANE_X[layout][lane] / 16.0F, LANE_Y[layout] / 16.0F, PIN_Z / 16.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(animation.spin + lane * 40.0F));
                poseStack.scale(size / 16.0F, size / 16.0F, size / 16.0F);
                itemRenderer.renderStatic(input, ItemDisplayContext.FIXED, fieldLight, overlay, poseStack, bufferSource, level, seed + lane);
                poseStack.popPose();
            }
        }
        ItemStack pattern = stack(state.pattern());
        if (!pattern.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(PATTERN_X / 16.0F, PATTERN_Y / 16.0F, PATTERN_Z / 16.0F);
            poseStack.scale(PATTERN_SIZE / 16.0F, PATTERN_SIZE / 16.0F, PATTERN_SIZE / 16.0F);
            itemRenderer.renderStatic(pattern, ItemDisplayContext.FIXED, light, overlay, poseStack, bufferSource, level, seed + 8);
            poseStack.popPose();
        }
        ItemStack output = stack(state.output());
        if (!output.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(TRAY_X / 16.0F, TRAY_Y / 16.0F, TRAY_Z / 16.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(TRAY_ITEM / 16.0F, TRAY_ITEM / 16.0F, TRAY_ITEM / 16.0F);
            itemRenderer.renderStatic(output, ItemDisplayContext.FIXED, light, overlay, poseStack, bufferSource, level, seed + 9);
            poseStack.popPose();
        }
    }

    private static ItemStack stack(Item item) {
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** A beam from each emitter lens to the item on its lane's pin, pulsing with the field. */
    private static void renderBeams(
            VertexConsumer consumer, PoseStack.Pose pose, ResonanceCalibratorVisualState state,
            ResonanceCalibratorVisualState.Animation animation, int layout, float time, int overlay
    ) {
        float half = 0.2F + 0.15F * LANE_SCALE[layout];
        for (int lane = 0; lane < Math.min(state.lanes(), LANE_X[layout].length); lane++) {
            float pulse = 0.6F + 0.4F * Mth.sin(time * 0.5F + lane * 2.1F);
            int color = FurnaceGlow.scale(FurnaceGlow.lerpColor(animation.flash, FIELD, FIELD_FLASH), Math.max(animation.field * pulse * 0.8F, animation.flash));
            beam(consumer, pose, LENS_X[layout][lane], LANE_Y[layout], EMITTER_Z - SURFACE,
                    LANE_X[layout][lane], LANE_Y[layout], PIN_Z + 0.2F, half, color, overlay);
        }
    }

    /** Two crossed quads from (x0, y0, z0) to (x1, y1, z1), bright along the middle and fading to the edges. */
    private static void beam(
            VertexConsumer consumer, PoseStack.Pose pose,
            float x0, float y0, float z0, float x1, float y1, float z1, float half, int color, int overlay
    ) {
        int light = LightTexture.FULL_BRIGHT;
        float dx = x1 - x0;
        float dz = z1 - z0;
        float length = Mth.sqrt(dx * dx + dz * dz);
        float sx = -dz / length * half;
        float sz = dx / length * half;
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f side = new Vector3f(dz / length, 0.0F, -dx / length);
        for (Vector3f normal : new Vector3f[] {up, new Vector3f(up).negate()}) {
            FurnaceGlow.quad(consumer, pose, normal, color, 255, light, overlay,
                    new float[] {x0 - sx, y0, z0 - sz, 0.0F, 0.0F}, new float[] {x0 + sx, y0, z0 + sz, 1.0F, 0.0F},
                    new float[] {x1 + sx, y1, z1 + sz, 1.0F, 1.0F}, new float[] {x1 - sx, y1, z1 - sz, 0.0F, 1.0F});
        }
        for (Vector3f normal : new Vector3f[] {side, new Vector3f(side).negate()}) {
            FurnaceGlow.quad(consumer, pose, normal, color, 255, light, overlay,
                    new float[] {x0, y0 - half, z0, 0.0F, 0.0F}, new float[] {x0, y0 + half, z0, 1.0F, 0.0F},
                    new float[] {x1, y1 + half, z1, 1.0F, 1.0F}, new float[] {x1, y1 - half, z1, 0.0F, 1.0F});
        }
    }

    /**
     * The scope: a waveform that sweeps across with the cycle's progress, its noise falling as the stability range
     * rises, a flat line while a recipe waits, and nothing without one; beside it the stability bar shows the range and
     * a white mark at the last roll.
     */
    private static void renderScope(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite led, ResonanceCalibratorVisualState state,
            ResonanceCalibratorVisualState.Animation animation, float time, int overlay
    ) {
        Vector3f north = new Vector3f(0.0F, 0.0F, -1.0F);
        int light = LightTexture.FULL_BRIGHT;
        float z = SCREEN_Z - SURFACE;
        float mid = (WAVE_Y0 + WAVE_Y1) / 2.0F;
        float column = (WAVE_X1 - WAVE_X0) / WAVE_COLUMNS;
        if (state.cycleTicks() > 0) {
            if (!state.working()) {
                FurnaceGlow.flat(consumer, pose, led, north, WAVE_X0, mid - 0.06F, WAVE_X1, mid + 0.06F, z, TRACE_IDLE, light, overlay);
            } else {
                float amplitude = (WAVE_Y1 - WAVE_Y0) * 0.28F;
                float noise = 1.0F - (state.stabilityMin() + state.stabilityMax()) / 200.0F;
                int step = (int) (time / 2.0F);
                int head = Math.min(WAVE_COLUMNS, Math.round(animation.progress * WAVE_COLUMNS));
                float previous = Float.NaN;
                for (int i = 0; i < head; i++) {
                    float share = (i + 0.5F) / WAVE_COLUMNS;
                    float jitter = noise * amplitude * 0.9F * (hash(i, step) * 2.0F - 1.0F);
                    float y = Mth.clamp(mid + amplitude * Mth.sin(share * Mth.TWO_PI * 3.0F + time * 0.15F) + jitter, WAVE_Y0 + 0.05F, WAVE_Y1 - 0.05F);
                    float low = Float.isNaN(previous) ? y : Math.min(previous, y);
                    float high = Float.isNaN(previous) ? y : Math.max(previous, y);
                    float x0 = WAVE_X0 + i * column;
                    FurnaceGlow.flat(consumer, pose, led, north, x0, low - 0.07F, x0 + column, high + 0.07F, z,
                            i == head - 1 ? TRACE_HEAD : TRACE, light, overlay);
                    previous = y;
                }
            }
            float height = WAVE_Y1 - WAVE_Y0;
            float low = WAVE_Y0 + height * Mth.clamp(state.stabilityMin() / 100.0F, 0.0F, 1.0F);
            float high = WAVE_Y0 + height * Mth.clamp(state.stabilityMax() / 100.0F, 0.0F, 1.0F);
            FurnaceGlow.flat(consumer, pose, led, north, BAR_X0, low, BAR_X1, Math.max(high, low + 0.1F), z, RANGE, light, overlay);
        }
        if (state.lastStability() >= 0) {
            float y = WAVE_Y0 + (WAVE_Y1 - WAVE_Y0) * Mth.clamp(state.lastStability() / 100.0F, 0.0F, 1.0F);
            int color = FurnaceGlow.lerpColor(0.6F + 0.4F * animation.flash, 0x3A4448, ROLL);
            FurnaceGlow.flat(consumer, pose, led, north, BAR_X0 - 0.15F, y - 0.08F, BAR_X1 + 0.15F, y + 0.08F, z - 0.005F, color, light, overlay);
        }
    }

    /** A steady pseudo-random value in [0, 1) for a column and a time step. */
    private static float hash(int column, int step) {
        int value = column * 73856093 ^ step * 19349663;
        value ^= value >>> 13;
        value *= 0x5BD1E995;
        value ^= value >>> 15;
        return (value & 0xFFFF) / 65536.0F;
    }

    /** The Harmonist's streak as pips under the scope, shown only once a Resonant Streak gives it a cap. */
    private static void renderPips(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite led, ResonanceCalibratorVisualState state, int overlay) {
        if (state.streakCap() <= 0) {
            return;
        }
        Vector3f north = new Vector3f(0.0F, 0.0F, -1.0F);
        int lit = state.streakPips();
        for (int i = 0; i < ResonanceCalibratorVisualState.STREAK_PIPS; i++) {
            float x0 = PIP_X0 + i * PIP_PITCH;
            FurnaceGlow.flat(consumer, pose, led, north, x0, PIP_Y0, x0 + PIP_WIDTH, PIP_Y1, BEZEL_Z - SURFACE,
                    i < lit ? PIP_ON : PIP_OFF, LightTexture.FULL_BRIGHT, overlay);
        }
    }

    /** The Control Board's LEDs blink while it calibrates. */
    private static void renderBoardLeds(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite led, float time, int overlay) {
        Vector3f west = new Vector3f(-1.0F, 0.0F, 0.0F);
        float x = BOARD_FACE_X - SURFACE;
        int beat = (int) (time / 4.0F);
        for (int i = 0; i < BOARD_LEDS.length; i++) {
            if ((beat + i * 3) % 5 >= 3) {
                continue;
            }
            float z0 = BOARD_LEDS[i][0];
            float z1 = BOARD_LEDS[i][1];
            FurnaceGlow.quad(consumer, pose, west, LED_COLOR, 255, LightTexture.FULL_BRIGHT, overlay,
                    new float[] {x, BOARD_LED_Y0, z0, led.getU0(), led.getV1()},
                    new float[] {x, BOARD_LED_Y0, z1, led.getU1(), led.getV1()},
                    new float[] {x, BOARD_LED_Y1, z1, led.getU1(), led.getV0()},
                    new float[] {x, BOARD_LED_Y1, z0, led.getU0(), led.getV0()});
        }
    }

    /** Lit segments of the charge meter, bottom up. */
    private static void renderCharge(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite led, int segments, int overlay) {
        Vector3f north = new Vector3f(0.0F, 0.0F, -1.0F);
        for (int i = 0; i < segments; i++) {
            float y0 = METER_Y0 + i * LED_PITCH;
            FurnaceGlow.flat(consumer, pose, led, north, METER_X0, y0, METER_X1, y0 + LED_HEIGHT, METER_Z - SURFACE,
                    LED_COLOR, LightTexture.FULL_BRIGHT, overlay);
        }
    }
}
