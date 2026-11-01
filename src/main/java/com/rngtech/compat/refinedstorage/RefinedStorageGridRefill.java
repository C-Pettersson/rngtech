package com.rngtech.compat.refinedstorage;

import com.rngtech.content.calibration.CalibrationState;
import com.rngtech.content.registry.ModDataComponents;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.grid.CraftingGrid;
import com.refinedmods.refinedstorage.common.support.RecipeMatrixContainer;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Refills a crafting grid slot with another calibration of the same item. Refined Storage only refills a slot with
 * an identical stack, so a grid stops once one exact Stability runs out, although other stored calibrations still
 * satisfy the recipe.
 */
public final class RefinedStorageGridRefill {
    /** Puts the closest valid calibration from the network into {@code slot}; false leaves the slot to empty. */
    public static boolean refillCalibrated(CraftingGrid grid, Player player, int slot, ItemStack used) {
        CalibrationState usedState = used.get(ModDataComponents.CALIBRATION_STATE.get());
        if (usedState == null || !grid.isGridActive()) {
            return false;
        }
        Level level = player.level();
        RecipeMatrixContainer matrix = grid.getCraftingMatrix();
        Optional<RecipeHolder<CraftingRecipe>> recipe =
                level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, matrix.asCraftInput(), level);
        if (recipe.isEmpty()) {
            return false;
        }
        Storage storage = grid.getItemStorage();
        ItemResource usedResource = ItemResource.ofItemStack(used);
        for (ItemResource candidate : candidates(storage, usedResource, usedState)) {
            ItemStack replacement = candidate.toItemStack(1);
            if (recipe.get().value().matches(withSlot(matrix, slot, replacement), level)
                    && storage.extract(candidate, 1, Action.EXECUTE, new PlayerActor(player)) == 1) {
                matrix.setItem(slot, replacement);
                return true;
            }
        }
        return false;
    }

    private static List<ItemResource> candidates(Storage storage, ItemResource used, CalibrationState usedState) {
        List<ItemResource> candidates = new ArrayList<>();
        for (ResourceAmount stored : storage.getAll()) {
            if (stored.resource() instanceof ItemResource item
                    && item.item() == used.item()
                    && !item.equals(used)
                    && item.toItemStack().has(ModDataComponents.CALIBRATION_STATE.get())) {
                candidates.add(item);
            }
        }
        candidates.sort(Comparator.<ItemResource>comparingInt(item -> Math.abs(stability(item) - usedState.stability()))
                .thenComparingInt(RefinedStorageGridRefill::stability));
        return candidates;
    }

    private static int stability(ItemResource item) {
        return item.toItemStack().get(ModDataComponents.CALIBRATION_STATE.get()).stability();
    }

    private static CraftingInput withSlot(RecipeMatrixContainer matrix, int slot, ItemStack stack) {
        List<ItemStack> items = new ArrayList<>(matrix.getItems());
        items.set(slot, stack);
        return CraftingInput.of(matrix.getWidth(), matrix.getHeight(), items);
    }

    private RefinedStorageGridRefill() {
    }
}
