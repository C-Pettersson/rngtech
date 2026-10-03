package com.rngtech.content.recipe;

import com.rngtech.content.item.MalformedIngotItem;
import com.rngtech.content.machine.CrusherChassisMaterial;
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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.Locale;
import java.util.Optional;

public record CrusherRecipe(
        String group,
        Ingredient ingredient,
        int inputCount,
        String malformedMaterial,
        ItemStack result,
        int processingTicks,
        int energy,
        int requiredProcessingLevel,
        boolean allowsBonusOutput,
        int machineXp,
        int machineXpBand
)
        implements Recipe<SingleRecipeInput>, BonusOutputRecipe {
    public CrusherRecipe {
        malformedMaterial = malformedMaterial == null ? "" : malformedMaterial.toLowerCase(Locale.ROOT);
        machineXp = Math.max(0, machineXp);
        machineXpBand = machineXp <= 0 ? 0 : Math.max(1, machineXpBand);
    }

    public int baseOutputCount() {
        return result.getCount();
    }

    public ItemStack outputStack(int count) {
        ItemStack stack = result.copy();
        stack.setCount(count);
        return stack;
    }

    public boolean matchesInput(ItemStack stack) {
        return ingredient.test(stack) && matchesMalformedMaterial(stack);
    }

    public boolean hasRequiredInput(ItemStack stack) {
        return matchesInput(stack) && stack.getCount() >= inputCount;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return matchesInput(input.item());
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
        return new ItemStack(ModItems.crusherChassis(CrusherChassisMaterial.WOODEN).get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CRUSHER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.CRUSHER_TYPE.get();
    }

    private boolean matchesMalformedMaterial(ItemStack stack) {
        return malformedMaterial.isBlank()
                || MalformedIngotItem.isMalformedIngot(stack)
                        && malformedMaterial.equals(MalformedIngotItem.material(stack));
    }

    public static class Serializer implements RecipeSerializer<CrusherRecipe> {
        private static final int DEFAULT_ENERGY_PER_TICK = 48;

        private static final MapCodec<CrusherRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(CrusherRecipe::group),
                        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CrusherRecipe::ingredient),
                        Codec.intRange(1, 64)
                                .fieldOf("input_count")
                                .orElse(1)
                                .forGetter(CrusherRecipe::inputCount),
                        Codec.STRING.optionalFieldOf("malformed_material", "").forGetter(CrusherRecipe::malformedMaterial),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CrusherRecipe::result),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("processing_ticks")
                                .orElse(120)
                                .forGetter(CrusherRecipe::processingTicks),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .optionalFieldOf("energy")
                                .forGetter(recipe -> Optional.of(recipe.energy())),
                        Codec.intRange(0, Integer.MAX_VALUE)
                                .fieldOf("required_processing_level")
                                .orElse(1)
                                .forGetter(CrusherRecipe::requiredProcessingLevel),
                        Codec.BOOL.fieldOf("bonus_output").orElse(true).forGetter(CrusherRecipe::allowsBonusOutput),
                        Codec.intRange(0, Integer.MAX_VALUE)
                                .fieldOf("machine_xp")
                                .orElse(0)
                                .forGetter(CrusherRecipe::machineXp),
                        Codec.intRange(0, Integer.MAX_VALUE)
                                .optionalFieldOf("machine_xp_band")
                                .forGetter(recipe -> recipe.machineXpBand() <= 0
                                        ? Optional.empty()
                                        : Optional.of(recipe.machineXpBand()))
                )
                .apply(instance, (group, ingredient, inputCount, malformedMaterial, result, processingTicks, energy, requiredProcessingLevel, allowsBonusOutput, machineXp, machineXpBand) -> new CrusherRecipe(
                        group,
                        ingredient,
                        inputCount,
                        malformedMaterial,
                        result,
                        processingTicks,
                        energy.orElse(defaultEnergy(processingTicks)),
                        requiredProcessingLevel,
                        allowsBonusOutput,
                        machineXp,
                        machineXpBand.orElse(defaultMachineXpBand(machineXp, requiredProcessingLevel))
                )));

        private static final StreamCodec<RegistryFriendlyByteBuf, CrusherRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public CrusherRecipe decode(RegistryFriendlyByteBuf buffer) {
                        return new CrusherRecipe(
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                ItemStack.STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.BOOL.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer)
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, CrusherRecipe recipe) {
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group);
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.inputCount);
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.malformedMaterial);
                        ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTicks);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.energy);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.requiredProcessingLevel);
                        ByteBufCodecs.BOOL.encode(buffer, recipe.allowsBonusOutput);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.machineXp);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.machineXpBand);
                    }
                };

        private static int defaultEnergy(int processingTicks) {
            long energy = Math.max(1, processingTicks) * (long) DEFAULT_ENERGY_PER_TICK;
            return (int) Math.min(Integer.MAX_VALUE, energy);
        }

        private static int defaultMachineXpBand(int machineXp, int requiredProcessingLevel) {
            if (machineXp <= 0) {
                return 0;
            }
            int level = Math.max(1, requiredProcessingLevel);
            return MachineProgressionState.progressionBand(1 + (level - 1) * 4);
        }

        @Override
        public MapCodec<CrusherRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CrusherRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
