package com.rngtech.compat.jei;

import com.rngtech.content.recipe.CalibratedIngredient;
import com.rngtech.content.recipe.CalibratedShapedRecipe;
import com.rngtech.content.recipe.CalibrationRequirement;

import com.mojang.datafixers.util.Pair;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CalibratedCraftingCategoryExtension implements ICraftingCategoryExtension<CalibratedShapedRecipe> {
    @Override
    public int getWidth(RecipeHolder<CalibratedShapedRecipe> recipeHolder) {
        return recipeHolder.value().pattern().getFirst().length();
    }

    @Override
    public int getHeight(RecipeHolder<CalibratedShapedRecipe> recipeHolder) {
        return recipeHolder.value().pattern().size();
    }

    @Override
    public void setRecipe(
            RecipeHolder<CalibratedShapedRecipe> recipeHolder,
            IRecipeLayoutBuilder builder,
            ICraftingGridHelper craftingGridHelper,
            IFocusGroup focuses
    ) {
        CalibratedShapedRecipe recipe = recipeHolder.value();
        List<DisplaySlot> displaySlots = displaySlots(recipe);
        List<Pair<String, Ingredient>> ingredients = displaySlots.stream()
                .map(slot -> Pair.of(slot.name(), slot.ingredient()))
                .toList();
        List<IRecipeSlotBuilder> slotBuilders = craftingGridHelper.createAndSetNamedIngredients(
                builder,
                ingredients,
                getWidth(recipeHolder),
                getHeight(recipeHolder)
        );
        for (int index = 0; index < Math.min(slotBuilders.size(), displaySlots.size()); index++) {
            IRecipeSlotBuilder slotBuilder = slotBuilders.get(index);
            Optional<CalibrationRequirement> requirement = displaySlots.get(index).requirement();
            requirement.ifPresent(value ->
                    slotBuilder.addRichTooltipCallback((view, tooltip) -> addRequirementTooltip(tooltip, value)));
        }
        craftingGridHelper.createAndSetOutputs(builder, List.of(recipe.getResultItem(null)));
    }

    private static List<DisplaySlot> displaySlots(CalibratedShapedRecipe recipe) {
        List<DisplaySlot> slots = new ArrayList<>();
        int slotIndex = 0;
        for (String row : recipe.pattern()) {
            for (int column = 0; column < row.length(); column++) {
                String symbol = Character.toString(row.charAt(column));
                CalibratedIngredient ingredient = symbol.equals(" ") ? null : recipe.key().get(symbol);
                slots.add(new DisplaySlot(
                        "rngtech.calibrated_crafting." + slotIndex,
                        ingredient == null ? Ingredient.EMPTY : ingredient.ingredient(),
                        ingredient == null ? Optional.empty() : ingredient.calibration()
                ));
                slotIndex++;
            }
        }
        return slots;
    }

    private static void addRequirementTooltip(ITooltipBuilder tooltip, CalibrationRequirement requirement) {
        tooltip.add(Component.translatable("rngtech.jei.calibrated_crafting.requirement"));
        tooltip.add(Component.translatable(
                "rngtech.jei.calibration.family",
                Component.translatable(requirement.family().translationKey())
        ));
        if (requirement.minStage() > 0) {
            tooltip.add(Component.translatable("rngtech.jei.calibrated_crafting.min_stage", requirement.minStage()));
        }
        if (requirement.minStability() > 0) {
            tooltip.add(Component.translatable(
                    "rngtech.jei.calibrated_crafting.min_stability",
                    requirement.minStability()
            ));
        }
        if (requirement.minRefinementPotential() > 0) {
            tooltip.add(Component.translatable(
                    "rngtech.jei.calibrated_crafting.min_rp",
                    requirement.minRefinementPotential()
            ));
        }
    }

    private record DisplaySlot(
            String name,
            Ingredient ingredient,
            Optional<CalibrationRequirement> requirement
    ) {
    }
}
