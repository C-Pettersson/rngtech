package com.rngtech.client.tooltip;

import com.rngtech.content.item.CraftedTraitOutputs;
import com.rngtech.content.registry.ModTags;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Marks items pressed under Hydrogen and says what annealing does for each kind. */
public final class AnnealedTooltip {
    private AnnealedTooltip() {
    }

    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!CraftedTraitOutputs.isAnnealed(stack)) {
            return;
        }
        String detail = stack.is(ModTags.Items.PLATES) ? "plate"
                : stack.is(ModTags.Items.ANNEALED_CALIBRATION_INPUTS) ? "circuit"
                : "gear";
        int at = Math.min(1, event.getToolTip().size());
        event.getToolTip().add(at, Component.translatable("rngtech.tooltip.annealed." + detail).withStyle(ChatFormatting.AQUA));
    }
}
