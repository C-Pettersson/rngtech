package com.rngtech.content.recipe;

import com.rngtech.content.energy.FluidFuelForm;
import com.rngtech.content.energy.FuelTankMaterial;
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
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

/** A fluid a Fuel Burner can burn: its FE per 1,000 mB before Gear, the Heat Core tier it needs, and its form. */
public record FluidFuelRecipe(String group, FluidIngredient fuel, FluidFuelForm form, int tier, long energy)
        implements Recipe<FluidFuelRecipeInput> {
    @Override
    public boolean matches(FluidFuelRecipeInput input, Level level) {
        return fuel.test(input.fuel());
    }

    @Override
    public ItemStack assemble(FluidFuelRecipeInput input, HolderLookup.Provider registries) {
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
        return NonNullList.create();
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.fuelTank(FuelTankMaterial.STEEL).get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.FLUID_FUEL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.FLUID_FUEL_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<FluidFuelRecipe> {
        private static final MapCodec<FluidFuelRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(FluidFuelRecipe::group),
                        FluidIngredient.CODEC_NON_EMPTY.fieldOf("fuel").forGetter(FluidFuelRecipe::fuel),
                        FluidFuelForm.CODEC.fieldOf("form").forGetter(FluidFuelRecipe::form),
                        Codec.intRange(1, 4).fieldOf("tier").forGetter(FluidFuelRecipe::tier),
                        Codec.LONG.fieldOf("energy").forGetter(FluidFuelRecipe::energy)
                )
                .apply(instance, FluidFuelRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, FluidFuelRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public FluidFuelRecipe decode(RegistryFriendlyByteBuf buffer) {
                        return new FluidFuelRecipe(
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                FluidIngredient.STREAM_CODEC.decode(buffer),
                                buffer.readEnum(FluidFuelForm.class),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_LONG.decode(buffer)
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, FluidFuelRecipe recipe) {
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group);
                        FluidIngredient.STREAM_CODEC.encode(buffer, recipe.fuel);
                        buffer.writeEnum(recipe.form);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.tier);
                        ByteBufCodecs.VAR_LONG.encode(buffer, recipe.energy);
                    }
                };

        @Override
        public MapCodec<FluidFuelRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FluidFuelRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
