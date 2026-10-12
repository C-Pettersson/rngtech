package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.PotentialReactorBlockEntity;
import com.rngtech.content.blockentity.PotentialReactorVisualState;
import com.rngtech.rpg.Rarity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Vector3f;

import java.util.Map;

/**
 * Draws the working parts of the Potential Reactor: the Containment Lining sleeve, the Reactor Chamber electrodes, the
 * core between them (sized by reaction intensity, steadier at higher stability, violet on salvage fuel and in the
 * item's rarity colour when recycling gear), the Recovery Filter cartridge, residue in the bin, the FE buffer in the
 * charge sight glass, and the stability needle. Positions are in model pixels and match
 * tools/blockmodels/potential_reactor.py.
 */
public class PotentialReactorRenderer implements BlockEntityRenderer<PotentialReactorBlockEntity> {
    private static final ResourceLocation LINING = RNGTech.id("block/potential_reactor/fx/lining");
    private static final ResourceLocation ELECTRODE = RNGTech.id("block/potential_reactor/fx/electrode");
    private static final ResourceLocation CARTRIDGE = RNGTech.id("block/potential_reactor/fx/cartridge");
    private static final ResourceLocation SCRAP = RNGTech.id("block/potential_reactor/fx/scrap");
    private static final ResourceLocation CHARGE = RNGTech.id("block/potential_reactor/fx/charge");
    private static final ResourceLocation GLOW = RNGTech.id("textures/block/potential_reactor/fx/glow.png");

    private static final float CORE_X = 8.0F;
    private static final float CORE_Y = 7.5F;
    private static final float CORE_Z = 7.0F;
    private static final float CUP_LOW = 5.5F;
    private static final float CUP_HIGH = 9.5F;
    private static final float LINING_APOTHEM = 3.85F;
    private static final float LINING_RADIUS = LINING_APOTHEM / Mth.cos((float) Math.toRadians(22.5));
    private static final float LINING_Y0 = 3.0F;
    private static final float LINING_Y1 = 12.0F;
    /** The octagon side facing the viewport is left open. */
    private static final int LINING_OPEN_SIDE = 5;
    private static final float[][] ELECTRODE_BOXES = {
            {6.5F, 3.0F, 5.5F, 9.5F, 4.0F, 8.5F},
            {7.25F, 4.0F, 6.25F, 8.75F, 5.0F, 7.75F},
            {6.75F, 5.0F, 5.75F, 9.25F, 5.5F, 8.25F},
            {6.75F, 9.5F, 5.75F, 9.25F, 10.0F, 8.25F},
            {7.25F, 10.0F, 6.25F, 8.75F, 11.0F, 7.75F},
            {6.5F, 11.0F, 5.5F, 9.5F, 12.0F, 8.5F},
    };
    private static final float[] CARTRIDGE_BOX = {14.1F, 7.0F, 3.1F, 15.4F, 10.5F, 5.9F};
    private static final float BIN_X0 = 13.95F;
    private static final float BIN_X1 = 15.55F;
    private static final float BIN_Z0 = 7.2F;
    private static final float BIN_Z1 = 11.05F;
    private static final float BIN_FLOOR = 2.5F;
    private static final float BIN_DEPTH = 2.6F;
    private static final float SIGHT_X0 = 0.65F;
    private static final float SIGHT_X1 = 2.35F;
    private static final float SIGHT_Z0 = 5.15F;
    private static final float SIGHT_Z1 = 7.35F;
    private static final float SIGHT_Y0 = 3.5F;
    private static final float SIGHT_Y1 = 11.5F;
    private static final float DIAL_X = 2.0F;
    private static final float DIAL_Y = 3.25F;
    private static final float DIAL_FACE_Z = 0.24F;
    private static final float NEEDLE_LENGTH = 0.7F;
    private static final float NEEDLE_SWEEP = 60.0F;
    private static final float DIAL_MIN_STABILITY = 0.5F;
    private static final float DIAL_MAX_STABILITY = 1.5F;
    /** Glow a paused run keeps, so a loaded reactor reads as banked rather than off. */
    private static final float HELD_GLOW = 0.3F;
    private static final int SALVAGE_GLOW = 0xB070FF;
    private static final int CHARGE_COLOR = 0x5BA850;
    private static final int NEEDLE_COLOR = 0xC43428;
    private static final int DEFAULT_METAL = 0xC0C0BA;
    private static final Map<String, Integer> METALS = Map.of(
            "iron", 0xC9C9C2,
            "copper", 0xD98A5A,
            "bronze", 0xC79A52,
            "steel", 0x8D98A4
    );

