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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.Optional;

public record CavitationRecipe(
        String group,
        SizedFluidIngredient fluidInput,
        Optional<Ingredient> rotor,
        Optional<Ingredient> nozzle,
        Optional<FluidStack> fluidOutput,
        int energy,
        int processingTicks,
        int minimumRotorStage,
        int pressureRating,
        int heatStrain,
        int wear
)
        implements Recipe<CavitationRecipeInput> {
    @Override
    public boolean matches(CavitationRecipeInput input, Level level) {
        return fluidInput.test(input.fluid())
                && rotor.map(ingredient -> ingredient.test(input.rotor())).orElse(true)
                && nozzle.map(ingredient -> ingredient.test(input.nozzle())).orElse(true);
    }

    public int specificity() {
        return (rotor.isPresent() ? 1 : 0) + (nozzle.isPresent() ? 1 : 0);
    }

    public FluidStack fluidOutputStack() {
        return fluidOutput.map(FluidStack::copy).orElse(FluidStack.EMPTY);
    }

    @Override
    public ItemStack assemble(CavitationRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
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
        rotor.ifPresent(ingredients::add);
        nozzle.ifPresent(ingredients::add);
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.CAVITATION_GENERATOR.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CAVITATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.CAVITATION_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<CavitationRecipe> {
        private static final MapCodec<CavitationRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(CavitationRecipe::group),
                        SizedFluidIngredient.NESTED_CODEC.fieldOf("fluid_input").forGetter(CavitationRecipe::fluidInput),
                        Ingredient.CODEC_NONEMPTY.optionalFieldOf("rotor").forGetter(CavitationRecipe::rotor),
                        Ingredient.CODEC_NONEMPTY.optionalFieldOf("nozzle").forGetter(CavitationRecipe::nozzle),
                        FluidStack.CODEC.optionalFieldOf("fluid_output").forGetter(CavitationRecipe::fluidOutput),
                        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("energy").forGetter(CavitationRecipe::energy),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("processing_ticks")
                                .orElse(240)
                                .forGetter(CavitationRecipe::processingTicks),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("minimum_rotor_stage")
                                .orElse(5)
                                .forGetter(CavitationRecipe::minimumRotorStage),
                        Codec.intRange(0, Integer.MAX_VALUE)
                                .fieldOf("pressure_rating")
                                .orElse(0)
                                .forGetter(CavitationRecipe::pressureRating),
                        Codec.intRange(0, Integer.MAX_VALUE)
                                .fieldOf("heat_strain")
                                .orElse(20)
                                .forGetter(CavitationRecipe::heatStrain),
                        Codec.intRange(0, Integer.MAX_VALUE)
                                .fieldOf("wear")
                                .orElse(25)
                                .forGetter(CavitationRecipe::wear)
                )
                .apply(instance, CavitationRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CavitationRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public CavitationRecipe decode(RegistryFriendlyByteBuf buffer) {
                        String group = ByteBufCodecs.STRING_UTF8.decode(buffer);
                        SizedFluidIngredient fluidInput = SizedFluidIngredient.STREAM_CODEC.decode(buffer);
                        Optional<Ingredient> rotor = ByteBufCodecs.BOOL.decode(buffer)
                                ? Optional.of(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer))
                                : Optional.empty();
                        Optional<Ingredient> nozzle = ByteBufCodecs.BOOL.decode(buffer)
                                ? Optional.of(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer))
                                : Optional.empty();
                        Optional<FluidStack> fluidOutput = ByteBufCodecs.BOOL.decode(buffer)
                                ? Optional.of(FluidStack.STREAM_CODEC.decode(buffer))
                                : Optional.empty();
                        return new CavitationRecipe(
                                group,
                                fluidInput,
                                rotor,
                                nozzle,
                                fluidOutput,
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer)
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, CavitationRecipe recipe) {
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group);
                        SizedFluidIngredient.STREAM_CODEC.encode(buffer, recipe.fluidInput);
                        ByteBufCodecs.BOOL.encode(buffer, recipe.rotor.isPresent());
                        recipe.rotor.ifPresent(ingredient -> Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient));
                        ByteBufCodecs.BOOL.encode(buffer, recipe.nozzle.isPresent());
                        recipe.nozzle.ifPresent(ingredient -> Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient));
                        ByteBufCodecs.BOOL.encode(buffer, recipe.fluidOutput.isPresent());
                        recipe.fluidOutput.ifPresent(output -> FluidStack.STREAM_CODEC.encode(buffer, output));
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.energy);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTicks);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.minimumRotorStage);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.pressureRating);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.heatStrain);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.wear);
                    }
                };

        @Override
        public MapCodec<CavitationRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CavitationRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
