package com.rngtech.content.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record GearSlotSpec(
        GearSlotArea area,
        int firstSlot,
        int slotCount,
        String labelKey,
        boolean required,
        List<ItemStack> validStacks,
        List<Component> notes
) {
    private static final int TOOLTIP_ITEM_LIMIT = 12;

    public GearSlotSpec {
        validStacks = validStacks.stream().map(ItemStack::copy).toList();
        notes = List.copyOf(notes);
    }

    public boolean matches(GearSlotArea testedArea, int slot) {
        return area == testedArea && slot >= firstSlot && slot < firstSlot + slotCount;
    }

    public Component label() {
        return Component.translatable(labelKey);
    }

    public List<ItemStack> validStackCopies() {
        return validStacks.stream().map(ItemStack::copy).toList();
    }

    public List<Component> tooltipLines(boolean expanded) {
        List<Component> lines = new ArrayList<>();
        lines.add(label().copy().withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable(required
                ? "rngtech.gear_guide.required"
                : "rngtech.gear_guide.optional").withStyle(ChatFormatting.GRAY));
        lines.addAll(notes);
        if (validStacks.isEmpty()) {
            lines.add(Component.translatable("rngtech.gear_guide.no_current_items").withStyle(ChatFormatting.DARK_GRAY));
        } else if (expanded) {
            lines.add(Component.translatable("rngtech.gear_guide.valid_items").withStyle(ChatFormatting.GRAY));
            int shown = Math.min(TOOLTIP_ITEM_LIMIT, validStacks.size());
            for (int index = 0; index < shown; index++) {
                lines.add(validStacks.get(index).getHoverName().copy().withStyle(ChatFormatting.DARK_GRAY));
            }
            if (validStacks.size() > shown) {
                lines.add(Component.translatable("rngtech.gear_guide.more_items", validStacks.size() - shown)
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            lines.add(Component.translatable("rngtech.gear_guide.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }
}
