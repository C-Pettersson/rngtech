package com.rngtech.content.blockentity;

import com.rngtech.content.blockentity.AffixSorterBlockEntity.FaceRoute;
import com.rngtech.content.blockentity.AffixSorterBlockEntity.RelativeFace;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * What an Affix Sorter shows in the world: the route of each face, which colours its port collar when something is
 * attached there, and the item in the input slot, which lies on the reader.
 *
 * @param routes    each {@link RelativeFace}'s {@link FaceRoute} ordinal, two bits per face in face order
 * @param connected one bit per {@link RelativeFace}, set when a cable connector or an item handler meets that face
 * @param input     the item waiting to be read, or air
 */
public record AffixSorterVisualState(int routes, int connected, Item input) {
    private static final int BITS = 2;
    public static final AffixSorterVisualState IDLE = new AffixSorterVisualState(0, 0, Items.AIR);

    public static int pack(FaceRoute[] faces) {
        int routes = 0;
        for (int face = 0; face < faces.length; face++) {
            routes |= faces[face].ordinal() << (face * BITS);
        }
        return routes;
    }

    public FaceRoute route(RelativeFace face) {
        int ordinal = (routes >> (face.ordinal() * BITS)) & ((1 << BITS) - 1);
        return ordinal < FaceRoute.values().length ? FaceRoute.values()[ordinal] : FaceRoute.NONE;
    }

    public boolean connected(RelativeFace face) {
        return (connected >> face.ordinal() & 1) != 0;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Routes", routes);
        tag.putInt("Connected", connected);
        tag.putString("Input", BuiltInRegistries.ITEM.getKey(input).toString());
        return tag;
    }

    public static AffixSorterVisualState load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return IDLE;
        }
        ResourceLocation input = ResourceLocation.tryParse(tag.getString("Input"));
        return new AffixSorterVisualState(tag.getInt("Routes"), tag.getInt("Connected"),
                input == null ? Items.AIR : BuiltInRegistries.ITEM.get(input));
    }
}