    public PotentialReactorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            PotentialReactorBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        PotentialReactorVisualState state = blockEntity.visualState();
        PotentialReactorVisualState.Animation animation = blockEntity.animation();
        float time = blockEntity.getLevel().getGameTime() + partialTick;
        animate(animation, state, time);

        Direction facing = blockEntity.getBlockState().getValue(BaseMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        PoseStack.Pose pose = poseStack.last();

        // The core lights the inside of the vessel.
        int innerLight = LightTexture.pack(
                Math.max(LightTexture.block(packedLight), Math.round(animation.glow * 13.0F)),
                LightTexture.sky(packedLight)
        );
        VertexConsumer cutout = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        if (!state.lining().isEmpty()) {
            renderLining(cutout, pose, metal(state.lining()), innerLight, packedOverlay);
        }
        if (!state.chamber().isEmpty()) {
            TextureAtlasSprite sprite = sprite(ELECTRODE);
            for (float[] part : ELECTRODE_BOXES) {
                cube(cutout, pose, sprite, 4.0F, part, metal(state.chamber()), 255, innerLight, packedOverlay);
            }
        }
        if (!state.filter().isEmpty()) {
            cube(cutout, pose, sprite(CARTRIDGE), 4.0F, CARTRIDGE_BOX, metal(state.filter()), 255, packedLight, packedOverlay);
        }
        if (state.residue() > 0.001F) {
            float top = BIN_FLOOR + Math.max(0.2F, BIN_DEPTH * Mth.clamp(state.residue(), 0.0F, 1.0F));
            cube(cutout, pose, sprite(SCRAP), 4.0F, new float[] {BIN_X0, BIN_FLOOR, BIN_Z0, BIN_X1, top, BIN_Z1}, 0xFFFFFF, 255, packedLight, packedOverlay);
        }
        renderNeedle(cutout, pose, animation.needle, packedLight, packedOverlay);

        if (state.charge() > 0.001F) {
            float top = SIGHT_Y0 + (SIGHT_Y1 - SIGHT_Y0) * Mth.clamp(state.charge(), 0.0F, 1.0F);
            cube(bufferSource.getBuffer(Sheets.translucentCullBlockSheet()), pose, sprite(CHARGE), 16.0F,
                    new float[] {SIGHT_X0, SIGHT_Y0, SIGHT_Z0, SIGHT_X1, top, SIGHT_Z1}, CHARGE_COLOR, 210, LightTexture.FULL_BRIGHT, packedOverlay);
        }
        if (animation.glow > 0.01F && !state.chamber().isEmpty()) {
            renderCore(bufferSource.getBuffer(RenderType.eyes(GLOW)), poseStack, state, animation, time, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void animate(PotentialReactorVisualState.Animation animation, PotentialReactorVisualState state, float time) {
        float elapsed = Double.isNaN(animation.lastTime) ? 0.0F : Mth.clamp((float) (time - animation.lastTime), 0.0F, 10.0F);
        animation.lastTime = time;
        float glow = switch (state.core()) {
            case PotentialReactorVisualState.CORE_REACTING -> 1.0F;
            case PotentialReactorVisualState.CORE_HELD -> HELD_GLOW;
            default -> 0.0F;
        };
        animation.glow += (glow - animation.glow) * Math.min(1.0F, elapsed * 0.08F);
        animation.intensity += (state.intensity() - animation.intensity) * Math.min(1.0F, elapsed * 0.05F);
        float needle = state.chamber().isEmpty()
                ? -1.0F
                : Mth.clamp((state.stability() - DIAL_MIN_STABILITY) / (DIAL_MAX_STABILITY - DIAL_MIN_STABILITY) * 2.0F - 1.0F, -1.0F, 1.0F);
        animation.needle += (needle - animation.needle) * Math.min(1.0F, elapsed * 0.1F);
    }

    /** The lining is a sleeve inside the vessel walls, open on the viewport side. */
    private static void renderLining(VertexConsumer consumer, PoseStack.Pose pose, int color, int light, int overlay) {
        TextureAtlasSprite sprite = sprite(LINING);
        float side = 2.0F * LINING_APOTHEM * (float) Math.tan(Math.toRadians(22.5));
        float vTop = sprite.getV((LINING_Y1 - LINING_Y0) / 16.0F);
        for (int i = 0; i < 8; i++) {
            if (i == LINING_OPEN_SIDE) {
                continue;
            }
            double a0 = Math.toRadians(22.5 + 45.0 * i);
            double a1 = Math.toRadians(22.5 + 45.0 * (i + 1));
            float x0 = CORE_X + LINING_RADIUS * (float) Math.cos(a0);
            float z0 = CORE_Z + LINING_RADIUS * (float) Math.sin(a0);
            float x1 = CORE_X + LINING_RADIUS * (float) Math.cos(a1);
            float z1 = CORE_Z + LINING_RADIUS * (float) Math.sin(a1);
            Vector3f inward = new Vector3f(CORE_X - (x0 + x1) / 2.0F, 0.0F, CORE_Z - (z0 + z1) / 2.0F).normalize();
            float u0 = sprite.getU(i % 4 / 4.0F);
            float u1 = sprite.getU(i % 4 / 4.0F + side / 16.0F);
            quad(consumer, pose, inward, color, 255, light, overlay,
                    new float[] {x0, LINING_Y0, z0, u0, sprite.getV0()},
                    new float[] {x1, LINING_Y0, z1, u1, sprite.getV0()},
                    new float[] {x1, LINING_Y1, z1, u1, vTop},
                    new float[] {x0, LINING_Y1, z0, u0, vTop});
        }
    }

    /**
     * The core hangs between the electrodes: a bright heart in a softer halo, with an arc to each electrode while it
     * reacts. Intensity sets its size and brightness; low stability makes it flicker, wander, and arc unevenly.
     */
    private static void renderCore(
            VertexConsumer consumer,
            PoseStack poseStack,
            PotentialReactorVisualState state,
            PotentialReactorVisualState.Animation animation,
            float time,
            int overlay
    ) {
        float intensity = Mth.clamp(animation.intensity, 0.0F, 1.0F);
        float instability = state.instability();
        float crackle = hash((int) Math.floor(time / 2.0F));
        float flicker = 1.0F - instability * (0.25F * (Mth.sin(time * 0.9F) + 1.0F) * 0.5F + 0.35F * crackle);
        float brightness = Mth.clamp(animation.glow * (0.55F + 0.45F * intensity) * flicker, 0.0F, 1.0F);
        int color = scale(glowColor(state.burn()), brightness);
        float wander = instability * 0.3F;
        float cx = CORE_X + Mth.sin(time * 0.37F) * wander;
        float cy = CORE_Y + Mth.sin(time * 0.53F + 1.3F) * wander * 0.5F;
        float cz = CORE_Z + Mth.cos(time * 0.41F) * wander;
        float heart = (0.35F + 0.45F * intensity) * (0.6F + 0.4F * animation.glow);
        float halo = Math.min(1.15F, heart * 1.6F);

        spinningCube(consumer, poseStack, cx, cy, cz, halo, time * 1.5F, scale(color, 0.55F), overlay);
        spinningCube(consumer, poseStack, cx, cy, cz, heart, -time * 3.0F, color, overlay);
        if (state.core() == PotentialReactorVisualState.CORE_REACTING) {
            float width = (0.12F + 0.12F * intensity) * (1.0F - 0.5F * instability * crackle);
            PoseStack.Pose pose = poseStack.last();
            cube(consumer, pose, null, 16.0F, new float[] {cx - width, CUP_LOW, cz - width, cx + width, cy, cz + width}, color, 255, LightTexture.FULL_BRIGHT, overlay);
            cube(consumer, pose, null, 16.0F, new float[] {cx - width, cy, cz - width, cx + width, CUP_HIGH, cz + width}, color, 255, LightTexture.FULL_BRIGHT, overlay);
        }
    }

    private static void spinningCube(
            VertexConsumer consumer, PoseStack poseStack, float cx, float cy, float cz, float half, float degrees, int color, int overlay
    ) {
        poseStack.pushPose();
        poseStack.translate(cx, cy, cz);
        poseStack.mulPose(Axis.YP.rotationDegrees(degrees));
        poseStack.mulPose(Axis.XP.rotationDegrees(35.0F));
        cube(consumer, poseStack.last(), null, 16.0F, new float[] {-half, -half, -half, half, half, half}, color, 255, LightTexture.FULL_BRIGHT, overlay);
        poseStack.popPose();
    }

    /** The stability needle sweeps from the red end (low) to the green end (high); it rests low with no chamber. */
    private static void renderNeedle(VertexConsumer consumer, PoseStack.Pose pose, float position, int light, int overlay) {
        TextureAtlasSprite sprite = sprite(ELECTRODE);
        double angle = Math.toRadians(position * NEEDLE_SWEEP);
        // The dial faces north, so the viewer's right is towards -x.
        float dx = (float) -Math.sin(angle);
        float dy = (float) Math.cos(angle);
        float px = -dy * 0.09F;
        float py = dx * 0.09F;
        float tipX = DIAL_X + dx * NEEDLE_LENGTH;
        float tipY = DIAL_Y + dy * NEEDLE_LENGTH;
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), NEEDLE_COLOR, 255, light, overlay,
                new float[] {DIAL_X - px, DIAL_Y - py, DIAL_FACE_Z, sprite.getU0(), sprite.getV1()},
                new float[] {DIAL_X + px, DIAL_Y + py, DIAL_FACE_Z, sprite.getU1(), sprite.getV1()},
                new float[] {tipX + px * 0.3F, tipY + py * 0.3F, DIAL_FACE_Z, sprite.getU1(), sprite.getV0()},
                new float[] {tipX - px * 0.3F, tipY - py * 0.3F, DIAL_FACE_Z, sprite.getU0(), sprite.getV0()});
    }

    private static int glowColor(int burn) {
        if (burn < 0 || burn >= Rarity.values().length) {
            return SALVAGE_GLOW;
        }
        return switch (Rarity.byId(burn)) {
            case NORMAL -> 0xE6E6F0;
            case MAGIC -> 0x55FFFF;
            case RARE -> 0xFFAA00;
            case UNIQUE -> 0xAF6025;
        };
    }

    private static int metal(String material) {
        return METALS.getOrDefault(material, DEFAULT_METAL);
    }

    /**
     * A box whose faces show {@code span} model pixels per full sprite, cropped to the face size. A null sprite maps
     * the whole texture to every face, for the standalone glow texture.
     */
    private static void cube(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, float span, float[] bounds,
            int color, int alpha, int light, int overlay
    ) {
        float x0 = bounds[0];
        float y0 = bounds[1];
        float z0 = bounds[2];
        float x1 = bounds[3];
        float y1 = bounds[4];
        float z1 = bounds[5];
        float u0 = sprite == null ? 0.0F : sprite.getU0();
        float v0 = sprite == null ? 0.0F : sprite.getV0();
        float uw = sprite == null ? 1.0F : sprite.getU1() - u0;
        float vh = sprite == null ? 1.0F : sprite.getV1() - v0;
        float du = sprite == null ? 1.0F : Math.min(1.0F, (x1 - x0) / span);
        float dz = sprite == null ? 1.0F : Math.min(1.0F, (z1 - z0) / span);
        float dy = sprite == null ? 1.0F : Math.min(1.0F, (y1 - y0) / span);
        float ux = u0 + uw * du;
        float uz = u0 + uw * dz;
        float vy = v0 + vh * dy;
        float vz = v0 + vh * dz;
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, -1.0F), color, alpha, light, overlay,
                new float[] {x0, y0, z0, u0, vy}, new float[] {x1, y0, z0, ux, vy}, new float[] {x1, y1, z0, ux, v0}, new float[] {x0, y1, z0, u0, v0});
        quad(consumer, pose, new Vector3f(0.0F, 0.0F, 1.0F), color, alpha, light, overlay,
                new float[] {x0, y0, z1, u0, vy}, new float[] {x1, y0, z1, ux, vy}, new float[] {x1, y1, z1, ux, v0}, new float[] {x0, y1, z1, u0, v0});
        quad(consumer, pose, new Vector3f(-1.0F, 0.0F, 0.0F), color, alpha, light, overlay,
                new float[] {x0, y0, z0, u0, vy}, new float[] {x0, y0, z1, uz, vy}, new float[] {x0, y1, z1, uz, v0}, new float[] {x0, y1, z0, u0, v0});
        quad(consumer, pose, new Vector3f(1.0F, 0.0F, 0.0F), color, alpha, light, overlay,
                new float[] {x1, y0, z0, u0, vy}, new float[] {x1, y0, z1, uz, vy}, new float[] {x1, y1, z1, uz, v0}, new float[] {x1, y1, z0, u0, v0});
        quad(consumer, pose, new Vector3f(0.0F, 1.0F, 0.0F), color, alpha, light, overlay,
                new float[] {x0, y1, z0, u0, v0}, new float[] {x1, y1, z0, ux, v0}, new float[] {x1, y1, z1, ux, vz}, new float[] {x0, y1, z1, u0, vz});
        quad(consumer, pose, new Vector3f(0.0F, -1.0F, 0.0F), color, alpha, light, overlay,
                new float[] {x0, y0, z0, u0, v0}, new float[] {x1, y0, z0, ux, v0}, new float[] {x1, y0, z1, ux, vz}, new float[] {x0, y0, z1, u0, vz});
    }

    /**
     * One quad from four {x, y, z, u, v} corners in perimeter order. The winding is flipped when needed so the quad
     * faces along {@code normal} and survives back-face culling.
     */
    private static void quad(
            VertexConsumer consumer, PoseStack.Pose pose, Vector3f normal,
            int color, int alpha, int light, int overlay,
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
                    .setColor(red, green, blue, alpha)
                    .setUv(corner[3], corner[4])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose, normal.x(), normal.y(), normal.z());
        }
    }

    private static TextureAtlasSprite sprite(ResourceLocation id) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(id);
    }

    private static int scale(int color, float factor) {
        int red = Math.round((color >> 16 & 0xFF) * factor);
        int green = Math.round((color >> 8 & 0xFF) * factor);
        int blue = Math.round((color & 0xFF) * factor);
        return red << 16 | green << 8 | blue;
    }

    /** A stable pseudo-random value in [0, 1) for each step, for the core's crackle. */
    private static float hash(int step) {
        int h = step * 0x9E3779B9;
        h ^= h >>> 15;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        return (h & 0xFFFF) / 65536.0F;
    }
}
