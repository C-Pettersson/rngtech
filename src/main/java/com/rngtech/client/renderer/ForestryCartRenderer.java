package com.rngtech.client.renderer;

import com.rngtech.content.blockentity.ForestryCartStationBlockEntity;
import com.rngtech.content.entity.ForestryCartEntity;
import com.rngtech.content.entity.ForestryTreeScan;
import com.rngtech.content.registry.ModItems;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("deprecation")
public class ForestryCartRenderer extends EntityRenderer<ForestryCartEntity> {
    private static final float SCAN_RED = 0.25F;
    private static final float SCAN_GREEN = 0.95F;
    private static final float SCAN_BLUE = 0.55F;
    private static final float LOG_RED = 1.0F;
    private static final float LOG_GREEN = 0.72F;
    private static final float LOG_BLUE = 0.18F;
    private static final float LEAF_RED = 0.48F;
    private static final float LEAF_GREEN = 0.9F;
    private static final float LEAF_BLUE = 0.25F;
    private static final float TARGET_RED = 1.0F;
    private static final float TARGET_GREEN = 0.18F;
    private static final float TARGET_BLUE = 0.12F;
    private static final float TOO_LARGE_RED = 0.95F;
    private static final float TOO_LARGE_GREEN = 0.25F;
    private static final float TOO_LARGE_BLUE = 1.0F;
    private static final int SCAN_FILL_ALPHA = 36;
    private static final int HARVEST_FILL_ALPHA = 18;
    private static final int TARGET_FILL_ALPHA = 54;
    private static final float SCAN_OUTLINE_ALPHA = 0.92F;
    private static final float HARVEST_OUTLINE_ALPHA = 0.88F;
    private static final float TARGET_OUTLINE_ALPHA = 1.0F;
    private static final int SCAN_BOUND_HORIZONTAL = 8;
    private static final int REMAINING_HARVEST_SEARCH_HORIZONTAL = SCAN_BOUND_HORIZONTAL;
    private static final int MAX_CONNECTED_LEAF_BLOCKS = 192;
    private static final int LEAF_SCAN_RADIUS = 4;
    private static final float HOVER_LIFT = 0.18F;
    private static final float HOVER_BOB_HEIGHT = 0.035F;
    private static final float HOVER_BOB_SPEED = 0.14F;
    private static final float HOVER_PULSE_SPEED = 0.22F;
    private static final int HOVER_BLUE_RED = 56;
    private static final int HOVER_BLUE_GREEN = 212;
    private static final int HOVER_BLUE_BLUE = 255;
    private static final float TURN_DEGREES_PER_TICK = 16.0F;
    private static final float TURN_BANK_DEGREES = 7.0F;
    private static final int TURN_STATE_EXPIRY_TICKS = 80;
    private static final int BROKEN_TOOL_CUSTOM_MODEL_DATA = 3;
    private static final int PLAYER_BLOCKED_CUSTOM_MODEL_DATA = 4;
    private static final int CHOPPING_LEAVES_CUSTOM_MODEL_DATA = 5;
    private static final int CHOPPING_LOGS_CUSTOM_MODEL_DATA = 6;
    private static final int TREEFELLER_CUSTOM_MODEL_DATA = 7;
    private static final int PLANTING_CUSTOM_MODEL_DATA = 8;
    private static final int SCANNING_CUSTOM_MODEL_DATA = 9;
    private static final int MOVING_CUSTOM_MODEL_DATA = 10;
    private static final int NO_POWER_CUSTOM_MODEL_DATA = 11;
    private static final int OUTPUT_FULL_CUSTOM_MODEL_DATA = 12;
    private static final int NO_SAPLINGS_CUSTOM_MODEL_DATA = 13;
    private static final int PATH_BLOCKED_CUSTOM_MODEL_DATA = 14;
    private static final int CLEARING_PATH_CUSTOM_MODEL_DATA = 15;
    private static final int TRANSFER_CUSTOM_MODEL_DATA = 16;
    private static final int SETUP_BLOCKED_CUSTOM_MODEL_DATA = 17;
    private static final int MANAGED_CUSTOM_MODEL_DATA = 18;
    private static final int COOLDOWN_CUSTOM_MODEL_DATA = 19;
    private static final int HARVEST_BLOCKED_CUSTOM_MODEL_DATA = 20;
    private static final int PLANTING_BLOCKED_CUSTOM_MODEL_DATA = 21;
    private static final int SNAPSHOT_CUSTOM_MODEL_DATA = 22;
    private static final int SPRINKLER_HEAD_CUSTOM_MODEL_DATA = 23;
    private static final float SPRINKLER_DEGREES_PER_TICK = 12.0F;
    private static final float SPRINKLER_HEAD_SCALE = 0.6F;
    /** Nozzle tips in the head's local space: the nozzles end at x = 0.5 and 15.5 of 16. */
    private static final float SPRINKLER_NOZZLE_OFFSET = 0.46875F * SPRINKLER_HEAD_SCALE;
    /** Nozzle height above the roof: the nozzles are centered 5 of 16 up the head model. */
    private static final float SPRINKLER_HEAD_HEIGHT = 0.3125F * SPRINKLER_HEAD_SCALE;
    /** A splash droplet launched level falls back to the deck height in about this many ticks, coasting this far per unit speed. */
    private static final double SPRINKLER_DISTANCE_PER_SPEED = 9.5D;
    private static final double[] SPRINKLER_JET_SPREAD = {0.35D, 0.6D, 0.85D, 1.0D};

