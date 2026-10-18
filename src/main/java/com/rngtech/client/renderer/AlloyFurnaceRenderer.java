package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.AlloyFurnaceBlockEntity;
import com.rngtech.content.blockentity.AlloyFurnaceVisualState;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.machine.AlloyCrucibleMaterial;
import com.rngtech.content.machine.ServoMaterial;

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
 * Draws the working parts of an Alloy Furnace: the installed Heat Core radiator, Crucible, Servo stirrer, and Battery
 * Cell with its charge meter; the molten pool, which rises as the charge melts and glows in the colour of the heat; the
 * glowing crucible walls, swinging with the wobble; the items in the feed hoppers, with lids on the hoppers it cannot
 * use; the Nitrogen cylinder's fill and lit valve; and the heat bar against the Heat Core's limit with the recipe's
 * target and the setpoint marked. Positions are in model pixels and match
 * tools/blockmodels/furnace.py.
 */
public class AlloyFurnaceRenderer implements BlockEntityRenderer<AlloyFurnaceBlockEntity> {
    private static final Map<String, ModelResourceLocation> HEAT_CORE_MODELS = new HashMap<>();
    private static final Map<String, ModelResourceLocation> CRUCIBLE_MODELS = new HashMap<>();
    private static final Map<String, ModelResourceLocation> SERVO_MODELS = new HashMap<>();
    private static final Map<String, ModelResourceLocation> STIRRER_MODELS = new HashMap<>();
    private static final ResourceLocation LED = RNGTech.id("block/solid_fuel_burner/fx/led");
    private static final ResourceLocation CRUST = RNGTech.id("block/furnace/fx/crust");
    private static final ResourceLocation HOPPER = RNGTech.id("block/alloy_furnace/hopper");
    private static final ResourceLocation GLOW = RNGTech.id("textures/block/furnace/fx/glow.png");
    private static final ResourceLocation MOLTEN = RNGTech.id("textures/block/furnace/fx/molten.png");
    // The crucible's inner pot, the molten pool's lowest and highest surface, and the stirrer's axis.
    private static final float POT_X0 = 5.75F;
    private static final float POT_X1 = 10.25F;
    private static final float POT_Z0 = 4.25F;
    private static final float POT_Z1 = 8.75F;
    private static final float POT_RIM = 10.5F;
    private static final float POOL_Y0 = 5.0F;
    private static final float POOL_Y1 = 10.0F;
    private static final float[] WELL = {5.0F, 4.0F, 3.5F, 11.0F, 10.5F, 9.5F};
    private static final float STIR_X = 8.0F;
    private static final float STIR_Z = 6.5F;
    /** Degrees per tick at full speed. */
    private static final float STIR_SPEED = 9.0F;
    // Heat bar on the front console.
    private static final float BAR_X0 = 5.25F;
    private static final float BAR_X1 = 10.75F;
    private static final float BAR_Y0 = 2.5F;
    private static final float BAR_Y1 = 3.5F;
    private static final float BAR_Z = 0.75F;
    // Feed hoppers on the cabinet.
    private static final float[][] HOPPERS = {{1.5F, 4.0F}, {4.25F, 6.75F}, {9.25F, 11.75F}, {12.0F, 14.5F}};
    private static final float HOPPER_Z0 = 11.75F;
    private static final float HOPPER_Z1 = 14.75F;
    private static final float HOPPER_TOP = 14.0F;
    private static final float HOPPER_ITEM_FLOOR = 12.75F;
    private static final float HOPPER_ITEM_SIZE = 2.0F;
    // Battery Cell standing in the west cradle; its part model is 6 pixels wide and 9.5 tall.
    private static final float CELL_X = 2.0F;
    private static final float CELL_Z = 5.5F;
    private static final float CELL_FLOOR = 2.75F;
    private static final float CELL_WIDTH_SCALE = 2.6F / 6.0F;
    private static final float CELL_HEIGHT_SCALE = 0.6F;
    // Charge meter on the cradle's front post.
    private static final float METER_X0 = 1.375F;
    private static final float METER_X1 = 2.625F;
    private static final float METER_Y0 = 3.5F;
    private static final float METER_Z = 1.75F;
    private static final float LED_PITCH = 0.6875F;
    private static final float LED_HEIGHT = 0.5F;
    // Nitrogen cylinder at the front east corner: the fill window on its north face and the valve collar on top.
    private static final float CYLINDER_X = 14.25F;
    private static final float CYLINDER_Z = 1.75F;
    private static final float WINDOW_Z = 0.4F;
    private static final float WINDOW_X0 = 13.875F;
    private static final float WINDOW_X1 = 14.625F;
    private static final float WINDOW_Y0 = 3.26F;
    private static final float WINDOW_Y1 = 8.24F;
    private static final float COLLAR_Y0 = 10.0F;
    private static final float COLLAR_Y1 = 10.75F;
    private static final float SURFACE = 0.02F;
    private static final int LED_COLOR = 0x8CF07A;

