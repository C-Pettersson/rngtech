package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.CrusherBlock;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.blockentity.CrusherVisualState;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;

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
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws the working parts of a Crusher: the installed Crush Head as two toothed rolls (four on the batching chassis)
 * that turn toward the nip, faster for a shorter craft; the Battery Cell lying in its cradle with a charge meter
 * beside it; the feed in the hopper, shaking while it is crushed; and the product in the output bin. A jammed Crusher
 * stops its rolls with a shudder, shows the feed stuck in the nip, and blinks its lamp red. Positions are in model
 * pixels and match tools/blockmodels/crusher.py.
 */
public class CrusherRenderer implements BlockEntityRenderer<CrusherBlockEntity> {
    private static final Map<String, ModelResourceLocation> ROLL_MODELS = new HashMap<>();
    private static final ResourceLocation LED = RNGTech.id("block/crusher/fx/led");
    // Primary rolls, axes along z; the secondary pair sits under them on the batching chassis.
    private static final float ROLL_Y = 8.5F;
    private static final float[] ROLL_XS = {5.2F, 10.8F};
    private static final float ROLL_TIP = 2.7F;
    private static final float SECONDARY_Y = 4.3F;
    private static final float[] SECONDARY_XS = {6.4F, 9.6F};
    private static final float SECONDARY_TIP = 1.4F;
    private static final float ROLL_Z0 = 3.0F;
    private static final float ROLL_Z1 = 9.5F;
    // The roll part model: a star around (8, 8) from z 4 to 12 with a tip radius of 4.
    private static final float PART_TIP = 4.0F;
    private static final float PART_Z0 = 4.0F;
    private static final float PART_LENGTH = 8.0F;
    /** Roll turns per craft, kept between a visible crawl and a blur. */
    private static final float TURNS_PER_CRAFT = 3.0F;
    private static final float MIN_TURNS_PER_TICK = 1.0F / 120.0F;
    private static final float MAX_TURNS_PER_TICK = 1.0F / 12.0F;
    // Battery Cell lying along z in the east cradle. Its part model is 6 pixels wide and 10 tall.
    private static final float CELL_X = 14.0F;
    private static final float CELL_Y = 4.25F;
    private static final float CELL_Z = 12.75F;
    private static final float CELL_SCALE = 0.5F;
    private static final float CELL_HALF_LENGTH = 5.0F;
    // Charge meter on the east face of the gearbox.
    private static final float METER_X = 12.75F;
    private static final float LED_Y0 = 6.5F;
    private static final float LED_PITCH = 1.0F;
    private static final float LED_HEIGHT = 0.75F;
    private static final float LED_Z0 = 11.75F;
    private static final float LED_Z1 = 13.25F;
    // Status lamp on the header, which a jam turns red.
    private static final float[] LAMP = {11.0F, 11.5F, 12.75F, 12.25F};
    private static final float LAMP_FRONT = 0.0F;
    // Feed on the hopper floor and product on the bin floor: x, z, and the lift of each copy above the floor. Each
    // Crusher scatters them by its position, within the spread, so neighbouring Crushers do not look stamped.
    private static final float HOPPER_FLOOR = 12.75F;
    private static final float[][] HOPPER_SPOTS = {{5.75F, 4.0F, 0.0F}, {10.25F, 4.75F, 0.1F}, {8.0F, 6.25F, 0.6F}};
    private static final float[] HOPPER_SPREAD = {1.0F, 0.75F};
    private static final float BIN_FLOOR = 2.5F;
    private static final float[][] BIN_SPOTS = {{5.0F, 2.5F, 0.0F}, {8.0F, 3.25F, 0.1F}, {11.0F, 2.25F, 0.0F}};
    private static final float[] BIN_SPREAD = {0.75F, 0.4F};
    private static final float MAX_TILT = 12.0F;
    private static final float ITEM_SIZE = 6.0F;
    private static final float NIP_Z = 2.5F;
    private static final float SURFACE = 0.02F;
    private static final int LED_COLOR = 0x8CF07A;
    private static final int JAM_COLOR = 0xFF4030;

