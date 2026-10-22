package com.rngtech.content.recipe;

import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModRecipes;
import com.rngtech.rpg.PhaseArrayRules;
import com.rngtech.rpg.refinement.PhaseArrayAction;
import com.rngtech.rpg.refinement.PhaseOutcome;
import com.rngtech.rpg.refinement.RefinementTargets;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * One Phase Array operation: what it does, how many Coils it needs, the Glyph tier it consumes, its base FE, its fixed
 * Refinement Potential cost, and its Partial and Overhold outcomes. Packs retune, remove, or add operations by datapack.
 */
public record PhaseArrayRecipe(
        PhaseArrayAction action,
        int coils,
        int glyphTier,
        long baseFe,
        int refinementPotentialCost,
        PhaseOutcome.Partial partial,
        PhaseOutcome.Overhold overhold
) implements Recipe<PhaseArrayRecipeInput> {
    public static final Codec<PhaseArrayAction> ACTION_CODEC = StringRepresentable.fromEnum(PhaseArrayAction::values);
    public static final Codec<PhaseOutcome.Partial> PARTIAL_CODEC = StringRepresentable.fromEnum(PhaseOutcome.Partial::values);
    public static final Codec<PhaseOutcome.Overhold> OVERHOLD_CODEC = StringRepresentable.fromEnum(PhaseOutcome.Overhold::values);

    public PhaseArrayRecipe {
        coils = Math.max(PhaseArrayRules.MIN_COILS, Math.min(PhaseArrayRules.MAX_COILS, coils));
        glyphTier = Math.max(0, Math.min(PhaseArrayRules.GLYPH_TIERS, glyphTier));
        baseFe = Math.max(0L, baseFe);
        refinementPotentialCost = Math.max(0, refinementPotentialCost);
    }

    public boolean needsGlyph() {
        return glyphTier > 0;
    }

    /** The FE this operation costs on {@code target}, before the line's Thrift. */
    public long feFor(ItemStack target) {
        if (target.isEmpty() || !RefinementTargets.canRefine(target)) {
            return PhaseArrayRules.pulseCost(baseFe, PhaseArrayRules.PART_STAGE, 0);
        }
        int quality = com.rngtech.rpg.refinement.RefinementEngine.phaseQuality(RefinementTargets.storedTraits(target));
        return PhaseArrayRules.pulseCost(baseFe, RefinementTargets.componentStage(target), quality);
    }

    @Override
    public boolean matches(PhaseArrayRecipeInput input, Level level) {
        return input.action() == action;
    }

    @Override
    public ItemStack assemble(PhaseArrayRecipeInput input, HolderLookup.Provider registries) {
        return input.target().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.EXOTIC_AFFIX_FORGE.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.PHASE_ARRAY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.PHASE_ARRAY_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<PhaseArrayRecipe> {
        private static final MapCodec<PhaseArrayRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ACTION_CODEC.fieldOf("action").forGetter(PhaseArrayRecipe::action),
                Codec.intRange(PhaseArrayRules.MIN_COILS, PhaseArrayRules.MAX_COILS).fieldOf("coils").forGetter(PhaseArrayRecipe::coils),
                Codec.intRange(0, PhaseArrayRules.GLYPH_TIERS).fieldOf("glyph_tier").orElse(0).forGetter(PhaseArrayRecipe::glyphTier),
                Codec.LONG.fieldOf("base_fe").forGetter(PhaseArrayRecipe::baseFe),
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("refinement_potential_cost").orElse(0)
                        .forGetter(PhaseArrayRecipe::refinementPotentialCost),
                PARTIAL_CODEC.fieldOf("partial").orElse(PhaseOutcome.Partial.NONE).forGetter(PhaseArrayRecipe::partial),
                OVERHOLD_CODEC.fieldOf("overhold").orElse(PhaseOutcome.Overhold.NONE).forGetter(PhaseArrayRecipe::overhold)
        ).apply(instance, PhaseArrayRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, PhaseArrayRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public PhaseArrayRecipe decode(RegistryFriendlyByteBuf buffer) {
                PhaseArrayAction action = buffer.readEnum(PhaseArrayAction.class);
                int coils = buffer.readVarInt();
                int glyphTier = buffer.readVarInt();
                long baseFe = buffer.readVarLong();
                int rp = buffer.readVarInt();
                PhaseOutcome.Partial partial = buffer.readEnum(PhaseOutcome.Partial.class);
                PhaseOutcome.Overhold overhold = buffer.readEnum(PhaseOutcome.Overhold.class);
                return new PhaseArrayRecipe(action, coils, glyphTier, baseFe, rp, partial, overhold);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, PhaseArrayRecipe recipe) {
                buffer.writeEnum(recipe.action());
                buffer.writeVarInt(recipe.coils());
                buffer.writeVarInt(recipe.glyphTier());
                buffer.writeVarLong(recipe.baseFe());
                buffer.writeVarInt(recipe.refinementPotentialCost());
                buffer.writeEnum(recipe.partial());
                buffer.writeEnum(recipe.overhold());
            }
        };

        @Override
        public MapCodec<PhaseArrayRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PhaseArrayRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
