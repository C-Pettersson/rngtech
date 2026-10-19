package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.MelterBlockEntity;
import com.rngtech.content.blockentity.MelterVisualState;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.FluidPumpMaterial;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws the working parts of a Melter: the Crush Head as two rolls in the feed hopper that turn while it melts, faster
 * for a shorter melt, with the two ingredients lying on them; the Heat Core fins, the Fluid Pump, and the Servo motor in
 * their materials; the input and output fluids in their sight columns; the melt in the crucible, where the ingredients
 * sink and shrink as the product rises; the chamber glowing in the colour of its heat; the heat bar against the Heat
 * Core's limit with the melting point and setpoint marked; and the Battery Cell in its cradle with its charge meter.
 * Positions are in model pixels and match tools/blockmodels/melter.py.
 */
public class MelterRenderer implements BlockEntityRenderer<MelterBlockEntity> {
    private static final Map<String, ModelResourceLocation> ROLL_MODELS = new HashMap<>();
    private static final Map<String, ModelResourceLocation> FIN_MODELS = new HashMap<>();
    private static final Map<String, ModelResourceLocation> PUMP_MODELS = new HashMap<>();
    private static final Map<String, ModelResourceLocation> SERVO_MODELS = new HashMap<>();
    private static final ResourceLocation LED = RNGTech.id("block/crusher/fx/led");
    private static final ResourceLocation GLOW = RNGTech.id("textures/block/furnace/fx/glow.png");
    // Crush Head rolls along x, built around (y, z) = (8, 8); the front one turns toward the back, the back one forward.
    private static final float PART_CENTRE = 8.0F;
    private static final float ROLL_Y = 13.75F;
    private static final float[] ROLL_ZS = {6.0F, 8.5F};
    /** Roll turns per melt, kept between a visible crawl and a blur. */
    private static final float TURNS_PER_MELT = 3.0F;
    private static final float MIN_TURNS_PER_TICK = 1.0F / 120.0F;
    private static final float MAX_TURNS_PER_TICK = 1.0F / 10.0F;
    // Ingredients on the rolls, in the nip between them.
    private static final float[] FEED_XS = {7.0F, 9.0F};
    private static final float FEED_Y = 14.85F;
    private static final float FEED_Z = 7.25F;
    private static final float FEED_SIZE = 3.0F;
    // Melt chamber behind the porthole, and the pool inside the crucible.
    private static final float[] CHAMBER = {5.5F, 4.5F, 2.0F, 10.5F, 9.0F, 8.5F};
    private static final float POOL_X0 = 6.5F;
    private static final float POOL_X1 = 9.5F;
    private static final float POOL_Z0 = 3.5F;
    private static final float POOL_Z1 = 7.25F;
    private static final float POOL_Y0 = 4.75F;
    private static final float POOL_Y1 = 6.25F;
    /** The pool starts as a film so the product shows from the first tick of a melt. */
    private static final float POOL_MIN = 0.12F;
    private static final float[] MELT_XS = {7.25F, 8.75F};
    private static final float MELT_Z = 5.25F;
    private static final float MELT_SIZE = 2.25F;
    // Heat bar under the porthole.
    private static final float BAR_X0 = 5.5F;
    private static final float BAR_X1 = 10.5F;
    private static final float BAR_Y0 = 2.5F;
    private static final float BAR_Y1 = 3.5F;
    private static final float BAR_Z = 1.75F;
    // Sight columns on the front corners: input on the west, output on the east, as {x0, z0, x1, z1} of the inside.
    private static final float[] COLUMN_IN = {1.25F, 1.0F, 3.25F, 3.0F};
    private static final float[] COLUMN_OUT = {12.75F, 1.0F, 14.75F, 3.0F};
    private static final float COLUMN_Y0 = 3.0F;
    private static final float COLUMN_Y1 = 11.0F;
    // Battery Cell lying along z in the back-west cradle. Its part model is 6 pixels wide and 10 tall.
    private static final float CELL_X = 2.125F;
    private static final float CELL_Y = 3.9F;
    private static final float CELL_Z = 13.375F;
    private static final float CELL_SCALE = 0.45F;
    private static final float CELL_HALF_LENGTH = 5.0F;
    // Charge meter on the west face of the drive cabinet.
    private static final float METER_X = 3.75F;
    private static final float LED_Y0 = 5.5F;
    private static final float LED_PITCH = 1.0F;
    private static final float LED_HEIGHT = 0.75F;
    private static final float LED_Z0 = 12.75F;
    private static final float LED_Z1 = 14.25F;
    private static final float SURFACE = 0.02F;
    private static final int FLUID_ALPHA = 220;
    private static final int LED_COLOR = 0x8CF07A;

