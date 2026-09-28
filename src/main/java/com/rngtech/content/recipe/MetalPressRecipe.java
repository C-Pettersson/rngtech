package com.rngtech.content.recipe;

import com.rngtech.content.registry.ModDataComponents;
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

import java.util.Optional;

public record MetalPressRecipe(
        String group,
        Ingredient ingredient,
        int inputCount,
        Ingredient mold,
        ItemStack result,
        int processingTicks,
        int energy,
        int minimumTemperature,
        int targetTemperature,
        int safeMaximumTemperature,
        double requiredTemperatureStability,
        ItemStack failureOutput,
        String failureMaterial,
        boolean powerSensitive,
        int machineXp,
        int machineXpBand
)
        implements Recipe<MetalPressRecipeInput> {
    public MetalPressRecipe {
        targetTemperature = targetTemperature > 0 ? targetTemperature : minimumTemperature;
        safeMaximumTemperature = Math.max(targetTemperature, safeMaximumTemperature);
        machineXp = Math.max(0, machineXp);
        machineXpBand = machineXp <= 0 ? 0 : Math.max(1, machineXpBand);
    }

    public ItemStack outputStack() {
        return result.copy();
    }

    public ItemStack failureStack() {
        ItemStack stack = failureOutput.copy();
        if (stack.is(ModItems.MALFORMED_INGOT.get()) && !failureMaterial.isBlank()) {
            stack.set(ModDataComponents.MATERIAL.get(), failureMaterial);
        }
        return stack;
    }

    public boolean hasFailureOutput() {
        return !failureOutput.isEmpty();
    }

    @Override
    public boolean matches(MetalPressRecipeInput input, Level level) {
        return input.input().getCount() >= inputCount && ingredient.test(input.input()) && mold.test(input.mold());
    }

    @Override
    public ItemStack assemble(MetalPressRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
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
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(ingredient);
        ingredients.add(mold);
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.METAL_PRESS.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.METAL_PRESS_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.METAL_PRESS_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<MetalPressRecipe> {
        private static final MapCodec<MetalPressRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(MetalPressRecipe::group),
                        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(MetalPressRecipe::ingredient),
                        Codec.intRange(1, 64)
                                .fieldOf("input_count")
                                .orElse(1)
                                .forGetter(MetalPressRecipe::inputCount),
                        Ingredient.CODEC_NONEMPTY.fieldOf("mold").forGetter(MetalPressRecipe::mold),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(MetalPressRecipe::result),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("processing_ticks")
                                .orElse(200)
                                .forGetter(MetalPressRecipe::processingTicks),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("energy")
                                .orElse(2400)
                                .forGetter(MetalPressRecipe::energy),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("minimum_temperature")
                                .orElse(1100)
                                .forGetter(MetalPressRecipe::minimumTemperature),
                        Codec.intRange(0, Integer.MAX_VALUE)
                                .optionalFieldOf("target_temperature")
                                .forGetter(recipe -> recipe.targetTemperature() == recipe.minimumTemperature()
                                        ? Optional.empty()
                                        : Optional.of(recipe.targetTemperature())),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("safe_maximum_temperature")
                                .orElse(1400)
                                .forGetter(MetalPressRecipe::safeMaximumTemperature),
                        Codec.doubleRange(0.0, Double.MAX_VALUE)
                                .fieldOf("required_temperature_stability")
                                .orElse(1.0)
                                .forGetter(MetalPressRecipe::requiredTemperatureStability),
                        ItemStack.STRICT_CODEC.fieldOf("failure_output").forGetter(MetalPressRecipe::failureOutput),
                        Codec.STRING.optionalFieldOf("failure_material", "").forGetter(MetalPressRecipe::failureMaterial),
                        Codec.BOOL.fieldOf("power_sensitive").orElse(true).forGetter(MetalPressRecipe::powerSensitive),
                        MachineXpFields.CODEC.forGetter(recipe -> new MachineXpFields(
                                recipe.machineXp(),
                                recipe.machineXpBand() <= 0 ? Optional.empty() : Optional.of(recipe.machineXpBand())
                        ))
                )
                .apply(instance, (
                        group,
                        ingredient,
                        inputCount,
                        mold,
                        result,
                        processingTicks,
                        energy,
                        minimumTemperature,
                        targetTemperature,
                        safeMaximumTemperature,
                        requiredTemperatureStability,
                        failureOutput,
                        failureMaterial,
                        powerSensitive,
                        machineXpFields
                ) -> {
                    int resolvedTarget = targetTemperature.orElse(minimumTemperature);
                    return new MetalPressRecipe(
                            group,
                            ingredient,
                            inputCount,
                            mold,
                            result,
                            processingTicks,
                            energy,
                            minimumTemperature,
                            resolvedTarget,
                            safeMaximumTemperature,
                            requiredTemperatureStability,
                            failureOutput,
                            failureMaterial,
                            powerSensitive,
                            machineXpFields.machineXp(),
                            machineXpFields.machineXpBand().orElse(defaultMachineXpBand(machineXpFields.machineXp(), resolvedTarget))
                    );
                }));

        private static final StreamCodec<RegistryFriendlyByteBuf, MetalPressRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public MetalPressRecipe decode(RegistryFriendlyByteBuf buffer) {
                        return new MetalPressRecipe(
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                                ItemStack.STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.DOUBLE.decode(buffer),
                                ItemStack.STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                ByteBufCodecs.BOOL.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer)
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, MetalPressRecipe recipe) {
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group);
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.inputCount);
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.mold);
                        ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTicks);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.energy);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.minimumTemperature);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.targetTemperature);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.safeMaximumTemperature);
                        ByteBufCodecs.DOUBLE.encode(buffer, recipe.requiredTemperatureStability);
                        ItemStack.STREAM_CODEC.encode(buffer, recipe.failureOutput);
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.failureMaterial);
                        ByteBufCodecs.BOOL.encode(buffer, recipe.powerSensitive);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.machineXp);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.machineXpBand);
                    }
                };

        private static int defaultMachineXpBand(int machineXp, int targetTemperature) {
            if (machineXp <= 0) {
                return 0;
            }
            if (targetTemperature <= 800) {
                return MachineProgressionState.progressionBand(1);
            }
            if (targetTemperature <= 1000) {
                return MachineProgressionState.progressionBand(5);
            }
            if (targetTemperature <= 1200) {
                return MachineProgressionState.progressionBand(9);
            }
            if (targetTemperature <= 1400) {
                return MachineProgressionState.progressionBand(13);
            }
            if (targetTemperature <= 1600) {
                return MachineProgressionState.progressionBand(17);
            }
            if (targetTemperature <= 1800) {
                return MachineProgressionState.progressionBand(21);
            }
            return MachineProgressionState.progressionBand(25);
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
        public MapCodec<MetalPressRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MetalPressRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
