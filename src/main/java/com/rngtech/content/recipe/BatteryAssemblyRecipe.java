package com.rngtech.content.recipe;

import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModRecipes;

import com.mojang.datafixers.util.Either;
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
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.List;
import java.util.Optional;

public record BatteryAssemblyRecipe(
        String group,
        List<CountedIngredient> ingredients,
        Optional<SizedFluidIngredient> fluidInput,
        ItemStack result,
        int processingTicks,
        int energy,
        boolean rollResultTraits
)
        implements Recipe<BatteryAssemblyRecipeInput> {
    public BatteryAssemblyRecipe {
        ingredients = List.copyOf(ingredients);
        if (ingredients.isEmpty() || ingredients.size() > 4) {
            throw new IllegalArgumentException("Component Assembly recipes must define 1-4 item ingredients");
        }
        fluidInput = fluidInput.filter(fluid -> fluid.amount() > 0);
    }

    public ItemStack outputStack() {
        return result.copy();
    }

    public int fluidAmount() {
        return fluidInput.map(SizedFluidIngredient::amount).orElse(0);
    }

    @Override
    public boolean matches(BatteryAssemblyRecipeInput input, Level level) {
        if (input.size() < ingredients.size()) {
            return false;
        }
        for (int slot = 0; slot < ingredients.size(); slot++) {
            CountedIngredient ingredient = ingredients.get(slot);
            ItemStack stack = input.getItem(slot);
            if (!ingredient.test(stack) || stack.getCount() < ingredient.count()) {
                return false;
            }
        }
        for (int slot = ingredients.size(); slot < input.size(); slot++) {
            if (!input.getItem(slot).isEmpty()) {
                return false;
            }
        }
        return fluidInput.map(fluid -> fluid.test(input.fluid())).orElse(true);
    }

    public void consumeFluid(IFluidHandler handler) {
        fluidInput.ifPresent(fluid -> handler.drain(fluid.amount(), IFluidHandler.FluidAction.EXECUTE));
    }

    @Override
    public ItemStack assemble(BatteryAssemblyRecipeInput input, HolderLookup.Provider registries) {
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
        NonNullList<Ingredient> list = NonNullList.create();
        ingredients.forEach(ingredient -> list.add(ingredient.ingredient()));
        return list;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.BATTERY_ASSEMBLER.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.BATTERY_ASSEMBLY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.BATTERY_ASSEMBLY_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<BatteryAssemblyRecipe> {
        private static final Codec<CountedIngredient> LEGACY_OR_COUNTED_INGREDIENT_CODEC =
                Codec.either(CountedIngredient.CODEC, Ingredient.CODEC_NONEMPTY)
                        .xmap(
                                either -> either.map(
                                        counted -> counted,
                                        ingredient -> new CountedIngredient(ingredient, 1)
                                ),
                                Either::left
                        );
        private static final MapCodec<BatteryAssemblyRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(BatteryAssemblyRecipe::group),
                        LEGACY_OR_COUNTED_INGREDIENT_CODEC.listOf()
                                .fieldOf("ingredients")
                                .forGetter(BatteryAssemblyRecipe::ingredients),
                        SizedFluidIngredient.NESTED_CODEC.optionalFieldOf("fluid_input")
                                .forGetter(BatteryAssemblyRecipe::fluidInput),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(BatteryAssemblyRecipe::result),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("processing_ticks")
                                .orElse(120)
                                .forGetter(BatteryAssemblyRecipe::processingTicks),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("energy")
                                .orElse(1200)
                                .forGetter(BatteryAssemblyRecipe::energy),
                        Codec.BOOL.fieldOf("roll_result_traits").orElse(false).forGetter(BatteryAssemblyRecipe::rollResultTraits)
                )
                .apply(instance, BatteryAssemblyRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, BatteryAssemblyRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public BatteryAssemblyRecipe decode(RegistryFriendlyByteBuf buffer) {
                        String group = ByteBufCodecs.STRING_UTF8.decode(buffer);
                        List<CountedIngredient> ingredients =
                                CountedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer);
                        Optional<SizedFluidIngredient> fluidInput = ByteBufCodecs.BOOL.decode(buffer)
                                ? Optional.of(SizedFluidIngredient.STREAM_CODEC.decode(buffer))
                                : Optional.empty();
                        ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
                        int processingTicks = ByteBufCodecs.VAR_INT.decode(buffer);
                        int energy = ByteBufCodecs.VAR_INT.decode(buffer);
                        boolean rollResultTraits = ByteBufCodecs.BOOL.decode(buffer);
                        return new BatteryAssemblyRecipe(
                                group,
                                ingredients,
                                fluidInput,
                                result,
                                processingTicks,
                                energy,
                                rollResultTraits
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, BatteryAssemblyRecipe recipe) {
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group);
                        CountedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, recipe.ingredients);
                        ByteBufCodecs.BOOL.encode(buffer, recipe.fluidInput.isPresent());
                        recipe.fluidInput.ifPresent(fluid -> SizedFluidIngredient.STREAM_CODEC.encode(buffer, fluid));
                        ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTicks);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.energy);
                        ByteBufCodecs.BOOL.encode(buffer, recipe.rollResultTraits);
                    }
                };

        @Override
        public MapCodec<BatteryAssemblyRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BatteryAssemblyRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
