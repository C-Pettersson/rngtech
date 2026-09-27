package com.rngtech.compat.jei;

import com.rngtech.content.material.OreDefinition;
import com.rngtech.content.material.OreHost;
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

public final class OreWorldgenRecipeCategory implements IRecipeCategory<OreWorldgenJeiRecipe> {
    private static final int HEIGHT = 112;
    private static final int ORE_X = 4;
    private static final int ORE_STEP = 22;
    private static final int SLOT_Y = 20;
    private static final int RAW_X = 26;
    private static final int RAW_Y = 55;
    private static final int DETAIL_X = 72;
    private static final int DETAIL_WIDTH = JeiCategoryUi.WIDTH - DETAIL_X - 4;

    private final IDrawable background;
    private final IDrawable icon;

    OreWorldgenRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.materialItem("tin_ore").get()));
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<OreWorldgenJeiRecipe> getRecipeType() {
        return JeiRecipeTypes.ORE_WORLDGEN;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.ore_worldgen");
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
    public void setRecipe(IRecipeLayoutBuilder builder, OreWorldgenJeiRecipe recipe, IFocusGroup focuses) {
        for (int index = 0; index < recipe.variants().size(); index++) {
            OreWorldgenJeiRecipe.Variant variant = recipe.variants().get(index);
            JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, ORE_X + index * ORE_STEP, SLOT_Y)
                    .addItemStack(variant.stack())
                    .addRichTooltipCallback((view, tooltip) -> addOreTooltip(tooltip, recipe, variant.host()));
        }
        JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, RAW_X, RAW_Y)
                .addItemStack(recipe.rawDrop())
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("rngtech.jei.ore_worldgen.raw_drop"));
                    tooltip.add(Component.translatable("rngtech.jei.ore_worldgen.fortune"));
                });
    }

    private static void addOreTooltip(
            ITooltipBuilder tooltip,
            OreWorldgenJeiRecipe recipe,
            OreHost host
    ) {
        OreDefinition ore = recipe.ore();
        tooltip.add(Component.translatable("rngtech.jei.ore_worldgen.slot"));
        tooltip.add(Component.translatable("rngtech.jei.ore_worldgen.host." + host.id()));
        tooltip.add(Component.translatable("rngtech.tooltip.ore.material", ore.materialDisplayName()));
        tooltip.add(Component.translatable("rngtech.tooltip.ore.hardness", ore.hardnessLevel()));
        tooltip.add(Component.translatable("rngtech.jei.ore_worldgen.override"));
    }

    @Override
    public void draw(
            OreWorldgenJeiRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        OreDefinition ore = recipe.ore();
        Font font = Minecraft.getInstance().font;
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.jei.ore_worldgen.blocks", 37, 9);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.jei.ore_worldgen.drops", 37, 44);
        JeiCategoryUi.drawClipped(
                guiGraphics,
                font,
                Component.literal(ore.materialDisplayName()),
                DETAIL_X,
                18,
                DETAIL_WIDTH,
                JeiCategoryUi.TEXT_COLOR
        );
        JeiCategoryUi.drawClipped(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.ore_worldgen.y_range", ore.minY(), ore.maxY()),
                DETAIL_X,
                29,
                DETAIL_WIDTH,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawClipped(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.ore_worldgen.veins", ore.veinsPerChunk(), ore.veinSize()),
                DETAIL_X,
                40,
                DETAIL_WIDTH,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawClipped(
                guiGraphics,
                font,
                Component.translatable(
                        "rngtech.jei.ore_worldgen.hardness_stage",
                        ore.hardnessLevel(),
                        ore.materialStage()
                ),
                DETAIL_X,
                56,
                DETAIL_WIDTH,
                JeiCategoryUi.TEXT_COLOR
        );
        JeiCategoryUi.drawClipped(
                guiGraphics,
                font,
                Component.translatable(
                        "rngtech.jei.ore_worldgen.air_discard",
                        JeiCategoryUi.percent(ore.discardChanceOnAirExposure())
                ),
                DETAIL_X,
                66,
                DETAIL_WIDTH,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable(ore.generatedByDefault()
                        ? "rngtech.jei.ore_worldgen.default_enabled"
                        : "rngtech.jei.ore_worldgen.default_disabled"),
                78,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawClipped(
                guiGraphics,
                font,
                Component.translatable(
                        recipe.configEnabled()
                                ? "rngtech.jei.ore_worldgen.config_enabled"
                                : "rngtech.jei.ore_worldgen.config_disabled",
                        recipe.biomeModifierId()
                ),
                4,
                90,
                JeiCategoryUi.WIDTH - 8,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
    }
}