    static {
        for (CrushHeadMaterial material : CrushHeadMaterial.values()) {
            ROLL_MODELS.put(material.getSerializedName(), part("roll_" + material.getSerializedName()));
        }
        ROLL_MODELS.put("unique", part("roll_unique"));
    }

    private final ModelBlockRenderer modelRenderer;
    private final ItemRenderer itemRenderer;

    public CrusherRenderer(BlockEntityRendererProvider.Context context) {
        modelRenderer = context.getBlockRenderDispatcher().getModelRenderer();
        itemRenderer = context.getItemRenderer();
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        ROLL_MODELS.values().forEach(event::register);
    }

    private static ModelResourceLocation part(String name) {
        return new ModelResourceLocation(RNGTech.id("block/crusher/" + name), ModelResourceLocation.STANDALONE_VARIANT);
    }

    /** The Battery Chassis renderer registers every cell part model. */
    private static ModelResourceLocation cellModel(String kind) {
        return new ModelResourceLocation(RNGTech.id("block/battery_chassis/cell_" + kind), ModelResourceLocation.STANDALONE_VARIANT);
    }

    @Override
    public void render(
            CrusherBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        CrusherVisualState state = blockEntity.visualState();
        CrusherVisualState.Animation animation = blockEntity.animation();
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        animate(animation, state, time);
        CrusherChassisMaterial chassis = blockEntity.getBlockState().getBlock() instanceof CrusherBlock crusher
                ? crusher.material()
                : CrusherChassisMaterial.IRON;
        boolean wooden = chassis == CrusherChassisMaterial.WOODEN;
        int seed = (int) blockEntity.getBlockPos().asLong();

        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());

        ModelResourceLocation roll = ROLL_MODELS.get(state.head());
        if (roll != null) {
            float turn = animation.angle * 360.0F + (state.jammed() ? Mth.sin(time * 2.3F) * 3.0F : 0.0F);
            for (int i = 0; i < ROLL_XS.length; i++) {
                renderRoll(poseStack, cutout, roll, ROLL_XS[i], ROLL_Y, ROLL_TIP, i == 0 ? -turn : turn, packedLight, packedOverlay);
            }
            if (batching(chassis)) {
                float fast = turn * ROLL_TIP / SECONDARY_TIP;
                for (int i = 0; i < SECONDARY_XS.length; i++) {
                    renderRoll(poseStack, cutout, roll, SECONDARY_XS[i], SECONDARY_Y, SECONDARY_TIP, i == 0 ? -fast : fast,
                            packedLight, packedOverlay);
                }
            }
        }
        if (!state.cell().isEmpty()) {
            renderCell(poseStack, cutout, state.cell(), packedLight, packedOverlay);
        }

