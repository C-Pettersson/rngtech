package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.BatteryAssemblerBlockEntity;
import com.rngtech.content.blockentity.BatteryAssemblerVisualState;

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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the working parts of a Component Assembler: the gantry's bridge, carriage, and quill, which follow the job,
 * picking the item from each filled jig pocket in turn, carrying it to its own spot on the nest, and pouring the assembly
 * fluid over them last; the item in each input slot lying in its pocket until it is picked, unless the slot holds more;
 * the puddle that covers the delivered items and soaks in to reveal the finished part, which replaces them without fluid
 * at the last set-down; the finished items standing
 * in the output cubbies; the fluid in the roof reservoir; and the Battery Cell in its cradle with its charge meter.
 * Positions are in model pixels and match tools/blockmodels/battery_assembler.py.
 */
public class BatteryAssemblerRenderer implements BlockEntityRenderer<BatteryAssemblerBlockEntity> {
    private static final ModelResourceLocation BRIDGE = part("bridge");
    private static final ModelResourceLocation CARRIAGE = part("carriage");
    private static final ModelResourceLocation QUILL = part("quill");
    private static final ResourceLocation LED = RNGTech.id("block/crusher/fx/led");
    /** The parts are built with the head over (8, 8); the bridge moves in z and the carriage and quill in x and z. */
    private static final float PART_CENTRE = 8.0F;
    // Jig pockets along the front of the bed, by input slot from left to right seen from the front, and the nest behind them.
    private static final float[] POCKET_XS = {12.1F, 9.4F, 6.6F, 3.9F};
    private static final float POCKET_Z = 3.25F;
    private static final float POCKET_FLOOR = 4.75F;
    private static final float INPUT_SIZE = 2.2F;
    private static final float NEST_X = 8.0F;
    private static final float NEST_Z = 7.5F;
    /** The floor of the nest's basin, under its lip. */
    private static final float NEST_FLOOR = 5.25F;
    /** Where each delivered item lies in the basin, in delivery order, as {dx, dz} from its centre; the fingers clear the lip. */
    private static final float[][] NEST_SPOTS = {{0.5F, -0.55F}, {-0.5F, -0.55F}, {0.5F, 0.55F}, {-0.5F, 0.55F}};
    private static final float PRODUCT_SIZE = 3.0F;
    // How far the quill dips at a pocket and at the nest, so the finger tips reach the pocket floor and the nest's basin
    // floor, and how far it lifts while idle; the rod stays inside the carriage over the whole range.
    private static final float POCKET_DIP = 1.5F;
    private static final float NEST_DIP = 1.0F;
    private static final float IDLE_LIFT = 0.25F;
    /** Finger tips at rest; a carried item hangs with its underside here. */
    private static final float FINGER_TIP = 6.25F;
    /** Each station is a travel and then a dip; this share of it is travel. */
    private static final float TRAVEL_SHARE = 0.55F;
    /** The share of a station at the bottom of its dip, where the head picks up or sets down. */
    private static final float GRAB = TRAVEL_SHARE + (1.0F - TRAVEL_SHARE) / 2.0F;
    /** The share of a job with fluid spent pouring it and letting it soak in, whatever the number of pockets. */
    private static final float DOSE_SHARE = 0.3F;
    // Within the dose: the head moves over the nest centre, pours until POUR_END, and the puddle then soaks in.
    private static final float DOSE_TRAVEL = 0.2F;
    private static final float POUR_END = 0.6F;
    /** The head hovers this far above its rest height while it pours, so the stream shows. */
    private static final float DOSE_HOVER = 1.25F;
    // Nozzle tip at rest, the stream from it, and the puddle it leaves over the delivered items, inside the nest's lip.
    private static final float NOZZLE_TIP = 6.5F;
    private static final float STREAM_HALF = 0.2F;
    private static final float[] PUDDLE = {6.52F, 6.27F, 9.48F, 8.73F};
    private static final float PUDDLE_DEPTH = 0.45F;
    // Output cubbies by output slot from left to right seen from the front: each centre, the floor, and where each copy
    // stands, front to back.
    private static final float[] CUBBY_XS = {12.875F, 9.625F, 6.375F, 3.125F};
    private static final float CUBBY_FLOOR = 6.0F;
    private static final float[][] CUBBY_COPIES = {{0.0F, 11.0F, 0.0F}, {-0.35F, 11.6F, 12.0F}, {0.35F, 12.1F, -10.0F}};
    private static final float OUTPUT_SIZE = 2.5F;
    private static final float OUTPUT_LEAN = -8.0F;
    // Reservoir on the roof, as the inside between its end caps and rails.
    private static final float[] TANK = {2.5F, 12.05F, 11.05F, 11.0F, 15.45F, 14.45F};
    // Battery Cell lying along z in the roof cradle. Its part model is 6 pixels wide and 10 tall.
    private static final float CELL_X = 13.5F;
    private static final float CELL_Y = 13.85F;
    private static final float CELL_Z = 12.875F;
    private static final float CELL_SCALE = 0.45F;
    private static final float CELL_HALF_LENGTH = 5.0F;
    // Charge meter on the east face of the cabinet.
    private static final float METER_X = 15.25F;
    private static final float LED_Y0 = 6.5F;
    private static final float LED_PITCH = 1.0F;
    private static final float LED_HEIGHT = 0.75F;
    private static final float LED_Z0 = 12.25F;
    private static final float LED_Z1 = 13.75F;
    private static final float SURFACE = 0.02F;
    private static final int FLUID_ALPHA = 220;
    private static final int LED_COLOR = 0x8CF07A;