    static {
        for (CrushHeadMaterial material : CrushHeadMaterial.values()) {
            ROLL_MODELS.put(material.getSerializedName(), part("roll_" + material.getSerializedName()));
        }
        ROLL_MODELS.put("unique", part("roll_unique"));
        for (HeatCoreMaterial material : HeatCoreMaterial.values()) {
            FIN_MODELS.put(material.getSerializedName(), part("fins_" + material.getSerializedName()));
        }
        FIN_MODELS.put("unique", part("fins_unique"));
        for (FluidPumpMaterial material : FluidPumpMaterial.values()) {
            PUMP_MODELS.put(material.getSerializedName(), part("pump_" + material.getSerializedName()));
        }
        PUMP_MODELS.put("unique", part("pump_unique"));
        for (ServoMaterial material : ServoMaterial.values()) {
            SERVO_MODELS.put(material.getSerializedName(), part("servo_" + material.getSerializedName()));
        }
        SERVO_MODELS.put("unique", part("servo_unique"));
    }

    private final ModelBlockRenderer modelRenderer;
    private final ItemRenderer itemRenderer;

    public MelterRenderer(BlockEntityRendererProvider.Context context) {
        modelRenderer = context.getBlockRenderDispatcher().getModelRenderer();
        itemRenderer = context.getItemRenderer();
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        ROLL_MODELS.values().forEach(event::register);
        FIN_MODELS.values().forEach(event::register);
        PUMP_MODELS.values().forEach(event::register);
        SERVO_MODELS.values().forEach(event::register);
    }

    private static ModelResourceLocation part(String name) {
        return new ModelResourceLocation(RNGTech.id("block/melter/" + name), ModelResourceLocation.STANDALONE_VARIANT);
    }

    /** The Battery Chassis renderer registers every cell part model. */
    private static ModelResourceLocation cellModel(String kind) {
        return new ModelResourceLocation(RNGTech.id("block/battery_chassis/cell_" + kind), ModelResourceLocation.STANDALONE_VARIANT);
    }

    @Override
    public void render(
            MelterBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        MelterVisualState state = blockEntity.visualState();
        MelterVisualState.Animation animation = blockEntity.animation();
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        animate(animation, state, time);
        int seed = (int) blockEntity.getBlockPos().asLong();

        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        ModelResourceLocation roll = ROLL_MODELS.get(state.crushHead());
        if (roll != null) {
            for (int i = 0; i < ROLL_ZS.length; i++) {
                renderRoll(poseStack, cutout, roll, ROLL_ZS[i], (i == 0 ? 360.0F : -360.0F) * animation.angle, packedLight, packedOverlay);
            }
        }
        renderPart(poseStack, cutout, FIN_MODELS.get(state.heatCore()), packedLight, packedOverlay);
        renderPart(poseStack, cutout, PUMP_MODELS.get(state.pump()), packedLight, packedOverlay);
        renderPart(poseStack, cutout, SERVO_MODELS.get(state.servo()), packedLight, packedOverlay);
        if (!state.cell().isEmpty()) {
            renderCell(poseStack, cutout, state.cell(), packedLight, packedOverlay);
        }

        float heat = FurnaceGlow.wobbled(animation.heat, state.setpoint() > 0 && state.working() ? state.wobble() : 0, time, 0);
        float glow = FurnaceGlow.strength(heat);
        int meltLight = LightTexture.pack(Math.max(LightTexture.block(packedLight), Math.round(15.0F * glow)), LightTexture.sky(packedLight));
        renderItems(blockEntity, poseStack, bufferSource, state, animation, meltLight, packedLight, packedOverlay, seed);

        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        PoseStack.Pose pose = poseStack.last();
        if (glow > 0.0F) {
            FurnaceGlow.chamberGlow(bufferSource.getBuffer(RenderType.eyes(GLOW)), pose, CHAMBER, FurnaceGlow.glow(heat, 0.7F), packedOverlay);
        }
        cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        FurnaceGlow.heatBar(cutout, pose, FurnaceGlow.sprite(LED), BAR_X0, BAR_Y0, BAR_X1, BAR_Y1, BAR_Z - SURFACE,
                heat, state.target(), state.setpoint(), state.overheating(), state.maxHeat(), LightTexture.FULL_BRIGHT, packedOverlay);
        if (state.charge() >= 0.0F) {
            renderCharge(cutout, pose, state.chargeSegments(), packedOverlay);
        }

        VertexConsumer translucent = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
        renderColumn(translucent, pose, fluid(state.inputFluid()), animation.inputFill, COLUMN_IN, packedLight, packedOverlay);
        renderColumn(translucent, pose, fluid(state.outputFluid()), animation.outputFill, COLUMN_OUT, packedLight, packedOverlay);
        Fluid product = fluid(state.product());
        if (product != Fluids.EMPTY && animation.progress > 0.0F) {
            float level = POOL_Y0 + (POOL_Y1 - POOL_Y0) * (POOL_MIN + (1.0F - POOL_MIN) * animation.progress);
            renderFluid(translucent, pose, product, POOL_X0, POOL_Y0, POOL_Z0, POOL_X1, level, POOL_Z1, meltLight, packedOverlay);
        }
        poseStack.popPose();
    }

