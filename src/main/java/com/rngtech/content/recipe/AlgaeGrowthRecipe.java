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
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

public record AlgaeGrowthRecipe(
        String group,
        SizedFluidIngredient waterInput,
        SizedFluidIngredient carbonInput,
        ItemStack result,
        int processingTicks,
        int minimumLight
)
        implements Recipe<AlgaeGrowthRecipeInput> {
    public ItemStack output() {
        return result.copy();
    }

    @Override
    public boolean matches(AlgaeGrowthRecipeInput input, Level level) {
        return waterInput.test(input.water()) && carbonInput.test(input.carbon());
    }

    @Override
    public ItemStack assemble(AlgaeGrowthRecipeInput input, HolderLookup.Provider registries) {
        return output();
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
        return output();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.ALGAE_PHOTOBIOREACTOR.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ALGAE_GROWTH_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ALGAE_GROWTH_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<AlgaeGrowthRecipe> {
        private static final MapCodec<AlgaeGrowthRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(AlgaeGrowthRecipe::group),
                        SizedFluidIngredient.NESTED_CODEC.fieldOf("water_input").forGetter(AlgaeGrowthRecipe::waterInput),
                        SizedFluidIngredient.NESTED_CODEC.fieldOf("carbon_input").forGetter(AlgaeGrowthRecipe::carbonInput),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AlgaeGrowthRecipe::result),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("processing_ticks")
                                .orElse(1200)
                                .forGetter(AlgaeGrowthRecipe::processingTicks),
                        Codec.intRange(0, 15)
                                .fieldOf("minimum_light")
                                .orElse(12)
                                .forGetter(AlgaeGrowthRecipe::minimumLight)
                )
                .apply(instance, AlgaeGrowthRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, AlgaeGrowthRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public AlgaeGrowthRecipe decode(RegistryFriendlyByteBuf buffer) {
                        return new AlgaeGrowthRecipe(
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                SizedFluidIngredient.STREAM_CODEC.decode(buffer),
                                SizedFluidIngredient.STREAM_CODEC.decode(buffer),
                                ItemStack.STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer)
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, AlgaeGrowthRecipe recipe) {
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group);
                        SizedFluidIngredient.STREAM_CODEC.encode(buffer, recipe.waterInput);
                        SizedFluidIngredient.STREAM_CODEC.encode(buffer, recipe.carbonInput);
                        ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTicks);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.minimumLight);
                    }
                };

        @Override
        public MapCodec<AlgaeGrowthRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AlgaeGrowthRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
