package com.rngtech.content.recipe;

import com.rngtech.content.machine.AlloyFurnaceChassisMaterial;
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
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public record AlloyFurnaceRecipe(
        String group,
        String mode,
        List<CountedIngredient> ingredients,
        ItemStack result,
        int processingTicks,
        int energy,
        int minimumComponentStage,
        int minimumTemperature,
        double requiredTemperatureStability,
        int targetTemperature,
        int safeMaximumTemperature,
        ItemStack failureOutput,
        String failureMaterial,
        boolean powerSensitive,
        int machineXp,
        int machineXpBand
)
        implements Recipe<AlloyFurnaceRecipeInput> {
    public AlloyFurnaceRecipe {
        ingredients = List.copyOf(ingredients);
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

    public Optional<Match> match(AlloyFurnaceRecipeInput input) {
        if (!allNonEmptyStacksAreEligible(input.stacks())) {
            return Optional.empty();
        }
        int[] remaining = Arrays.stream(input.stacks()).mapToInt(ItemStack::getCount).toArray();
        int[] consumed = new int[input.size()];
        return matchIngredient(0, input.stacks(), remaining, consumed)
                ? Optional.of(new Match(Arrays.copyOf(consumed, consumed.length)))
                : Optional.empty();
    }

    @Override
    public boolean matches(AlloyFurnaceRecipeInput input, Level level) {
        return match(input).isPresent();
    }

    @Override
    public ItemStack assemble(AlloyFurnaceRecipeInput input, HolderLookup.Provider registries) {
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
    public NonNullList<net.minecraft.world.item.crafting.Ingredient> getIngredients() {
        NonNullList<net.minecraft.world.item.crafting.Ingredient> list = NonNullList.create();
        for (CountedIngredient ingredient : ingredients) {
            list.add(ingredient.ingredient());
        }
        return list;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.alloyFurnaceChassis(AlloyFurnaceChassisMaterial.BRONZE).get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ALLOY_FURNACE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ALLOY_FURNACE_TYPE.get();
    }

    private boolean matchIngredient(int ingredientIndex, ItemStack[] stacks, int[] remaining, int[] consumed) {
        if (ingredientIndex >= ingredients.size()) {
            return true;
        }
        CountedIngredient ingredient = ingredients.get(ingredientIndex);
        return consumeIngredient(ingredient, ingredient.count(), 0, stacks, remaining, consumed, ingredientIndex);
    }

    private boolean allNonEmptyStacksAreEligible(ItemStack[] stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && ingredients.stream().noneMatch(ingredient -> ingredient.test(stack))) {
                return false;
            }
        }
        return true;
    }

    private boolean consumeIngredient(
            CountedIngredient ingredient,
            int needed,
            int slot,
            ItemStack[] stacks,
            int[] remaining,
            int[] consumed,
            int ingredientIndex
    ) {
        if (needed <= 0) {
            return matchIngredient(ingredientIndex + 1, stacks, remaining, consumed);
        }
        if (slot >= stacks.length) {
            return false;
        }

        int available = ingredient.test(stacks[slot]) ? remaining[slot] : 0;
        int maximum = Math.min(available, needed);
        for (int amount = maximum; amount >= 0; amount--) {
            remaining[slot] -= amount;
            consumed[slot] += amount;
            if (consumeIngredient(ingredient, needed - amount, slot + 1, stacks, remaining, consumed, ingredientIndex)) {
                return true;
            }
            consumed[slot] -= amount;
            remaining[slot] += amount;
        }
        return false;
    }

    private static int defaultSafeMaximumTemperature(int targetTemperature) {
        if (targetTemperature <= 0) {
            return 0;
        }
        return Math.max(targetTemperature, (int) Math.round(targetTemperature * 1.15));
    }

    public record Match(int[] consumed) {
    }

    public static class Serializer implements RecipeSerializer<AlloyFurnaceRecipe> {
        private static final MapCodec<AlloyFurnaceRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(AlloyFurnaceRecipe::group),
                        Codec.STRING.fieldOf("mode").orElse("blend").forGetter(AlloyFurnaceRecipe::mode),
                        CountedIngredient.CODEC.listOf().fieldOf("ingredients").forGetter(AlloyFurnaceRecipe::ingredients),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AlloyFurnaceRecipe::result),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("processing_ticks")
                                .orElse(180)
                                .forGetter(AlloyFurnaceRecipe::processingTicks),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("energy")
                                .orElse(1800)
                                .forGetter(AlloyFurnaceRecipe::energy),
                        Codec.intRange(0, Integer.MAX_VALUE)
                                .fieldOf("minimum_component_stage")
                                .orElse(2)
                                .forGetter(AlloyFurnaceRecipe::minimumComponentStage),
                        Codec.intRange(0, Integer.MAX_VALUE)
                                .fieldOf("minimum_temperature")
                                .orElse(700)
                                .forGetter(AlloyFurnaceRecipe::minimumTemperature),
                        Codec.doubleRange(0.0, Double.MAX_VALUE)
                                .fieldOf("required_temperature_stability")
                                .orElse(0.0)
                                .forGetter(AlloyFurnaceRecipe::requiredTemperatureStability),
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
                                .forGetter(AlloyFurnaceRecipe::failureOutput),
                        Codec.STRING.optionalFieldOf("failure_material", "").forGetter(AlloyFurnaceRecipe::failureMaterial),
                        Codec.BOOL.fieldOf("power_sensitive").orElse(false).forGetter(AlloyFurnaceRecipe::powerSensitive),
                        MachineXpFields.CODEC.forGetter(recipe -> new MachineXpFields(
                                recipe.machineXp(),
                                recipe.machineXpBand() <= 0 ? Optional.empty() : Optional.of(recipe.machineXpBand())
                        ))
                )
                .apply(instance, (
                        group,
                        mode,
                        ingredients,
                        result,
                        processingTicks,
                        energy,
                        minimumComponentStage,
                        minimumTemperature,
                        requiredTemperatureStability,
                        targetTemperature,
                        safeMaximumTemperature,
                        failureOutput,
                        failureMaterial,
                        powerSensitive,
                        machineXpFields
                ) -> {
                    int resolvedTarget = targetTemperature.orElse(minimumTemperature);
                    return new AlloyFurnaceRecipe(
                            group,
                            mode,
                            ingredients,
                            result,
                            processingTicks,
                            energy,
                            minimumComponentStage,
                            minimumTemperature,
                            requiredTemperatureStability,
                            resolvedTarget,
                            safeMaximumTemperature.orElse(defaultSafeMaximumTemperature(resolvedTarget)),
                            failureOutput,
                            failureMaterial,
                            powerSensitive,
                            machineXpFields.machineXp(),
                            machineXpFields.machineXpBand().orElse(defaultMachineXpBand(machineXpFields.machineXp(), resolvedTarget))
                    );
                }));

        private static final StreamCodec<RegistryFriendlyByteBuf, AlloyFurnaceRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public AlloyFurnaceRecipe decode(RegistryFriendlyByteBuf buffer) {
                        return new AlloyFurnaceRecipe(
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                CountedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer),
                                ItemStack.STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.DOUBLE.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                ByteBufCodecs.BOOL.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer)
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, AlloyFurnaceRecipe recipe) {
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group);
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.mode);
                        CountedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, recipe.ingredients);
                        ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTicks);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.energy);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.minimumComponentStage);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.minimumTemperature);
                        ByteBufCodecs.DOUBLE.encode(buffer, recipe.requiredTemperatureStability);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.targetTemperature);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.safeMaximumTemperature);
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.failureOutput);
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
        public MapCodec<AlloyFurnaceRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AlloyFurnaceRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
