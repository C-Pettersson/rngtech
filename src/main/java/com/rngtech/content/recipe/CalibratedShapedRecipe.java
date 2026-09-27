package com.rngtech.content.recipe;

import com.rngtech.content.item.CraftedTraitOutputs;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record CalibratedShapedRecipe(
        String group,
        CraftingBookCategory category,
        List<String> pattern,
        Map<String, CalibratedIngredient> key,
        ItemStack result
) implements CraftingRecipe {
    public CalibratedShapedRecipe {
        pattern = List.copyOf(pattern);
        key = Map.copyOf(key);
        result = result.copy();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int width = pattern.getFirst().length();
        int height = pattern.size();
        for (int xOffset = 0; xOffset <= input.width() - width; xOffset++) {
            for (int yOffset = 0; yOffset <= input.height() - height; yOffset++) {
                if (matchesAt(input, xOffset, yOffset, false) || matchesAt(input, xOffset, yOffset, true)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return CraftedTraitOutputs.seedIfMissing(result.copy(), RandomSource.create());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= pattern.getFirst().length() && height >= pattern.size();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return CraftedTraitOutputs.seedIfMissing(result.copy(), RandomSource.create());
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (String row : pattern) {
            for (int index = 0; index < row.length(); index++) {
                char symbol = row.charAt(index);
                if (symbol != ' ') {
                    CalibratedIngredient ingredient = key.get(Character.toString(symbol));
                    if (ingredient != null) {
                        ingredients.add(ingredient.ingredient());
                    }
                }
            }
        }
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.CALIBRATED_COMPONENTS.get(com.rngtech.content.calibration.CalibrationFamily.STRUCTURAL).get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CALIBRATED_SHAPED_SERIALIZER.get();
    }

    @Override
    public boolean isIncomplete() {
        return pattern.isEmpty()
                || key.isEmpty()
                || getIngredients().isEmpty()
                || getIngredients().stream().anyMatch(Ingredient::hasNoItems);
    }

    private boolean matchesAt(CraftingInput input, int xOffset, int yOffset, boolean mirrored) {
        int width = pattern.getFirst().length();
        int height = pattern.size();
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                int patternX = x - xOffset;
                int patternY = y - yOffset;
                CalibratedIngredient ingredient = null;
                if (patternX >= 0 && patternY >= 0 && patternX < width && patternY < height) {
                    int symbolX = mirrored ? width - patternX - 1 : patternX;
                    char symbol = pattern.get(patternY).charAt(symbolX);
                    if (symbol != ' ') {
                        ingredient = key.get(Character.toString(symbol));
                    }
                }
                ItemStack stack = input.getItem(x + y * input.width());
                if (ingredient == null) {
                    if (!stack.isEmpty()) {
                        return false;
                    }
                } else if (!ingredient.test(stack)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static class Serializer implements RecipeSerializer<CalibratedShapedRecipe> {
        private static final MapCodec<CalibratedShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(CalibratedShapedRecipe::group),
                        CraftingBookCategory.CODEC
                                .fieldOf("category")
                                .orElse(CraftingBookCategory.MISC)
                                .forGetter(CalibratedShapedRecipe::category),
                        Codec.STRING.listOf().fieldOf("pattern").forGetter(CalibratedShapedRecipe::pattern),
                        Codec.unboundedMap(Codec.STRING, CalibratedIngredient.CODEC)
                                .fieldOf("key")
                                .forGetter(CalibratedShapedRecipe::key),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CalibratedShapedRecipe::result)
                )
                .apply(instance, CalibratedShapedRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CalibratedShapedRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public CalibratedShapedRecipe decode(RegistryFriendlyByteBuf buffer) {
                        String group = ByteBufCodecs.STRING_UTF8.decode(buffer);
                        CraftingBookCategory category = CraftingBookCategory.STREAM_CODEC.decode(buffer);
                        int patternSize = ByteBufCodecs.VAR_INT.decode(buffer);
                        java.util.ArrayList<String> pattern = new java.util.ArrayList<>(patternSize);
                        for (int index = 0; index < patternSize; index++) {
                            pattern.add(ByteBufCodecs.STRING_UTF8.decode(buffer));
                        }
                        int keySize = ByteBufCodecs.VAR_INT.decode(buffer);
                        Map<String, CalibratedIngredient> key = new LinkedHashMap<>();
                        for (int index = 0; index < keySize; index++) {
                            key.put(ByteBufCodecs.STRING_UTF8.decode(buffer), CalibratedIngredient.STREAM_CODEC.decode(buffer));
                        }
                        ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
                        return new CalibratedShapedRecipe(group, category, pattern, key, result);
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, CalibratedShapedRecipe recipe) {
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group);
                        CraftingBookCategory.STREAM_CODEC.encode(buffer, recipe.category);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.pattern.size());
                        for (String row : recipe.pattern) {
                            ByteBufCodecs.STRING_UTF8.encode(buffer, row);
                        }
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.key.size());
                        for (Map.Entry<String, CalibratedIngredient> entry : recipe.key.entrySet()) {
                            ByteBufCodecs.STRING_UTF8.encode(buffer, entry.getKey());
                            CalibratedIngredient.STREAM_CODEC.encode(buffer, entry.getValue());
                        }
                        ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
                    }
                };

        @Override
        public MapCodec<CalibratedShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CalibratedShapedRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
