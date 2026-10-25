package com.rngtech.content.item;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/** The client world, for tooltips, which get no level of their own. On a server or with no world it reads as absent. */
public final class ClientWorld {
    /** The client world's synced recipes, or null. */
    public static RecipeManager recipes() {
        return FMLEnvironment.dist == Dist.CLIENT ? Client.manager() : null;
    }

    /** The client world's game time, or -1. */
    public static long gameTime() {
        return FMLEnvironment.dist == Dist.CLIENT ? Client.gameTime() : -1;
    }

    private static final class Client {
        private static RecipeManager manager() {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft.level == null ? null : minecraft.level.getRecipeManager();
        }

        private static long gameTime() {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft.level == null ? -1 : minecraft.level.getGameTime();
        }
    }

    private ClientWorld() {
    }
}
