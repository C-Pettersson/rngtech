package com.rngtech.client;

import com.rngtech.content.blockentity.CableBlockEntity;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Tints the glow strip of Universal Cable with its dye colour. */
public final class CableTintClient {
    private static final int UNDYED_TINT = 0x1BFFF4;

    private CableTintClient() {
    }

    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, level, pos, tintIndex) -> level != null
                        && pos != null
                        && level.getBlockEntity(pos) instanceof CableBlockEntity cable
                        ? tint(cable.color())
                        : tint(null),
                ModBlocks.CABLE.get()
        );
    }

    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> tintIndex == 0 ? tint(stack.get(DataComponents.BASE_COLOR)) : -1,
                ModItems.CABLE.get()
        );
    }

    private static int tint(DyeColor color) {
        return FastColor.ARGB32.opaque(color == null ? UNDYED_TINT : color.getTextureDiffuseColor());
    }
}
