package com.rngtech.compat.jei;

import com.rngtech.compat.refinedstorage.RefinedStorageGridStacks;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Adds the real stacks the open crafting grid holds to a recipe slot. Refined Storage's recipe transfer only takes a
 * slot's exact stacks, so rolled items such as Calibrated Crystals count as missing unless the slot lists them.
 */
final class CraftingTransferVariants {
    private static final String REFINED_STORAGE = "refinedstorage";

    private final List<ItemStack> available;

    private CraftingTransferVariants(List<ItemStack> available) {
        this.available = available;
    }

    static CraftingTransferVariants forOpenMenu() {
        Player player = Minecraft.getInstance().player;
        if (player == null || !ModList.get().isLoaded(REFINED_STORAGE)) {
            return new CraftingTransferVariants(List.of());
        }
        return new CraftingTransferVariants(RefinedStorageGridStacks.available(player.containerMenu));
    }

    List<ItemStack> stacks(Ingredient ingredient, Predicate<ItemStack> accepts) {
        List<ItemStack> stacks = new ArrayList<>(List.of(ingredient.getItems()));
        if (ingredient.isEmpty()) {
            return stacks;
        }
        for (ItemStack candidate : available) {
            if (accepts.test(candidate)
                    && stacks.stream().noneMatch(stack -> ItemStack.isSameItemSameComponents(stack, candidate))) {
                stacks.add(candidate);
            }
        }
        return stacks;
    }
}