    /**
     * Heat and tank levels ease toward the synced values. The melt's progress runs on between updates at one tick's
     * share of the melt per tick while the Melter works, and snaps to each update. The rolls spin up quickly and run
     * down slowly.
     */
    private static void animate(MelterVisualState.Animation animation, MelterVisualState state, float time) {
        boolean first = Double.isNaN(animation.lastTime);
        float elapsed = first ? 0.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        animation.heat = first ? state.heat() : animation.heat + (state.heat() - animation.heat) * Math.min(1.0F, elapsed * 0.08F);
        animation.inputFill = ease(first, animation.inputFill, state.inputFill(), elapsed);
        animation.outputFill = ease(first, animation.outputFill, state.outputFill(), elapsed);
        if (state.progress() != animation.syncedProgress) {
            animation.syncedProgress = state.progress();
            animation.progress = state.progress();
        } else if (state.working() && state.cycleTicks() > 0) {
            animation.progress = Math.min(1.0F, animation.progress + elapsed / state.cycleTicks());
        }
        float target = 0.0F;
        if (state.working() && state.cycleTicks() > 0) {
            target = Mth.clamp(TURNS_PER_MELT / state.cycleTicks(), MIN_TURNS_PER_TICK, MAX_TURNS_PER_TICK);
        }
        float rate = target > animation.speed ? 0.12F : 0.05F;
        animation.speed += (target - animation.speed) * Math.min(1.0F, elapsed * rate);
        animation.angle = (animation.angle + animation.speed * elapsed) % 1.0F;
        if (animation.primary.getItem() != state.primary()) {
            animation.primary = state.primary() == Items.AIR ? ItemStack.EMPTY : new ItemStack(state.primary());
        }
        if (animation.secondary.getItem() != state.secondary()) {
            animation.secondary = state.secondary() == Items.AIR ? ItemStack.EMPTY : new ItemStack(state.secondary());
        }
    }

    private static float ease(boolean first, float current, float target, float elapsed) {
        return first ? target : current + (target - current) * Math.min(1.0F, elapsed * 0.1F);
    }

