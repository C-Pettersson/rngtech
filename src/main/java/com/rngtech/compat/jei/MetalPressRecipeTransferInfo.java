package com.rngtech.compat.jei;

import com.rngtech.content.blockentity.MetalPressBlockEntity;
import com.rngtech.content.menu.MetalPressMenu;
import com.rngtech.content.recipe.MetalPressRecipe;
import com.rngtech.content.registry.ModMenus;

import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Optional;

final class MetalPressRecipeTransferInfo implements IRecipeTransferInfo<MetalPressMenu, RecipeHolder<MetalPressRecipe>> {
    private static final int INPUT_SLOT = 0;
    private static final int PLAYER_INVENTORY_COUNT = 36;

    @Override
    public Class<? extends MetalPressMenu> getContainerClass() {
        return MetalPressMenu.class;
    }

    @Override
    public Optional<MenuType<MetalPressMenu>> getMenuType() {
        return Optional.of(ModMenus.METAL_PRESS.get());
    }

    @Override
    public RecipeType<RecipeHolder<MetalPressRecipe>> getRecipeType() {
        return JeiRecipeTypes.METAL_PRESS;
    }

    @Override
    public boolean canHandle(MetalPressMenu container, RecipeHolder<MetalPressRecipe> recipe) {
        return true;
    }

    @Override
    public IRecipeTransferError getHandlingError(MetalPressMenu container, RecipeHolder<MetalPressRecipe> recipe) {
        return null;
    }

    @Override
    public List<Slot> getRecipeSlots(MetalPressMenu container, RecipeHolder<MetalPressRecipe> recipe) {
        return List.of(container.getSlot(INPUT_SLOT), preferredMoldSlot(container, recipe.value()));
    }

    @Override
    public List<Slot> getInventorySlots(MetalPressMenu container, RecipeHolder<MetalPressRecipe> recipe) {
        int start = MetalPressMenu.playerInventoryStart();
        int end = Math.min(container.slots.size(), start + PLAYER_INVENTORY_COUNT);
        return container.slots.subList(start, end);
    }

    private static Slot preferredMoldSlot(MetalPressMenu container, MetalPressRecipe recipe) {
        Slot selectedSlot = container.getSlot(MetalPressMenu.menuSlotForGearSlot(MetalPressBlockEntity.SLOT_MOLD + container.selectedMold()));
        if (!selectedSlot.hasItem() || recipe.mold().test(selectedSlot.getItem())) {
            return selectedSlot;
        }

        int firstMoldMenuSlot = MetalPressMenu.menuSlotForGearSlot(MetalPressBlockEntity.SLOT_MOLD);
        int firstEmptyMenuSlot = -1;
        for (int index = 0; index < MetalPressBlockEntity.MOLD_SLOT_COUNT; index++) {
            int menuSlot = MetalPressMenu.menuSlotForGearSlot(MetalPressBlockEntity.SLOT_MOLD + index);
            Slot slot = container.getSlot(menuSlot);
            if (slot.hasItem() && recipe.mold().test(slot.getItem())) {
                return slot;
            }
            if (!slot.hasItem() && firstEmptyMenuSlot < 0) {
                firstEmptyMenuSlot = menuSlot;
            }
        }
        return container.getSlot(firstEmptyMenuSlot >= 0 ? firstEmptyMenuSlot : firstMoldMenuSlot);
    }
}
