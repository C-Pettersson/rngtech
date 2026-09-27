package com.rngtech.compat.jei;

import com.rngtech.RNGTech;
import com.rngtech.content.registry.ModTags;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Predicate;

public record BioGeneratorJeiFuel(
        ResourceLocation id,
        Component family,
        int baseEnergy,
        int baseBurnTicks,
        List<ItemStack> fuels
) {
    private static final int BASE_GENERATION_RATE = 8;

    public BioGeneratorJeiFuel {
        fuels = fuels.stream().map(ItemStack::copy).toList();
    }

    static List<BioGeneratorJeiFuel> recipes() {
        return List.of(
                        tagRecipe(
                                "potatoes",
                                "rngtech.jei.bio_generator.fuel.potatoes",
                                120,
                                ModTags.Items.BIO_GENERATOR_POTATO_FUELS,
                                stack -> !stack.is(Items.POISONOUS_POTATO)
                        ),
                        itemRecipe(
                                "poisonous_potato",
                                "rngtech.jei.bio_generator.fuel.poisonous_potato",
                                200,
                                Items.POISONOUS_POTATO
                        ),
                        tagRecipe(
                                "carrots",
                                "rngtech.jei.bio_generator.fuel.carrots",
                                120,
                                ModTags.Items.BIO_GENERATOR_CARROT_FUELS
                        ),
                        tagRecipe(
                                "bread",
                                "rngtech.jei.bio_generator.fuel.bread",
                                240,
                                ModTags.Items.BIO_GENERATOR_BREAD_FUELS
                        ),
                        tagRecipe(
                                "saplings",
                                "rngtech.jei.bio_generator.fuel.saplings",
                                80,
                                ModTags.Items.BIO_GENERATOR_SAPLING_BIOMASS
                        ),
                        tagRecipe(
                                "seeds",
                                "rngtech.jei.bio_generator.fuel.seeds",
                                40,
                                ModTags.Items.BIO_GENERATOR_SEED_BIOMASS
                        ),
                        tagRecipe(
                                "plant_biomass",
                                "rngtech.jei.bio_generator.fuel.plant_biomass",
                                80,
                                ModTags.Items.BIO_GENERATOR_PLANT_BIOMASS,
                                stack -> !stack.is(ModTags.Items.BIO_GENERATOR_SAPLING_BIOMASS)
                                        && !stack.is(ModTags.Items.BIO_GENERATOR_SEED_BIOMASS)
                        ),
                        tagRecipe(
                                "organic_reagents",
                                "rngtech.jei.bio_generator.fuel.organic_reagents",
                                546,
                                ModTags.Items.BIO_GENERATOR_ORGANIC_REAGENTS
                        ),
                        tagRecipe(
                                "composted_biomass",
                                "rngtech.jei.bio_generator.fuel.composted_biomass",
                                1600,
                                ModTags.Items.BIO_GENERATOR_COMPOSTED_BIOMASS
                        ),
                        tagRecipe(
                                "algae_biomass",
                                "rngtech.jei.bio_generator.fuel.algae_biomass",
                                400,
                                ModTags.Items.BIO_GENERATOR_ALGAE_BIOMASS
                        ),
                        tagRecipe(
                                "dense_algae_biomass",
                                "rngtech.jei.bio_generator.fuel.dense_algae_biomass",
                                2400,
                                ModTags.Items.BIO_GENERATOR_DENSE_ALGAE_BIOMASS
                        ),
                        tagRecipe(
                                "rich_biomass",
                                "rngtech.jei.bio_generator.fuel.rich_biomass",
                                3200,
                                ModTags.Items.BIO_GENERATOR_RICH_BIOMASS
                        )
                )
                .stream()
                .filter(recipe -> !recipe.fuels().isEmpty())
                .toList();
    }

    private static BioGeneratorJeiFuel tagRecipe(String path, String familyKey, int baseEnergy, TagKey<Item> tag) {
        return tagRecipe(path, familyKey, baseEnergy, tag, stack -> true);
    }

    private static BioGeneratorJeiFuel tagRecipe(
            String path,
            String familyKey,
            int baseEnergy,
            TagKey<Item> tag,
            Predicate<ItemStack> extraFilter
    ) {
        List<ItemStack> fuels = BuiltInRegistries.ITEM.stream()
                .map(ItemStack::new)
                .filter(stack -> stack.is(tag))
                .filter(stack -> !stack.is(ModTags.Items.BIO_GENERATOR_EXCLUDED_FOODS))
                .filter(extraFilter)
                .toList();
        return recipe(path, familyKey, baseEnergy, fuels);
    }

    private static BioGeneratorJeiFuel itemRecipe(String path, String familyKey, int baseEnergy, Item item) {
        ItemStack fuel = new ItemStack(item);
        List<ItemStack> fuels = fuel.is(ModTags.Items.BIO_GENERATOR_EXCLUDED_FOODS) ? List.of() : List.of(fuel);
        return recipe(path, familyKey, baseEnergy, fuels);
    }

    private static BioGeneratorJeiFuel recipe(String path, String familyKey, int baseEnergy, List<ItemStack> fuels) {
        int baseBurnTicks = Math.max(1, (int) Math.ceil(baseEnergy / (double) BASE_GENERATION_RATE));
        return new BioGeneratorJeiFuel(
                RNGTech.id("bio_generator/" + path),
                Component.translatable(familyKey),
                baseEnergy,
                baseBurnTicks,
                fuels
        );
    }
}
