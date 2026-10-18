package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.MetalPressBlock;
import com.rngtech.content.blockentity.MetalPressBlockEntity;
import com.rngtech.content.blockentity.MetalPressVisualState;
import com.rngtech.content.energy.HeatCoreMaterial;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws the working parts of a Metal Press: the selected Mold as the die on the bed with the input lying on it, the ram
 * that comes down as the cycle runs and strikes when it finishes, the Crude press's screw and fly bar turning with it,
 * the parts in the output tray, the other stored Molds standing in the rack, the installed Heat Core, Servo, and Battery
 * Cell with its charge meter, the bed and die glowing with the heat, the heat bar against the Heat Core's limit with the
 * recipe's target and the setpoint marked, the Steel press's failure-strain meter, and the atmosphere cylinder's fill
 * and lit valve. Positions are in model pixels and match tools/blockmodels/metal_press.py.
 */
public class MetalPressRenderer implements BlockEntityRenderer<MetalPressBlockEntity> {
    private static final Map<String, ModelResourceLocation> HEAT_CORE_MODELS = new HashMap<>();
    private static final Map<String, ModelResourceLocation> SERVO_MODELS = new HashMap<>();
    private static final Map<String, ModelResourceLocation> DIE_MODELS = new HashMap<>();
    private static final ModelResourceLocation RAM = part("ram");
    private static final ModelResourceLocation CRUDE_RAM = part("crude_ram");
    private static final ModelResourceLocation SCREW = part("screw");
    private static final String[] MOLDS = {"plate", "gear", "casing", "circuit", "connector"};
    private static final ResourceLocation LED = RNGTech.id("block/solid_fuel_burner/fx/led");
    private static final ResourceLocation GLOW = RNGTech.id("textures/block/furnace/fx/glow.png");
    private static final ResourceLocation COIL = RNGTech.id("textures/block/furnace/fx/coil.png");
    // The die on the bed and the point the screw turns about.
    private static final float WORK_X = 7.25F;
    private static final float WORK_Z = 5.75F;
    private static final float[] DIE = {4.75F, 5.5F, 3.25F, 9.75F, 7.0F, 8.25F};
    private static final float[] STEEL_PLATEN = {3.75F, 5.0F, 3.0F, 10.75F, 5.5F, 9.5F};
    private static final float[] CRUDE_ANVIL = {4.5F, 5.0F, 3.0F, 10.0F, 5.5F, 8.5F};
    private static final float[] CRUDE_COIL = {2.25F, 4.0F, 0.5F, 12.75F, 4.75F, 10.75F};
    // Ram travel: the part models draw the ram raised, with the upper die's face UPPER_DIE_DEPTH under RAM_UP.
    private static final float RAM_UP = 10.0F;
    private static final float UPPER_DIE_DEPTH = 0.75F;
    /** How far the ram creeps down over a cycle before the strike that finishes it. */
    private static final float CREEP = 0.8F;
    private static final float STRIKE_TICKS = 8.0F;
    /** Screw turns for a full stroke of the Crude press's ram. */
    private static final float SCREW_DEGREES = 540.0F;
    // Items: the input lies on the die, pressed parts in the tray at the front of the bed.
    private static final float INPUT_SIZE = 3.5F;
    private static final float LAYER = 0.25F;
    private static final int MAX_LAYERS = 3;
    private static final float TRAY_FLOOR = 5.25F;
    private static final float TRAY_Z = 1.75F;
    private static final float[] TRAY_XS = {5.75F, 7.25F, 8.75F};
    private static final float OUTPUT_SIZE = 2.5F;
    // Mold rack on the back cabinet: five slots, each standing Mold scaled down with its cavity facing the back.
    private static final float RACK_X0 = 2.95F;
    private static final float RACK_PITCH = (12.25F - 2.75F - 0.2F) / MetalPressVisualState.RACK_SLOTS;
    private static final float RACK_FLOOR = 13.0F;
    private static final float RACK_Z = 13.0F;
    private static final float RACK_SCALE = 0.32F;
    // The Crude press's Servo sits on a bracket behind the crosshead instead of on a crown.
    private static final float[] CRUDE_SERVO_OFFSET = {0.0F, -0.75F, 3.125F};
    // Heat bar on the front of the bed, and the Steel press's failure-strain meter beside it.
    private static final float BAR_Y0 = 2.75F;
    private static final float BAR_Y1 = 3.75F;
    private static final float BAR_X0 = 3.25F;
    private static final float STEEL_BAR_X1 = 8.75F;
    private static final float CRUDE_BAR_X1 = 11.5F;
    private static final float STRAIN_X0 = 9.5F;
    private static final float STRAIN_X1 = 11.5F;
    private static final float BAR_Z = 0.5F;
    // The Furnace's cradle and cylinder: the cell, its charge meter, the fill window, and the valve collar.
    private static final float CELL_X = 14.0F;
    private static final float CELL_Z = 5.25F;
    private static final float CELL_FLOOR = 2.75F;
    private static final float CELL_WIDTH_SCALE = 2.6F / 6.0F;
    private static final float CELL_HEIGHT_SCALE = 0.6F;
    private static final float METER_X0 = 13.375F;
    private static final float METER_X1 = 14.625F;
    private static final float METER_Y0 = 3.5F;
    private static final float METER_Z = 1.0F;
    private static final float LED_PITCH = 0.6875F;
    private static final float LED_HEIGHT = 0.5F;
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
    private static final int LED_COLOR = 0x8CF07A;
    private static final int STRAIN_LOW = 0xF2B33C;
    private static final int STRAIN_HIGH = 0xD0302A;

