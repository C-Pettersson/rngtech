package com.rngtech.content.blockentity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * What an Affix Forge shows in the world: the target on the anvil plate, the catalyst in the dish, and the focus lens
 * or crystal. The installed upgrades are blockstate parts and are not repeated here.
 *
 * @param reforges a counter that steps with each successful Reforge, so the client can flash the forge
 */
public record AffixForgeVisualState(Item target, Item catalyst, Item focus, int reforges) {
    public static final AffixForgeVisualState IDLE = new AffixForgeVisualState(Items.AIR, Items.AIR, Items.AIR, 0);

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Target", BuiltInRegistries.ITEM.getKey(target).toString());
        tag.putString("Catalyst", BuiltInRegistries.ITEM.getKey(catalyst).toString());
        tag.putString("Focus", BuiltInRegistries.ITEM.getKey(focus).toString());
        tag.putInt("Reforges", reforges);
        return tag;
    }

    public static AffixForgeVisualState load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return IDLE;
        }
        return new AffixForgeVisualState(
                item(tag.getString("Target")),
                item(tag.getString("Catalyst")),
                item(tag.getString("Focus")),
                tag.getInt("Reforges")
        );
    }

    private static Item item(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location == null ? Items.AIR : BuiltInRegistries.ITEM.get(location);
    }

    /** Client-side easing: a successful Reforge flares the ring, the loupe, and the matrix, and turns the target once. */
    public static final class Animation {
        public double lastTime = Double.NaN;
        public float flash;
        public float turn;
        public int reforges = -1;
    }
}
