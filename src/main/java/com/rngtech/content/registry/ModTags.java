package com.rngtech.content.registry;

import com.rngtech.RNGTech;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.Fluid;

public final class ModTags {
    public static final class Items {
        public static final TagKey<Item> SOLID_FUEL_ITEM_FUELS = solidFuel("item_fuels");
        public static final TagKey<Item> SOLID_FUEL_BLOCK_FUELS = solidFuel("block_fuels");
        public static final TagKey<Item> SOLID_FUEL_TIER_1 = solidFuel("tier_1");
        public static final TagKey<Item> SOLID_FUEL_TIER_2 = solidFuel("tier_2");
        public static final TagKey<Item> SOLID_FUEL_TIER_3 = solidFuel("tier_3");
        public static final TagKey<Item> SOLID_FUEL_TIER_4 = solidFuel("tier_4");
        public static final TagKey<Item> ALLOY_BLEND_SMELTABLES =
                TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "alloy_blend_smeltables"));
        public static final TagKey<Item> BIO_GENERATOR_POTATO_FUELS = bioGenerator("potato_fuels");
        public static final TagKey<Item> BIO_GENERATOR_CARROT_FUELS = bioGenerator("carrot_fuels");
        public static final TagKey<Item> BIO_GENERATOR_BREAD_FUELS = bioGenerator("bread_fuels");
        public static final TagKey<Item> BIO_GENERATOR_SAPLING_BIOMASS = bioGenerator("sapling_biomass");
        public static final TagKey<Item> BIO_GENERATOR_SEED_BIOMASS = bioGenerator("seed_biomass");
        public static final TagKey<Item> BIO_GENERATOR_PLANT_BIOMASS = bioGenerator("plant_biomass");
        public static final TagKey<Item> BIO_GENERATOR_ORGANIC_REAGENTS = bioGenerator("organic_reagents");
        public static final TagKey<Item> BIO_GENERATOR_COMPOSTED_BIOMASS = bioGenerator("composted_biomass");
        public static final TagKey<Item> BIO_GENERATOR_RICH_BIOMASS = bioGenerator("rich_biomass");
        public static final TagKey<Item> BIO_GENERATOR_ALGAE_BIOMASS = bioGenerator("algae_biomass");
        public static final TagKey<Item> BIO_GENERATOR_DENSE_ALGAE_BIOMASS = bioGenerator("dense_algae_biomass");
        public static final TagKey<Item> BIO_GENERATOR_RICH_BIOMASS_CATALYSTS = bioGenerator("rich_biomass_catalysts");
        public static final TagKey<Item> BIO_GENERATOR_EXCLUDED_FOODS = bioGenerator("excluded_foods");
        public static final TagKey<Item> WOODEN_COMPOSTER_INPUTS = woodenComposter("inputs");
        public static final TagKey<Item> WOODEN_COMPOSTER_PREPARED_INPUTS = woodenComposter("prepared_inputs");
        /** Unique items: never refined, rerolled, or recycled. Packs may add their own find-only items. */
        public static final TagKey<Item> UNIQUES =
                TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "uniques"));
        public static final TagKey<Item> MALFORMED_INGOTS =
                TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "malformed_ingots"));
        public static final TagKey<Item> CRUSHED_MATERIALS =
                TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "materials/form/crushed"));
        /** Ore, raw, and crushed inputs whose smelts feed the Bloomer's ledger. */
        public static final TagKey<Item> BLOOM_LEDGER_INPUTS =
                TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "bloom_ledger_inputs"));
        public static final TagKey<Item> PLATE_MOLDS =
                TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "plate_molds"));
        public static final TagKey<Item> GEAR_MOLDS =
                TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "gear_molds"));
        public static final TagKey<Item> CASING_MOLDS =
                TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "casing_molds"));
        public static final TagKey<Item> CIRCUIT_MOLDS =
                TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "circuit_molds"));
        /** Saplings Ancient Grove plants as 2x2 giant trees. */
        public static final TagKey<Item> GIANT_SAPLINGS =
                TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "giant_saplings"));
        /** Crop items a Field Hand Forestry Companion plants on farmland. */
        public static final TagKey<Item> CART_CROPS =
                TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "cart_crops"));
        public static final TagKey<Item> METAL_PRESS_MOLDS =
                TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "metal_press_molds"));
        public static final TagKey<Item> MANUAL_RECYCLER_INPUTS =
                TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "manual_recycler_inputs"));
        public static final TagKey<Item> COMMON_COAL_DUSTS =
                TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "dusts/coal"));

        private static TagKey<Item> solidFuel(String path) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "solid_fuel/" + path));
        }

        private static TagKey<Item> bioGenerator(String path) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "bio_generator/" + path));
        }

        private static TagKey<Item> woodenComposter(String path) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "wooden_composter/" + path));
        }

        private Items() {
        }
    }

    public static final class Fluids {
        public static final TagKey<Fluid> LUBRICANTS =
                TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "lubricants"));
        public static final TagKey<Fluid> ELECTROLYTES =
                TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "electrolytes"));
        public static final TagKey<Fluid> MELTER_OUTPUTS =
                TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "melter_outputs"));
        public static final TagKey<Fluid> COMMON_LUBRICANTS =
                TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "lubricants"));
        public static final TagKey<Fluid> GASES =
                TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "gases"));
        public static final TagKey<Fluid> CHEMICAL_GASES =
                TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "chemical_gases"));
        public static final TagKey<Fluid> COMBUSTIBLE_GASES =
                TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "combustible_gases"));
        private Fluids() {
        }
    }

    public static final class Biomes {
        public static final TagKey<Biome> DEHUMIDIFIER_WET = dehumidifier("wet");
        public static final TagKey<Biome> DEHUMIDIFIER_NORMAL = dehumidifier("normal");
        public static final TagKey<Biome> DEHUMIDIFIER_DRY = dehumidifier("dry");
        public static final TagKey<Biome> DEHUMIDIFIER_BLOCKED = dehumidifier("blocked");

        private static TagKey<Biome> dehumidifier(String path) {
            return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "dehumidifier_humidity/" + path));
        }

        private Biomes() {
        }
    }

    private ModTags() {
    }
}
