package com.rngtech.content.blockentity;

import com.rngtech.content.recipe.BatteryAssemblyRecipe;
import com.rngtech.content.recipe.BatteryAssemblyRecipeInput;
import com.rngtech.content.registry.ModRecipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Arrays;
import java.util.Optional;

final class BatteryAssemblyRecipes {
    static Optional<BatteryAssemblyRecipe> find(Level level, ItemStack[] items, FluidStack fluid) {
        if (level == null || Arrays.stream(items).allMatch(ItemStack::isEmpty)) {
            return Optional.empty();
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.BATTERY_ASSEMBLY_TYPE.get(), new BatteryAssemblyRecipeInput(Arrays.asList(items), fluid), level)
                .map(holder -> holder.value());
    }

    private BatteryAssemblyRecipes() {
    }
}
