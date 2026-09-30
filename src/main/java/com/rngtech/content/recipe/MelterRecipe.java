package com.rngtech.content.recipe;

import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModRecipes;
import com.rngtech.rpg.progression.MachineProgressionState;

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

public record MelterRecipe(
        String group,
        Ingredient primaryIngredient,
        Ingredient secondaryIngredient,
        SizedFluidIngredient fluidInput,
        FluidStack fluidOutput,
        int processingTicks,
        int energy,
        int minimumTemperature,
        int requiredProcessingLevel,
        boolean allowsBonusOutput,
        int machineXp,
        int machineXpBand
)
        implements Recipe<MelterRecipeInput>, BonusOutputRecipe {
    public MelterRecipe {
        machineXp = Math.max(0, machineXp);
        machineXpBand = machineXp <= 0 ? 0 : Math.max(1, machineXpBand);
    }

    public FluidStack outputFluid() {
        return fluidOutput.copy();
    }

    @Override
    public boolean matches(MelterRecipeInput input, Level level) {
        boolean direct = primaryIngredient.test(input.primary()) && secondaryIngredient.test(input.secondary());
        boolean swapped = primaryIngredient.test(input.secondary()) && secondaryIngredient.test(input.primary());
        return (direct || swapped) && fluidInput.test(input.fluid());
    }

    @Override
    public ItemStack assemble(MelterRecipeInput input, HolderLookup.Provider registries) {
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
        ingredients.add(primaryIngredient);
        ingredients.add(secondaryIngredient);
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.MELTER.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MELTER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.MELTER_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<MelterRecipe> {
        private static final MapCodec<MelterRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(MelterRecipe::group),
                        Ingredient.CODEC_NONEMPTY.fieldOf("primary_ingredient").forGetter(MelterRecipe::primaryIngredient),
                        Ingredient.CODEC_NONEMPTY.fieldOf("secondary_ingredient").forGetter(MelterRecipe::secondaryIngredient),
                        SizedFluidIngredient.NESTED_CODEC.fieldOf("fluid_input").forGetter(MelterRecipe::fluidInput),
                        FluidStack.CODEC.fieldOf("fluid_output").forGetter(MelterRecipe::fluidOutput),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("processing_ticks")
                                .orElse(240)
                                .forGetter(MelterRecipe::processingTicks),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("energy")
                                .orElse(4800)
                                .forGetter(MelterRecipe::energy),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("minimum_temperature")
                                .orElse(1200)
                                .forGetter(MelterRecipe::minimumTemperature),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("required_processing_level")
                                .orElse(1)
                                .forGetter(MelterRecipe::requiredProcessingLevel),
                        Codec.BOOL.optionalFieldOf("bonus_output", true).forGetter(MelterRecipe::allowsBonusOutput),
                        MachineXpFields.CODEC.forGetter(recipe -> new MachineXpFields(
                                recipe.machineXp(),
                                recipe.machineXpBand() <= 0 ? Optional.empty() : Optional.of(recipe.machineXpBand())
                        ))
                )
                .apply(instance, (
                        group,
                        primaryIngredient,
                        secondaryIngredient,
                        fluidInput,
                        fluidOutput,
                        processingTicks,
                        energy,
                        minimumTemperature,
                        requiredProcessingLevel,
                        allowsBonusOutput,
                        machineXpFields
                ) -> new MelterRecipe(
                        group,
                        primaryIngredient,
                        secondaryIngredient,
                        fluidInput,
                        fluidOutput,
                        processingTicks,
                        energy,
                        minimumTemperature,
                        requiredProcessingLevel,
                        allowsBonusOutput,
                        machineXpFields.machineXp(),
                        machineXpFields.machineXpBand()
                                .orElse(defaultMachineXpBand(machineXpFields.machineXp(), requiredProcessingLevel))
                )));

        private static final StreamCodec<RegistryFriendlyByteBuf, MelterRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public MelterRecipe decode(RegistryFriendlyByteBuf buffer) {
                        return new MelterRecipe(
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                                SizedFluidIngredient.STREAM_CODEC.decode(buffer),
                                FluidStack.STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.BOOL.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer)
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, MelterRecipe recipe) {
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group);
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.primaryIngredient);
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.secondaryIngredient);
                        SizedFluidIngredient.STREAM_CODEC.encode(buffer, recipe.fluidInput);
                        FluidStack.STREAM_CODEC.encode(buffer, recipe.fluidOutput);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTicks);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.energy);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.minimumTemperature);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.requiredProcessingLevel);
                        ByteBufCodecs.BOOL.encode(buffer, recipe.allowsBonusOutput);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.machineXp);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.machineXpBand);
                    }
                };

        private static int defaultMachineXpBand(int machineXp, int requiredProcessingLevel) {
            if (machineXp <= 0) {
                return 0;
            }
            int level = Math.max(1, requiredProcessingLevel);
            return MachineProgressionState.progressionBand(1 + (level - 1) * 4);
        }

        private record MachineXpFields(int machineXp, Optional<Integer> machineXpBand) {
            private static final MapCodec<MachineXpFields> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                            Codec.intRange(0, Integer.MAX_VALUE)
                                    .fieldOf("machine_xp")
                                    .orElse(0)
                                    .forGetter(MachineXpFields::machineXp),
                            Codec.intRange(0, Integer.MAX_VALUE)
                                    .optionalFieldOf("machine_xp_band")
                                    .forGetter(MachineXpFields::machineXpBand)
                    )
                    .apply(instance, MachineXpFields::new));
        }

        @Override
        public MapCodec<MelterRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MelterRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
