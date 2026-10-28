package com.rngtech.client;

import com.rngtech.content.block.CableBlock;
import com.rngtech.content.block.UniversalConnectorBlock;
import com.rngtech.content.blockentity.CableBlockEntity;
import com.rngtech.content.item.CableItem;
import com.rngtech.content.item.PlacementPreview;
import com.rngtech.content.item.UniversalConnectorItem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Draws a translucent ghost of what a right-click with Universal Cable or a Universal Connector would build. The ghost
 * comes from the item's own placement decision and is rebuilt only when the target or the client tick changes, so a
 * frame just replays the cached quads.
 */
public final class PlacementPreviewClient {
    private static final float ALPHA = 0.5F;
    private static final long MODEL_SEED = 42L;
    private static final Direction[] DIRECTIONS = Direction.values();
    /** The ghost block itself, then one part per neighbouring cable that grows an arm toward it. */
    private static final int MAX_PARTS = 1 + DIRECTIONS.length;
    private static final RandomSource RANDOM = RandomSource.create();
    private static final List<BakedQuad> QUADS = new ArrayList<>();
    private static final List<BakedQuad> EXISTING_QUADS = new ArrayList<>();
    private static final float[] BRIGHTNESS = new float[4];
    private static final int[] LIGHTS = new int[4];
    private static final BlockPos.MutableBlockPos[] PART_POS = new BlockPos.MutableBlockPos[MAX_PARTS];
    private static final int[] PART_END = new int[MAX_PARTS];
    private static final int[] PART_LIGHT = new int[MAX_PARTS];
    private static final float[] PART_RED = new float[MAX_PARTS];
    private static final float[] PART_GREEN = new float[MAX_PARTS];
    private static final float[] PART_BLUE = new float[MAX_PARTS];
    private static final BlockPos.MutableBlockPos TARGET_POS = new BlockPos.MutableBlockPos();

    private static float[] shades = new float[32];
    private static int parts;

    static {
        for (int part = 0; part < MAX_PARTS; part++) {
            PART_POS[part] = new BlockPos.MutableBlockPos();
        }
    }

    private static Direction targetFace;
    private static ItemStack targetStack;
    private static boolean targetSneaking;
    private static int targetTick = -1;

    private PlacementPreviewClient() {
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        InteractionHand hand = player == null ? null : previewHand(player);
        if (hand == null
                || level == null
                || minecraft.options.hideGui
                || minecraft.gameMode == null
                || minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR
                || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            targetTick = -1;
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (player.tickCount != targetTick
                || stack != targetStack
                || hit.getDirection() != targetFace
                || player.isSecondaryUseActive() != targetSneaking
                || !hit.getBlockPos().equals(TARGET_POS)) {
            targetTick = player.tickCount;
            targetStack = stack;
            targetFace = hit.getDirection();
            targetSneaking = player.isSecondaryUseActive();
            TARGET_POS.set(hit.getBlockPos());
            rebuild(minecraft, level, player, hand, stack, hit);
        }
        if (parts > 0) {
            render(minecraft, event);
        }
    }

    /** The main hand is used first; the off hand only gets a turn when the main hand is empty. */
    private static InteractionHand previewHand(LocalPlayer player) {
        if (player.getMainHandItem().getItem() instanceof PlacementPreview.Source) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getMainHandItem().isEmpty() && player.getOffhandItem().getItem() instanceof PlacementPreview.Source) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static void rebuild(
            Minecraft minecraft,
            ClientLevel level,
            LocalPlayer player,
            InteractionHand hand,
            ItemStack stack,
            BlockHitResult hit
    ) {
        parts = 0;
        QUADS.clear();
        if (!player.getAbilities().mayBuild
                || !level.getWorldBorder().isWithinBounds(hit.getBlockPos())
                || blockUseComesFirst(level, player, hand, stack, hit)) {
            return;
        }
        PlacementPreview preview = ((PlacementPreview.Source) stack.getItem())
                .placementPreview(new UseOnContext(player, hand, hit));
        if (preview == null) {
            return;
        }

        BlockPos pos = preview.pos();
        BlockState existing = level.getBlockState(pos);
        BlockState shown = preview.state();
        if (existing.getBlock() instanceof UniversalConnectorBlock && shown.getBlock() instanceof CableBlock) {
            // The connector frames already stand there, so the ghost adds only the cable core and arms.
            shown = withConnectorFramesAsArms(shown);
        }
        DyeColor color = level.getBlockEntity(pos) instanceof CableBlockEntity cable
                ? cable.color()
                : stack.get(DataComponents.BASE_COLOR);
        addPart(minecraft, level, pos, existing, shown, color);
        if (shown.getBlock() instanceof CableBlock) {
            // A neighbouring cable grows its own half of each new link once the cable is placed.
            for (Direction direction : DIRECTIONS) {
                if (!CableBlock.hasCableConnection(shown, direction) || CableBlock.hasCableConnection(existing, direction)) {
                    continue;
                }
                BlockPos neighborPos = pos.relative(direction);
                BlockState neighbor = level.getBlockState(neighborPos);
                if (neighbor.getBlock() instanceof CableBlock
                        && !CableBlock.hasCableConnection(neighbor, direction.getOpposite())
                        && level.getBlockEntity(neighborPos) instanceof CableBlockEntity neighborCable) {
                    BlockState linked = neighbor.setValue(CableBlock.cableProperty(direction.getOpposite()), true);
                    addPart(minecraft, level, neighborPos, neighbor, linked, neighborCable.color());
                }
            }
        }
    }

    /** Appends the quads that {@code shown} draws and {@code existing} does not draw yet at {@code pos}. */
    private static void addPart(
            Minecraft minecraft,
            ClientLevel level,
            BlockPos pos,
            BlockState existing,
            BlockState shown,
            DyeColor color
    ) {
        int start = QUADS.size();
        collectQuads(minecraft, shown, QUADS);
        if (existing.getBlock() == shown.getBlock()) {
            // Same multipart model: the new parts are the quads the current state does not draw yet.
            collectQuads(minecraft, existing, EXISTING_QUADS);
            for (int index = QUADS.size() - 1; index >= start; index--) {
                if (containsSame(EXISTING_QUADS, QUADS.get(index))) {
                    QUADS.remove(index);
                }
            }
            EXISTING_QUADS.clear();
        }
        if (QUADS.size() == start) {
            return;
        }

        if (shades.length < QUADS.size()) {
            shades = Arrays.copyOf(shades, Math.max(QUADS.size(), shades.length * 2));
        }
        for (int index = start; index < QUADS.size(); index++) {
            BakedQuad quad = QUADS.get(index);
            shades[index] = level.getShade(quad.getDirection(), quad.isShade());
        }
        int tint = CableTintClient.tint(color);
        PART_RED[parts] = FastColor.ARGB32.red(tint) / 255.0F;
        PART_GREEN[parts] = FastColor.ARGB32.green(tint) / 255.0F;
        PART_BLUE[parts] = FastColor.ARGB32.blue(tint) / 255.0F;
        PART_LIGHT[parts] = LevelRenderer.getLightColor(level, pos);
        PART_POS[parts].set(pos);
        PART_END[parts] = QUADS.size();
        parts++;
    }

    /**
     * Predicts whether the clicked block handles the click before the item does, as {@code MultiPlayerGameMode} runs
     * it. Cable and connector blocks are exact; any other block counts as taking the click when it opens a menu.
     */
    private static boolean blockUseComesFirst(
            ClientLevel level,
            LocalPlayer player,
            InteractionHand hand,
            ItemStack stack,
            BlockHitResult hit
    ) {
        BlockPos pos = hit.getBlockPos();
        boolean sneakSkipsBlock = !player.getMainHandItem().doesSneakBypassUse(level, pos, player)
                || !player.getOffhandItem().doesSneakBypassUse(level, pos, player);
        if (player.isSecondaryUseActive() && sneakSkipsBlock) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof CableBlock) {
            BlockState target = level.getBlockState(pos.relative(hit.getDirection()));
            if (stack.getItem() instanceof CableItem
                    || (stack.getItem() instanceof UniversalConnectorItem
                    && (target.isAir() || CableBlock.isValidConnectorTarget(target)))) {
                // The cable passes Cable on to the item, and fits a connector on the clicked face just as the item does.
                return false;
            }
            Direction menuSide = CableBlock.resolveInstalledConnectorDirection(state, hit, true);
            return hand == InteractionHand.MAIN_HAND
                    && menuSide != null
                    && level.getBlockEntity(pos) instanceof CableBlockEntity cable
                    && cable.hasAnyConnector(menuSide);
        }
        if (hand != InteractionHand.MAIN_HAND) {
            return false;
        }
        return block instanceof UniversalConnectorBlock
                || state.getMenuProvider(level, pos) != null
                || level.getBlockEntity(pos) instanceof MenuProvider;
    }

