package com.rngtech.compat.jei;

import com.rngtech.content.blockentity.ResonanceCalibratorBlockEntity;
import com.rngtech.content.menu.ResonanceCalibratorMenu;
import com.rngtech.content.recipe.CalibrationRecipe;
import com.rngtech.content.registry.ModMenus;

import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class ResonanceCalibrationRecipeTransferInfo implements IRecipeTransferInfo<ResonanceCalibratorMenu, RecipeHolder<CalibrationRecipe>> {
    private static final int PLAYER_INVENTORY_COUNT = 36;

    @Override
    public Class<? extends ResonanceCalibratorMenu> getContainerClass() {
        return ResonanceCalibratorMenu.class;
    }

    @Override
    public Optional<MenuType<ResonanceCalibratorMenu>> getMenuType() {
        return Optional.of(ModMenus.RESONANCE_CALIBRATOR.get());
    }

    @Override
    public RecipeType<RecipeHolder<CalibrationRecipe>> getRecipeType() {
        return JeiRecipeTypes.CALIBRATION;
    }

    @Override
    public boolean canHandle(ResonanceCalibratorMenu container, RecipeHolder<CalibrationRecipe> recipe) {
        return true;
    }

    @Override
    public IRecipeTransferError getHandlingError(ResonanceCalibratorMenu container, RecipeHolder<CalibrationRecipe> recipe) {
        return null;
    }

    @Override
    public List<Slot> getRecipeSlots(ResonanceCalibratorMenu container, RecipeHolder<CalibrationRecipe> recipe) {
        List<Slot> slots = new ArrayList<>();
        slots.add(container.getSlot(ResonanceCalibratorBlockEntity.SLOT_INPUT));
        slots.add(preferredPatternSlot(container, recipe.value()));
        slots.add(container.getSlot(ResonanceCalibratorBlockEntity.SLOT_CATALYST));
        if (recipe.value().stabilizer().isPresent()) {
            slots.add(container.getSlot(ResonanceCalibratorBlockEntity.SLOT_STABILIZER));
        }
        return slots;
    }

    @Override
    public List<Slot> getInventorySlots(ResonanceCalibratorMenu container, RecipeHolder<CalibrationRecipe> recipe) {
        int start = ResonanceCalibratorMenu.playerInventoryStart();
        int end = Math.min(container.slots.size(), start + PLAYER_INVENTORY_COUNT);
        return container.slots.subList(start, end);
    }

    private static Slot preferredPatternSlot(ResonanceCalibratorMenu container, CalibrationRecipe recipe) {
        int selectedSlot = ResonanceCalibratorMenu.menuSlotForGearSlot(
                ResonanceCalibratorBlockEntity.SLOT_PATTERN_STORAGE + container.selectedPattern()
        );
        Slot selected = container.getSlot(selectedSlot);
        if (!selected.hasItem() || recipe.pattern().test(selected.getItem())) {
            return selected;
        }

        int firstPatternMenuSlot = ResonanceCalibratorMenu.menuSlotForGearSlot(
                ResonanceCalibratorBlockEntity.SLOT_PATTERN_STORAGE
        );
        int firstEmptyMenuSlot = -1;
        for (int index = 0; index < ResonanceCalibratorBlockEntity.PATTERN_SLOT_COUNT; index++) {
            int menuSlot = ResonanceCalibratorMenu.menuSlotForGearSlot(
                    ResonanceCalibratorBlockEntity.SLOT_PATTERN_STORAGE + index
            );
            Slot slot = container.getSlot(menuSlot);
            if (slot.hasItem() && recipe.pattern().test(slot.getItem())) {
                return slot;
            }
            if (!slot.hasItem() && firstEmptyMenuSlot < 0) {
                firstEmptyMenuSlot = menuSlot;
            }
        }
        return container.getSlot(firstEmptyMenuSlot >= 0 ? firstEmptyMenuSlot : firstPatternMenuSlot);
    }
}
