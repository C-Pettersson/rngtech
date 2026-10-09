package com.rngtech.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

public final class FluidBarRenderer {
    private static final int TILE_SIZE = 16;

    private FluidBarRenderer() {
    }

    public static void fill(GuiGraphics guiGraphics, Fluid fluid, int x1, int y1, int x2, int y2, int fallbackColor) {
        if (x2 <= x1 || y2 <= y1) {
            return;
        }
        FluidStack stack = new FluidStack(fluid, 1);
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation stillTexture = fluid == Fluids.EMPTY ? null : extensions.getStillTexture(stack);
        if (stillTexture == null) {
            guiGraphics.fill(x1, y1, x2, y2, fallbackColor);
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);
        int tint = extensions.getTintColor(stack);
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
        float alpha = ((tint >>> 24) & 0xFF) / 255.0F;
        guiGraphics.enableScissor(x1, y1, x2, y2);
        for (int tileY = y2 - TILE_SIZE; tileY > y1 - TILE_SIZE; tileY -= TILE_SIZE) {
            for (int tileX = x1; tileX < x2; tileX += TILE_SIZE) {
                guiGraphics.blit(tileX, tileY, 0, TILE_SIZE, TILE_SIZE, sprite, red, green, blue, alpha);
            }
        }
        guiGraphics.disableScissor();
    }
}