    private final ModelBlockRenderer modelRenderer;
    private final ItemRenderer itemRenderer;

    public BatteryAssemblerRenderer(BlockEntityRendererProvider.Context context) {
        modelRenderer = context.getBlockRenderDispatcher().getModelRenderer();
        itemRenderer = context.getItemRenderer();
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(BRIDGE);
        event.register(CARRIAGE);
        event.register(QUILL);
    }

    private static ModelResourceLocation part(String name) {
        return new ModelResourceLocation(RNGTech.id("block/battery_assembler/" + name), ModelResourceLocation.STANDALONE_VARIANT);
    }

    /** The Battery Chassis renderer registers every cell part model. */
    private static ModelResourceLocation cellModel(String kind) {
        return new ModelResourceLocation(RNGTech.id("block/battery_chassis/cell_" + kind), ModelResourceLocation.STANDALONE_VARIANT);
    }

    @Override
    public void render(
            BatteryAssemblerBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        BatteryAssemblerVisualState state = blockEntity.visualState();
        BatteryAssemblerVisualState.Animation animation = blockEntity.animation();
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        List<float[]> stations = stations(state);
        float cycle = animate(animation, state, stations, time);
        float dose = state.doses() && cycle >= stations.size() - 1 ? cycle - (stations.size() - 1) : -1.0F;
        int seed = (int) blockEntity.getBlockPos().asLong();

        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        float dx = (animation.x - PART_CENTRE) / 16.0F;
        float dz = (animation.z - PART_CENTRE) / 16.0F;
        renderPart(poseStack, cutout, BRIDGE, 0.0F, 0.0F, dz, packedLight, packedOverlay);
        renderPart(poseStack, cutout, CARRIAGE, dx, 0.0F, dz, packedLight, packedOverlay);
        renderPart(poseStack, cutout, QUILL, dx, -animation.dip / 16.0F, dz, packedLight, packedOverlay);
        if (!state.cell().isEmpty()) {
            renderCell(poseStack, cutout, state.cell(), packedLight, packedOverlay);
        }

        // Each item is picked at the bottom of its pocket's dip, carried, and set down at the bottom of its nest dip. The
        // finished part takes their place under a full puddle once the pour ends, or at the last set-down without fluid.
        float finished = cycle < 0.0F ? Float.MAX_VALUE : stations.size() - 1 + (state.doses() ? POUR_END : GRAB);
        int delivery = 0;
        for (int slot = 0; slot < state.inputs().size() && slot < POCKET_XS.length; slot++) {
            ItemStack input = state.inputs().get(slot);
            if (input.isEmpty()) {
                continue;
            }
            float yaw = (seed & 0x3F) + slot * 37.0F;
            boolean picked = cycle >= 2 * delivery + GRAB;
            float placed = 2 * delivery + 1 + GRAB;
            if (!picked || input.getCount() > 1) {
                renderLying(input, INPUT_SIZE, POCKET_XS[slot], POCKET_FLOOR, POCKET_Z, yaw, blockEntity, poseStack, bufferSource,
                        packedLight, packedOverlay, seed + slot);
            }
            if (picked && cycle < placed) {
                renderLying(input, INPUT_SIZE, animation.x, FINGER_TIP - animation.dip, animation.z, yaw, blockEntity, poseStack,
                        bufferSource, packedLight, packedOverlay, seed + slot);
            } else if (picked && cycle < finished) {
                float[] spot = NEST_SPOTS[delivery % NEST_SPOTS.length];
                renderLying(input, INPUT_SIZE, NEST_X + spot[0], NEST_FLOOR, NEST_Z + spot[1], yaw, blockEntity, poseStack,
                        bufferSource, packedLight, packedOverlay, seed + slot);
            }
            delivery++;
        }
        if (cycle >= finished && !state.product().isEmpty()) {
            renderLying(state.product(), PRODUCT_SIZE, NEST_X, NEST_FLOOR, NEST_Z, 15.0F, blockEntity, poseStack, bufferSource,
                    packedLight, packedOverlay, seed + 4);
        }
        for (int slot = 0; slot < state.outputs().size() && slot < CUBBY_XS.length; slot++) {
            ItemStack output = state.outputs().get(slot);
            for (int copy = Math.min(output.getCount(), CUBBY_COPIES.length) - 1; copy >= 0; copy--) {
                float[] spot = CUBBY_COPIES[copy];
                renderStanding(output, CUBBY_XS[slot] + spot[0], spot[1], spot[2], blockEntity, poseStack, bufferSource,
                        packedLight, packedOverlay, seed + 7 * slot + copy);
            }
        }

        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        PoseStack.Pose pose = poseStack.last();
        if (state.charge() >= 0.0F) {
            renderCharge(bufferSource.getBuffer(Sheets.cutoutBlockSheet()), pose, state.chargeSegments(), packedOverlay);
        }
        Fluid fluid = fluid(state.fluid());
        if (fluid != Fluids.EMPTY) {
            VertexConsumer translucent = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
            if (animation.fill > 0.005F) {
                float top = TANK[1] + (TANK[4] - TANK[1]) * Mth.clamp(animation.fill, 0.0F, 1.0F);
                renderFluid(translucent, pose, fluid, TANK[0], TANK[1], TANK[2], TANK[3], top, TANK[5], packedLight, packedOverlay);
            }
            if (dose >= DOSE_TRAVEL) {
                renderDose(translucent, pose, fluid, dose, state.working(), animation, packedLight, packedOverlay);
            }
        }
        poseStack.popPose();
    }

