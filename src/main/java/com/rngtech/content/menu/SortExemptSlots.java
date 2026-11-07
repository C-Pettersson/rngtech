package com.rngtech.content.menu;

import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * Slot classes that inventory sort mods must leave alone. Sort mods match exact class names, so every slot class over
 * a machine, filter, or Gear inventory belongs here. Player inventory slots stay sortable inside machine screens.
 * {@code tools/moddex/check-sort-exempt-slots.mjs} fails the build when a new slot class is missing.
 */
public final class SortExemptSlots {
    public static final List<Class<? extends Slot>> TYPES = List.of(
            MachineSlot.class,
            DriveBaySlot.class,
            JewelSocketSlot.class,
            AdvancedItemFilterMenu.GhostSampleSlot.class,
            MinersCompanionMenu.GhostFilterSlot.class,
            UniversalConnectorMenu.GhostFilterSlot.class
    );

    private SortExemptSlots() {
    }
}
