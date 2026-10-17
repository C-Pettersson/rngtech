package com.rngtech.content.registry;

import com.rngtech.RNGTech;
import com.rngtech.content.tool.OreReach;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

public final class ModDataMaps {
    /** Ore hardness reach for tools that are not Modular tools, such as other mods' pickaxes and drills. */
    public static final DataMapType<Item, OreReach> ORE_REACH = DataMapType
            .builder(RNGTech.id("ore_reach"), Registries.ITEM, OreReach.CODEC)
            .synced(OreReach.CODEC, false)
            .build();

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(ORE_REACH);
    }

    private ModDataMaps() {
    }
}
