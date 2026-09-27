package com.rngtech.content.recipe;

import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModRecipes;
import com.rngtech.rpg.refinement.ExoticAffixForgeAction;
import com.rngtech.rpg.refinement.RefinementTargets;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Optional;

public record ExoticAffixForgeRecipe(
        String group,
        ExoticAffixForgeAction action,
        Optional<Ingredient> target,
        Ingredient catalyst,
        int catalystCount,
        int maxCatalystCount,
        long energy,
        int processingTicks,
        int refinementPotentialCost,
        double totalUseEnergyMultiplier,
        double actionUseEnergyMultiplier
) implements Recipe<ExoticAffixForgeRecipeInput> {
    public ExoticAffixForgeRecipe {
        catalystCount = Math.max(1, catalystCount);
        maxCatalystCount = Math.max(catalystCount, maxCatalystCount);
        energy = Math.max(1L, energy);
        processingTicks = Math.max(1, processingTicks);
        refinementPotentialCost = Math.max(0, refinementPotentialCost);
        totalUseEnergyMultiplier = Math.max(0.0D, totalUseEnergyMultiplier);
        actionUseEnergyMultiplier = Math.max(0.0D, actionUseEnergyMultiplier);
    }

    @Override
    public boolean matches(ExoticAffixForgeRecipeInput input, Level level) {
        return action == input.action()
                && RefinementTargets.canRefine(input.target())
                && target.map(targetIngredient -> targetIngredient.test(input.target())).orElse(true)
                && catalyst.test(input.catalyst());
    }

    @Override
    public ItemStack assemble(ExoticAffixForgeRecipeInput input, HolderLookup.Provider registries) {
        return input.target().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        target.ifPresent(ingredients::add);
        ingredients.add(catalyst);
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.EXOTIC_AFFIX_FORGE.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.EXOTIC_AFFIX_FORGE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.EXOTIC_AFFIX_FORGE_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<ExoticAffixForgeRecipe> {
        private static final MapCodec<ExoticAffixForgeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(ExoticAffixForgeRecipe::group),
                        ExoticAffixForgeAction.CODEC.fieldOf("action").forGetter(ExoticAffixForgeRecipe::action),
                        Ingredient.CODEC_NONEMPTY.optionalFieldOf("target").forGetter(ExoticAffixForgeRecipe::target),
                        Ingredient.CODEC_NONEMPTY.fieldOf("catalyst").forGetter(ExoticAffixForgeRecipe::catalyst),
                        Codec.intRange(1, 64)
                                .fieldOf("catalyst_count")
                                .orElse(1)
                                .forGetter(ExoticAffixForgeRecipe::catalystCount),
                        Codec.intRange(1, 64)
                                .fieldOf("max_catalyst_count")
                                .orElse(16)
                                .forGetter(ExoticAffixForgeRecipe::maxCatalystCount),
                        Codec.LONG.fieldOf("energy").forGetter(ExoticAffixForgeRecipe::energy),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("processing_ticks")
                                .orElse(200)
                                .forGetter(ExoticAffixForgeRecipe::processingTicks),
                        Codec.intRange(0, Integer.MAX_VALUE)
                                .fieldOf("refinement_potential_cost")
                                .orElse(0)
                                .forGetter(ExoticAffixForgeRecipe::refinementPotentialCost),
                        Codec.DOUBLE
                                .fieldOf("total_use_energy_multiplier")
                                .orElse(0.0D)
                                .forGetter(ExoticAffixForgeRecipe::totalUseEnergyMultiplier),
                        Codec.DOUBLE
                                .fieldOf("action_use_energy_multiplier")
                                .orElse(0.0D)
                                .forGetter(ExoticAffixForgeRecipe::actionUseEnergyMultiplier)
                )
                .apply(instance, ExoticAffixForgeRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ExoticAffixForgeRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public ExoticAffixForgeRecipe decode(RegistryFriendlyByteBuf buffer) {
                        String group = buffer.readUtf();
                        ExoticAffixForgeAction action = ExoticAffixForgeAction.STREAM_CODEC.decode(buffer);
                        Optional<Ingredient> target = buffer.readBoolean()
                                ? Optional.of(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer))
                                : Optional.empty();
                        Ingredient catalyst = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                        int catalystCount = buffer.readVarInt();
                        int maxCatalystCount = buffer.readVarInt();
                        long energy = buffer.readVarLong();
                        int processingTicks = buffer.readVarInt();
                        int refinementPotentialCost = buffer.readVarInt();
                        double totalUseEnergyMultiplier = buffer.readDouble();
                        double actionUseEnergyMultiplier = buffer.readDouble();
                        return new ExoticAffixForgeRecipe(
                                group,
                                action,
                                target,
                                catalyst,
                                catalystCount,
                                maxCatalystCount,
                                energy,
                                processingTicks,
                                refinementPotentialCost,
                                totalUseEnergyMultiplier,
                                actionUseEnergyMultiplier
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, ExoticAffixForgeRecipe recipe) {
                        buffer.writeUtf(recipe.group);
                        ExoticAffixForgeAction.STREAM_CODEC.encode(buffer, recipe.action);
                        buffer.writeBoolean(recipe.target.isPresent());
                        recipe.target.ifPresent(ingredient -> Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient));
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.catalyst);
                        buffer.writeVarInt(recipe.catalystCount);
                        buffer.writeVarInt(recipe.maxCatalystCount);
                        buffer.writeVarLong(recipe.energy);
                        buffer.writeVarInt(recipe.processingTicks);
                        buffer.writeVarInt(recipe.refinementPotentialCost);
                        buffer.writeDouble(recipe.totalUseEnergyMultiplier);
                        buffer.writeDouble(recipe.actionUseEnergyMultiplier);
                    }
                };

        @Override
        public MapCodec<ExoticAffixForgeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ExoticAffixForgeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
