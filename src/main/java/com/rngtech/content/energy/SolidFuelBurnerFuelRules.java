package com.rngtech.content.energy;

import com.rngtech.content.registry.ModTags;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

public final class SolidFuelBurnerFuelRules {
    private static final String VANILLA_NAMESPACE = "minecraft";
    private static final TagKey<Item> VANILLA_LOGS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(VANILLA_NAMESPACE, "logs"));
    private static final int LOG_BURN_TIME_DIVISOR = 4;

    public static int burnTicks(ItemStack stack) {
        int baseBurnTime = stack.getBurnTime(RecipeType.SMELTING);
        if (baseBurnTime <= 0) {
            return 0;
        }
        if (stack.is(VANILLA_LOGS)) {
            return Math.max(1, baseBurnTime / LOG_BURN_TIME_DIVISOR);
        }
        return baseBurnTime;
    }

    public static boolean isItemFuel(ItemStack stack) {
        return stack.is(ModTags.Items.SOLID_FUEL_ITEM_FUELS) || isVanillaFallbackItemFuel(stack);
    }

    public static boolean isBlockFuel(ItemStack stack) {
        return stack.is(ModTags.Items.SOLID_FUEL_BLOCK_FUELS);
    }

    public static int tier(ItemStack stack) {
        if (stack.is(ModTags.Items.SOLID_FUEL_TIER_4)) {
            return 4;
        }
        if (stack.is(ModTags.Items.SOLID_FUEL_TIER_3)) {
            return 3;
        }
        if (stack.is(ModTags.Items.SOLID_FUEL_TIER_2)) {
            return 2;
        }
        if (stack.is(ModTags.Items.SOLID_FUEL_TIER_1)) {
            return 1;
        }
        return isVanillaFallbackItemFuel(stack) ? 1 : 0;
    }

    private static boolean isVanillaFallbackItemFuel(ItemStack stack) {
        return !stack.isEmpty()
                && !isBlockFuel(stack)
                && burnTicks(stack) > 0
                && stack.getCraftingRemainingItem().isEmpty()
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(VANILLA_NAMESPACE);
    }

    private SolidFuelBurnerFuelRules() {
    }
}
