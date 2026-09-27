package com.rngtech.compat.jei;

import com.rngtech.RNGTech;
import com.rngtech.content.energy.SolidFuelBurnerFuelRules;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record SolidFuelBurnerJeiFuel(ResourceLocation id, int tier, boolean blockFuel, List<ItemStack> fuels) {
    public SolidFuelBurnerJeiFuel {
        fuels = fuels.stream().map(ItemStack::copy).toList();
    }

    static List<SolidFuelBurnerJeiFuel> recipes() {
        return List.of(
                        recipe(1, false),
                        recipe(2, false),
                        recipe(3, false),
                        recipe(4, false),
                        recipe(1, true),
                        recipe(2, true),
                        recipe(3, true),
                        recipe(4, true)
                )
                .stream()
                .filter(recipe -> !recipe.fuels().isEmpty())
                .toList();
    }

    private static SolidFuelBurnerJeiFuel recipe(int tier, boolean blockFuel) {
        List<ItemStack> fuels = BuiltInRegistries.ITEM.stream()
                .map(ItemStack::new)
                .filter(stack -> SolidFuelBurnerFuelRules.tier(stack) == tier)
                .filter(stack -> blockFuel
                        ? SolidFuelBurnerFuelRules.isBlockFuel(stack)
                        : SolidFuelBurnerFuelRules.isItemFuel(stack))
                .filter(stack -> SolidFuelBurnerFuelRules.burnTicks(stack) > 0)
                .toList();
        String form = blockFuel ? "block" : "item";
        return new SolidFuelBurnerJeiFuel(RNGTech.id("solid_fuel_burning/tier_" + tier + "_" + form), tier, blockFuel, fuels);
    }
}