    /**
     * The dose, by its share {@code dose} of the last station: while the head pours, a stream runs from the nozzle onto
     * the nest and a puddle rises over the delivered items; once the pour ends the puddle soaks in, revealing the
     * finished part.
     */
    private static void renderDose(
            VertexConsumer consumer, PoseStack.Pose pose, Fluid fluid, float dose, boolean working,
            BatteryAssemblerVisualState.Animation animation, int light, int overlay
    ) {
        boolean pouring = dose < POUR_END;
        float depth = pouring
                ? PUDDLE_DEPTH * (dose - DOSE_TRAVEL) / (POUR_END - DOSE_TRAVEL)
                : PUDDLE_DEPTH * (1.0F - (dose - POUR_END) / (1.0F - POUR_END));
        float surface = NEST_FLOOR + Math.max(0.05F, depth);
        if (depth > 0.01F) {
            renderFluid(consumer, pose, fluid, PUDDLE[0], NEST_FLOOR + 0.01F, PUDDLE[1], PUDDLE[2], surface, PUDDLE[3], light, overlay);
        }
        if (pouring && working) {
            float tip = NOZZLE_TIP - animation.dip;
            renderFluid(consumer, pose, fluid, animation.x - STREAM_HALF, surface, animation.z - STREAM_HALF,
                    animation.x + STREAM_HALF, tip, animation.z + STREAM_HALF, light, overlay);
        }
    }

