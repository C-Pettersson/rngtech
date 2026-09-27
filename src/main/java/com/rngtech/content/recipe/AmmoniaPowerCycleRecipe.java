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

public record AmmoniaPowerCycleRecipe(
        String group,
        Ingredient membrane,
        SizedFluidIngredient ammoniaInput,
        long energy,
        int processingTicks,
        ItemStack residue,
        int minimumMembraneStage
)
        implements Recipe<AmmoniaPowerCycleRecipeInput> {
    @Override
    public boolean matches(AmmoniaPowerCycleRecipeInput input, Level level) {
        return membrane.test(input.membrane()) && ammoniaInput.test(input.ammonia());
    }

    @Override
    public ItemStack assemble(AmmoniaPowerCycleRecipeInput input, HolderLookup.Provider registries) {
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
        ingredients.add(membrane);
        return ingredients;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.AMMONIA_FUEL_CELL.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.AMMONIA_POWER_CYCLE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.AMMONIA_POWER_CYCLE_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<AmmoniaPowerCycleRecipe> {
        private static final MapCodec<AmmoniaPowerCycleRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(AmmoniaPowerCycleRecipe::group),
                        Ingredient.CODEC_NONEMPTY.fieldOf("membrane").forGetter(AmmoniaPowerCycleRecipe::membrane),
                        SizedFluidIngredient.NESTED_CODEC.fieldOf("ammonia_input").forGetter(AmmoniaPowerCycleRecipe::ammoniaInput),
                        Codec.LONG.fieldOf("energy").forGetter(AmmoniaPowerCycleRecipe::energy),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("processing_ticks")
                                .orElse(400)
                                .forGetter(AmmoniaPowerCycleRecipe::processingTicks),
                        ItemStack.OPTIONAL_CODEC
                                .optionalFieldOf("residue", ItemStack.EMPTY)
                                .forGetter(AmmoniaPowerCycleRecipe::residue),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("minimum_membrane_stage")
                                .orElse(6)
                                .forGetter(AmmoniaPowerCycleRecipe::minimumMembraneStage)
                )
                .apply(instance, AmmoniaPowerCycleRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, AmmoniaPowerCycleRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public AmmoniaPowerCycleRecipe decode(RegistryFriendlyByteBuf buffer) {
                        return new AmmoniaPowerCycleRecipe(
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                                SizedFluidIngredient.STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.VAR_LONG.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer)
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, AmmoniaPowerCycleRecipe recipe) {
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group);
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.membrane);
                        SizedFluidIngredient.STREAM_CODEC.encode(buffer, recipe.ammoniaInput);
                        ByteBufCodecs.VAR_LONG.encode(buffer, recipe.energy);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTicks);
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.residue);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.minimumMembraneStage);
                    }
                };

        @Override
        public MapCodec<AmmoniaPowerCycleRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AmmoniaPowerCycleRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
