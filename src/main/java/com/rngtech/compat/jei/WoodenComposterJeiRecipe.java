package com.rngtech.compat.jei;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.WoodenComposterBlockEntity;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record WoodenComposterJeiRecipe(ResourceLocation id, List<ItemStack> inputs, ItemStack output) {
    public WoodenComposterJeiRecipe {
        inputs = inputs.stream().map(ItemStack::copy).toList();
        output = output.copy();
    }

    static List<WoodenComposterJeiRecipe> recipes() {
        List<ItemStack> compostables = BuiltInRegistries.ITEM.stream()
                .map(ItemStack::new)
                .filter(WoodenComposterBlockEntity::isCompostable)
                .toList();
        if (compostables.isEmpty()) {
            return List.of();
        }
        return List.of(new WoodenComposterJeiRecipe(
                RNGTech.id("wooden_composter/composted_biomass"),
                compostables,
                new ItemStack(ModItems.COMPOSTED_BIOMASS.get())
        ));
    }
}