    /**
     * Runs the job on between updates and moves the head along it. Each filled pocket is two stations, the pocket and
     * then that item's spot on the nest, and a recipe with fluid ends with a dose over the nest, which takes
     * {@value #DOSE_SHARE} of the job. Each pocket and nest station is a travel, then a dip; the dose is a travel, then a
     * hover while it pours and soaks in. Returns how far through the stations the job is, or -1 when no job is under way.
     */
    private static float animate(
            BatteryAssemblerVisualState.Animation animation, BatteryAssemblerVisualState state, List<float[]> stations, float time
    ) {
        boolean first = Double.isNaN(animation.lastTime);
        float elapsed = first ? 0.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        animation.fill = first ? state.fill() : animation.fill + (state.fill() - animation.fill) * Math.min(1.0F, elapsed * 0.1F);
        if (state.progress() != animation.syncedProgress) {
            animation.syncedProgress = state.progress();
            animation.progress = state.progress();
        } else if (state.working() && state.craftTicks() > 0) {
            animation.progress = Math.min(1.0F, animation.progress + elapsed / state.craftTicks());
        }

        float cycle = animation.progress > 0.0F && !stations.isEmpty() ? cycle(Math.min(animation.progress, 0.9999F), stations.size(), state.doses()) : -1.0F;
        float targetX = NEST_X;
        float targetZ = NEST_Z;
        float dip = -IDLE_LIFT;
        if (cycle >= 0.0F) {
            int index = (int) cycle;
            float share = cycle - index;
            float[] from = index == 0 ? new float[] {NEST_X, NEST_Z, 0.0F, 0.0F} : stations.get(index - 1);
            float[] to = stations.get(index);
            if (to[3] > 0.0F) {
                float s = Math.min(1.0F, share / DOSE_TRAVEL);
                s = s * s * (3.0F - 2.0F * s);
                targetX = Mth.lerp(s, from[0], to[0]);
                targetZ = Mth.lerp(s, from[1], to[1]);
                dip = -DOSE_HOVER * s;
            } else if (share < TRAVEL_SHARE) {
                float s = share / TRAVEL_SHARE;
                s = s * s * (3.0F - 2.0F * s);
                targetX = Mth.lerp(s, from[0], to[0]);
                targetZ = Mth.lerp(s, from[1], to[1]);
                dip = 0.0F;
            } else {
                targetX = to[0];
                targetZ = to[1];
                dip = to[2] * Mth.sin(Mth.PI * (share - TRAVEL_SHARE) / (1.0F - TRAVEL_SHARE));
            }
        }
        float follow = first ? 1.0F : Math.min(1.0F, elapsed * 0.5F);
        animation.x += (targetX - animation.x) * follow;
        animation.z += (targetZ - animation.z) * follow;
        animation.dip += (dip - animation.dip) * (first ? 1.0F : Math.min(1.0F, elapsed * 0.6F));
        return cycle;
    }

    /** How far through its stations a job is at {@code progress}: evenly spread, except that a dose takes a fixed share. */
    private static float cycle(float progress, int stations, boolean doses) {
        if (!doses) {
            return progress * stations;
        }
        float handling = 1.0F - DOSE_SHARE;
        return progress < handling
                ? progress / handling * (stations - 1)
                : stations - 1 + (progress - handling) / DOSE_SHARE;
    }

