package com.rngtech.compat.jei;

import com.rngtech.content.blockentity.WoodenComposterBlockEntity;
import com.rngtech.content.menu.WoodenComposterMenu;
import com.rngtech.content.registry.ModMenus;

import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.Optional;

/** Moves one compost input, and on the Rich Biomass page a catalyst into its own slot, which is not next to the inputs. */
final class WoodenComposterRecipeTransferInfo implements IRecipeTransferInfo<WoodenComposterMenu, WoodenComposterJeiRecipe> {
    private static final int INPUT_SLOT = 0;
    private static final int PLAYER_INVENTORY_COUNT = 36;

    @Override
    public Class<? extends WoodenComposterMenu> getContainerClass() {
        return WoodenComposterMenu.class;
    }

    @Override
    public Optional<MenuType<WoodenComposterMenu>> getMenuType() {
        return Optional.of(ModMenus.WOODEN_COMPOSTER.get());
    }

    @Override
    public RecipeType<WoodenComposterJeiRecipe> getRecipeType() {
        return JeiRecipeTypes.WOODEN_COMPOSTER;
    }

    @Override
    public boolean canHandle(WoodenComposterMenu container, WoodenComposterJeiRecipe recipe) {
        return true;
    }

    @Override
    public IRecipeTransferError getHandlingError(WoodenComposterMenu container, WoodenComposterJeiRecipe recipe) {
        return null;
    }

    @Override
    public List<Slot> getRecipeSlots(WoodenComposterMenu container, WoodenComposterJeiRecipe recipe) {
        Slot input = container.getSlot(INPUT_SLOT);
        return recipe.catalysts().isEmpty()
                ? List.of(input)
                : List.of(input, container.getSlot(WoodenComposterBlockEntity.SLOT_CATALYST));
    }

    @Override
    public List<Slot> getInventorySlots(WoodenComposterMenu container, WoodenComposterJeiRecipe recipe) {
        int start = WoodenComposterBlockEntity.SLOT_COUNT;
        return container.slots.subList(start, start + PLAYER_INVENTORY_COUNT);
    }
}
