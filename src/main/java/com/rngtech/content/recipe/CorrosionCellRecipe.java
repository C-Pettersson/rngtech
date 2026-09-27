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
import net.minecraft.world.level.Level;

public record CorrosionCellRecipe(
        String group,
        Ingredient plate,
        Ingredient electrolyte,
        int energy,
        int processingTicks,
        ItemStack residue,
        int minimumMaterialStage
)
        implements Recipe<CorrosionCellRecipeInput> {
    @Override
    public boolean matches(CorrosionCellRecipeInput input, Level level) {
        return plate.test(input.plate()) && electrolyte.test(input.electrolyte());
    }

    @Override
    public ItemStack assemble(CorrosionCellRecipeInput input, HolderLookup.Provider registries) {
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
        ingredients.add(plate);
        ingredients.add(electrolyte);
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.CORROSION_CELL.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CORROSION_CELL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.CORROSION_CELL_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<CorrosionCellRecipe> {
        private static final MapCodec<CorrosionCellRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(CorrosionCellRecipe::group),
                        Ingredient.CODEC_NONEMPTY.fieldOf("plate").forGetter(CorrosionCellRecipe::plate),
                        Ingredient.CODEC_NONEMPTY.fieldOf("electrolyte").forGetter(CorrosionCellRecipe::electrolyte),
                        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("energy").forGetter(CorrosionCellRecipe::energy),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("processing_ticks")
                                .orElse(200)
                                .forGetter(CorrosionCellRecipe::processingTicks),
                        ItemStack.STRICT_CODEC.fieldOf("residue").forGetter(CorrosionCellRecipe::residue),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("minimum_material_stage")
                                .orElse(4)
                                .forGetter(CorrosionCellRecipe::minimumMaterialStage)
                )
                .apply(instance, CorrosionCellRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CorrosionCellRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public CorrosionCellRecipe decode(RegistryFriendlyByteBuf buffer) {
                        return new CorrosionCellRecipe(
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ItemStack.STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer)
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, CorrosionCellRecipe recipe) {
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group);
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.plate);
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.electrolyte);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.energy);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTicks);
                        ItemStack.STREAM_CODEC.encode(buffer, recipe.residue);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.minimumMaterialStage);
                    }
                };

        @Override
        public MapCodec<CorrosionCellRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CorrosionCellRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
