package com.rngtech.compat.jei;

import com.rngtech.content.gear.GearSlotSpec;
import com.rngtech.content.registry.ModItems;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class MachineGearRecipeCategory implements IRecipeCategory<MachineGearJeiRecipe> {
    private static final int HEIGHT = 112;
    private static final int MACHINE_X = 4;
    private static final int SLOT_START_X = 36;
    private static final int SLOT_START_Y = 20;
    private static final int SLOT_SPACING_X = 28;
    private static final int DETAIL_Y = 48;
    private static final int DETAIL_LINE_HEIGHT = 10;

    private final IDrawable background;
    private final IDrawable icon;

    MachineGearRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.WRENCH.get()));
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<MachineGearJeiRecipe> getRecipeType() {
        return JeiRecipeTypes.MACHINE_GEAR;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.machine_gear");
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
    public void setRecipe(IRecipeLayoutBuilder builder, MachineGearJeiRecipe recipe, IFocusGroup focuses) {
        JeiCategoryUi.slot(builder, RecipeIngredientRole.CATALYST, MACHINE_X, SLOT_START_Y)
                .addItemStacks(recipe.spec().catalystCopies())
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("rngtech.jei.machine_gear.machine"));
                    tooltip.add(recipe.spec().title());
                });

        List<GearSlotSpec> slots = recipe.spec().slots();
        for (int index = 0; index < slots.size(); index++) {
            GearSlotSpec slot = slots.get(index);
            var builderSlot = JeiCategoryUi.slot(
                    builder,
                    RecipeIngredientRole.INPUT,
                    SLOT_START_X + index * SLOT_SPACING_X,
                    SLOT_START_Y
            ).addRichTooltipCallback((view, tooltip) -> addGearSlotTooltip(tooltip, slot));
            List<ItemStack> validStacks = slot.validStackCopies();
            if (!validStacks.isEmpty()) {
                builderSlot.addItemStacks(validStacks);
            }
        }
    }

    @Override
    public void draw(
            MachineGearJeiRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        Font font = Minecraft.getInstance().font;
        JeiCategoryUi.drawClipped(guiGraphics, font, recipe.spec().title(), 4, 6, JeiCategoryUi.WIDTH - 8, JeiCategoryUi.TEXT_COLOR);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.jei.machine_gear.machine", MACHINE_X + 8, SLOT_START_Y - 11);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.tab.gear", SLOT_START_X + 50, SLOT_START_Y - 11);

        List<GearSlotSpec> slots = recipe.spec().slots();
        for (int index = 0; index < slots.size(); index++) {
            GearSlotSpec slot = slots.get(index);
            int y = DETAIL_Y + index * DETAIL_LINE_HEIGHT;
            if (y > HEIGHT - DETAIL_LINE_HEIGHT) {
                break;
            }
            Component line = Component.translatable(
                    slot.required() ? "rngtech.jei.machine_gear.required_slot" : "rngtech.jei.machine_gear.optional_slot",
                    slot.label()
            );
            JeiCategoryUi.drawClipped(guiGraphics, font, line, 4, y, JeiCategoryUi.WIDTH - 8, JeiCategoryUi.MUTED_TEXT_COLOR);
        }
    }

    private static void addGearSlotTooltip(ITooltipBuilder tooltip, GearSlotSpec slot) {
        for (Component line : slot.tooltipLines(false)) {
            tooltip.add(line);
        }
        tooltip.add(Component.translatable("rngtech.jei.machine_gear.right_click_component"));
    }
}
