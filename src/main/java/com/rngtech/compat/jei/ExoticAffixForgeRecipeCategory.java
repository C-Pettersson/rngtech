package com.rngtech.compat.jei;

import com.rngtech.RNGTech;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.recipe.ExoticAffixForgeRecipe;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.refinement.RefinementTargets;
import com.rngtech.rpg.unique.UniqueItems;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

public final class ExoticAffixForgeRecipeCategory implements IRecipeCategory<RecipeHolder<ExoticAffixForgeRecipe>> {
    private static final int HEIGHT = 96;
    private static final int TARGET_X = 4;
    private static final int CATALYST_X = 40;
    private static final int ARROW_X = 90;
    private static final int OUTPUT_X = 144;
    private static final int SLOT_Y = 18;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    ExoticAffixForgeRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.EXOTIC_AFFIX_FORGE.get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<ExoticAffixForgeRecipe>> getRecipeType() {
        return JeiRecipeTypes.EXOTIC_AFFIX_FORGE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.exotic_affix_forge");
    }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public int getWidth() {
        return JeiCategoryUi.WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ExoticAffixForgeRecipe> recipeHolder, IFocusGroup focuses) {
        ExoticAffixForgeRecipe recipe = recipeHolder.value();
        IRecipeSlotBuilder targetSlot = JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, TARGET_X, SLOT_Y)
                .addItemStacks(targets(recipe))
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("rngtech.jei.exotic_affix_forge.target"));
                    tooltip.add(Component.translatable("rngtech.jei.exotic_affix_forge.target.tooltip"));
                });
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, CATALYST_X, SLOT_Y)
                .addIngredients(recipe.catalyst())
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("rngtech.jei.exotic_affix_forge.catalyst", recipe.catalystCount()));
                    tooltip.add(Component.translatable("rngtech.jei.exotic_affix_forge.catalyst.scaling", recipe.maxCatalystCount()));
                });
        IRecipeSlotBuilder outputSlot = JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .addItemStacks(targets(recipe))
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("rngtech.jei.exotic_affix_forge.output"));
                    tooltip.add(Component.translatable("rngtech.jei.exotic_affix_forge.output.tooltip"));
                });
        builder.createFocusLink(targetSlot, outputSlot);
    }

    @Override
    public void draw(
            RecipeHolder<ExoticAffixForgeRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        ExoticAffixForgeRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y + 1);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.refinement.target.short", TARGET_X + 8, 7);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.refinement.catalyst.short", CATALYST_X + 8, 7);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.output.tiny", OUTPUT_X + 8, 7);
        JeiCategoryUi.drawLine(guiGraphics, font, Component.translatable(recipe.action().translationKey()), 42, JeiCategoryUi.TEXT_COLOR);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.exotic_affix_forge.base_cost", recipe.energy(), recipe.processingTicks()),
                54,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.exotic_affix_forge.base_catalysts", recipe.catalystCount(), recipe.maxCatalystCount()),
                66,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable(
                        "rngtech.jei.exotic_affix_forge.scaling",
                        JeiCategoryUi.compactDouble(recipe.totalUseEnergyMultiplier()),
                        JeiCategoryUi.compactDouble(recipe.actionUseEnergyMultiplier())
                ),
                78,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
    }

    private static List<ItemStack> targets(ExoticAffixForgeRecipe recipe) {
        return recipe.target().map(ExoticAffixForgeRecipeCategory::matchingTargets).orElseGet(ExoticAffixForgeRecipeCategory::refinementTargets);
    }

    private static List<ItemStack> matchingTargets(Ingredient ingredient) {
        List<ItemStack> targets = refinementTargets().stream()
                .filter(ingredient::test)
                .toList();
        return targets.isEmpty() ? refinementTargets() : targets;
    }

    private static List<ItemStack> refinementTargets() {
        List<ItemStack> targets = BuiltInRegistries.ITEM.stream()
                .filter(ExoticAffixForgeRecipeCategory::isRngTechItem)
                .map(ItemStack::new)
                .filter(RefinementTargets::canRefine)
                .filter(ExoticAffixForgeRecipeCategory::isNotUniqueBatteryCell)
                .toList();
        return targets.isEmpty() ? List.of(new ItemStack(ModItems.crusherChassis(CrusherChassisMaterial.WOODEN).get())) : targets;
    }

    private static boolean isRngTechItem(Item item) {
        return RNGTech.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace());
    }

    private static boolean isNotUniqueBatteryCell(ItemStack stack) {
        return !UniqueItems.isUnique(stack);
    }
}
