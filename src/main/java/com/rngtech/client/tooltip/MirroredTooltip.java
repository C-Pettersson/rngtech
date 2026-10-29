package com.rngtech.client.tooltip;

import com.rngtech.rpg.refinement.RefinementTargets;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Marks a Mirror Catalyst's copy under its name. */
public final class MirroredTooltip {
    private static final TextColor MIRRORED = TextColor.fromRgb(0xC9DCE6);

    private MirroredTooltip() {
    }

    public static void onTooltip(ItemTooltipEvent event) {
        if (!RefinementTargets.isMirrored(event.getItemStack())) {
            return;
        }
        int at = Math.min(1, event.getToolTip().size());
        event.getToolTip().add(at, Component.translatable("rngtech.tooltip.mirrored").withStyle(style -> style.withColor(MIRRORED)));
    }
}
