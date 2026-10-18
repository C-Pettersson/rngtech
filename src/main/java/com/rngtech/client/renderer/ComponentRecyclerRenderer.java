package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.ComponentRecyclerBlock;
import com.rngtech.content.blockentity.ComponentRecyclerBlockEntity;
import com.rngtech.content.blockentity.ComponentRecyclerVisualState;
import com.rngtech.content.energy.RecoveryFilterMaterial;
import com.rngtech.content.recycling.DisassemblyHeadMaterial;

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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws the working parts of a Component Recycler: the installed Disassembly Head as a claw that turns on its spindle,
 * faster for a shorter craft, and dips onto the item on the turntable while it works; the Recovery Filter cartridge in
 * its cage; the Battery Cell lying in its cradle with a charge meter beside it; and each output slot's returns standing
 * in its tray. The Crude Recycler turns its runner stone, with the input on it, while its Hand Crank turns. Positions
 * are in model pixels and match tools/blockmodels/component_recycler.py.
 */
public class ComponentRecyclerRenderer implements BlockEntityRenderer<ComponentRecyclerBlockEntity> {
    private static final Map<String, ModelResourceLocation> CLAW_MODELS = new HashMap<>();
    private static final Map<String, ModelResourceLocation> FILTER_MODELS = new HashMap<>();
    private static final ModelResourceLocation RUNNER = part("runner");
    private static final ResourceLocation LED = RNGTech.id("block/crusher/fx/led");
    // The claw part is built around (8, 8) and hangs over the turntable; it lifts by CLAW_LIFT while idle.
    private static final float PART_CENTRE = 8.0F;
    private static final float TABLE_X = 8.0F;
    private static final float TABLE_Z = 5.0F;
    private static final float TABLE_TOP = 7.0F;
    private static final float CLAW_LIFT = 1.0F;
    private static final float CLAW_REACH = 0.6F;
    /** Claw turns per craft, kept between a visible crawl and a blur. */
    private static final float TURNS_PER_CRAFT = 4.0F;
    private static final float MIN_TURNS_PER_TICK = 1.0F / 80.0F;
    private static final float MAX_TURNS_PER_TICK = 1.0F / 8.0F;
    /** The Hand Crank turns a quarter turn clockwise per crank, and its shaft turns the runner stone with it. */
    private static final float RUNNER_TURNS_PER_CRANK = 1.0F / 4.0F;
    private static final float RUNNER_EASE_PER_TICK = 0.3F;
    private static final float RUNNER_TOP = 7.0F;
    private static final float RUNNER_Z = 8.0F;
    /** The Crude Recycler's input rides the runner stone this far out from the drive shaft. */
    private static final float RUNNER_ORBIT = 1.9F;
    // Battery Cell lying along z in the east cradle. Its part model is 6 pixels wide and 10 tall.
    private static final float CELL_X = 14.0F;
    private static final float CELL_Y = 4.25F;
    private static final float CELL_Z = 12.75F;
    private static final float CELL_SCALE = 0.5F;
    private static final float CELL_HALF_LENGTH = 5.0F;
    // Charge meter on the east face of the cabinet.
    private static final float METER_X = 12.75F;
    private static final float LED_Y0 = 6.5F;
    private static final float LED_PITCH = 1.0F;
    private static final float LED_HEIGHT = 0.75F;
    private static final float LED_Z0 = 11.75F;
    private static final float LED_Z1 = 13.25F;
    // Return trays, west to east: the centre of each, its floor, and where each copy stands, front to back.
    private static final float[] TRAY_XS = {4.125F, 8.0F, 11.875F};
    private static final float TRAY_FLOOR = 2.25F;
    private static final float[][] TRAY_COPIES = {{0.0F, 1.75F, 0.0F}, {-0.4F, 3.25F, 14.0F}, {0.4F, 4.75F, -12.0F}};
    private static final float[] CRUDE_TRAY_XS = {4.5F, 8.0F, 11.5F};
    private static final float CRUDE_TRAY_FLOOR = 2.25F;
    private static final float[][] CRUDE_COPIES = {{0.0F, 2.5F, 0.0F}, {-0.35F, 3.0F, 12.0F}, {0.35F, 3.5F, -10.0F}};
    private static final float RETURN_SIZE = 6.0F;
    private static final float RETURN_LEAN = -8.0F;
    private static final float INPUT_SIZE = 9.0F;
    private static final float CRUDE_INPUT_SIZE = 6.0F;
    private static final float SURFACE = 0.02F;
    private static final int LED_COLOR = 0x8CF07A;

