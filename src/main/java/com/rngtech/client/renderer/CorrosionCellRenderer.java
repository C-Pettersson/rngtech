package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.CorrosionCellBlockEntity;
import com.rngtech.content.blockentity.CorrosionVisualState;
import com.rngtech.content.registry.ModFluids;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.Map;

/**
 * Draws the live parts of the Corrosion Cell: the fuel dissolving in the electrolyte bath, hydrogen bubbles at the cell
 * current, the installed Cathode, settled sludge, the reservoir level, the ammeter needle, and the blower fan. Positions
 * are in model pixels and match tools/blockmodels/corrosion_cell.py.
 */
public class CorrosionCellRenderer implements BlockEntityRenderer<CorrosionCellBlockEntity> {
    private static final ModelResourceLocation FAN = new ModelResourceLocation(
            RNGTech.id("block/corrosion_cell/fan"),
            ModelResourceLocation.STANDALONE_VARIANT
    );
    private static final ResourceLocation ANODE = RNGTech.id("block/corrosion_cell/anode");
    private static final ResourceLocation CATHODE = RNGTech.id("block/corrosion_cell/cathode");
    private static final ResourceLocation SLUDGE = RNGTech.id("block/corrosion_cell/sludge");
    private static final ResourceLocation BUBBLE = RNGTech.id("block/corrosion_cell/fx/bubble");
    private static final ResourceLocation NEEDLE = RNGTech.id("block/corrosion_cell/fx/needle");

    private static final float BATH_X0 = 3.05F;
    private static final float BATH_X1 = 12.95F;
    private static final float BATH_Z0 = 2.55F;
    private static final float BATH_Z1 = 8.95F;
    private static final float BATH_FLOOR = 3.0F;
    private static final float BATH_SURFACE = 12.5F;
    private static final float CATHODE_Z0 = 8.2F;
    private static final float ANODE_X = 8.0F;
    private static final float ANODE_Z = 5.5F;
    private static final float ANODE_TOP = 12.0F;
    private static final float ANODE_LENGTH = 8.0F;
    private static final float STEM_TOP = 13.5F;
    private static final float SLUDGE_MAX = 1.5F;
    private static final float FAN_X = 8.0F;
    private static final float FAN_Z = 13.0F;
    private static final float FAN_DEGREES_PER_TICK = 36.0F;
    private static final float DIAL_X = 1.75F;
    private static final float DIAL_Y = 3.25F;
    private static final float DIAL_FACE_Z = 0.5F;
    private static final float NEEDLE_LENGTH = 0.95F;
    private static final float NEEDLE_SWEEP = 60.0F;
    private static final int MAX_BUBBLES = 16;
    private static final int MILKY = 0xD6DED8;
    private static final float MAX_CLOUDING = 0.55F;
    private static final int DEFAULT_METAL = 0xA0A4A8;
    private static final Map<String, Integer> METALS = Map.of(
            "aluminum", 0xBFCDDA,
            "titanium", 0x8EA0B7,
            "tungstensteel", 0x5A6370,
            "naquadah", 0x2D8C74,
            "iron", 0xB7B7AD,
            "copper", 0xD77A3C,
            "scrap", 0x8A6A52
    );

    private final ModelBlockRenderer modelRenderer;

    public CorrosionCellRenderer(BlockEntityRendererProvider.Context context) {
        modelRenderer = context.getBlockRenderDispatcher().getModelRenderer();
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(FAN);
    }

    @Override
    public void render(
            CorrosionCellBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        CorrosionVisualState state = blockEntity.visualState();
        CorrosionVisualState.Animation animation = blockEntity.animation();
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        animate(animation, state, time);

        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);

