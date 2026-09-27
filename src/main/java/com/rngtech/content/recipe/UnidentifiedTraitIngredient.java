package com.rngtech.content.recipe;

import com.rngtech.content.item.CraftedTraitOutputs;
import com.rngtech.content.registry.ModRecipes;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class UnidentifiedTraitIngredient implements ICustomIngredient {
    public static final MapCodec<UnidentifiedTraitIngredient> CODEC = Ingredient.CODEC_NONEMPTY
            .fieldOf("base")
            .xmap(UnidentifiedTraitIngredient::new, UnidentifiedTraitIngredient::base);
    public static final StreamCodec<RegistryFriendlyByteBuf, UnidentifiedTraitIngredient> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UnidentifiedTraitIngredient decode(RegistryFriendlyByteBuf buffer) {
                    return new UnidentifiedTraitIngredient(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, UnidentifiedTraitIngredient value) {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, value.base);
                }
            };

    private final Ingredient base;

    private UnidentifiedTraitIngredient(Ingredient base) {
        this.base = base;
    }

    public static Ingredient wrapIfNeeded(Ingredient ingredient) {
        if (ingredient.isEmpty()
                || ingredient.getCustomIngredient() instanceof UnidentifiedTraitIngredient) {
            return ingredient;
        }
        return new UnidentifiedTraitIngredient(ingredient).toVanilla();
    }

    public static ShapedRecipePattern wrapPattern(ShapedRecipePattern pattern) {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(pattern.ingredients().size(), Ingredient.EMPTY);
        for (int index = 0; index < pattern.ingredients().size(); index++) {
            ingredients.set(index, wrapIfNeeded(pattern.ingredients().get(index)));
        }
        return new ShapedRecipePattern(pattern.width(), pattern.height(), ingredients, Optional.empty());
    }

    public Ingredient base() {
        return base;
    }

    @Override
    public boolean test(ItemStack stack) {
        return base.test(stack);
    }

    @Override
    public Stream<ItemStack> getItems() {
        ItemStack[] items = base.getItems();
        return Stream.concat(
                Arrays.stream(items),
                Arrays.stream(items)
                        .filter(CraftedTraitOutputs::canReceiveCraftedTraits)
                        .map(CraftedTraitOutputs::unidentifiedVariant)
        );
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return ModRecipes.UNIDENTIFIED_TRAIT_INGREDIENT_TYPE.get();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof UnidentifiedTraitIngredient ingredient && base.equals(ingredient.base);
    }

    @Override
    public int hashCode() {
        return Objects.hash(base);
    }
}