    static {
        for (HeatCoreMaterial material : HeatCoreMaterial.values()) {
            HEAT_CORE_MODELS.put(material.getSerializedName(), part("heat_core_" + material.getSerializedName()));
        }
        HEAT_CORE_MODELS.put("unique", part("heat_core_unique"));
        for (ServoMaterial material : ServoMaterial.values()) {
            SERVO_MODELS.put(material.getSerializedName(), part("servo_" + material.getSerializedName()));
        }
        SERVO_MODELS.put("unique", part("servo_unique"));
        for (String mold : MOLDS) {
            DIE_MODELS.put(mold, part("die_" + mold));
        }
    }

    private final ModelBlockRenderer modelRenderer;
    private final ItemRenderer itemRenderer;

    public MetalPressRenderer(BlockEntityRendererProvider.Context context) {
        modelRenderer = context.getBlockRenderDispatcher().getModelRenderer();
        itemRenderer = context.getItemRenderer();
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        HEAT_CORE_MODELS.values().forEach(event::register);
        SERVO_MODELS.values().forEach(event::register);
        DIE_MODELS.values().forEach(event::register);
        event.register(RAM);
        event.register(CRUDE_RAM);
        event.register(SCREW);
    }

    private static ModelResourceLocation part(String name) {
        return new ModelResourceLocation(RNGTech.id("block/metal_press/" + name), ModelResourceLocation.STANDALONE_VARIANT);
    }

    /** The Battery Chassis renderer registers every cell part model. */
    private static ModelResourceLocation cellModel(String kind) {
        return new ModelResourceLocation(RNGTech.id("block/battery_chassis/cell_" + kind), ModelResourceLocation.STANDALONE_VARIANT);
    }