    static {
        for (HeatCoreMaterial material : HeatCoreMaterial.values()) {
            HEAT_CORE_MODELS.put(material.getSerializedName(), part("heat_core_" + material.getSerializedName()));
        }
        HEAT_CORE_MODELS.put("unique", part("heat_core_unique"));
        for (AlloyCrucibleMaterial material : AlloyCrucibleMaterial.values()) {
            CRUCIBLE_MODELS.put(material.getSerializedName(), part("crucible_" + material.getSerializedName()));
        }
        CRUCIBLE_MODELS.put("unique", part("crucible_unique"));
        for (ServoMaterial material : ServoMaterial.values()) {
            SERVO_MODELS.put(material.getSerializedName(), part("servo_" + material.getSerializedName()));
            STIRRER_MODELS.put(material.getSerializedName(), part("stirrer_" + material.getSerializedName()));
        }
        SERVO_MODELS.put("unique", part("servo_unique"));
        STIRRER_MODELS.put("unique", part("stirrer_unique"));
    }

    private final ModelBlockRenderer modelRenderer;
    private final ItemRenderer itemRenderer;

    public AlloyFurnaceRenderer(BlockEntityRendererProvider.Context context) {
        modelRenderer = context.getBlockRenderDispatcher().getModelRenderer();
        itemRenderer = context.getItemRenderer();
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        HEAT_CORE_MODELS.values().forEach(event::register);
        CRUCIBLE_MODELS.values().forEach(event::register);
        SERVO_MODELS.values().forEach(event::register);
        STIRRER_MODELS.values().forEach(event::register);
    }

    private static ModelResourceLocation part(String name) {
        return new ModelResourceLocation(RNGTech.id("block/alloy_furnace/" + name), ModelResourceLocation.STANDALONE_VARIANT);
    }

    /** The Battery Chassis renderer registers every cell part model. */
    private static ModelResourceLocation cellModel(String kind) {
        return new ModelResourceLocation(RNGTech.id("block/battery_chassis/cell_" + kind), ModelResourceLocation.STANDALONE_VARIANT);
    }

    @Override
    public void render(
            AlloyFurnaceBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        AlloyFurnaceVisualState state = blockEntity.visualState();
        AlloyFurnaceVisualState.Animation animation = blockEntity.animation();
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        animate(animation, state, time);

        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        renderPart(poseStack, cutout, HEAT_CORE_MODELS.get(state.heatCore()), packedLight, packedOverlay);
        renderPart(poseStack, cutout, CRUCIBLE_MODELS.get(state.crucible()), packedLight, packedOverlay);
        if (SERVO_MODELS.containsKey(state.servo())) {
            renderPart(poseStack, cutout, SERVO_MODELS.get(state.servo()), packedLight, packedOverlay);
            poseStack.pushPose();
            poseStack.translate(STIR_X / 16.0F, 0.0F, STIR_Z / 16.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(animation.angle));
            poseStack.translate(-STIR_X / 16.0F, 0.0F, -STIR_Z / 16.0F);
            renderPart(poseStack, cutout, STIRRER_MODELS.get(state.servo()), packedLight, packedOverlay);
            poseStack.popPose();
        }
        if (!state.cell().isEmpty()) {
            renderCell(poseStack, cutout, state.cell(), packedLight, packedOverlay);
        }
        renderHopperItems(blockEntity, poseStack, bufferSource, state, animation, packedLight, packedOverlay);

        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        PoseStack.Pose pose = poseStack.last();
        renderLids(cutout, pose, state.hoppers(), packedLight, packedOverlay);
        float heat = FurnaceGlow.wobbled(animation.heat, state.setpoint() > 0 ? state.wobble() : 0, time, 0);
        boolean crucible = CRUCIBLE_MODELS.containsKey(state.crucible());
        if (crucible && animation.melt > 0.01F) {
            renderPool(bufferSource, pose, animation.melt, heat, time, packedLight, packedOverlay);
        }
        if (FurnaceGlow.strength(heat) > 0.0F) {
            float bottom = crucible && animation.melt > 0.01F ? poolLevel(animation.melt) : POOL_Y0 - 1.0F;
            float[] walls = crucible ? new float[] {POT_X0, bottom, POT_Z0, POT_X1, POT_RIM, POT_Z1} : WELL;
            renderWallGlow(bufferSource.getBuffer(RenderType.eyes(GLOW)), pose, walls, FurnaceGlow.glow(heat, 0.5F), packedOverlay);
        }
        TextureAtlasSprite led = FurnaceGlow.sprite(LED);
        FurnaceGlow.heatBar(cutout, pose, led, BAR_X0, BAR_Y0, BAR_X1, BAR_Y1, BAR_Z - SURFACE,
                heat, state.target(), state.setpoint(), state.overheating(), state.maxHeat(), LightTexture.FULL_BRIGHT, packedOverlay);
        if (state.charge() >= 0.0F) {
            renderCharge(cutout, pose, state.chargeSegments(), packedOverlay);
        }
        FurnaceGlow.nitrogenFill(cutout, pose, led, false, WINDOW_Z - SURFACE / 2.0F, WINDOW_X0, WINDOW_X1, WINDOW_Y0, WINDOW_Y1,
                animation.nitrogen, packedOverlay);
        FurnaceGlow.collarGlow(cutout, pose, led, CYLINDER_X, CYLINDER_Z, COLLAR_Y0, COLLAR_Y1, animation.atmosphere, packedOverlay);
        poseStack.popPose();
    }

