package com.rngtech.compat.jei;

import com.rngtech.RNGTech;
import com.rngtech.content.material.OreCatalog;
import com.rngtech.content.material.OreDefinition;
import com.rngtech.content.material.OreHost;
import com.rngtech.content.material.OreWorldgenEnablement;
import com.rngtech.content.registry.ModItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record OreWorldgenJeiRecipe(ResourceLocation id, OreDefinition ore, List<Variant> variants, ItemStack rawDrop) {
    public OreWorldgenJeiRecipe {
        variants = variants.stream().map(Variant::copy).toList();
        rawDrop = rawDrop.copy();
    }

    static List<OreWorldgenJeiRecipe> recipes() {
        List<OreWorldgenJeiRecipe> recipes = new ArrayList<>();
        for (OreDefinition ore : OreCatalog.ores()) {
            if (ore.veinsPerChunk() <= 0) {
                continue;
            }
            recipes.add(new OreWorldgenJeiRecipe(
                    RNGTech.id("ore_worldgen/" + ore.materialId()),
                    ore,
                    ore.generationHosts().stream()
                            .map(host -> new Variant(host, new ItemStack(ModItems.materialItem(ore.blockId(host)).get())))
                            .toList(),
                    new ItemStack(ModItems.materialItem("raw_" + ore.materialId()).get())
            ));
        }
        return List.copyOf(recipes);
    }

    boolean configEnabled() {
        return OreWorldgenEnablement.isEnabled(ore.materialId());
    }

    String biomeModifierId() {
        return RNGTech.MOD_ID + ":ore_" + ore.materialId();
    }

    public record Variant(OreHost host, ItemStack stack) {
        private Variant copy() {
            return new Variant(host, stack.copy());
        }
    }
}
