package com.rngtech.compat.jei;

import com.rngtech.RNGTech;
import com.rngtech.content.item.GlyphItem;
import com.rngtech.content.recipe.PhaseArrayRecipe;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.PhaseArrayRules;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** One Phase Array operation: what it needs and what it costs on a fresh Stage 8 item. */
public final class PhaseArrayRecipeCategory implements IRecipeCategory<RecipeHolder<PhaseArrayRecipe>> {
    private static final int HEIGHT = 84;
    private static final int TEXT = 0xFF404040;

    private final IDrawable background;
    private final IDrawable icon;

    public PhaseArrayRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.EXOTIC_AFFIX_FORGE.get()));
    }

    @Override
    public RecipeType<RecipeHolder<PhaseArrayRecipe>> getRecipeType() {
        return JeiRecipeTypes.PHASE_ARRAY;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.phase_array");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<PhaseArrayRecipe> holder, IFocusGroup focuses) {
        PhaseArrayRecipe recipe = holder.value();
        JeiCategoryUi.slot(builder, RecipeIngredientRole.CATALYST, 4, 4).addItemStack(new ItemStack(ModItems.EXOTIC_AFFIX_FORGE.get()));
        if (recipe.needsGlyph()) {
            JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, 28, 4)
                    .addItemStack(GlyphItem.of(ModItems.GLYPH.get(), recipe.glyphTier()));
        }
    }

    @Override
    public void draw(RecipeHolder<PhaseArrayRecipe> holder, IRecipeSlotsView slotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        PhaseArrayRecipe recipe = holder.value();
        var font = Minecraft.getInstance().font;
        JeiCategoryUi.drawLine(guiGraphics, font, Component.translatable(recipe.action().translationKey()), 26, TEXT);
        JeiCategoryUi.drawLine(guiGraphics, font,
                Component.translatable("rngtech.jei.phase_array.coils", recipe.coils(),
                        recipe.needsGlyph() ? GlyphItem.numeral(recipe.glyphTier()) : "-"), 38, TEXT);
        long fresh = PhaseArrayRules.pulseCost(recipe.baseFe(), PhaseArrayRules.PART_STAGE, 12);
        JeiCategoryUi.drawLine(guiGraphics, font,
                Component.translatable("rngtech.jei.phase_array.cost", String.format("%,d", recipe.baseFe()),
                        String.format("%,d", fresh), recipe.refinementPotentialCost()), 50, TEXT);
        JeiCategoryUi.drawLine(guiGraphics, font,
                Component.translatable("rngtech.jei.phase_array.outcomes",
                        Component.translatable("rngtech.phase_array.partial." + recipe.partial().getSerializedName()),
                        Component.translatable("rngtech.phase_array.overhold." + recipe.overhold().getSerializedName())), 62, TEXT);
    }

    @Override
    public net.minecraft.resources.ResourceLocation getRegistryName(RecipeHolder<PhaseArrayRecipe> holder) {
        return holder.id();
    }

    static net.minecraft.resources.ResourceLocation id() {
        return RNGTech.id("phase_array");
    }
}
