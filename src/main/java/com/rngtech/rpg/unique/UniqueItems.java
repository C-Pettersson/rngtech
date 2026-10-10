package com.rngtech.rpg.unique;

import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.UniquePartItem;
import com.rngtech.content.registry.ModTags;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The one place that decides whether an item is a Unique and which catalog entry defines it. */
public final class UniqueItems {
    private UniqueItems() {
    }

    /** Catalog Uniques, plus anything a pack adds to the {@code rngtech:uniques} tag. */
    public static boolean isUnique(ItemStack stack) {
        return !stack.isEmpty() && (definition(stack.getItem()) != null || stack.is(ModTags.Items.UNIQUES));
    }

    public static UniqueDefinition definition(ItemStack stack) {
        return stack.isEmpty() ? null : definition(stack.getItem());
    }

    public static UniqueDefinition definition(Item item) {
        if (item instanceof UniquePartItem part) {
            return part.definition();
        }
        if (item instanceof BatteryCellItem cell && cell.material().unique()) {
            return UniqueCatalog.get(cell.material().itemId());
        }
        return null;
    }

    /** The Unique Battery Cell material registered under item id {@code id}, or null. */
    public static BatteryCellMaterial batteryCellMaterial(String id) {
        for (BatteryCellMaterial material : BatteryCellMaterial.values()) {
            if (material.unique() && material.itemId().equals(id)) {
                return material;
            }
        }
        return null;
    }

    /** The stage a Unique counts as for Gear stage gates, or -1 for anything else. */
    public static int slotStage(ItemStack stack) {
        UniqueDefinition definition = definition(stack);
        return definition == null ? -1 : definition.slotStage();
    }
}