    /** A roll part built around (8, 8), moved onto its shaft in the hopper and turned about it. */
    private void renderRoll(
            PoseStack poseStack, VertexConsumer consumer, ModelResourceLocation location, float z, float degrees, int light, int overlay
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0F, ROLL_Y / 16.0F, z / 16.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(degrees));
        poseStack.translate(0.0F, -PART_CENTRE / 16.0F, -PART_CENTRE / 16.0F);
        renderPart(poseStack, consumer, location, light, overlay);
        poseStack.popPose();
    }

    private void renderPart(PoseStack poseStack, VertexConsumer consumer, ModelResourceLocation location, int light, int overlay) {
        if (location == null) {
            return;
        }
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
        modelRenderer.renderModel(poseStack.last(), consumer, null, model, 1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY, RenderType.cutout());
    }

    /** The cell lies on its side across the saddles, its axis along z. */
    private void renderCell(PoseStack poseStack, VertexConsumer consumer, String kind, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(CELL_X / 16.0F, CELL_Y / 16.0F, CELL_Z / 16.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(CELL_SCALE, CELL_SCALE, CELL_SCALE);
        poseStack.translate(-0.5F, -CELL_HALF_LENGTH / 16.0F, -0.5F);
        renderPart(poseStack, consumer, cellModel(kind), light, overlay);
        poseStack.popPose();
    }

    /**
     * The ingredients lie on the rolls, jiggling while they turn. While a melt runs, they also sink into the pool,
     * shrinking as it rises, lit by the chamber's glow.
     */
    private void renderItems(
            MelterBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, MelterVisualState state,
            MelterVisualState.Animation animation, int meltLight, int light, int overlay, int seed
    ) {
        ItemStack[] stacks = {animation.primary, animation.secondary};
        float jiggle = state.working() ? Mth.sin(animation.angle * Mth.TWO_PI * 6.0F) * 0.15F : 0.0F;
        float melt = Mth.clamp(animation.progress, 0.0F, 1.0F);
        float pool = POOL_Y0 + (POOL_Y1 - POOL_Y0) * (POOL_MIN + (1.0F - POOL_MIN) * melt);
        for (int i = 0; i < stacks.length; i++) {
            ItemStack stack = stacks[i];
            if (stack.isEmpty()) {
                continue;
            }
            renderLying(stack, FEED_SIZE, FEED_XS[i], FEED_Y + (i == 0 ? jiggle : -jiggle), FEED_Z, 25.0F + i * 130.0F,
                    blockEntity, poseStack, bufferSource, light, overlay, seed + i);
            if (melt > 0.0F && !state.product().isEmpty()) {
                float size = MELT_SIZE * (1.0F - 0.75F * melt);
                renderLying(stack, size, MELT_XS[i], pool - size * 0.15F, MELT_Z, 70.0F + i * 110.0F,
                        blockEntity, poseStack, bufferSource, meltLight, overlay, seed + 2 + i);
            }
        }
    }

    /** An item lying flat, or a block item sitting as a small cube, centred on (x, z) with its underside at y. */
    private void renderLying(
            ItemStack stack, float size, float x, float y, float z, float yaw, MelterBlockEntity blockEntity, PoseStack poseStack,
            MultiBufferSource bufferSource, int light, int overlay, int seed
    ) {
        boolean block = stack.getItem() instanceof BlockItem;
        poseStack.pushPose();
        poseStack.translate(x / 16.0F, (y + (block ? size / 4.0F : 0.2F)) / 16.0F, z / 16.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        if (!block) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        }
        poseStack.scale(size / 16.0F, size / 16.0F, size / 16.0F);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, poseStack, bufferSource, blockEntity.getLevel(), seed);
        poseStack.popPose();
    }

    private static void renderColumn(VertexConsumer consumer, PoseStack.Pose pose, Fluid fluid, float fill, float[] inside, int light, int overlay) {
        if (fluid == Fluids.EMPTY || fill <= 0.005F) {
            return;
        }
        float top = COLUMN_Y0 + (COLUMN_Y1 - COLUMN_Y0) * Mth.clamp(fill, 0.0F, 1.0F);
        renderFluid(consumer, pose, fluid, inside[0], COLUMN_Y0, inside[1], inside[2], top, inside[3], light, overlay);
    }

    private static void renderFluid(
            VertexConsumer consumer, PoseStack.Pose pose, Fluid fluid,
            float x0, float y0, float z0, float x1, float y1, float z1, int light, int overlay
    ) {
        IClientFluidTypeExtensions fluidType = IClientFluidTypeExtensions.of(fluid);
        TextureAtlasSprite sprite = FurnaceGlow.sprite(fluidType.getStillTexture());
        FurnaceGlow.cube(consumer, pose, sprite, 16.0F, x0, y0, z0, x1, y1, z1, fluidType.getTintColor() & 0xFFFFFF, FLUID_ALPHA, light, overlay);
    }

    private static Fluid fluid(String id) {
        ResourceLocation location = id.isEmpty() ? null : ResourceLocation.tryParse(id);
        return location == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(location);
    }

    /** Lit segments of the charge meter, bottom up. */
    private static void renderCharge(VertexConsumer consumer, PoseStack.Pose pose, int segments, int overlay) {
        TextureAtlasSprite sprite = FurnaceGlow.sprite(LED);
        float x = METER_X - SURFACE;
        Vector3f west = new Vector3f(-1.0F, 0.0F, 0.0F);
        for (int i = 0; i < segments; i++) {
            float y0 = LED_Y0 + i * LED_PITCH;
            float y1 = y0 + LED_HEIGHT;
            FurnaceGlow.quad(consumer, pose, west, LED_COLOR, 255, LightTexture.FULL_BRIGHT, overlay,
                    new float[] {x, y0, LED_Z0, sprite.getU0(), sprite.getV1()},
                    new float[] {x, y0, LED_Z1, sprite.getU1(), sprite.getV1()},
                    new float[] {x, y1, LED_Z1, sprite.getU1(), sprite.getV0()},
                    new float[] {x, y1, LED_Z0, sprite.getU0(), sprite.getV0()});
        }
    }
}