    static {
        for (DisassemblyHeadMaterial material : DisassemblyHeadMaterial.values()) {
            CLAW_MODELS.put(material.getSerializedName(), part("claw_" + material.getSerializedName()));
        }
        for (RecoveryFilterMaterial material : RecoveryFilterMaterial.values()) {
            FILTER_MODELS.put(material.getSerializedName(), part("filter_" + material.getSerializedName()));
        }
    }

    private final ModelBlockRenderer modelRenderer;
    private final ItemRenderer itemRenderer;

    public ComponentRecyclerRenderer(BlockEntityRendererProvider.Context context) {
        modelRenderer = context.getBlockRenderDispatcher().getModelRenderer();
        itemRenderer = context.getItemRenderer();
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        CLAW_MODELS.values().forEach(event::register);
        FILTER_MODELS.values().forEach(event::register);
        event.register(RUNNER);
    }

    private static ModelResourceLocation part(String name) {
        return new ModelResourceLocation(RNGTech.id("block/component_recycler/" + name), ModelResourceLocation.STANDALONE_VARIANT);
    }

    /** The Battery Chassis renderer registers every cell part model. */
    private static ModelResourceLocation cellModel(String kind) {
        return new ModelResourceLocation(RNGTech.id("block/battery_chassis/cell_" + kind), ModelResourceLocation.STANDALONE_VARIANT);
    }

    @Override
    public void render(
            ComponentRecyclerBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        BlockState blockState = blockEntity.getBlockState();
        boolean manual = blockState.getBlock() instanceof ComponentRecyclerBlock recycler && recycler.chassis().manual();
        boolean working = blockState.getValue(BaseMachineBlock.ACTIVE);
        ComponentRecyclerVisualState state = blockEntity.visualState();
        ComponentRecyclerVisualState.Animation animation = blockEntity.animation();
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        animate(animation, state, manual, working, time);
        int seed = (int) blockEntity.getBlockPos().asLong();

        Direction facing = blockState.getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        // A positive turn about +Y is anticlockwise seen from above; the runner follows the crank clockwise.
        float turn = animation.angle * (manual ? -360.0F : 360.0F);

        if (manual) {
            renderTurning(poseStack, cutout, RUNNER, RUNNER_Z, 0.0F, turn, packedLight, packedOverlay);
        } else {
            ModelResourceLocation claw = CLAW_MODELS.get(state.head());
            if (claw != null) {
                float lift = CLAW_LIFT - animation.dip * (1.0F - CLAW_REACH + CLAW_REACH * Mth.sin(time * 0.3F));
                renderTurning(poseStack, cutout, claw, TABLE_Z, lift, turn, packedLight, packedOverlay);
            }
            ModelResourceLocation filter = FILTER_MODELS.get(state.filter());
            if (filter != null) {
                renderModel(poseStack, cutout, filter, packedLight, packedOverlay);
            }
            if (!state.cell().isEmpty()) {
                renderCell(poseStack, cutout, state.cell(), packedLight, packedOverlay);
            }
        }

        poseStack.pushPose();
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        if (!state.input().isEmpty()) {
            if (manual) {
                float radians = turn * Mth.DEG_TO_RAD;
                renderInput(state.input(), CRUDE_INPUT_SIZE, TABLE_X + RUNNER_ORBIT * Mth.sin(radians), RUNNER_TOP,
                        RUNNER_Z + RUNNER_ORBIT * Mth.cos(radians), turn, blockEntity, poseStack, bufferSource, packedLight,
                        packedOverlay, seed);
            } else {
                float shake = working ? Mth.sin(time * 1.7F) * 0.1F : 0.0F;
                renderInput(state.input(), INPUT_SIZE, TABLE_X + shake, TABLE_TOP, TABLE_Z, (seed & 0xFF) * 1.4F, blockEntity,
                        poseStack, bufferSource, packedLight, packedOverlay, seed);
            }
        }
        float[] trays = manual ? CRUDE_TRAY_XS : TRAY_XS;
        float[][] copies = manual ? CRUDE_COPIES : TRAY_COPIES;
        for (int i = 0; i < state.outputs().size() && i < trays.length; i++) {
            ItemStack output = state.outputs().get(i);
            for (int copy = Math.min(output.getCount(), copies.length) - 1; copy >= 0; copy--) {
                float[] spot = copies[copy];
                renderReturn(output, trays[i] + spot[0], manual ? CRUDE_TRAY_FLOOR : TRAY_FLOOR, spot[1], spot[2], blockEntity,
                        poseStack, bufferSource, packedLight, packedOverlay, seed + 7 * i + copy);
            }
        }
        if (!manual && state.charge() >= 0.0F) {
            renderCharge(cutout, poseStack.last(), state.chargeSegments(), packedOverlay);
        }
        poseStack.popPose();
        poseStack.popPose();
    }

