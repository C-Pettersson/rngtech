package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.content.registry.ModDataComponents;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A Mirror Catalyst's copy draws flipped left to right, in hand, in slots, on the ground and in frames. Every RNGTech item
 * model resolves a Mirrored stack to the same model mirrored across its x axis; models drawn by a custom renderer keep
 * their usual look.
 */
public final class MirroredItemModels {
    private MirroredItemModels() {
    }

    public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((location, model) -> isItemModel(location) && !model.isCustomRenderer()
                ? new MirrorAware(model)
                : model);
    }

    private static boolean isItemModel(ModelResourceLocation location) {
        return location.id().getNamespace().equals(RNGTech.MOD_ID)
                && location.variant().equals(ModelResourceLocation.INVENTORY_VARIANT);
    }

    /** The baked model unchanged, except that its overrides send a Mirrored stack to the mirrored model. */
    private static final class MirrorAware extends BakedModelWrapper<BakedModel> {
        private final ItemOverrides overrides;

        private MirrorAware(BakedModel original) {
            super(original);
            overrides = new MirrorOverrides(original);
        }

        @Override
        public ItemOverrides getOverrides() {
            return overrides;
        }
    }

    private static final class MirrorOverrides extends ItemOverrides {
        private final BakedModel original;
        private final Map<BakedModel, BakedModel> mirrored = Collections.synchronizedMap(new IdentityHashMap<>());

        private MirrorOverrides(BakedModel original) {
            this.original = original;
        }

        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
            BakedModel resolved = original.getOverrides().resolve(original, stack, level, entity, seed);
            if (resolved == null || !stack.has(ModDataComponents.MIRRORED.get())) {
                return resolved;
            }
            return mirrored.computeIfAbsent(resolved, Mirrored::new);
        }
    }

    /** The model's quads reflected across x = 0.5, wound the other way so they still face out. */
    private static final class Mirrored extends BakedModelWrapper<BakedModel> {
        private Mirrored(BakedModel original) {
            super(original);
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            return mirror(originalModel.getQuads(state, flip(side), random));
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType renderType) {
            return mirror(originalModel.getQuads(state, flip(side), random, data, renderType));
        }

        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean applyLeftHandTransform) {
            BakedModel applied = originalModel.applyTransform(context, poseStack, applyLeftHandTransform);
            return applied == originalModel ? this : new Mirrored(applied);
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            return originalModel.getRenderPasses(stack, fabulous).stream()
                    .map(pass -> pass == originalModel ? (BakedModel) this : new Mirrored(pass))
                    .toList();
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }
    }

    private static List<BakedQuad> mirror(List<BakedQuad> quads) {
        List<BakedQuad> mirrored = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            mirrored.add(mirror(quad));
        }
        return mirrored;
    }

    static BakedQuad mirror(BakedQuad quad) {
        int[] source = quad.getVertices();
        int stride = IQuadTransformer.STRIDE;
        int count = source.length / stride;
        int[] vertices = new int[source.length];
        for (int vertex = 0; vertex < count; vertex++) {
            int to = vertex * stride;
            System.arraycopy(source, (count - 1 - vertex) * stride, vertices, to, stride);
            int x = to + IQuadTransformer.POSITION;
            vertices[x] = Float.floatToRawIntBits(1.0F - Float.intBitsToFloat(vertices[x]));
            int normal = to + IQuadTransformer.NORMAL;
            vertices[normal] = (vertices[normal] & ~0xFF) | (-(byte) vertices[normal] & 0xFF);
        }
        return new BakedQuad(vertices, quad.getTintIndex(), flip(quad.getDirection()), quad.getSprite(), quad.isShade(),
                quad.hasAmbientOcclusion());
    }

    private static Direction flip(Direction side) {
        return side != null && side.getAxis() == Direction.Axis.X ? side.getOpposite() : side;
    }
}
