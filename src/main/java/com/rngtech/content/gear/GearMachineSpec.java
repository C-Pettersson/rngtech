package com.rngtech.content.gear;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record GearMachineSpec(
        ResourceLocation id,
        List<ItemStack> catalysts,
        List<GearSlotSpec> slots
) {
    public GearMachineSpec {
        catalysts = catalysts.stream().map(ItemStack::copy).toList();
        slots = List.copyOf(slots);
    }

    public Component title() {
        return displayStack().getHoverName();
    }

    public ItemStack displayStack() {
        return catalysts.isEmpty() ? ItemStack.EMPTY : catalysts.get(0).copy();
    }

    public List<ItemStack> catalystCopies() {
        return catalysts.stream().map(ItemStack::copy).toList();
    }

    public Optional<GearSlotSpec> slot(GearSlotArea area, int slotIndex) {
        return slots.stream().filter(slot -> slot.matches(area, slotIndex)).findFirst();
    }

    public boolean matchesMachineStack(ItemStack stack) {
        return !stack.isEmpty() && catalysts.stream().anyMatch(catalyst -> stack.is(catalyst.getItem()));
    }
}
