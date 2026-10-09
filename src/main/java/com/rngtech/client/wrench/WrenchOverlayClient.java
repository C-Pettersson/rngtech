package com.rngtech.client.wrench;

import com.rngtech.content.block.CableBlock;
import com.rngtech.content.block.UniversalConnectorBlock;
import com.rngtech.content.blockentity.CableBlockEntity;
import com.rngtech.content.cable.CableConnectorMode;
import com.rngtech.content.cable.EnergyDistributionMode;
import com.rngtech.content.cable.FluidConnectorMode;
import com.rngtech.content.cable.ItemConnectorMode;
import com.rngtech.content.cable.NetworkBridgeType;
import com.rngtech.content.network.WrenchOverlayDataPayload;
import com.rngtech.content.network.WrenchOverlayRequestPayload;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.wrench.ConnectorOverlaySnapshot;
import com.rngtech.content.wrench.WrenchOverlayPage;
import com.rngtech.content.wrench.WrenchOverlayTarget;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class WrenchOverlayClient {
    private static final int REQUEST_INTERVAL_TICKS = 5;
    private static final int SNAPSHOT_TTL_TICKS = 20;
    private static final int MAX_LINE_WIDTH = 180;
    private static final float TEXT_SCALE = 0.0125F;
    private static final int PANEL_COLOR_R = 12;
    private static final int PANEL_COLOR_G = 42;
    private static final int PANEL_COLOR_B = 48;
    private static final int PANEL_COLOR_A = 178;
    private static final int HEADER = 0xFF7BE2F0;
    private static final int TEXT = 0xFFE7FBFF;
    private static final int MUTED = 0xFF9BC2C8;
    private static final int WARNING = 0xFFFFC46B;
    private static final String KEY_CATEGORY = "key.categories.rngtech";

    private static final KeyMapping CYCLE_VIEW = new KeyMapping(
            "key.rngtech.cycle_wrench_view",
            InputConstants.UNKNOWN.getType(),
            InputConstants.UNKNOWN.getValue(),
            KEY_CATEGORY
    );

    private static WrenchOverlayPage page = WrenchOverlayPage.SUMMARY;
    private static WrenchOverlayTarget currentTarget;
    private static ConnectorOverlaySnapshot snapshot;
    private static int nextRequestTick;
    private static int lastSnapshotTick;

    private WrenchOverlayClient() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CYCLE_VIEW);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        WrenchOverlayTarget target = resolveTarget(minecraft);
        if (target == null) {
            clear();
            consumeCycleClicks();
            return;
        }

        if (!target.equals(currentTarget)) {
            currentTarget = target;
            snapshot = null;
            nextRequestTick = 0;
        }

        int tick = minecraft.player == null ? 0 : minecraft.player.tickCount;
        if (tick >= nextRequestTick) {
            requestSnapshot(target, tick);
        }

        if (snapshot != null && tick - lastSnapshotTick > SNAPSHOT_TTL_TICKS) {
            snapshot = null;
        }

        while (CYCLE_VIEW.consumeClick()) {
            if (hasVisibleTarget(minecraft)) {
                cyclePage(1);
            }
        }
    }

    public static void onUseInput(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.isUseItem() || !Screen.hasShiftDown() || !hasCycleTarget(minecraft)) {
            return;
        }
        cyclePage(1);
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!hasVisibleTarget(minecraft) || snapshot == null || currentTarget == null) {
            return;
        }

        List<OverlayLine> lines = overlayLines(minecraft.font, snapshot);
        if (lines.isEmpty()) {
            return;
        }

        Vec3 anchor = anchor(currentTarget.pos(), snapshot.mountedFace());
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(anchor.x - camera.x, anchor.y - camera.y, anchor.z - camera.z);
        poseStack.mulPose(event.getCamera().rotation());
        poseStack.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        Font font = minecraft.font;
        int width = 0;
        for (OverlayLine line : lines) {
            width = Math.max(width, font.width(line.text()));
        }
        int lineHeight = font.lineHeight + 2;
        int panelWidth = width + 12;
        int panelHeight = lines.size() * lineHeight + 8;
        int left = -panelWidth / 2;
        int top = -panelHeight / 2;

        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        Matrix4f matrix = poseStack.last().pose();
        renderPanel(buffer, matrix, left, top, panelWidth, panelHeight);

        int y = top + 5;
        int textX = left + 6;
        for (OverlayLine line : lines) {
            font.drawInBatch(
                    line.text(),
                    textX,
                    y,
                    line.color(),
                    false,
                    matrix,
                    buffer,
                    Font.DisplayMode.SEE_THROUGH,
                    0,
                    LightTexture.FULL_BRIGHT
            );
            y += lineHeight;
        }
        buffer.endBatch();
        poseStack.popPose();
    }

    public static void acceptData(WrenchOverlayDataPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Objects.equals(payload.target(), currentTarget)) {
            return;
        }
        snapshot = payload.snapshot().orElse(null);
        lastSnapshotTick = minecraft.player == null ? 0 : minecraft.player.tickCount;
    }

    private static void requestSnapshot(WrenchOverlayTarget target, int tick) {
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        nextRequestTick = tick + REQUEST_INTERVAL_TICKS;
        PacketDistributor.sendToServer(new WrenchOverlayRequestPayload(target));
    }

    private static boolean hasVisibleTarget(Minecraft minecraft) {
        if (snapshot == null || currentTarget == null || minecraft.player == null || minecraft.screen != null) {
            return false;
        }
        if (minecraft.player.tickCount - lastSnapshotTick > SNAPSHOT_TTL_TICKS) {
            return false;
        }
        WrenchOverlayTarget target = resolveTarget(minecraft);
        return currentTarget.equals(target);
    }

    private static boolean hasCycleTarget(Minecraft minecraft) {
        if (hasVisibleTarget(minecraft)) {
            return true;
        }
        WrenchOverlayTarget target = resolveTarget(minecraft);
        if (target == null) {
            return false;
        }
        if (target.isStandalone()) {
            return true;
        }
        Direction side = target.side();
        return side != null
                && minecraft.level != null
                && minecraft.level.getBlockEntity(target.pos()) instanceof CableBlockEntity cable
                && cable.hasUniversalConnector(side);
    }

    private static WrenchOverlayTarget resolveTarget(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null || !isHoldingWrench(minecraft)) {
            return null;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult hitResult) || hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = minecraft.level.getBlockState(pos);
        if (state.getBlock() instanceof UniversalConnectorBlock) {
            return new WrenchOverlayTarget(pos, WrenchOverlayTarget.STANDALONE_SIDE);
        }
        if (state.getBlock() instanceof CableBlock) {
            Direction side = CableBlock.resolveInstalledConnectorDirection(state, hitResult, true);
            if (side != null && CableBlock.hasConnector(state, side)) {
                return new WrenchOverlayTarget(pos, side);
            }
        }
        return null;
    }

    private static boolean isHoldingWrench(Minecraft minecraft) {
        return isWrench(minecraft.player.getMainHandItem()) || isWrench(minecraft.player.getOffhandItem());
    }

    private static boolean isWrench(ItemStack stack) {
        return stack.is(ModItems.WRENCH.get());
    }

    private static void cyclePage(int amount) {
        page = page.next(amount);
    }

    private static void clear() {
        currentTarget = null;
        snapshot = null;
        nextRequestTick = 0;
        lastSnapshotTick = 0;
    }

    private static void consumeCycleClicks() {
        while (CYCLE_VIEW.consumeClick()) {
            // Discard queued clicks while no connector is visible.
        }
    }

    private static Vec3 anchor(BlockPos pos, Direction face) {
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        return center.add(normal.scale(0.72D)).add(0.0D, 0.35D, 0.0D);
    }

    private static List<OverlayLine> overlayLines(Font font, ConnectorOverlaySnapshot snapshot) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(new OverlayLine(
                clipped(font, Component.translatable(page.translationKey()).getString() + "  [" + (page.ordinal() + 1) + "/4]"),
                HEADER
        ));
        lines.add(new OverlayLine(clipped(font, locationLine(snapshot)), MUTED));

        switch (page) {
            case ENERGY -> energyLines(font, snapshot, lines);
            case ITEM -> moduleLines(font, snapshot.itemModules(), false, lines);
            case FLUID -> moduleLines(font, snapshot.fluidModules(), true, lines);
            default -> summaryLines(font, snapshot, lines);
        }
        return lines;
    }

    private static void summaryLines(Font font, ConnectorOverlaySnapshot snapshot, List<OverlayLine> lines) {
        String energy = snapshot.hasEnergyConnector()
                ? "Yes C" + snapshot.energyChannel() + " " + energyModeName(snapshot.energyModeOrdinal())
                : "No";
        lines.add(new OverlayLine(clipped(font, Component.translatable("rngtech.wrench_overlay.summary.energy", energy).getString()), TEXT));
        lines.add(new OverlayLine(clipped(font, Component.translatable(
                "rngtech.wrench_overlay.summary.item",
                snapshot.installedItemModules(),
                snapshot.itemModules().size()
        ).getString()), TEXT));
        lines.add(new OverlayLine(clipped(font, Component.translatable(
                "rngtech.wrench_overlay.summary.fluid",
                snapshot.installedFluidModules(),
                snapshot.fluidModules().size()
        ).getString()), TEXT));
        lines.add(new OverlayLine(clipped(font, Component.translatable(
                "rngtech.wrench_overlay.summary.bridge",
                bridgeSummary(snapshot)
        ).getString()), snapshot.hasBridgeConnector() && (!snapshot.bridgeModLoaded() || snapshot.bridgeEndpointCount() <= 1)
                ? WARNING
                : TEXT));
    }

    private static String bridgeSummary(ConnectorOverlaySnapshot snapshot) {
        NetworkBridgeType type = NetworkBridgeType.byDataId(snapshot.bridgeTypeId());
        if (type == NetworkBridgeType.NONE) {
            return Component.translatable("rngtech.wrench_overlay.none").getString();
        }
        return Component.translatable(type.translationKey()).getString() + " ch " + snapshot.bridgeChannel();
    }

    private static void energyLines(Font font, ConnectorOverlaySnapshot snapshot, List<OverlayLine> lines) {
        String connector = snapshot.hasEnergyConnector()
                ? moduleName(snapshot.energyConnectorKey())
                : Component.translatable("rngtech.wrench_overlay.no_connector").getString();
        lines.add(new OverlayLine(clipped(font, connector), snapshot.hasEnergyConnector() ? TEXT : WARNING));
        lines.add(new OverlayLine(clipped(font, Component.translatable(
                "rngtech.wrench_overlay.mode_channel",
                energyModeName(snapshot.energyModeOrdinal()),
                snapshot.energyChannel()
        ).getString()), TEXT));
        lines.add(new OverlayLine(clipped(font, Component.translatable(
                "rngtech.wrench_overlay.distribution",
                energyDistributionName(snapshot.energyDistributionOrdinal())
        ).getString()), TEXT));
        lines.add(new OverlayLine(clipped(font, Component.translatable(
                "rngtech.wrench_overlay.attach",
                directionName(snapshot.energyAttachOrdinal())
        ).getString()), TEXT));
        lines.add(new OverlayLine(clipped(font, Component.translatable(
                "rngtech.wrench_overlay.energy_transfer",
                snapshot.energyTransferRate()
        ).getString()), TEXT));
        lines.add(new OverlayLine(clipped(font, Component.translatable(
                "rngtech.wrench_overlay.energy_live",
                snapshot.lastEnergyInput(),
                snapshot.lastEnergyOutput()
        ).getString()), TEXT));
        if (snapshot.hasEnergyConnector() && !snapshot.energyTargetAccess()) {
            lines.add(new OverlayLine(clipped(font, Component.translatable(
                    "rngtech.cable_connector.no_energy_access"
            ).getString()), WARNING));
        }
    }

    private static void moduleLines(
            Font font,
            List<ConnectorOverlaySnapshot.ModuleSnapshot> modules,
            boolean fluid,
            List<OverlayLine> lines
    ) {
        for (int index = 0; index < modules.size(); index++) {
            ConnectorOverlaySnapshot.ModuleSnapshot module = modules.get(index);
            lines.add(new OverlayLine(clipped(font, moduleLine(index, module, fluid)), module.installed() ? TEXT : WARNING));
        }
    }

    private static String moduleLine(int index, ConnectorOverlaySnapshot.ModuleSnapshot module, boolean fluid) {
        if (!module.installed()) {
            return (index + 1) + " " + Component.translatable("rngtech.wrench_overlay.empty").getString();
        }
        String shipment = fluid
                ? Component.translatable("rngtech.wrench_overlay.shipment_fluid", module.shipment()).getString()
                : Component.translatable("rngtech.wrench_overlay.shipment_item", module.shipment()).getString();
        StringBuilder line = new StringBuilder()
                .append(index + 1)
                .append(' ')
                .append(moduleName(module.moduleKey()))
                .append(' ')
                .append(shipment)
                .append(' ')
                .append(fluid ? fluidModeName(module.modeOrdinal()) : itemModeName(module.modeOrdinal()))
                .append(" C")
                .append(module.channel())
                .append(' ')
                .append(directionName(module.attachOrdinal()));
        if (module.cooldownTicks() > 0) {
            line.append(" W").append(module.cooldownTicks());
        }
        if (module.jamTicks() > 0) {
            line.append(" J").append(module.jamTicks());
        }
        return line.toString();
    }

    private static String locationLine(ConnectorOverlaySnapshot snapshot) {
        String face = directionName(snapshot.mountedFaceOrdinal());
        return snapshot.cableSide()
                ? Component.translatable("rngtech.wrench_overlay.location.cable", face).getString()
                : Component.translatable("rngtech.wrench_overlay.location.standalone", face).getString();
    }

    private static String moduleName(String key) {
        return key == null || key.isBlank()
                ? Component.translatable("rngtech.wrench_overlay.empty").getString()
                : Component.translatable(key).getString();
    }

    private static String energyModeName(int ordinal) {
        CableConnectorMode[] values = CableConnectorMode.values();
        CableConnectorMode mode = ordinal >= 0 && ordinal < values.length ? values[ordinal] : CableConnectorMode.BOTH;
        return Component.translatable(mode.translationKey()).getString();
    }

    private static String energyDistributionName(int ordinal) {
        EnergyDistributionMode[] values = EnergyDistributionMode.values();
        EnergyDistributionMode mode = ordinal >= 0 && ordinal < values.length
                ? values[ordinal]
                : EnergyDistributionMode.ROUND_ROBIN;
        return Component.translatable(mode.translationKey()).getString();
    }

    private static String fluidModeName(int ordinal) {
        FluidConnectorMode[] values = FluidConnectorMode.values();
        FluidConnectorMode mode = ordinal >= 0 && ordinal < values.length ? values[ordinal] : FluidConnectorMode.INPUT;
        return Component.translatable(mode.translationKey()).getString();
    }

    private static String itemModeName(int ordinal) {
        ItemConnectorMode[] values = ItemConnectorMode.values();
        ItemConnectorMode mode = ordinal >= 0 && ordinal < values.length ? values[ordinal] : ItemConnectorMode.INPUT;
        return Component.translatable(mode.translationKey()).getString();
    }

    private static String directionName(int ordinal) {
        if (ordinal < 0) {
            return Component.translatable("rngtech.universal_connector.attach.none").getString();
        }
        Direction[] values = Direction.values();
        Direction direction = ordinal >= 0 && ordinal < values.length ? values[ordinal] : Direction.NORTH;
        return Component.translatable("rngtech.direction." + direction.getSerializedName()).getString();
    }

    private static String clipped(Font font, String text) {
        if (font.width(text) <= MAX_LINE_WIDTH) {
            return text;
        }
        return font.plainSubstrByWidth(text, MAX_LINE_WIDTH - font.width("...")) + "...";
    }

    private static void renderPanel(MultiBufferSource buffer, Matrix4f matrix, int x, int y, int width, int height) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.textBackgroundSeeThrough());
        float left = x;
        float right = x + width;
        float top = y;
        float bottom = y + height;
        consumer.addVertex(matrix, left, bottom, 0.0F)
                .setColor(PANEL_COLOR_R, PANEL_COLOR_G, PANEL_COLOR_B, PANEL_COLOR_A)
                .setLight(LightTexture.FULL_BRIGHT);
        consumer.addVertex(matrix, right, bottom, 0.0F)
                .setColor(PANEL_COLOR_R, PANEL_COLOR_G, PANEL_COLOR_B, PANEL_COLOR_A)
                .setLight(LightTexture.FULL_BRIGHT);
        consumer.addVertex(matrix, right, top, 0.0F)
                .setColor(PANEL_COLOR_R, PANEL_COLOR_G, PANEL_COLOR_B, PANEL_COLOR_A)
                .setLight(LightTexture.FULL_BRIGHT);
        consumer.addVertex(matrix, left, top, 0.0F)
                .setColor(PANEL_COLOR_R, PANEL_COLOR_G, PANEL_COLOR_B, PANEL_COLOR_A)
                .setLight(LightTexture.FULL_BRIGHT);
    }

    private record OverlayLine(String text, int color) {
    }
}