    /** Heat and melt ease toward the synced values; the stirrer spins up while the furnace works and coasts down. */
    private static void animate(AlloyFurnaceVisualState.Animation animation, AlloyFurnaceVisualState state, float time) {
        boolean first = Double.isNaN(animation.lastTime);
        float elapsed = first ? 0.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        animation.heat = first ? state.heat() : animation.heat + (state.heat() - animation.heat) * Math.min(1.0F, elapsed * 0.08F);
        animation.melt = first ? state.melt() : animation.melt + (state.melt() - animation.melt) * Math.min(1.0F, elapsed * 0.05F);
        float stir = state.working() ? 1.0F : 0.0F;
        animation.stir += (stir - animation.stir) * Math.min(1.0F, elapsed * 0.04F);
        animation.angle = (animation.angle + elapsed * STIR_SPEED * animation.stir) % 360.0F;
        animation.nitrogen = first ? state.nitrogen() : animation.nitrogen + (state.nitrogen() - animation.nitrogen) * Math.min(1.0F, elapsed * 0.1F);
        float atmosphere = state.atmosphere() ? 1.0F : 0.0F;
        animation.atmosphere += (atmosphere - animation.atmosphere) * Math.min(1.0F, elapsed * 0.2F);
        for (int hopper = 0; hopper < AlloyFurnaceVisualState.HOPPERS; hopper++) {
            if (animation.inputs[hopper].getItem() != state.input(hopper)) {
                animation.inputs[hopper] = state.input(hopper) == Items.AIR ? ItemStack.EMPTY : new ItemStack(state.input(hopper));
            }
        }
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

    /** Each input slot's item stands in its feed hopper. */
    private void renderHopperItems(
            AlloyFurnaceBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource,
            AlloyFurnaceVisualState state, AlloyFurnaceVisualState.Animation animation, int light, int overlay
    ) {
        for (int hopper = 0; hopper < state.hoppers(); hopper++) {
            ItemStack stack = animation.inputs[hopper];
            if (stack.isEmpty()) {
                continue;
            }
            float x = (HOPPERS[hopper][0] + HOPPERS[hopper][1]) / 2.0F;
            float z = (HOPPER_Z0 + HOPPER_Z1) / 2.0F;
            poseStack.pushPose();
            poseStack.translate(x / 16.0F, (HOPPER_ITEM_FLOOR + HOPPER_ITEM_SIZE / 2.0F) / 16.0F, z / 16.0F);
            poseStack.scale(HOPPER_ITEM_SIZE / 16.0F, HOPPER_ITEM_SIZE / 16.0F, HOPPER_ITEM_SIZE / 16.0F);
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, poseStack, bufferSource,
                    blockEntity.getLevel(), (int) blockEntity.getBlockPos().asLong() + hopper);
            poseStack.popPose();
        }
    }