    private final ItemRenderer itemRenderer;
    private final ItemStack cartStack;
    private final Map<Integer, ItemStack> actionCartStacks = new HashMap<>();
    private final Map<Integer, TurnState> turnStates = new HashMap<>();
    private final Map<Integer, Integer> sprinklerJetTicks = new HashMap<>();
    private final ItemStack sprinklerHeadStack;

    public ForestryCartRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.cartStack = new ItemStack(ModItems.FORESTRY_CART.get());
        registerActionCartStack(BROKEN_TOOL_CUSTOM_MODEL_DATA);
        registerActionCartStack(PLAYER_BLOCKED_CUSTOM_MODEL_DATA);
        registerActionCartStack(CHOPPING_LEAVES_CUSTOM_MODEL_DATA);
        registerActionCartStack(CHOPPING_LOGS_CUSTOM_MODEL_DATA);
        registerActionCartStack(TREEFELLER_CUSTOM_MODEL_DATA);
        registerActionCartStack(PLANTING_CUSTOM_MODEL_DATA);
        registerActionCartStack(SCANNING_CUSTOM_MODEL_DATA);
        registerActionCartStack(MOVING_CUSTOM_MODEL_DATA);
        registerActionCartStack(NO_POWER_CUSTOM_MODEL_DATA);
        registerActionCartStack(OUTPUT_FULL_CUSTOM_MODEL_DATA);
        registerActionCartStack(NO_SAPLINGS_CUSTOM_MODEL_DATA);
        registerActionCartStack(PATH_BLOCKED_CUSTOM_MODEL_DATA);
        registerActionCartStack(CLEARING_PATH_CUSTOM_MODEL_DATA);
        registerActionCartStack(TRANSFER_CUSTOM_MODEL_DATA);
        registerActionCartStack(SETUP_BLOCKED_CUSTOM_MODEL_DATA);
        registerActionCartStack(MANAGED_CUSTOM_MODEL_DATA);
        registerActionCartStack(COOLDOWN_CUSTOM_MODEL_DATA);
        registerActionCartStack(HARVEST_BLOCKED_CUSTOM_MODEL_DATA);
        registerActionCartStack(PLANTING_BLOCKED_CUSTOM_MODEL_DATA);
        registerActionCartStack(SNAPSHOT_CUSTOM_MODEL_DATA);
        this.sprinklerHeadStack = new ItemStack(ModItems.FORESTRY_CART.get());
        sprinklerHeadStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(SPRINKLER_HEAD_CUSTOM_MODEL_DATA));
        this.shadowRadius = 0.7F;
    }

    @Override
    public void render(
            ForestryCartEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        renderScanDebug(entity, partialTick, poseStack, buffer);
        poseStack.pushPose();
        applyMinecartPose(entity, entityYaw, partialTick, poseStack);
        float animationTicks = entity.tickCount + partialTick;
        renderHoverPulse(animationTicks, poseStack, buffer);
        poseStack.translate(0.0F, HOVER_LIFT + Mth.sin(animationTicks * HOVER_BOB_SPEED) * HOVER_BOB_HEIGHT, 0.0F);
        poseStack.scale(1.0F, 0.9F, 1.0F);
        itemRenderer.renderStatic(
                cartStack(entity),
                ItemDisplayContext.NONE,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );
        if (entity.sprinklerReach() > 0) {
            renderSprinkler(entity, animationTicks, poseStack, buffer, packedLight);
        }
        poseStack.popPose();
    }

    /**
     * A sprinkler head spins on the cart's roof and sprays from both arm ends. Jets start at the nozzles as drawn and
     * land about the sprinkler's reach away, once per game tick whatever the frame rate.
     */
    private void renderSprinkler(ForestryCartEntity entity, float animationTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(animationTicks * SPRINKLER_DEGREES_PER_TICK));
        poseStack.pushPose();
        poseStack.translate(0.0F, SPRINKLER_HEAD_SCALE * 0.5F, 0.0F);
        poseStack.scale(SPRINKLER_HEAD_SCALE, SPRINKLER_HEAD_SCALE, SPRINKLER_HEAD_SCALE);
        itemRenderer.renderStatic(sprinklerHeadStack, ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
        Integer lastTick = sprinklerJetTicks.get(entity.getId());
        if (lastTick == null || lastTick != entity.tickCount) {
            sprinklerJetTicks.put(entity.getId(), entity.tickCount);
            sprayJets(entity, poseStack.last().pose());
        }
        poseStack.popPose();
    }

    private void sprayJets(ForestryCartEntity entity, Matrix4f pose) {
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double reach = entity.sprinklerReach() + 0.5D;
        for (int side = -1; side <= 1; side += 2) {
            Vector4f tip = pose.transform(new Vector4f(side * SPRINKLER_NOZZLE_OFFSET, SPRINKLER_HEAD_HEIGHT, 0.0F, 1.0F));
            Vector3f direction = pose.transformDirection(new Vector3f(side, 0.0F, 0.0F));
            double length = Math.hypot(direction.x(), direction.z());
            if (length < 1.0E-4D) {
                continue;
            }
            double dx = direction.x() / length;
            double dz = direction.z() / length;
            for (double spread : SPRINKLER_JET_SPREAD) {
                double speed = reach * spread / SPRINKLER_DISTANCE_PER_SPEED * (0.92D + entity.getRandom().nextDouble() * 0.16D);
                entity.level().addParticle(ParticleTypes.SPLASH, camera.x + tip.x(), camera.y + tip.y(), camera.z + tip.z(), dx * speed, 0.0D, dz * speed);
            }
        }
    }

    private ItemStack cartStack(ForestryCartEntity entity) {
        int customModelData = cartCustomModelData(entity);
        return customModelData == 0 ? cartStack : actionCartStacks.getOrDefault(customModelData, cartStack);
    }

    private void registerActionCartStack(int customModelData) {
        ItemStack stack = new ItemStack(ModItems.FORESTRY_CART.get());
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(customModelData));
        actionCartStacks.put(customModelData, stack);
    }

    private static int cartCustomModelData(ForestryCartEntity entity) {
        if (entity.visualStatusCode() == ForestryCartStationBlockEntity.STATUS_BROKEN_TOOL) {
            return BROKEN_TOOL_CUSTOM_MODEL_DATA;
        }
        return switch (entity.visualActionCode()) {
            case ForestryCartStationBlockEntity.ACTION_PLAYER_BLOCKING_PATH -> PLAYER_BLOCKED_CUSTOM_MODEL_DATA;
            case ForestryCartStationBlockEntity.ACTION_HARVESTING_LEAVES,
                    ForestryCartStationBlockEntity.ACTION_HARVESTING_CROP -> CHOPPING_LEAVES_CUSTOM_MODEL_DATA;
            case ForestryCartStationBlockEntity.ACTION_HARVESTING_LOG -> CHOPPING_LOGS_CUSTOM_MODEL_DATA;
            case ForestryCartStationBlockEntity.ACTION_TREEFELLER_BATCH -> TREEFELLER_CUSTOM_MODEL_DATA;
            case ForestryCartStationBlockEntity.ACTION_PLANTING -> PLANTING_CUSTOM_MODEL_DATA;
            case ForestryCartStationBlockEntity.ACTION_SCANNING_LOG_BASES -> SCANNING_CUSTOM_MODEL_DATA;
            case ForestryCartStationBlockEntity.ACTION_CREATING_SNAPSHOT,
                    ForestryCartStationBlockEntity.ACTION_PROCESSING_SNAPSHOT,
                    ForestryCartStationBlockEntity.ACTION_SNAPSHOT_OUT_OF_RANGE -> SNAPSHOT_CUSTOM_MODEL_DATA;
            case ForestryCartStationBlockEntity.ACTION_MOVING -> MOVING_CUSTOM_MODEL_DATA;
            case ForestryCartStationBlockEntity.ACTION_NO_POWER -> NO_POWER_CUSTOM_MODEL_DATA;
            case ForestryCartStationBlockEntity.ACTION_OUTPUT_FULL -> OUTPUT_FULL_CUSTOM_MODEL_DATA;
            case ForestryCartStationBlockEntity.ACTION_NO_SAPLINGS -> NO_SAPLINGS_CUSTOM_MODEL_DATA;
            case ForestryCartStationBlockEntity.ACTION_PATH_BLOCKED -> PATH_BLOCKED_CUSTOM_MODEL_DATA;
            case ForestryCartStationBlockEntity.ACTION_CLEARING_PATH -> CLEARING_PATH_CUSTOM_MODEL_DATA;
            case ForestryCartStationBlockEntity.ACTION_DOCK_TRANSFER,
                    ForestryCartStationBlockEntity.ACTION_RETURNING_CARGO,
                    ForestryCartStationBlockEntity.ACTION_DOCKED_READY,
                    ForestryCartStationBlockEntity.ACTION_UNLOADING,
                    ForestryCartStationBlockEntity.ACTION_SEEKING_TRANSFER -> TRANSFER_CUSTOM_MODEL_DATA;
            case ForestryCartStationBlockEntity.ACTION_SETUP_BLOCKED -> SETUP_BLOCKED_CUSTOM_MODEL_DATA;
            case ForestryCartStationBlockEntity.ACTION_MANAGED -> MANAGED_CUSTOM_MODEL_DATA;
            case ForestryCartStationBlockEntity.ACTION_WAITING_COOLDOWN,
                    ForestryCartStationBlockEntity.ACTION_HOLDING -> COOLDOWN_CUSTOM_MODEL_DATA;
            case ForestryCartStationBlockEntity.ACTION_HARVEST_BLOCKED -> HARVEST_BLOCKED_CUSTOM_MODEL_DATA;
            case ForestryCartStationBlockEntity.ACTION_PLANTING_BLOCKED -> PLANTING_BLOCKED_CUSTOM_MODEL_DATA;
            default -> 0;
        };
    }

    @Override
    public ResourceLocation getTextureLocation(ForestryCartEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    private static void renderScanDebug(
            ForestryCartEntity entity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer
    ) {
        if (!entity.scanDebugVisible()) {
            return;
        }

        BlockPos railPos = entity.railPosition();
        if (railPos == null) {
            return;
        }

        double entityX = Mth.lerp((double) partialTick, entity.xOld, entity.getX());
        double entityY = Mth.lerp((double) partialTick, entity.yOld, entity.getY());
        double entityZ = Mth.lerp((double) partialTick, entity.zOld, entity.getZ());
        for (BlockPos root : entity.plantingScanRoots(railPos)) {
            renderScanBlock(poseStack, buffer, root, entityX, entityY, entityZ);
            renderHarvestDebug(entity, poseStack, buffer, root, entityX, entityY, entityZ);
        }
    }

    private static void renderHarvestDebug(
            ForestryCartEntity entity,
            PoseStack poseStack,
            MultiBufferSource buffer,
            BlockPos root,
            double entityX,
            double entityY,
            double entityZ
    ) {
        HarvestDebugScan scan = harvestDebugScan(entity, root);
        if (scan.blocks().isEmpty()) {
            return;
        }

        for (BlockPos pos : scan.blocks()) {
            if (!pos.equals(scan.target())) {
                boolean leaves = entity.level().getBlockState(pos).is(BlockTags.LEAVES);
                float red = leaves ? LEAF_RED : LOG_RED;
                float green = leaves ? LEAF_GREEN : LOG_GREEN;
                float blue = leaves ? LEAF_BLUE : LOG_BLUE;
                renderDebugBlock(poseStack, buffer, pos, entityX, entityY, entityZ, red, green, blue, HARVEST_FILL_ALPHA, HARVEST_OUTLINE_ALPHA);
            }
        }
        renderDebugBlock(
                poseStack,
                buffer,
                scan.target(),
                entityX,
                entityY,
                entityZ,
                TARGET_RED,
                TARGET_GREEN,
                TARGET_BLUE,
                TARGET_FILL_ALPHA,
                TARGET_OUTLINE_ALPHA
        );
        if (scan.tooLarge()) {
            renderScanBounds(poseStack, buffer, root, entityX, entityY, entityZ, scan.maxHeight());
        }
    }

    private static void renderScanBlock(
            PoseStack poseStack,
            MultiBufferSource buffer,
            BlockPos pos,
            double entityX,
            double entityY,
            double entityZ
    ) {
        renderDebugBlock(
                poseStack,
                buffer,
                pos,
                entityX,
                entityY,
                entityZ,
                SCAN_RED,
                SCAN_GREEN,
                SCAN_BLUE,
                SCAN_FILL_ALPHA,
                SCAN_OUTLINE_ALPHA
        );
    }

    private static void renderDebugBlock(
            PoseStack poseStack,
            MultiBufferSource buffer,
            BlockPos pos,
            double entityX,
            double entityY,
            double entityZ,
            float red,
            float green,
            float blue,
            int fillAlpha,
            float outlineAlpha
    ) {
        poseStack.pushPose();
        poseStack.translate(pos.getX() - entityX, pos.getY() - entityY, pos.getZ() - entityZ);
        if (fillAlpha > 0) {
            renderFilledBlock(poseStack, buffer.getBuffer(RenderType.debugQuads()), red, green, blue, fillAlpha);
        }
        LevelRenderer.renderLineBox(
                poseStack,
                buffer.getBuffer(RenderType.lines()),
                new AABB(0.02D, 0.02D, 0.02D, 0.98D, 1.02D, 0.98D),
                red,
                green,
                blue,
                outlineAlpha
        );
        poseStack.popPose();
    }

    private static void renderScanBounds(
            PoseStack poseStack,
            MultiBufferSource buffer,
            BlockPos root,
            double entityX,
            double entityY,
            double entityZ,
            int maxHeight
    ) {
        poseStack.pushPose();
        poseStack.translate(
                root.getX() - entityX - SCAN_BOUND_HORIZONTAL,
                root.getY() - entityY,
                root.getZ() - entityZ - SCAN_BOUND_HORIZONTAL
        );
        LevelRenderer.renderLineBox(
                poseStack,
                buffer.getBuffer(RenderType.lines()),
                new AABB(0.0D, 0.0D, 0.0D, SCAN_BOUND_HORIZONTAL * 2.0D + 1.0D, maxHeight + 1.0D, SCAN_BOUND_HORIZONTAL * 2.0D + 1.0D),
                TOO_LARGE_RED,
                TOO_LARGE_GREEN,
                TOO_LARGE_BLUE,
                TARGET_OUTLINE_ALPHA
        );
        poseStack.popPose();
    }

    private static void renderFilledBlock(
            PoseStack poseStack,
            VertexConsumer consumer,
            float red,
            float green,
            float blue,
            int alpha
    ) {
        PoseStack.Pose pose = poseStack.last();
        int redByte = colorByte(red);
        int greenByte = colorByte(green);
        int blueByte = colorByte(blue);
        float min = 0.04F;
        float max = 0.96F;
        float maxY = 1.01F;
        quad(consumer, pose, min, min, min, max, min, min, max, min, max, min, min, max, redByte, greenByte, blueByte, alpha);
        quad(consumer, pose, min, maxY, min, min, maxY, max, max, maxY, max, max, maxY, min, redByte, greenByte, blueByte, alpha);
        quad(consumer, pose, min, min, min, min, maxY, min, max, maxY, min, max, min, min, redByte, greenByte, blueByte, alpha);
        quad(consumer, pose, max, min, min, max, maxY, min, max, maxY, max, max, min, max, redByte, greenByte, blueByte, alpha);
        quad(consumer, pose, max, min, max, max, maxY, max, min, maxY, max, min, min, max, redByte, greenByte, blueByte, alpha);
        quad(consumer, pose, min, min, max, min, maxY, max, min, maxY, min, min, min, min, redByte, greenByte, blueByte, alpha);
    }

    private static void renderHoverPulse(
            float animationTicks,
            PoseStack poseStack,
            MultiBufferSource buffer
    ) {
        float pulse = (Mth.sin(animationTicks * HOVER_PULSE_SPEED) + 1.0F) * 0.5F;
        VertexConsumer consumer = buffer.getBuffer(RenderType.debugQuads());
        PoseStack.Pose pose = poseStack.last();
        renderHoverDiamond(consumer, pose, 0.0F, -0.27F, 0.0F, 0.62F + pulse * 0.08F, 0.82F + pulse * 0.12F, Math.round(46.0F + pulse * 46.0F));
        renderHoverDiamond(consumer, pose, 0.0F, -0.25F, 0.0F, 0.32F + pulse * 0.04F, 0.46F + pulse * 0.06F, Math.round(96.0F + pulse * 64.0F));
        renderHoverDiamond(consumer, pose, -0.32F, -0.18F, 0.0F, 0.18F, 0.38F, Math.round(92.0F + pulse * 62.0F));
        renderHoverDiamond(consumer, pose, 0.32F, -0.18F, 0.0F, 0.18F, 0.38F, Math.round(92.0F + pulse * 62.0F));
    }

    private static void renderHoverDiamond(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float centerX,
            float centerY,
            float centerZ,
            float halfX,
            float halfZ,
            int alpha
    ) {
        quad(
                consumer,
                pose,
                centerX,
                centerY,
                centerZ - halfZ,
                centerX + halfX,
                centerY,
                centerZ,
                centerX,
                centerY,
                centerZ + halfZ,
                centerX - halfX,
                centerY,
                centerZ,
                HOVER_BLUE_RED,
                HOVER_BLUE_GREEN,
                HOVER_BLUE_BLUE,
                alpha
        );
    }

    private static void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        consumer.addVertex(pose, x1, y1, z1).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, x2, y2, z2).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, x3, y3, z3).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, x4, y4, z4).setColor(red, green, blue, alpha);
    }

    private static int colorByte(float color) {
        return Math.round(color * 255.0F);
    }

    private static HarvestDebugScan harvestDebugScan(ForestryCartEntity entity, BlockPos root) {
        Level level = entity.level();
        int maxHeight = Math.max(0, entity.debugMaxTreeHeight());
        boolean canHarvestLeaves = entity.debugCanHarvestLeaves();
        BlockState rootState = level.getBlockState(root);
        BlockPos harvestRoot = null;
        if (rootState.canBeReplaced()) {
            harvestRoot = findRemainingHarvestRoot(level, root, maxHeight, canHarvestLeaves);
        }
        if (harvestRoot == null && isBreakableTreeBlock(rootState, canHarvestLeaves)) {
            harvestRoot = root;
        }
        if (harvestRoot == null) {
            harvestRoot = findLogRoot(level, root, maxHeight);
        }
        if (harvestRoot == null) {
            harvestRoot = findRemainingHarvestRoot(level, root, maxHeight, canHarvestLeaves);
        }
        if (harvestRoot == null) {
            return HarvestDebugScan.empty(maxHeight);
        }
        return scanConnectedHarvestBlocks(level, root, harvestRoot, entity.debugMaxConnectedLogs(), maxHeight, canHarvestLeaves);
    }

    private static BlockPos findLogRoot(Level level, BlockPos root, int maxHeight) {
        for (int y = 0; y <= maxHeight; y++) {
            BlockPos candidate = root.above(y);
            if (level.getBlockState(candidate).is(BlockTags.LOGS)) {
                return candidate;
            }
        }
        return null;
    }

    private static BlockPos findRemainingHarvestRoot(Level level, BlockPos root, int maxHeight, boolean canHarvestLeaves) {
        return findRemainingRoot(level, root, maxHeight, canHarvestLeaves);
    }

    private static BlockPos findRemainingRoot(Level level, BlockPos root, int maxHeight, boolean includeLeaves) {
        for (int y = maxHeight; y >= 0; y--) {
            for (int radius = 0; radius <= REMAINING_HARVEST_SEARCH_HORIZONTAL; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                            continue;
                        }
                        BlockPos candidate = root.offset(dx, y, dz);
                        if (isHarvestScanBlock(level.getBlockState(candidate), includeLeaves)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static HarvestDebugScan scanConnectedHarvestBlocks(
            Level level,
            BlockPos root,
            BlockPos start,
            int logLimit,
            int maxHeight,
            boolean canHarvestLeaves
    ) {
        if (!isHarvestScanBlock(level.getBlockState(start), canHarvestLeaves)) {
            return HarvestDebugScan.empty(maxHeight);
        }

        if (logLimit <= 0) {
            return HarvestDebugScan.empty(maxHeight);
        }
        if (level.getBlockState(start).is(BlockTags.LOGS)) {
            return scanOwnedTree(level, root, start, logLimit, maxHeight, canHarvestLeaves);
        }

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> logs = new ArrayList<>();
        List<BlockPos> leaves = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && leaves.size() < MAX_CONNECTED_LEAF_BLOCKS && logs.size() < logLimit) {
            BlockPos current = queue.remove();
            if (level.getBlockState(current).is(BlockTags.LEAVES)) {
                leaves.add(current);
            } else {
                logs.add(current);
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos next = current.offset(dx, dy, dz);
                        if (!withinTreeBounds(root, next, maxHeight)
                                || !isHarvestScanBlock(level.getBlockState(next), canHarvestLeaves)
                                || !seen.add(next)) {
                            continue;
                        }
                        queue.add(next);
                    }
                }
            }
        }

        List<BlockPos> blocks = new ArrayList<>(leaves.size() + logs.size());
        blocks.addAll(leaves);
        blocks.addAll(logs);
        if (blocks.isEmpty()) {
            return HarvestDebugScan.empty(maxHeight);
        }
        BlockPos target = leaves.isEmpty() ? selectHarvestBlock(root, logs) : selectHarvestBlock(root, leaves);
        return new HarvestDebugScan(List.copyOf(blocks), target, false, maxHeight);
    }

    private static HarvestDebugScan scanOwnedTree(
            Level level,
            BlockPos root,
            BlockPos start,
            int logLimit,
            int maxHeight,
            boolean canHarvestLeaves
    ) {
        ForestryTreeScan.TreeBlocks blocks = ForestryTreeScan.TreeBlocks.of(level);
        ForestryTreeScan.Bounds bounds = new ForestryTreeScan.Bounds(root, maxHeight, SCAN_BOUND_HORIZONTAL);
        ForestryTreeScan.OwnedLogs owned = ForestryTreeScan.ownedLogs(blocks, bounds, start, logLimit);
        if (owned.tooLarge()) {
            return new HarvestDebugScan(List.of(start), start, true, maxHeight);
        }
        if (owned.logs().isEmpty()) {
            return HarvestDebugScan.empty(maxHeight);
        }
        List<BlockPos> leaves = canHarvestLeaves
                ? ForestryTreeScan.leavesAround(blocks, bounds, owned.logs(), LEAF_SCAN_RADIUS, MAX_CONNECTED_LEAF_BLOCKS)
                : List.of();
        List<BlockPos> treeBlocks = new ArrayList<>(leaves.size() + owned.logs().size());
        treeBlocks.addAll(leaves);
        treeBlocks.addAll(owned.logs());
        BlockPos target = leaves.isEmpty() ? selectHarvestBlock(root, owned.logs()) : selectHarvestBlock(root, leaves);
        return new HarvestDebugScan(List.copyOf(treeBlocks), target, false, maxHeight);
    }

    private static BlockPos selectHarvestBlock(BlockPos root, List<BlockPos> blocks) {
        BlockPos selected = blocks.get(0);
        for (int i = 1; i < blocks.size(); i++) {
            BlockPos candidate = blocks.get(i);
            if (harvestsBefore(root, candidate, selected)) {
                selected = candidate;
            }
        }
        return selected;
    }

    private static boolean harvestsBefore(BlockPos root, BlockPos candidate, BlockPos selected) {
        if (candidate.getY() != selected.getY()) {
            return candidate.getY() > selected.getY();
        }
        int candidateDistance = horizontalDistanceSquared(root, candidate);
        int selectedDistance = horizontalDistanceSquared(root, selected);
        if (candidateDistance != selectedDistance) {
            return candidateDistance < selectedDistance;
        }
        if (candidate.getX() != selected.getX()) {
            return candidate.getX() < selected.getX();
        }
        return candidate.getZ() < selected.getZ();
    }

    private static int horizontalDistanceSquared(BlockPos root, BlockPos candidate) {
        int dx = candidate.getX() - root.getX();
        int dz = candidate.getZ() - root.getZ();
        return dx * dx + dz * dz;
    }

    private static boolean withinTreeBounds(BlockPos root, BlockPos candidate, int maxHeight) {
        return candidate.getY() >= root.getY()
                && candidate.getY() <= root.getY() + maxHeight
                && Math.abs(candidate.getX() - root.getX()) <= SCAN_BOUND_HORIZONTAL
                && Math.abs(candidate.getZ() - root.getZ()) <= SCAN_BOUND_HORIZONTAL;
    }

    private static boolean isHarvestScanBlock(BlockState state, boolean includeLeaves) {
        return state.is(BlockTags.LOGS) || includeLeaves && state.is(BlockTags.LEAVES);
    }

    private static boolean isBreakableTreeBlock(BlockState state, boolean leavesBreakable) {
        return state.is(BlockTags.LOGS) || leavesBreakable && state.is(BlockTags.LEAVES);
    }

    private void applyMinecartPose(
            ForestryCartEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack
    ) {
        long offsetSeed = (long) entity.getId() * 493286711L;
        offsetSeed = offsetSeed * offsetSeed * 4392167121L + offsetSeed * 98761L;
        float xOffset = (((float) (offsetSeed >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float yOffset = (((float) (offsetSeed >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float zOffset = (((float) (offsetSeed >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        poseStack.translate(xOffset, yOffset, zOffset);

        double x = Mth.lerp((double) partialTick, entity.xOld, entity.getX());
        double y = Mth.lerp((double) partialTick, entity.yOld, entity.getY());
        double z = Mth.lerp((double) partialTick, entity.zOld, entity.getZ());
        Vec3 railPosition = entity.getPos(x, y, z);
        float targetYaw = entityYaw;
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        if (railPosition != null) {
            Vec3 forward = entity.getPosOffs(x, y, z, 0.3F);
            Vec3 backward = entity.getPosOffs(x, y, z, -0.3F);
            if (forward == null) {
                forward = railPosition;
            }
            if (backward == null) {
                backward = railPosition;
            }

            poseStack.translate(
                    railPosition.x - x,
                    (forward.y + backward.y) / 2.0D - y,
                    railPosition.z - z
            );
            Vec3 railDirection = backward.add(-forward.x, -forward.y, -forward.z);
            if (railDirection.length() != 0.0D) {
                railDirection = alignRailDirectionToTravel(entity, railDirection.normalize());
                targetYaw = (float) (Math.atan2(-railDirection.x, railDirection.z) * 180.0D / Math.PI);
                pitch = (float) (Math.atan(railDirection.y) * 73.0D);
            }
        }

        float visualYaw = smoothedTurnYaw(entity, targetYaw, partialTick);
        float turnBank = Mth.clamp(Mth.wrapDegrees(targetYaw - visualYaw) * 0.08F, -TURN_BANK_DEGREES, TURN_BANK_DEGREES);
        poseStack.translate(0.0F, 0.37F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - visualYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(turnBank));
        applyDamageWobble(entity, partialTick, poseStack);
    }

    private float smoothedTurnYaw(ForestryCartEntity entity, float targetYaw, float partialTick) {
        int entityId = entity.getId();
        TurnState state = turnStates.get(entityId);
        if (state == null || entity.tickCount - state.lastSeenTick > TURN_STATE_EXPIRY_TICKS) {
            state = new TurnState(targetYaw, entity.tickCount, partialTick);
            turnStates.put(entityId, state);
            return targetYaw;
        }

        float elapsedTicks = entity.tickCount - state.tick + partialTick - state.partialTick;
        elapsedTicks = Mth.clamp(elapsedTicks, 0.0F, 4.0F);
        float yawDelta = Mth.wrapDegrees(targetYaw - state.yaw);
        float maxStep = TURN_DEGREES_PER_TICK * elapsedTicks;
        if (Math.abs(yawDelta) <= maxStep) {
            state.yaw = targetYaw;
        } else if (maxStep > 0.0F) {
            state.yaw = Mth.wrapDegrees(state.yaw + Math.copySign(maxStep, yawDelta));
        }
        state.tick = entity.tickCount;
        state.partialTick = partialTick;
        state.lastSeenTick = entity.tickCount;
        if ((entity.tickCount & 31) == 0) {
            pruneTurnStates(entity.tickCount);
        }
        return state.yaw;
    }

    private void pruneTurnStates(int currentTick) {
        turnStates.entrySet().removeIf(entry -> currentTick - entry.getValue().lastSeenTick > TURN_STATE_EXPIRY_TICKS);
    }

    private static Vec3 alignRailDirectionToTravel(ForestryCartEntity entity, Vec3 railDirection) {
        Vec3 travelDirection = travelDirection(entity);
        double horizontalDot = railDirection.x * travelDirection.x + railDirection.z * travelDirection.z;
        return horizontalDot < 0.0D ? railDirection.scale(-1.0D) : railDirection;
    }

    private static Vec3 travelDirection(ForestryCartEntity entity) {
        Vec3 movement = entity.getDeltaMovement();
        if (movement.horizontalDistanceSqr() > 1.0E-8D) {
            return movement;
        }

        double xMovement = entity.getX() - entity.xOld;
        double zMovement = entity.getZ() - entity.zOld;
        if (xMovement * xMovement + zMovement * zMovement > 1.0E-8D) {
            return new Vec3(xMovement, 0.0D, zMovement);
        }

        Direction routeDirection = entity.routeDirection();
        return new Vec3(routeDirection.getStepX(), 0.0D, routeDirection.getStepZ());
    }

    private static void applyDamageWobble(ForestryCartEntity entity, float partialTick, PoseStack poseStack) {
        float hurtTime = (float) entity.getHurtTime() - partialTick;
        float damage = entity.getDamage() - partialTick;
        if (damage < 0.0F) {
            damage = 0.0F;
        }
        if (hurtTime > 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurtTime) * hurtTime * damage / 10.0F * entity.getHurtDir()));
        }
    }

    private record HarvestDebugScan(List<BlockPos> blocks, BlockPos target, boolean tooLarge, int maxHeight) {
        private static HarvestDebugScan empty(int maxHeight) {
            return new HarvestDebugScan(List.of(), null, false, maxHeight);
        }
    }

    private static final class TurnState {
        private float yaw;
        private int tick;
        private float partialTick;
        private int lastSeenTick;

        private TurnState(float yaw, int tick, float partialTick) {
            this.yaw = yaw;
            this.tick = tick;
            this.partialTick = partialTick;
            this.lastSeenTick = tick;
        }
    }
}