        poseStack.pushPose();
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        if (animation.hopperSpots == null) {
            long position = blockEntity.getBlockPos().asLong();
            animation.hopperSpots = scatter(HOPPER_SPOTS, HOPPER_SPREAD, position);
            animation.binSpots = scatter(BIN_SPOTS, BIN_SPREAD, position * 31L + 7L);
        }
        float shake = state.running() ? Mth.sin(time * 1.9F) * 0.12F : 0.0F;
        for (int i = 0; i < state.inputPile(); i++) {
            float[] spot = animation.hopperSpots[i];
            renderItem(state.input(), spot[0] + shake, HOPPER_FLOOR + spot[2], spot[1], spot[3], spot[4], blockEntity, poseStack,
                    bufferSource, packedLight, packedOverlay, seed + i);
        }
        for (int i = 0; i < state.outputPile(); i++) {
            float[] spot = animation.binSpots[i];
            renderItem(state.output(), spot[0], BIN_FLOOR + spot[2], spot[1], spot[3], spot[4], blockEntity, poseStack,
                    bufferSource, packedLight, packedOverlay, seed + 7 + i);
        }
        if (state.jammed() && !state.input().isEmpty()) {
            renderStuck(state.input(), blockEntity, poseStack, bufferSource, packedLight, packedOverlay, seed);
        }
        PoseStack.Pose pose = poseStack.last();
        if (!wooden && state.charge() >= 0.0F) {
            renderCharge(cutout, pose, state.chargeSegments(), packedOverlay);
        }
        if (!wooden && state.jammed() && Mth.sin(time * 0.6F) > -0.2F) {
            renderJamLamp(cutout, pose, packedOverlay);
        }
        poseStack.popPose();
        poseStack.popPose();
    }

    /** Each spot moved within its spread, with a random yaw and a small tilt: x, z, lift, yaw, tilt. */
    private static float[][] scatter(float[][] spots, float[] spread, long seed) {
        RandomSource random = RandomSource.create(seed);
        float[][] scattered = new float[spots.length][];
        for (int i = 0; i < spots.length; i++) {
            scattered[i] = new float[] {
                    spots[i][0] + (random.nextFloat() * 2.0F - 1.0F) * spread[0],
                    spots[i][1] + (random.nextFloat() * 2.0F - 1.0F) * spread[1],
                    spots[i][2],
                    random.nextFloat() * 360.0F,
                    (random.nextFloat() * 2.0F - 1.0F) * MAX_TILT
            };
        }
        return scattered;
    }

    private static boolean batching(CrusherChassisMaterial chassis) {
        return chassis == CrusherChassisMaterial.TUNGSTENSTEEL || chassis == CrusherChassisMaterial.EXOTIC;
    }

    /** The rolls spin up quickly and run down slowly; a jam stops them at once. */
    private static void animate(CrusherVisualState.Animation animation, CrusherVisualState state, float time) {
        float elapsed = Double.isNaN(animation.lastTime) ? 0.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        float target = 0.0F;
        if (state.running() && state.craftTicks() > 0) {
            target = Mth.clamp(TURNS_PER_CRAFT / state.craftTicks(), MIN_TURNS_PER_TICK, MAX_TURNS_PER_TICK);
        }
        if (state.jammed()) {
            animation.speed = 0.0F;
        } else {
            float rate = target > animation.speed ? 0.12F : 0.04F;
            animation.speed += (target - animation.speed) * Math.min(1.0F, elapsed * rate);
        }
        animation.angle = (animation.angle + animation.speed * elapsed) % 1.0F;
    }

    private void renderRoll(
            PoseStack poseStack, VertexConsumer consumer, ModelResourceLocation roll,
            float x, float y, float tip, float degrees, int light, int overlay
    ) {
        float scale = tip / PART_TIP;
        poseStack.pushPose();
        poseStack.translate(x / 16.0F, y / 16.0F, ROLL_Z0 / 16.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(degrees));
        poseStack.scale(scale, scale, (ROLL_Z1 - ROLL_Z0) / PART_LENGTH);
        poseStack.translate(-0.5F, -0.5F, -PART_Z0 / 16.0F);
        renderModel(poseStack, consumer, roll, light, overlay);
        poseStack.popPose();
    }

    /** The cell lies on its side across the saddles, its axis along z. */
    private void renderCell(PoseStack poseStack, VertexConsumer consumer, String kind, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(CELL_X / 16.0F, CELL_Y / 16.0F, CELL_Z / 16.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(CELL_SCALE, CELL_SCALE, CELL_SCALE);
        poseStack.translate(-0.5F, -CELL_HALF_LENGTH / 16.0F, -0.5F);
        renderModel(poseStack, consumer, cellModel(kind), light, overlay);
        poseStack.popPose();
    }

    private void renderModel(PoseStack poseStack, VertexConsumer consumer, ModelResourceLocation location, int light, int overlay) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
        modelRenderer.renderModel(poseStack.last(), consumer, null, model, 1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY, RenderType.cutout());
    }

    /** One copy of an item lying on a floor: flat items lie flat, block items sit as small cubes, each a little tilted. */
    private void renderItem(
            ItemStack stack, float x, float floor, float z, float yaw, float tilt, CrusherBlockEntity blockEntity,
            PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay, int seed
    ) {
        boolean block = stack.getItem() instanceof BlockItem;
        poseStack.pushPose();
        poseStack.translate(x, floor + (block ? ITEM_SIZE / 4.0F : 0.25F), z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(block ? tilt * 0.5F : 90.0F + tilt));
        poseStack.scale(ITEM_SIZE, ITEM_SIZE, ITEM_SIZE);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, poseStack, bufferSource, blockEntity.getLevel(), seed);
        poseStack.popPose();
    }

    /** The feed a jam left wedged in the nip, in front of the rolls. */
    private void renderStuck(
            ItemStack stack, CrusherBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource,
            int light, int overlay, int seed
    ) {
        boolean block = stack.getItem() instanceof BlockItem;
        float size = block ? ITEM_SIZE * 0.8F : ITEM_SIZE * 0.6F;
        poseStack.pushPose();
        poseStack.translate(8.0F, ROLL_Y + 0.5F, NIP_Z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(25.0F));
        poseStack.scale(size, size, size);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, poseStack, bufferSource, blockEntity.getLevel(), seed + 13);
        poseStack.popPose();
    }

    /** Lit segments of the charge meter, bottom up. */
    private static void renderCharge(VertexConsumer consumer, PoseStack.Pose pose, int segments, int overlay) {
        TextureAtlasSprite sprite = sprite(LED);
        float x = METER_X + SURFACE;
        Vector3f east = new Vector3f(1.0F, 0.0F, 0.0F);
        for (int i = 0; i < segments; i++) {
            float y0 = LED_Y0 + i * LED_PITCH;
            float y1 = y0 + LED_HEIGHT;
            quad(consumer, pose, east, LED_COLOR, LightTexture.FULL_BRIGHT, overlay,
                    new float[] {x, y0, LED_Z0, sprite.getU0(), sprite.getV1()},
                    new float[] {x, y0, LED_Z1, sprite.getU1(), sprite.getV1()},
                    new float[] {x, y1, LED_Z1, sprite.getU1(), sprite.getV0()},
                    new float[] {x, y1, LED_Z0, sprite.getU0(), sprite.getV0()});
        }
    }

    private static void renderJamLamp(VertexConsumer consumer, PoseStack.Pose pose, int overlay) {
        TextureAtlasSprite sprite = sprite(LED);
        float z = LAMP_FRONT - SURFACE;
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), JAM_COLOR, LightTexture.FULL_BRIGHT, overlay,
                new float[] {LAMP[0], LAMP[1], z, sprite.getU0(), sprite.getV1()},
                new float[] {LAMP[2], LAMP[1], z, sprite.getU1(), sprite.getV1()},
                new float[] {LAMP[2], LAMP[3], z, sprite.getU1(), sprite.getV0()},
                new float[] {LAMP[0], LAMP[3], z, sprite.getU0(), sprite.getV0()});
    }

    /**
     * One quad from four {x, y, z, u, v} corners in perimeter order. The winding is flipped when needed so the quad
     * faces along {@code normal} and survives back-face culling.
     */
    private static void quad(
            VertexConsumer consumer, PoseStack.Pose pose, Vector3f normal, int color, int light, int overlay,
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
                    .setColor(red, green, blue, 255)
                    .setUv(corner[3], corner[4])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose, normal.x(), normal.y(), normal.z());
        }
    }

    private static TextureAtlasSprite sprite(ResourceLocation id) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(id);
    }
}
