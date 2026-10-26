package com.rngtech.compat.refinedstorage;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.grid.AbstractCraftingGridContainerMenu;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class RefinedStorageGridStacks {
    /** Every item an open crafting grid can fill its matrix from: the network, the matrix, and the inventory. */
    public static List<ItemStack> available(AbstractContainerMenu menu) {
        if (!(menu instanceof AbstractCraftingGridContainerMenu grid)) {
            return List.of();
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (ResourceKey resource : grid.getAvailableListForRecipeTransfer().getAll()) {
            if (resource instanceof ItemResource item) {
                stacks.add(item.toItemStack());
            }
        }
        return stacks;
    }

    private RefinedStorageGridStacks() {
    }
}