    /**
     * The head's stations as {x, z, dip, dose}: each filled pocket and then that item's spot on the nest, and a final
     * dose on the nest when the recipe takes fluid.
     */
    private static List<float[]> stations(BatteryAssemblerVisualState state) {
        List<float[]> stations = new ArrayList<>();
        for (int slot = 0; slot < state.inputs().size() && slot < POCKET_XS.length; slot++) {
            if (!state.inputs().get(slot).isEmpty()) {
                float[] spot = NEST_SPOTS[stations.size() / 2 % NEST_SPOTS.length];
                stations.add(new float[] {POCKET_XS[slot], POCKET_Z, POCKET_DIP, 0.0F});
                stations.add(new float[] {NEST_X + spot[0], NEST_Z + spot[1], NEST_DIP, 0.0F});
            }
        }
        if (state.doses()) {
            stations.add(new float[] {NEST_X, NEST_Z, 0.0F, 1.0F});
        }
        return stations;
    }

    private void renderPart(
            PoseStack poseStack, VertexConsumer consumer, ModelResourceLocation location, float dx, float dy, float dz, int light, int overlay
    ) {
        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
        modelRenderer.renderModel(poseStack.last(), consumer, null, model, 1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY, RenderType.cutout());
        poseStack.popPose();
    }

    /** The cell lies on its side across the saddles, its axis along z. */
    private void renderCell(PoseStack poseStack, VertexConsumer consumer, String kind, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(CELL_X / 16.0F, CELL_Y / 16.0F, CELL_Z / 16.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(CELL_SCALE, CELL_SCALE, CELL_SCALE);
        poseStack.translate(-0.5F, -CELL_HALF_LENGTH / 16.0F, -0.5F);
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(cellModel(kind));
        modelRenderer.renderModel(poseStack.last(), consumer, null, model, 1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY, RenderType.cutout());
        poseStack.popPose();
    }

    /** An item lying flat, or a block item sitting as a small cube, centred on (x, z) with its underside at y. */
    private void renderLying(
            ItemStack stack, float size, float x, float y, float z, float yaw, BatteryAssemblerBlockEntity blockEntity,
            PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay, int seed
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

    /** One copy of a finished item standing in its cubby, facing out of the front and leaning back a little. */
    private void renderStanding(
            ItemStack stack, float x, float z, float yaw, BatteryAssemblerBlockEntity blockEntity, PoseStack poseStack,
            MultiBufferSource bufferSource, int light, int overlay, int seed
    ) {
        poseStack.pushPose();
        poseStack.translate(x / 16.0F, (CUBBY_FLOOR + OUTPUT_SIZE / 2.0F) / 16.0F, z / 16.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(OUTPUT_LEAN));
        poseStack.scale(OUTPUT_SIZE / 16.0F, OUTPUT_SIZE / 16.0F, OUTPUT_SIZE / 16.0F);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, poseStack, bufferSource, blockEntity.getLevel(), seed);
        poseStack.popPose();
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
        float x = METER_X + SURFACE;
        Vector3f east = new Vector3f(1.0F, 0.0F, 0.0F);
        for (int i = 0; i < segments; i++) {
            float y0 = LED_Y0 + i * LED_PITCH;
            float y1 = y0 + LED_HEIGHT;
            FurnaceGlow.quad(consumer, pose, east, LED_COLOR, 255, LightTexture.FULL_BRIGHT, overlay,
                    new float[] {x, y0, LED_Z0, sprite.getU0(), sprite.getV1()},
                    new float[] {x, y0, LED_Z1, sprite.getU1(), sprite.getV1()},
                    new float[] {x, y1, LED_Z1, sprite.getU1(), sprite.getV0()},
                    new float[] {x, y1, LED_Z0, sprite.getU0(), sprite.getV0()});
        }
    }
}
