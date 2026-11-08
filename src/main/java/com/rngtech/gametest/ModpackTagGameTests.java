package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModTags;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * RNGTech recipes take alloys, plates, gears, and rods through {@code c:} tags, so other mods' items and the items a
 * unifier such as AlmostUnified swaps in satisfy them.
 */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ModpackTagGameTests {
    private ModpackTagGameTests() {
    }

    /** Uses Mekanism's steel, a dev runtime mod, as the foreign item in {@code c:ingots/steel}. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void foreignSteelSatisfiesSteelRecipes(GameTestHelper helper) {
        List<ItemStack> foreignSteel = foreignItems(commonTag("ingots/steel"));
        check(!foreignSteel.isEmpty(), "Expected a non-RNGTech item in c:ingots/steel; is Mekanism in the dev runtime?");
        ItemStack rngtechSteel = new ItemStack(ModItems.materialItem("steel_ingot").get());

        RecipeHolder<?> steelPlate = helper.getLevel().getRecipeManager()
                .byKey(RNGTech.id("metal_press/steel_plate"))
                .orElseThrow(() -> new AssertionError("Missing rngtech:metal_press/steel_plate"));
        Ingredient steelInput = steelPlate.value().getIngredients().getFirst();
        check(steelInput.test(rngtechSteel), "The Steel Plate press recipe should take RNGTech steel");
        for (ItemStack foreign : foreignSteel) {
            check(steelInput.test(foreign), "The Steel Plate press recipe should take " + foreign.getItem());
        }
        helper.succeed();
    }

    /** Every RNGTech plate is in {@code c:plates}, which the Metal Press anneals and crafted parts count. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void rngtechPlatesAreAnnealablePlates(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(rngtechTag("materials/form/plate"))) {
            if (!new ItemStack(holder).is(ModTags.Items.PLATES)) {
                missing.add(BuiltInRegistries.ITEM.getKey(holder.value()).toString());
            }
        }
        check(missing.isEmpty(), "RNGTech plates missing from c:plates: " + missing);
        helper.succeed();
    }

    private static TagKey<Item> commonTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private static TagKey<Item> rngtechTag(String path) {
        return TagKey.create(Registries.ITEM, RNGTech.id(path));
    }

    private static List<ItemStack> foreignItems(TagKey<Item> tag) {
        List<ItemStack> foreign = new ArrayList<>();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            if (!BuiltInRegistries.ITEM.getKey(holder.value()).getNamespace().equals(RNGTech.MOD_ID)) {
                foreign.add(new ItemStack(holder));
            }
        }
        return foreign;
    }
}
