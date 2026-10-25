package com.rngtech.content.item;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/** The client world's synced recipes for tooltips, which get no level of their own; null on a server or with no world. */
public final class ClientRecipes {
    public static RecipeManager manager() {
        return FMLEnvironment.dist == Dist.CLIENT ? Client.manager() : null;
    }

    private static final class Client {
        private static RecipeManager manager() {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft.level == null ? null : minecraft.level.getRecipeManager();
        }
    }

    private ClientRecipes() {
    }
}