        renderFan(animation, poseStack, bufferSource, packedLight, packedOverlay);
        renderNeedle(animation.current, poseStack, bufferSource, packedLight, packedOverlay);
        renderCathode(state.cathode(), poseStack, bufferSource, packedLight, packedOverlay);
        renderSludge(state.sludge(), poseStack, bufferSource, packedLight, packedOverlay);
        boolean fuel = !state.fuel().isEmpty() && animation.fuelLeft > 0.001F;
        if (fuel) {
            renderFuel(state.fuel(), animation.fuelLeft, poseStack, bufferSource, packedLight, packedOverlay);
        }
        if (state.bath()) {
            if (fuel) {
                renderBubbles(state.fuel(), animation, time, poseStack, bufferSource, packedLight, packedOverlay);
            }
            renderElectrolyte(fuel ? 1.0F - animation.fuelLeft : 0.0F, poseStack, bufferSource, packedLight, packedOverlay);
        }
        renderReservoir(state.reservoirFluid(), state.reservoir(), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void animate(CorrosionVisualState.Animation animation, CorrosionVisualState state, float time) {
        float elapsed = Double.isNaN(animation.lastTime) ? 10.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        animation.current += (state.current() - animation.current) * Math.min(1.0F, elapsed * 0.25F);
        animation.fanAngle = (animation.fanAngle + animation.current * FAN_DEGREES_PER_TICK * elapsed) % 360.0F;
        if (state.fuelLeft() > animation.fuelLeft + 0.2F || state.fuelLeft() <= 0.0F) {
            animation.fuelLeft = state.fuelLeft();
        } else {
            animation.fuelLeft += (state.fuelLeft() - animation.fuelLeft) * Math.min(1.0F, elapsed * 0.1F);
        }
    }

    /** The blower on top feeds the air Cathode, so it spins with the cell current. */
    private void renderFan(
            CorrosionVisualState.Animation animation,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(FAN);
        poseStack.pushPose();
        poseStack.translate(FAN_X, 0.0F, FAN_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(animation.fanAngle));
        poseStack.translate(-FAN_X, 0.0F, -FAN_Z);
        poseStack.scale(16.0F, 16.0F, 16.0F);
        modelRenderer.renderModel(
                poseStack.last(),
                bufferSource.getBuffer(Sheets.cutoutBlockSheet()),
                null,
                model,
                1.0F,
                1.0F,
                1.0F,
                packedLight,
                packedOverlay,
                ModelData.EMPTY,
                RenderType.cutout()
        );
        poseStack.popPose();
    }

    /** The ammeter needle sweeps 120 degrees over the top of the dial, from no current to rated. */
    private static void renderNeedle(float current, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay) {
        TextureAtlasSprite sprite = sprite(NEEDLE);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        PoseStack.Pose pose = poseStack.last();
        // Seen from the front, +x runs to the viewer's left, so a clockwise sweep moves the tip toward -x.
        double angle = Math.toRadians(-NEEDLE_SWEEP + 2.0F * NEEDLE_SWEEP * Mth.clamp(current, 0.0F, 1.0F));
        float dx = (float) (-Math.sin(angle));
        float dy = (float) Math.cos(angle);
        float width = 0.07F;
        float px = -dy * width;
        float py = dx * width;
        float tipX = DIAL_X + dx * NEEDLE_LENGTH;
        float tipY = DIAL_Y + dy * NEEDLE_LENGTH;
        float z = DIAL_FACE_Z - 0.03F;
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        vertex(consumer, pose, DIAL_X - px, DIAL_Y - py, z, u0, v1, 196, 52, 40, 255, light, overlay, 0.0F, 0.0F, -1.0F);
        vertex(consumer, pose, DIAL_X + px, DIAL_Y + py, z, u1, v1, 196, 52, 40, 255, light, overlay, 0.0F, 0.0F, -1.0F);
        vertex(consumer, pose, tipX + px * 0.3F, tipY + py * 0.3F, z, u1, v0, 196, 52, 40, 255, light, overlay, 0.0F, 0.0F, -1.0F);
        vertex(consumer, pose, tipX - px * 0.3F, tipY - py * 0.3F, z, u0, v0, 196, 52, 40, 255, light, overlay, 0.0F, 0.0F, -1.0F);
    }

    /** The air Cathode forms the back wall of the bath, coloured by its material; without one the plenum shows. */
    private static void renderCathode(String material, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay) {
        if (material.isEmpty()) {
            return;
        }
        int color = METALS.getOrDefault(material, DEFAULT_METAL);
        box(bufferSource.getBuffer(Sheets.cutoutBlockSheet()), poseStack.last(), sprite(CATHODE),
                3.75F, 3.25F, CATHODE_Z0, 12.25F, 12.75F, CATHODE_Z0 + 0.5F, color, 255, light, overlay);
    }

    /** Settled residue fills the sludge trap as the residue slot fills, so a full trap means a stopped cell. */
    private static void renderSludge(float sludge, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay) {
        if (sludge <= 0.0F) {
            return;
        }
        float top = BATH_FLOOR + Math.max(0.15F, SLUDGE_MAX * Mth.clamp(sludge, 0.0F, 1.0F));
        box(bufferSource.getBuffer(Sheets.cutoutBlockSheet()), poseStack.last(), sprite(SLUDGE),
                BATH_X0, BATH_FLOOR, BATH_Z0, BATH_X1, top, CATHODE_Z0 - 0.05F, 0xFFFFFF, 255, light, overlay);
    }

    /** The fuel hangs from the clamp and dissolves from the bottom up, thinning as it goes; its stem stays to the end. */
    private static void renderFuel(String fuel, float left, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay) {
        FuelShape shape = FuelShape.of(fuel);
        int color = metalColor(fuel);
        VertexConsumer consumer = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        PoseStack.Pose pose = poseStack.last();
        TextureAtlasSprite sprite = sprite(ANODE);
        float remaining = Mth.clamp(left, 0.0F, 1.0F);
        float halfWidth = shape.halfWidth * (0.55F + 0.45F * remaining);
        float halfThickness = shape.halfThickness * (0.5F + 0.5F * remaining);
        float bottom = ANODE_TOP - ANODE_LENGTH * remaining;
        box(consumer, pose, sprite, ANODE_X - halfWidth, bottom, ANODE_Z - halfThickness,
                ANODE_X + halfWidth, ANODE_TOP, ANODE_Z + halfThickness, color, 255, light, overlay);
        box(consumer, pose, sprite, ANODE_X - 0.75F, ANODE_TOP, ANODE_Z - 0.4F,
                ANODE_X + 0.75F, STEM_TOP, ANODE_Z + 0.4F, color, 255, light, overlay);
    }

    /**
     * Hydrogen bubbles rise from both faces of the fuel, more and faster at higher current. Positions come from a fixed
     * hash per bubble, so they need no state.
     */
    private static void renderBubbles(
            String fuel,
            CorrosionVisualState.Animation animation,
            float time,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay
    ) {
        int count = Math.round(MAX_BUBBLES * Mth.clamp(animation.current, 0.0F, 1.0F));
        if (count <= 0) {
            return;
        }
        FuelShape shape = FuelShape.of(fuel);
        float remaining = Mth.clamp(animation.fuelLeft, 0.0F, 1.0F);
        float halfWidth = shape.halfWidth * (0.55F + 0.45F * remaining);
        float halfThickness = shape.halfThickness * (0.5F + 0.5F * remaining);
        float bottom = ANODE_TOP - ANODE_LENGTH * remaining;
        float height = BATH_SURFACE - bottom;
        VertexConsumer consumer = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
        PoseStack.Pose pose = poseStack.last();
        TextureAtlasSprite sprite = sprite(BUBBLE);
        float size = 0.2F;
        for (int index = 0; index < count; index++) {
            float a = hash(index * 3 + 1);
            float b = hash(index * 3 + 2);
            float c = hash(index * 3 + 3);
            float speed = 0.06F + 0.06F * a;
            float rise = ((time * speed + c * height) % height + height) % height;
            float y = bottom + rise;
            float x = ANODE_X - halfWidth + b * halfWidth * 2.0F + 0.15F * Mth.sin(time * 0.3F + index);
            float z = index % 2 == 0 ? ANODE_Z - halfThickness - 0.3F : ANODE_Z + halfThickness + 0.3F;
            box(consumer, pose, sprite, x - size, y - size, z - size, x + size, y + size, z + size, 0xFFFFFF, 255, light, overlay);
        }
    }

    /** The bath clouds toward milky white as the fuel dissolves, then clears with the next fresh dose. */
    private static void renderElectrolyte(float saturation, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay) {
        Fluid fluid = ModFluids.ELECTROLYTE_SOLUTION_SOURCE.get();
        IClientFluidTypeExtensions fluidType = IClientFluidTypeExtensions.of(fluid);
        TextureAtlasSprite sprite = sprite(fluidType.getStillTexture());
        int color = mix(fluidType.getTintColor() & 0xFFFFFF, MILKY, MAX_CLOUDING * Mth.clamp(saturation, 0.0F, 1.0F));
        VertexConsumer consumer = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
        boxFaces(consumer, poseStack.last(), sprite, BATH_X0, BATH_FLOOR, BATH_Z0, BATH_X1, BATH_SURFACE, BATH_Z1,
                color, 200, light, overlay, false);
    }

    private static void renderReservoir(Fluid fluid, float level, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay) {
        if (fluid == Fluids.EMPTY || level <= 0.001F) {
            return;
        }
        IClientFluidTypeExtensions fluidType = IClientFluidTypeExtensions.of(fluid);
        TextureAtlasSprite sprite = sprite(fluidType.getStillTexture());
        float top = 3.5F + 8.5F * Mth.clamp(level, 0.0F, 1.0F);
        VertexConsumer consumer = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
        boxFaces(consumer, poseStack.last(), sprite, 0.7F, 3.5F, 5.95F, 2.3F, top, 8.55F,
                fluidType.getTintColor() & 0xFFFFFF, 230, light, overlay, false);
    }

    private static int metalColor(String itemId) {
        String path = itemId.contains(":") ? itemId.substring(itemId.indexOf(':') + 1) : itemId;
        for (Map.Entry<String, Integer> entry : METALS.entrySet()) {
            if (path.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return DEFAULT_METAL;
    }

    private static TextureAtlasSprite sprite(ResourceLocation location) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(location);
    }

    private static float hash(int seed) {
        int value = seed * 0x9E3779B1;
        value ^= value >>> 15;
        value *= 0x85EBCA6B;
        value ^= value >>> 13;
        return (value & 0xFFFF) / 65535.0F;
    }

    private static int mix(int from, int to, float amount) {
        int red = Math.round(Mth.lerp(amount, from >> 16 & 0xFF, to >> 16 & 0xFF));
        int green = Math.round(Mth.lerp(amount, from >> 8 & 0xFF, to >> 8 & 0xFF));
        int blue = Math.round(Mth.lerp(amount, from & 0xFF, to & 0xFF));
        return red << 16 | green << 8 | blue;
    }

    private static void box(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
            float x0, float y0, float z0, float x1, float y1, float z1,
            int color, int alpha, int light, int overlay
    ) {
        boxFaces(consumer, pose, sprite, x0, y0, z0, x1, y1, z1, color, alpha, light, overlay, true);
    }

    /** An axis-aligned box with each face wound outward and textured by position, 16 model pixels to a sprite. */
    private static void boxFaces(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
            float x0, float y0, float z0, float x1, float y1, float z1,
            int color, int alpha, int light, int overlay, boolean bottom
    ) {
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        // North (-z): u = 16 - x, v = 16 - y.
        quad(consumer, pose, sprite, red, green, blue, alpha, light, overlay, 0.0F, 0.0F, -1.0F,
                x1, y0, z0, 16 - x1, 16 - y0,
                x0, y0, z0, 16 - x0, 16 - y0,
                x0, y1, z0, 16 - x0, 16 - y1,
                x1, y1, z0, 16 - x1, 16 - y1);
        // South (+z): u = x.
        quad(consumer, pose, sprite, red, green, blue, alpha, light, overlay, 0.0F, 0.0F, 1.0F,
                x0, y0, z1, x0, 16 - y0,
                x1, y0, z1, x1, 16 - y0,
                x1, y1, z1, x1, 16 - y1,
                x0, y1, z1, x0, 16 - y1);
        // West (-x): u = z.
        quad(consumer, pose, sprite, red, green, blue, alpha, light, overlay, -1.0F, 0.0F, 0.0F,
                x0, y0, z0, z0, 16 - y0,
                x0, y0, z1, z1, 16 - y0,
                x0, y1, z1, z1, 16 - y1,
                x0, y1, z0, z0, 16 - y1);
        // East (+x): u = 16 - z.
        quad(consumer, pose, sprite, red, green, blue, alpha, light, overlay, 1.0F, 0.0F, 0.0F,
                x1, y0, z1, 16 - z1, 16 - y0,
                x1, y0, z0, 16 - z0, 16 - y0,
                x1, y1, z0, 16 - z0, 16 - y1,
                x1, y1, z1, 16 - z1, 16 - y1);
        // Up (+y): u = x, v = z.
        quad(consumer, pose, sprite, red, green, blue, alpha, light, overlay, 0.0F, 1.0F, 0.0F,
                x0, y1, z0, x0, z0,
                x0, y1, z1, x0, z1,
                x1, y1, z1, x1, z1,
                x1, y1, z0, x1, z0);
        if (bottom) {
            // Down (-y): u = x, v = 16 - z.
            quad(consumer, pose, sprite, red, green, blue, alpha, light, overlay, 0.0F, -1.0F, 0.0F,
                    x0, y0, z1, x0, 16 - z1,
                    x0, y0, z0, x0, 16 - z0,
                    x1, y0, z0, x1, 16 - z0,
                    x1, y0, z1, x1, 16 - z1);
        }
    }

    private static void quad(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
            int red, int green, int blue, int alpha, int light, int overlay, float nx, float ny, float nz,
            float xa, float ya, float za, float ua, float va,
            float xb, float yb, float zb, float ub, float vb,
            float xc, float yc, float zc, float uc, float vc,
            float xd, float yd, float zd, float ud, float vd
    ) {
        vertex(consumer, pose, xa, ya, za, u(sprite, ua), v(sprite, va), red, green, blue, alpha, light, overlay, nx, ny, nz);
        vertex(consumer, pose, xb, yb, zb, u(sprite, ub), v(sprite, vb), red, green, blue, alpha, light, overlay, nx, ny, nz);
        vertex(consumer, pose, xc, yc, zc, u(sprite, uc), v(sprite, vc), red, green, blue, alpha, light, overlay, nx, ny, nz);
        vertex(consumer, pose, xd, yd, zd, u(sprite, ud), v(sprite, vd), red, green, blue, alpha, light, overlay, nx, ny, nz);
    }

    private static float u(TextureAtlasSprite sprite, float pixels) {
        return sprite.getU(Mth.clamp(pixels / 16.0F, 0.0F, 1.0F));
    }

    private static float v(TextureAtlasSprite sprite, float pixels) {
        return sprite.getV(Mth.clamp(pixels / 16.0F, 0.0F, 1.0F));
    }

    private static void vertex(
            VertexConsumer consumer, PoseStack.Pose pose,
            float x, float y, float z, float u, float v,
            int red, int green, int blue, int alpha, int light, int overlay,
            float nx, float ny, float nz
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    /** Anodes are thick assembled electrodes; plates are thinner, and Scrap is a ragged scrap of either. */
    private enum FuelShape {
        ANODE(3.0F, 0.75F),
        PLATE(2.75F, 0.375F),
        SCRAP(2.25F, 0.5F);

        private final float halfWidth;
        private final float halfThickness;

        FuelShape(float halfWidth, float halfThickness) {
            this.halfWidth = halfWidth;
            this.halfThickness = halfThickness;
        }

        private static FuelShape of(String itemId) {
            if (itemId.endsWith("_anode")) {
                return ANODE;
            }
            return itemId.endsWith("scrap") ? SCRAP : PLATE;
        }
    }
}
