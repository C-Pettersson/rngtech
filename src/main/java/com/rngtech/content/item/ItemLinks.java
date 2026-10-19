package com.rngtech.content.item;

import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.unique.UniqueItems;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

/** Chat links for items with a rarity: a {@code [name]} in the rarity's colour that shows the item's tooltip on hover. */
public final class ItemLinks {
    private ItemLinks() {
    }

    /** Rolled traits, a pending roll, or a Unique. */
    public static boolean linkable(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.has(ModDataComponents.MACHINE_TRAITS.get())
                || CraftedTraitOutputs.isUnidentified(stack)
                || UniqueItems.isUnique(stack));
    }

    public static MutableComponent link(ItemStack stack) {
        ItemStack shown = stack.copy();
        return Component.literal("[")
                .append(shown.getHoverName())
                .append("]")
                .withStyle(style -> nameStyle(style, shown)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(shown))));
    }

    private static Style nameStyle(Style style, ItemStack stack) {
        MachineTraits traits = stack.get(ModDataComponents.MACHINE_TRAITS.get());
        if (traits != null) {
            return MachineTraitTooltip.rarityStyle(style, traits.rarity());
        }
        return UniqueItems.isUnique(stack) ? style.withColor(UniqueTooltip.UNIQUE_COLOR) : style;
    }
}
