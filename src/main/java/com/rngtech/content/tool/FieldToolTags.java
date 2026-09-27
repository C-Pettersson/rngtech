package com.rngtech.content.tool;

import com.rngtech.RNGTech;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class FieldToolTags {
    public static final TagKey<Block> ORE_BURST_TARGETS = TagKey.create(Registries.BLOCK, RNGTech.id("ore_burst_targets"));
    public static final TagKey<Item> TOOL_HEADS = item("tool_heads");
    public static final TagKey<Item> TOOL_RODS = item("tool_rods");
    public static final TagKey<Item> MODULAR_FIELD_TOOLS = item("modular_field_tools");

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, RNGTech.id(path));
    }

    private FieldToolTags() {
    }
}
