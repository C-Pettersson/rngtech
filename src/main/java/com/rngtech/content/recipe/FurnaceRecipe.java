package com.rngtech.content.recipe;

import com.rngtech.content.item.MalformedIngotItem;
import com.rngtech.content.registry.ModDataComponents;
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

import java.util.Locale;
import java.util.Optional;

public record FurnaceRecipe(
        String group,
        Ingredient ingredient,
        String malformedMaterial,
        ItemStack result,
        int processingTicks,
        int energy,
        double experience,
        int minimumTemperature,
        double requiredTemperatureStability,
        int targetTemperature,
        int safeMaximumTemperature,
        ItemStack failureOutput,
        String failureMaterial,
        boolean powerSensitive,
        boolean allowsBonusOutput,
        int machineXp,
        int machineXpBand
)
        implements Recipe<SingleRecipeInput> {
    public FurnaceRecipe {
        malformedMaterial = malformedMaterial == null ? "" : malformedMaterial.toLowerCase(Locale.ROOT);
        targetTemperature = targetTemperature > 0 ? targetTemperature : minimumTemperature;
        safeMaximumTemperature = safeMaximumTemperature > 0
                ? safeMaximumTemperature
                : defaultSafeMaximumTemperature(targetTemperature);
        failureOutput = failureOutput == null ? ItemStack.EMPTY : failureOutput.copy();
        failureMaterial = failureMaterial == null ? "" : failureMaterial;
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
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item()) && matchesMalformedMaterial(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
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
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.FURNACE.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.FURNACE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.FURNACE_TYPE.get();
    }

    private static int defaultSafeMaximumTemperature(int targetTemperature) {
        if (targetTemperature <= 0) {
            return 0;
        }
        return Math.max(targetTemperature, (int) Math.round(targetTemperature * 1.15));
    }

    private boolean matchesMalformedMaterial(ItemStack stack) {
        return malformedMaterial.isBlank()
                || MalformedIngotItem.isMalformedIngot(stack)
                        && malformedMaterial.equals(MalformedIngotItem.material(stack));
    }

    public static class Serializer implements RecipeSerializer<FurnaceRecipe> {
        private static final int DEFAULT_ENERGY_PER_TICK = 24;

        private static final MapCodec<FurnaceRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(FurnaceRecipe::group),
                        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(FurnaceRecipe::ingredient),
                        Codec.STRING.optionalFieldOf("malformed_material", "").forGetter(FurnaceRecipe::malformedMaterial),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(FurnaceRecipe::result),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("processing_ticks")
                                .orElse(200)
                                .forGetter(FurnaceRecipe::processingTicks),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .optionalFieldOf("energy")
                                .forGetter(recipe -> Optional.of(recipe.energy())),
                        Codec.doubleRange(0.0, Double.MAX_VALUE)
                                .fieldOf("experience")
                                .orElse(0.0)
                                .forGetter(FurnaceRecipe::experience),
                        Codec.intRange(0, Integer.MAX_VALUE)
                                .fieldOf("minimum_temperature")
                                .orElse(0)
                                .forGetter(FurnaceRecipe::minimumTemperature),
                        Codec.doubleRange(0.0, Double.MAX_VALUE)
                                .fieldOf("required_temperature_stability")
                                .orElse(0.0)
                                .forGetter(FurnaceRecipe::requiredTemperatureStability),
                        Codec.intRange(0, Integer.MAX_VALUE)
                                .optionalFieldOf("target_temperature")
                                .forGetter(recipe -> recipe.targetTemperature() == recipe.minimumTemperature()
                                        ? Optional.empty()
                                        : Optional.of(recipe.targetTemperature())),
                        Codec.intRange(0, Integer.MAX_VALUE)
                                .optionalFieldOf("safe_maximum_temperature")
                                .forGetter(recipe -> recipe.safeMaximumTemperature()
                                                == defaultSafeMaximumTemperature(recipe.targetTemperature())
                                        ? Optional.empty()
                                        : Optional.of(recipe.safeMaximumTemperature())),
                        ItemStack.OPTIONAL_CODEC
                                .optionalFieldOf("failure_output", ItemStack.EMPTY)
                                .forGetter(FurnaceRecipe::failureOutput),
                        Codec.STRING.optionalFieldOf("failure_material", "").forGetter(FurnaceRecipe::failureMaterial),
                        Codec.BOOL.fieldOf("power_sensitive").orElse(false).forGetter(FurnaceRecipe::powerSensitive),
                        Codec.BOOL.fieldOf("bonus_output").orElse(true).forGetter(FurnaceRecipe::allowsBonusOutput),
                        MachineXpFields.CODEC.forGetter(recipe -> new MachineXpFields(
                                recipe.machineXp(),
                                recipe.machineXpBand() <= 0 ? Optional.empty() : Optional.of(recipe.machineXpBand())
                        ))
                )
                .apply(instance, (
                        group,
                        ingredient,
                        malformedMaterial,
                        result,
                        processingTicks,
                        energy,
                        experience,
                        minimumTemperature,
                        requiredTemperatureStability,
                        targetTemperature,
                        safeMaximumTemperature,
                        failureOutput,
                        failureMaterial,
                        powerSensitive,
                        allowsBonusOutput,
                        machineXpFields
                ) -> {
                    int resolvedTarget = targetTemperature.orElse(minimumTemperature);
                    return new FurnaceRecipe(
                        group,
                        ingredient,
                        malformedMaterial,
                        result,
                        processingTicks,
                        energy.orElse(defaultEnergy(processingTicks)),
                        experience,
                        minimumTemperature,
                        requiredTemperatureStability,
                        resolvedTarget,
                        safeMaximumTemperature.orElse(defaultSafeMaximumTemperature(resolvedTarget)),
                        failureOutput,
                        failureMaterial,
                        powerSensitive,
                        allowsBonusOutput,
                        machineXpFields.machineXp(),
                        machineXpFields.machineXpBand().orElse(defaultMachineXpBand(machineXpFields.machineXp(), resolvedTarget))
                    );
                }));

        private static final StreamCodec<RegistryFriendlyByteBuf, FurnaceRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public FurnaceRecipe decode(RegistryFriendlyByteBuf buffer) {
                        return new FurnaceRecipe(
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                ItemStack.STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.DOUBLE.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.DOUBLE.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                ByteBufCodecs.BOOL.decode(buffer),
                                ByteBufCodecs.BOOL.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer)
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, FurnaceRecipe recipe) {
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group);
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.malformedMaterial);
                        ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTicks);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.energy);
                        ByteBufCodecs.DOUBLE.encode(buffer, recipe.experience);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.minimumTemperature);
                        ByteBufCodecs.DOUBLE.encode(buffer, recipe.requiredTemperatureStability);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.targetTemperature);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.safeMaximumTemperature);
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.failureOutput);
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.failureMaterial);
                        ByteBufCodecs.BOOL.encode(buffer, recipe.powerSensitive);
                        ByteBufCodecs.BOOL.encode(buffer, recipe.allowsBonusOutput);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.machineXp);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.machineXpBand);
                    }
                };

        private static int defaultEnergy(int processingTicks) {
            long energy = Math.max(1, processingTicks) * (long) DEFAULT_ENERGY_PER_TICK;
            return (int) Math.min(Integer.MAX_VALUE, energy);
        }

        private static int defaultMachineXpBand(int machineXp, int targetTemperature) {
            if (machineXp <= 0) {
                return 0;
            }
            if (targetTemperature <= 800) {
                return 1;
            }
            if (targetTemperature <= 1000) {
                return 5;
            }
            if (targetTemperature <= 1200) {
                return 9;
            }
            if (targetTemperature <= 1400) {
                return 13;
            }
            if (targetTemperature <= 1600) {
                return 17;
            }
            if (targetTemperature <= 1800) {
                return 21;
            }
            return 25;
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
        public MapCodec<FurnaceRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FurnaceRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