    @Override
    public void render(
            MetalPressBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        boolean crude = blockEntity.getBlockState().getBlock() instanceof MetalPressBlock press && press.isCrude();
        MetalPressVisualState state = blockEntity.visualState();
        MetalPressVisualState.Animation animation = blockEntity.animation();
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        animate(animation, state, time);
        float heat = FurnaceGlow.wobbled(animation.heat, state.setpoint() > 0 ? state.wobble() : 0, time, 0);
        int layers = Math.min(MAX_LAYERS, state.batch());
        float travel = travel(animation);
        float contact = DIE[4] + UPPER_DIE_DEPTH + (animation.input.isEmpty() ? 0.0F : LAYER * layers);
        float ramDrop = (RAM_UP - contact) * travel;

        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        renderPart(poseStack, cutout, HEAT_CORE_MODELS.get(state.heatCore()), packedLight, packedOverlay);
        renderPart(poseStack, cutout, DIE_MODELS.get(state.die()), packedLight, packedOverlay);
        renderServo(poseStack, cutout, state.servo(), crude, packedLight, packedOverlay);
        if (!state.cell().isEmpty()) {
            renderCell(poseStack, cutout, state.cell(), packedLight, packedOverlay);
        }
        renderRack(poseStack, cutout, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, -ramDrop / 16.0F, 0.0F);
        renderPart(poseStack, cutout, crude ? CRUDE_RAM : RAM, packedLight, packedOverlay);
        poseStack.popPose();
        if (crude) {
            poseStack.pushPose();
            poseStack.translate(WORK_X / 16.0F, 0.0F, WORK_Z / 16.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(SCREW_DEGREES * travel));
            poseStack.translate(-WORK_X / 16.0F, 0.0F, -WORK_Z / 16.0F);
            renderPart(poseStack, cutout, SCREW, packedLight, packedOverlay);
            poseStack.popPose();
        }
        renderItems(blockEntity, poseStack, bufferSource, state, animation, heat, layers, packedLight, packedOverlay);

        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        PoseStack.Pose pose = poseStack.last();
        // One pass per render type: asking for the coil buffer ends the glow buffer, so they must not interleave.
        if (FurnaceGlow.strength(heat) > 0.0F) {
            VertexConsumer glow = bufferSource.getBuffer(RenderType.eyes(GLOW));
            float[] bed = crude ? CRUDE_ANVIL : STEEL_PLATEN;
            glowBox(glow, pose, bed, FurnaceGlow.glow(heat, 0.8F), packedOverlay);
            glowBox(glow, pose, DIE, FurnaceGlow.glow(heat, state.die().isEmpty() ? 0.0F : 0.45F), packedOverlay);
            if (crude) {
                VertexConsumer coils = bufferSource.getBuffer(RenderType.eyes(COIL));
                FurnaceGlow.cube(coils, pose, 16.0F, CRUDE_COIL[0] - SURFACE, CRUDE_COIL[1] - SURFACE, CRUDE_COIL[2] - SURFACE,
                        CRUDE_COIL[3] + SURFACE, CRUDE_COIL[4] + SURFACE, CRUDE_COIL[5] + SURFACE, FurnaceGlow.glow(heat, 1.0F),
                        LightTexture.FULL_BRIGHT, packedOverlay);
            }
        }
        cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        TextureAtlasSprite led = FurnaceGlow.sprite(LED);
        FurnaceGlow.heatBar(cutout, pose, led, BAR_X0, BAR_Y0, crude ? CRUDE_BAR_X1 : STEEL_BAR_X1, BAR_Y1, BAR_Z - SURFACE,
                heat, state.target(), state.setpoint(), state.overheating(), state.maxHeat(), LightTexture.FULL_BRIGHT, packedOverlay);
        if (!crude && state.strain() >= 0.0F) {
            renderStrain(cutout, pose, led, animation.strain, time, packedOverlay);
        }
        if (state.charge() >= 0.0F) {
            renderCharge(cutout, pose, led, state.chargeSegments(), packedOverlay);
        }
        FurnaceGlow.gasFill(cutout, pose, led, true, WINDOW_X + SURFACE / 2.0F, WINDOW_Z0, WINDOW_Z1, WINDOW_Y0, WINDOW_Y1,
                animation.gas, FurnaceGlow.gasColor(state.hydrogen()), packedOverlay);
        FurnaceGlow.collarGlow(cutout, pose, led, CYLINDER_X, CYLINDER_Z, COLLAR_Y0, COLLAR_Y1, animation.atmosphere, packedOverlay);
        poseStack.popPose();
    }