    /** Hoppers past the furnace's input slots are closed with a lid. */
    private static void renderLids(VertexConsumer consumer, PoseStack.Pose pose, int hoppers, int light, int overlay) {
        TextureAtlasSprite sprite = FurnaceGlow.sprite(HOPPER);
        for (int hopper = hoppers; hopper < HOPPERS.length; hopper++) {
            FurnaceGlow.cube(consumer, pose, sprite, 16.0F, HOPPERS[hopper][0], HOPPER_TOP, HOPPER_Z0,
                    HOPPERS[hopper][1], HOPPER_TOP + 0.375F, HOPPER_Z1, 0xFFFFFF, 255, light, overlay);
        }
    }

    /** The pool: a cooled crust, with the molten surface glowing over it in the colour of the heat. */
    private static void renderPool(
            MultiBufferSource bufferSource, PoseStack.Pose pose, float melt, float heat, float time, int light, int overlay
    ) {
        float level = poolLevel(melt);
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        TextureAtlasSprite crust = FurnaceGlow.sprite(CRUST);
        FurnaceGlow.quad(bufferSource.getBuffer(Sheets.cutoutBlockSheet()), pose, up, 0xFFFFFF, 255, light, overlay,
                new float[] {POT_X0, level, POT_Z0, crust.getU0(), crust.getV0()},
                new float[] {POT_X1, level, POT_Z0, crust.getU1(), crust.getV0()},
                new float[] {POT_X1, level, POT_Z1, crust.getU1(), crust.getV1()},
                new float[] {POT_X0, level, POT_Z1, crust.getU0(), crust.getV1()});
        if (FurnaceGlow.strength(heat) <= 0.0F) {
            return;
        }
        float shimmer = 0.92F + 0.08F * Mth.sin(time * 0.21F) * Mth.sin(time * 0.07F + 1.3F);
        float drift = (time * 0.002F) % 0.5F;
        int color = FurnaceGlow.glow(heat, shimmer);
        float surface = level + SURFACE;
        FurnaceGlow.quad(bufferSource.getBuffer(RenderType.eyes(MOLTEN)), pose, up, color, 255, LightTexture.FULL_BRIGHT, overlay,
                new float[] {POT_X0, surface, POT_Z0, drift, 0.0F},
                new float[] {POT_X1, surface, POT_Z0, drift + 0.5F, 0.0F},
                new float[] {POT_X1, surface, POT_Z1, drift + 0.5F, 0.5F},
                new float[] {POT_X0, surface, POT_Z1, drift, 0.5F});
    }

    /** Glow on the four inner walls of the crucible or the empty well {x0, y0, z0, x1, y1, z1}, facing inward. */
    private static void renderWallGlow(VertexConsumer consumer, PoseStack.Pose pose, float[] walls, int color, int overlay) {
        float x0 = walls[0];
        float y0 = walls[1];
        float z0 = walls[2];
        float x1 = walls[3];
        float y1 = walls[4];
        float z1 = walls[5];
        int light = LightTexture.FULL_BRIGHT;
        for (float z : new float[] {z0 + SURFACE, z1 - SURFACE}) {
            Vector3f normal = new Vector3f(0.0F, 0.0F, z < (z0 + z1) / 2.0F ? 1.0F : -1.0F);
            FurnaceGlow.quad(consumer, pose, normal, color, 255, light, overlay,
                    new float[] {x0, y0, z, 0.0F, 1.0F}, new float[] {x1, y0, z, 1.0F, 1.0F},
                    new float[] {x1, y1, z, 1.0F, 0.0F}, new float[] {x0, y1, z, 0.0F, 0.0F});
        }
        for (float x : new float[] {x0 + SURFACE, x1 - SURFACE}) {
            Vector3f normal = new Vector3f(x < (x0 + x1) / 2.0F ? 1.0F : -1.0F, 0.0F, 0.0F);
            FurnaceGlow.quad(consumer, pose, normal, color, 255, light, overlay,
                    new float[] {x, y0, z0, 0.0F, 1.0F}, new float[] {x, y0, z1, 1.0F, 1.0F},
                    new float[] {x, y1, z1, 1.0F, 0.0F}, new float[] {x, y1, z0, 0.0F, 0.0F});
        }
    }

    private static float poolLevel(float melt) {
        return POOL_Y0 + (POOL_Y1 - POOL_Y0) * Mth.clamp(melt, 0.0F, 1.0F);
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