    private static BlockState withConnectorFramesAsArms(BlockState cableState) {
        BlockState shown = cableState;
        for (Direction direction : DIRECTIONS) {
            if (CableBlock.hasConnector(cableState, direction)) {
                shown = shown
                        .setValue(CableBlock.connectorProperty(direction), false)
                        .setValue(CableBlock.cableProperty(direction), true);
            }
        }
        return shown;
    }

    private static void collectQuads(Minecraft minecraft, BlockState state, List<BakedQuad> quads) {
        BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
        for (Direction direction : DIRECTIONS) {
            RANDOM.setSeed(MODEL_SEED);
            quads.addAll(model.getQuads(state, direction, RANDOM, ModelData.EMPTY, null));
        }
        RANDOM.setSeed(MODEL_SEED);
        quads.addAll(model.getQuads(state, null, RANDOM, ModelData.EMPTY, null));
    }

    private static boolean containsSame(List<BakedQuad> quads, BakedQuad quad) {
        for (BakedQuad candidate : quads) {
            if (candidate == quad) {
                return true;
            }
        }
        return false;
    }

    private static void render(Minecraft minecraft, RenderLevelStageEvent event) {
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        RenderType renderType = NeoForgeRenderTypes.TRANSLUCENT_ON_PARTICLES_TARGET.get();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(renderType);
        int index = 0;
        for (int part = 0; part < parts; part++) {
            BlockPos pos = PART_POS[part];
            poseStack.pushPose();
            poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            PoseStack.Pose pose = poseStack.last();
            Arrays.fill(LIGHTS, PART_LIGHT[part]);
            for (; index < PART_END[part]; index++) {
                BakedQuad quad = QUADS.get(index);
                Arrays.fill(BRIGHTNESS, shades[index]);
                boolean tinted = quad.isTinted();
                consumer.putBulkData(
                        pose,
                        quad,
                        BRIGHTNESS,
                        tinted ? PART_RED[part] : 1.0F,
                        tinted ? PART_GREEN[part] : 1.0F,
                        tinted ? PART_BLUE[part] : 1.0F,
                        ALPHA,
                        LIGHTS,
                        OverlayTexture.NO_OVERLAY,
                        false
                );
            }
            poseStack.popPose();
        }
        buffers.endBatch(renderType);
    }
}