    /**
     * Heat and gauges ease toward the synced values. The ram creeps down with the cycle's progress and springs back when
     * a new cycle starts; each finished press, counted by the server, strikes it down and back.
     */
    private static void animate(MetalPressVisualState.Animation animation, MetalPressVisualState state, float time) {
        boolean first = Double.isNaN(animation.lastTime);
        float elapsed = first ? 0.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        animation.heat = first ? state.heat() : animation.heat + (state.heat() - animation.heat) * Math.min(1.0F, elapsed * 0.08F);
        float creep = state.working() && state.input() != Items.AIR ? state.progress() * CREEP : 0.0F;
        float rate = creep < animation.ram ? 0.35F : 0.12F;
        animation.ram = first ? creep : animation.ram + (creep - animation.ram) * Math.min(1.0F, elapsed * rate);
        if (!first && animation.presses >= 0 && animation.presses != state.presses()) {
            animation.strike = 1.0F;
        }
        animation.presses = state.presses();
        animation.strike = Math.max(0.0F, animation.strike - elapsed / STRIKE_TICKS);
        float gas = Math.max(0.0F, state.gas());
        animation.gas = first ? gas : animation.gas + (gas - animation.gas) * Math.min(1.0F, elapsed * 0.1F);
        float atmosphere = state.atmosphere() ? 1.0F : 0.0F;
        animation.atmosphere += (atmosphere - animation.atmosphere) * Math.min(1.0F, elapsed * 0.2F);
        float strain = Math.max(0.0F, state.strain());
        animation.strain = first ? strain : animation.strain + (strain - animation.strain) * Math.min(1.0F, elapsed * 0.15F);
        if (animation.input.getItem() != state.input()) {
            animation.input = state.input() == Items.AIR ? ItemStack.EMPTY : new ItemStack(state.input());
        }
        if (animation.output.getItem() != state.output()) {
            animation.output = state.output() == Items.AIR ? ItemStack.EMPTY : new ItemStack(state.output());
        }
    }

    /** How far down the ram is, 0 raised and 1 on the work: the creep, or the strike while one runs. */
    private static float travel(MetalPressVisualState.Animation animation) {
        float strike = animation.strike > 0.0F ? Mth.sin((float) Math.PI * (1.0F - animation.strike)) : 0.0F;
        return Mth.clamp(Math.max(animation.ram, strike), 0.0F, 1.0F);
    }

    private void renderPart(PoseStack poseStack, VertexConsumer consumer, ModelResourceLocation location, int light, int overlay) {
        if (location == null) {
            return;
        }
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
        modelRenderer.renderModel(poseStack.last(), consumer, null, model, 1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY, RenderType.cutout());
    }

