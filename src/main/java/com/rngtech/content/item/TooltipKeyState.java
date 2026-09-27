package com.rngtech.content.item;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public final class TooltipKeyState {
    public static boolean hasShiftDown() {
        return FMLEnvironment.dist == Dist.CLIENT && ClientKeys.hasShiftDown();
    }

    public static boolean hasAltDown() {
        return FMLEnvironment.dist == Dist.CLIENT && ClientKeys.hasAltDown();
    }

    public static boolean hasDetailKeyDown() {
        return hasShiftDown() || hasAltDown();
    }

    private static final class ClientKeys {
        private static boolean hasShiftDown() {
            return Screen.hasShiftDown();
        }

        private static boolean hasAltDown() {
            return Screen.hasAltDown();
        }
    }

    private TooltipKeyState() {
    }
}
