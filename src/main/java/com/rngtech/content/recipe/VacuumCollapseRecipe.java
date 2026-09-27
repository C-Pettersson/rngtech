package com.rngtech.content.recipe;

import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModRecipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public record VacuumCollapseRecipe(
        String group,
        Ingredient ingredient,
        int minimumChamberStage,
        double minimumStability,
        long energy,
        int processingTicks,
        double instability,
        ItemStack residue,
        boolean residueRequiresStabilizer
) implements Recipe<SingleRecipeInput> {
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return residue.copy();
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
        return residue;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(ingredient);
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.VACUUM_COLLAPSE_GENERATOR.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.VACUUM_COLLAPSE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.VACUUM_COLLAPSE_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<VacuumCollapseRecipe> {
        private static final MapCodec<VacuumCollapseRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(VacuumCollapseRecipe::group),
                        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(VacuumCollapseRecipe::ingredient),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("minimum_chamber_stage")
                                .orElse(7)
                                .forGetter(VacuumCollapseRecipe::minimumChamberStage),
                        Codec.DOUBLE.fieldOf("minimum_stability").orElse(0.0).forGetter(VacuumCollapseRecipe::minimumStability),
                        Codec.LONG.fieldOf("energy").forGetter(VacuumCollapseRecipe::energy),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("processing_ticks")
                                .orElse(400)
                                .forGetter(VacuumCollapseRecipe::processingTicks),
                        Codec.DOUBLE.fieldOf("instability").orElse(0.0).forGetter(VacuumCollapseRecipe::instability),
                        ItemStack.OPTIONAL_CODEC
                                .optionalFieldOf("residue", ItemStack.EMPTY)
                                .forGetter(VacuumCollapseRecipe::residue),
                        Codec.BOOL
                                .optionalFieldOf("residue_requires_stabilizer", false)
                                .forGetter(VacuumCollapseRecipe::residueRequiresStabilizer)
                )
                .apply(instance, VacuumCollapseRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, VacuumCollapseRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public VacuumCollapseRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new VacuumCollapseRecipe(
                        ByteBufCodecs.STRING_UTF8.decode(buffer),
                        Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                        ByteBufCodecs.VAR_INT.decode(buffer),
                        buffer.readDouble(),
                        buffer.readLong(),
                        ByteBufCodecs.VAR_INT.decode(buffer),
                        buffer.readDouble(),
                        ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                        buffer.readBoolean()
                );
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, VacuumCollapseRecipe recipe) {
                ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group());
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient());
                ByteBufCodecs.VAR_INT.encode(buffer, recipe.minimumChamberStage());
                buffer.writeDouble(recipe.minimumStability());
                buffer.writeLong(recipe.energy());
                ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTicks());
                buffer.writeDouble(recipe.instability());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.residue());
                buffer.writeBoolean(recipe.residueRequiresStabilizer());
            }
        };

        @Override
        public MapCodec<VacuumCollapseRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, VacuumCollapseRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
