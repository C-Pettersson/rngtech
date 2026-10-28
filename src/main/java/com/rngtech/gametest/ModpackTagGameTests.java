package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.registry.ModItems;

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

/** Other mods' items in a {@code c:} tag do not satisfy RNGTech recipes that ask for the matching RNGTech-only tag. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ModpackTagGameTests {
    private static final List<String> RNGTECH_TAG_FORMS = List.of("ingots", "nuggets", "plates", "gears", "rods");

    private ModpackTagGameTests() {
    }

    /** Uses Mekanism's steel, a dev runtime mod, as the foreign item in {@code c:ingots/steel}. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void foreignSteelDoesNotSatisfySteelRecipes(GameTestHelper helper) {
        List<ItemStack> foreignSteel = foreignItems(commonTag("ingots/steel"));
        check(!foreignSteel.isEmpty(), "Expected a non-RNGTech item in c:ingots/steel; is Mekanism in the dev runtime?");
        ItemStack rngtechSteel = new ItemStack(ModItems.materialItem("steel_ingot").get());
        check(rngtechSteel.is(commonTag("ingots/steel")), "RNGTech steel should stay in c:ingots/steel for other mods");

        RecipeHolder<?> steelPlate = helper.getLevel().getRecipeManager()
                .byKey(RNGTech.id("metal_press/steel_plate"))
                .orElseThrow(() -> new AssertionError("Missing rngtech:metal_press/steel_plate"));
        Ingredient steelInput = steelPlate.value().getIngredients().getFirst();
        check(steelInput.test(rngtechSteel), "The Steel Plate press recipe should take RNGTech steel");
        for (ItemStack foreign : foreignSteel) {
            check(!steelInput.test(foreign), "The Steel Plate press recipe should reject " + foreign.getItem());
        }
        helper.succeed();
    }

    /** No RNGTech recipe takes another mod's alloy, plate, gear, or rod through the matching {@code c:} tag. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void rngtechRecipesRejectForeignTaggedItems(GameTestHelper helper) {
        List<ItemStack> probes = new ArrayList<>();
        BuiltInRegistries.ITEM.getTags()
                .map(pair -> pair.getFirst().location())
                .filter(tag -> tag.getNamespace().equals(RNGTech.MOD_ID) && RNGTECH_TAG_FORMS.contains(tag.getPath().split("/")[0]))
                .forEach(tag -> probes.addAll(foreignItems(commonTag(tag.getPath()))));
        check(!probes.isEmpty(), "Expected non-RNGTech items in the c: twins of RNGTech-only tags; is Mekanism in the dev runtime?");

        List<String> leaks = new ArrayList<>();
        for (RecipeHolder<?> recipe : helper.getLevel().getRecipeManager().getRecipes()) {
            if (!recipe.id().getNamespace().equals(RNGTech.MOD_ID)) {
                continue;
            }
            for (Ingredient ingredient : recipe.value().getIngredients()) {
                for (ItemStack probe : probes) {
                    if (ingredient.test(probe)) {
                        leaks.add(recipe.id() + " takes " + BuiltInRegistries.ITEM.getKey(probe.getItem()));
                    }
                }
            }
        }
        check(leaks.isEmpty(), "RNGTech recipes take other mods' items: " + leaks.subList(0, Math.min(5, leaks.size())));
        helper.succeed();
    }

    private static TagKey<Item> commonTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
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