    /** The Servo lies on the Steel press's crown, or on the bracket behind the Crude press's crosshead. */
    private void renderServo(PoseStack poseStack, VertexConsumer consumer, String servo, boolean crude, int light, int overlay) {
        if (servo.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        if (crude) {
            poseStack.translate(CRUDE_SERVO_OFFSET[0] / 16.0F, CRUDE_SERVO_OFFSET[1] / 16.0F, CRUDE_SERVO_OFFSET[2] / 16.0F);
        }
        renderPart(poseStack, consumer, SERVO_MODELS.get(servo), light, overlay);
        poseStack.popPose();
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

    /** Each stored Mold stands in its rack slot, scaled down, with its cavity facing the back. */
    private void renderRack(PoseStack poseStack, VertexConsumer consumer, MetalPressVisualState state, int light, int overlay) {
        float dieCentreY = (DIE[1] + DIE[4]) / 2.0F;
        float half = (DIE[5] - DIE[2]) * RACK_SCALE / 2.0F;
        for (int slot = 0; slot < MetalPressVisualState.RACK_SLOTS; slot++) {
            ModelResourceLocation model = DIE_MODELS.get(state.rack(slot));
            if (model == null) {
                continue;
            }
            float x = RACK_X0 + RACK_PITCH * slot + (RACK_PITCH - 0.2F) / 2.0F;
            poseStack.pushPose();
            poseStack.translate(x / 16.0F, (RACK_FLOOR + half) / 16.0F, RACK_Z / 16.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(RACK_SCALE, RACK_SCALE, RACK_SCALE);
            poseStack.translate(-WORK_X / 16.0F, -dieCentreY / 16.0F, -WORK_Z / 16.0F);
            renderPart(poseStack, consumer, model, light, overlay);
            poseStack.popPose();
        }
    }

    /**
     * The input lies on the die, one layer per batched input set up to three, lit by the die's heat; pressed parts lie in
     * the tray, more of them as the output slot fills.
     */
    private void renderItems(
            MetalPressBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, MetalPressVisualState state,
            MetalPressVisualState.Animation animation, float heat, int layers, int light, int overlay
    ) {
        int seed = (int) blockEntity.getBlockPos().asLong();
        if (!animation.input.isEmpty()) {
            int glowLight = Math.round(15.0F * FurnaceGlow.strength(heat));
            int itemLight = LightTexture.pack(Math.max(LightTexture.block(light), glowLight), LightTexture.sky(light));
            for (int layer = 0; layer < layers; layer++) {
                renderFlat(animation.input, WORK_X, DIE[4] + LAYER * layer, WORK_Z, 12.0F * layer - 6.0F, INPUT_SIZE,
                        blockEntity, poseStack, bufferSource, itemLight, overlay, seed + layer);
            }
        }
        if (!animation.output.isEmpty()) {
            float fill = state.outputFill();
            int shown = fill <= 0.0F ? 0 : fill < 0.25F ? 1 : fill < 0.6F ? 2 : 3;
            for (int i = 0; i < shown; i++) {
                renderFlat(animation.output, TRAY_XS[i], TRAY_FLOOR + 0.05F * i, TRAY_Z, 25.0F * i - 20.0F, OUTPUT_SIZE,
                        blockEntity, poseStack, bufferSource, light, overlay, seed + 7 + i);
            }
        }
    }

    /** One item lying on a floor: flat items lie flat, block items sit as small cubes. */
    private void renderFlat(
            ItemStack stack, float x, float floor, float z, float yaw, float size, MetalPressBlockEntity blockEntity,
            PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay, int seed
    ) {
        boolean block = stack.getItem() instanceof BlockItem;
        float scale = block ? size * 0.6F : size;
        poseStack.pushPose();
        poseStack.translate(x / 16.0F, (floor + (block ? scale / 4.0F : 0.12F)) / 16.0F, z / 16.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        if (!block) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        }
        poseStack.scale(scale / 16.0F, scale / 16.0F, scale / 16.0F);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, poseStack, bufferSource, blockEntity.getLevel(), seed);
        poseStack.popPose();
    }

    /** Additive glow on every face of a box {x0, y0, z0, x1, y1, z1}, just outside it. */
    private static void glowBox(VertexConsumer consumer, PoseStack.Pose pose, float[] box, int color, int overlay) {
        if (color == 0) {
            return;
        }
        FurnaceGlow.cube(consumer, pose, 16.0F, box[0] - SURFACE, box[1] - SURFACE, box[2] - SURFACE,
                box[3] + SURFACE, box[4] + SURFACE, box[5] + SURFACE, color, LightTexture.FULL_BRIGHT, overlay);
    }

    /** The Steel press's failure strain fills its meter from amber to red, and blinks once it nears failure. */
    private static void renderStrain(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, float strain, float time, int overlay) {
        if (strain <= 0.01F) {
            return;
        }
        if (strain > 0.75F && Mth.sin(time * 0.6F) < -0.2F) {
            return;
        }
        int color = FurnaceGlow.lerpColor(Mth.clamp(strain, 0.0F, 1.0F), STRAIN_LOW, STRAIN_HIGH);
        float x1 = STRAIN_X0 + (STRAIN_X1 - STRAIN_X0) * Mth.clamp(strain, 0.0F, 1.0F);
        FurnaceGlow.flat(consumer, pose, sprite, new Vector3f(0.0F, 0.0F, -1.0F), STRAIN_X0, BAR_Y0, x1, BAR_Y1, BAR_Z - SURFACE,
                color, LightTexture.FULL_BRIGHT, overlay);
    }

    /** Lit segments of the charge meter, bottom up. */
    private static void renderCharge(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, int segments, int overlay) {
        Vector3f north = new Vector3f(0.0F, 0.0F, -1.0F);
        for (int i = 0; i < segments; i++) {
            float y0 = METER_Y0 + i * LED_PITCH;
            FurnaceGlow.flat(consumer, pose, sprite, north, METER_X0, y0, METER_X1, y0 + LED_HEIGHT, METER_Z - SURFACE,
                    LED_COLOR, LightTexture.FULL_BRIGHT, overlay);
        }
    }
}
