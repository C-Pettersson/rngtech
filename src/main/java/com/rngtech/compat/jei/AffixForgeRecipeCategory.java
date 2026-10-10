package com.rngtech.compat.jei;

import com.rngtech.RNGTech;
import com.rngtech.content.machine.CrusherChassisMaterial;
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

import java.util.List;

public final class AffixForgeRecipeCategory implements IRecipeCategory<AffixForgeJeiRecipe> {
    private static final int WIDTH = 166;
    private static final int HEIGHT = 96;
    private static final int TARGET_X = 4;
    private static final int CATALYST_X = 34;
    private static final int MODIFIER_X = 64;
    private static final int UPGRADE_X = 86;
    private static final int SLOT_Y = 18;
    private static final int ARROW_X = 108;
    private static final int OUTPUT_X = 144;
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int MUTED_TEXT_COLOR = 0xFF686868;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    AffixForgeRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.AFFIX_FORGE.get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<AffixForgeJeiRecipe> getRecipeType() {
        return JeiRecipeTypes.AFFIX_FORGE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.affix_forge");
    }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public int getWidth() {
        return WIDTH;
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
    public void setRecipe(IRecipeLayoutBuilder builder, AffixForgeJeiRecipe recipe, IFocusGroup focuses) {
        List<ItemStack> targets = refinementTargets();
        IRecipeSlotBuilder targetSlot = slot(builder, RecipeIngredientRole.INPUT, TARGET_X, SLOT_Y)
                .addItemStacks(targets)
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("rngtech.jei.affix_forge.target"));
                    tooltip.add(Component.translatable("rngtech.jei.affix_forge.target.tooltip"));
                });
        slot(builder, RecipeIngredientRole.INPUT, CATALYST_X, SLOT_Y)
                .addItemStack(recipe.catalyst())
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("rngtech.jei.affix_forge.catalyst"));
                    tooltip.add(Component.translatable(recipe.operation().tooltipKey()));
                });
        if (!recipe.optionalModifiers().isEmpty()) {
            slot(builder, RecipeIngredientRole.CATALYST, MODIFIER_X, SLOT_Y)
                    .addItemStacks(recipe.optionalModifiers())
                    .addRichTooltipCallback((view, tooltip) -> {
                        tooltip.add(Component.translatable("rngtech.jei.affix_forge.focus"));
                        tooltip.add(Component.translatable("rngtech.jei.affix_forge.focus.tooltip"));
                    });
        }
        if (!recipe.requiredUpgrade().isEmpty()) {
            slot(builder, RecipeIngredientRole.CATALYST, UPGRADE_X, SLOT_Y)
                    .addItemStack(recipe.requiredUpgrade())
                    .addRichTooltipCallback((view, tooltip) -> {
                        tooltip.add(Component.translatable("rngtech.jei.affix_forge.upgrade"));
                        tooltip.add(Component.translatable(recipe.requirementKey()));
                    });
        }
        IRecipeSlotBuilder outputSlot = slot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .addItemStacks(targets)
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("rngtech.jei.affix_forge.output"));
                    tooltip.add(Component.translatable("rngtech.jei.affix_forge.output.tooltip"));
                });
        builder.createFocusLink(targetSlot, outputSlot);
    }

    @Override
    public void draw(
            AffixForgeJeiRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y + 1);
        drawLabel(guiGraphics, font, "rngtech.refinement.target.short", TARGET_X + 8, 7);
        drawLabel(guiGraphics, font, "rngtech.refinement.catalyst.short", CATALYST_X + 8, 7);
        if (!recipe.optionalModifiers().isEmpty()) {
            drawLabel(guiGraphics, font, "rngtech.refinement.modifier.short", MODIFIER_X + 8, 7);
        }
        if (!recipe.requiredUpgrade().isEmpty()) {
            drawLabel(guiGraphics, font, "rngtech.jei.affix_forge.upgrade.short", UPGRADE_X + 8, 7);
        }
        drawLabel(guiGraphics, font, "rngtech.processing.output.tiny", OUTPUT_X + 8, 7);

        drawClipped(guiGraphics, font, recipe.catalyst().getHoverName(), 4, 42, WIDTH - 8, TEXT_COLOR);
        drawClipped(guiGraphics, font, Component.translatable(recipe.actionKey()), 4, 52, WIDTH - 8, MUTED_TEXT_COLOR);
        drawClipped(guiGraphics, font, Component.translatable(recipe.selectionKey()), 4, 62, WIDTH - 8, MUTED_TEXT_COLOR);
        drawClipped(guiGraphics, font, Component.translatable(recipe.requirementKey()), 4, 72, WIDTH - 8, MUTED_TEXT_COLOR);
        drawClipped(guiGraphics, font, Component.translatable(recipe.costKey()), 4, 82, WIDTH - 8, MUTED_TEXT_COLOR);
    }

    private static List<ItemStack> refinementTargets() {
        List<ItemStack> targets = BuiltInRegistries.ITEM.stream()
                .filter(AffixForgeRecipeCategory::isRngTechItem)
                .map(ItemStack::new)
                .filter(RefinementTargets::canRefine)
                .filter(AffixForgeRecipeCategory::isNotUniqueBatteryCell)
                .toList();
        return targets.isEmpty() ? List.of(new ItemStack(ModItems.crusherChassis(CrusherChassisMaterial.WOODEN).get())) : targets;
    }

    private static boolean isRngTechItem(Item item) {
        return RNGTech.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace());
    }

    private static boolean isNotUniqueBatteryCell(ItemStack stack) {
        return !UniqueItems.isUnique(stack);
    }

    private static IRecipeSlotBuilder slot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y) {
        return builder.addSlot(role, x, y).setStandardSlotBackground();
    }

    private static void drawLabel(GuiGraphics guiGraphics, Font font, String translationKey, int centerX, int y) {
        Component label = Component.translatable(translationKey);
        guiGraphics.drawString(font, label, centerX - font.width(label) / 2, y, MUTED_TEXT_COLOR, false);
    }

    private static void drawClipped(
            GuiGraphics guiGraphics,
            Font font,
            Component component,
            int x,
            int y,
            int width,
            int color
    ) {
        guiGraphics.drawString(font, Component.literal(font.plainSubstrByWidth(component.getString(), width)), x, y, color, false);
    }
}