    /** The claw spins up quickly and runs down slowly, and reaches down to the turntable while it works. */
    private static void animate(
            ComponentRecyclerVisualState.Animation animation, ComponentRecyclerVisualState state, boolean manual,
            boolean working, float time
    ) {
        boolean first = Double.isNaN(animation.lastTime);
        float elapsed = first ? 0.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        if (manual) {
            // The stone moves only when the crank does: it eases toward a step per crank, and jumps there when it
            // first appears or when the count restarts after a reload.
            float step = state.cranks() * RUNNER_TURNS_PER_CRANK;
            if (first || step < animation.angle - 1.0F) {
                animation.angle = step;
            }
            animation.angle += (step - animation.angle) * Math.min(1.0F, elapsed * RUNNER_EASE_PER_TICK);
            return;
        }
        float target = 0.0F;
        if (working && state.craftTicks() > 0) {
            target = Mth.clamp(TURNS_PER_CRAFT / state.craftTicks(), MIN_TURNS_PER_TICK, MAX_TURNS_PER_TICK);
        }
        float rate = target > animation.speed ? 0.12F : 0.05F;
        animation.speed += (target - animation.speed) * Math.min(1.0F, elapsed * rate);
        animation.angle = (animation.angle + animation.speed * elapsed) % 1.0F;
        float reach = working ? 1.0F : 0.0F;
        animation.dip += (reach - animation.dip) * Math.min(1.0F, elapsed * 0.08F);
    }

    /** A part built around (8, 8), drawn over the turntable or quern at {@code z}, raised by {@code lift}, and turned. */
    private void renderTurning(
            PoseStack poseStack, VertexConsumer consumer, ModelResourceLocation location, float z, float lift, float degrees,
            int light, int overlay
    ) {
        poseStack.pushPose();
        poseStack.translate(TABLE_X / 16.0F, lift / 16.0F, z / 16.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(degrees));
        poseStack.translate(-PART_CENTRE / 16.0F, 0.0F, -PART_CENTRE / 16.0F);
        renderModel(poseStack, consumer, location, light, overlay);
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

    /** The item being recycled, lying on the turntable or runner stone: flat items lie flat, block items sit as a cube. */
    private void renderInput(
            ItemStack stack, float size, float x, float floor, float z, float yaw, ComponentRecyclerBlockEntity blockEntity,
            PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay, int seed
    ) {
        boolean block = stack.getItem() instanceof BlockItem;
        poseStack.pushPose();
        poseStack.translate(x, floor + (block ? size / 4.0F : 0.25F), z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        if (!block) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        }
        poseStack.scale(size, size, size);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, poseStack, bufferSource, blockEntity.getLevel(), seed);
        poseStack.popPose();
    }

    /** One copy of a return standing in its tray, facing out of the front and leaning back a little. */
    private void renderReturn(
            ItemStack stack, float x, float floor, float z, float yaw, ComponentRecyclerBlockEntity blockEntity,
            PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay, int seed
    ) {
        poseStack.pushPose();
        poseStack.translate(x, floor + RETURN_SIZE / 4.0F, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(RETURN_LEAN));
        poseStack.scale(RETURN_SIZE, RETURN_SIZE, RETURN_SIZE);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, poseStack, bufferSource, blockEntity.getLevel(), seed);
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
